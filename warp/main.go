// warp runs a userspace Cloudflare WARP (WireGuard) tunnel and exposes it as a local
// HTTP proxy, so only the app's traffic goes through it and no VPN permission is needed.
//
// Usage: warp <stateDir>
// Prints "READY <port>" once traffic flows through WARP, or "FAIL <reason>" and exits.
// Exits when stdin closes, i.e. when the app process dies.
package main

import (
	"bytes"
	"context"
	"crypto/rand"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net"
	"net/http"
	"net/http/httputil"
	"net/netip"
	"os"
	"path/filepath"
	"strings"
	"time"

	"golang.org/x/crypto/curve25519"
	"golang.zx2c4.com/wireguard/conn"
	"golang.zx2c4.com/wireguard/device"
	"golang.zx2c4.com/wireguard/tun/netstack"
)

type account struct {
	PrivateKey string `json:"private_key"`
	PeerKey    string `json:"peer_key"`
	V4         string `json:"v4"`
	V6         string `json:"v6"`
	Endpoint   string `json:"endpoint"`
}

// WARP answers on many UDP ports; ISPs that block 2408 usually miss the others.
var endpoints = []string{
	"162.159.192.1:2408", "162.159.192.1:500", "162.159.192.1:1701", "162.159.192.1:4500",
	"162.159.195.1:2408", "162.159.193.1:854", "162.159.192.1:894", "162.159.195.1:908",
}

func main() {
	if len(os.Args) < 2 {
		fail("usage: warp <stateDir>")
	}
	go func() { io.Copy(io.Discard, os.Stdin); os.Exit(0) }()
	// Static Linux builds don't know Android's CA store (moved into the Conscrypt APEX on Android 14)
	if os.Getenv("SSL_CERT_DIR") == "" {
		os.Setenv("SSL_CERT_DIR", "/apex/com.android.conscrypt/cacerts:/system/etc/security/cacerts")
	}

	// Android has no /etc/resolv.conf; only the WARP API lookup uses this resolver.
	net.DefaultResolver = &net.Resolver{PreferGo: true, Dial: func(ctx context.Context, _, _ string) (net.Conn, error) {
		var d net.Dialer
		return d.DialContext(ctx, "udp", "1.1.1.1:53")
	}}

	path := filepath.Join(os.Args[1], "warp.json")
	acc, err := load(path)
	if err != nil {
		if acc, err = register(); err != nil {
			fail("register: " + err.Error())
		}
		save(path, acc)
	}
	log("account ok, starting tunnel")
	tnet, ep, err := up(acc)
	if err != nil {
		fail(err.Error())
	}
	if ep != acc.Endpoint {
		acc.Endpoint = ep
		save(path, acc)
	}

	ln, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		fail(err.Error())
	}
	fmt.Printf("READY %d\n", ln.Addr().(*net.TCPAddr).Port)
	tr := &http.Transport{DialContext: tnet.DialContext, MaxIdleConnsPerHost: 8, IdleConnTimeout: 60 * time.Second}
	http.Serve(ln, &proxy{tnet: tnet, rp: &httputil.ReverseProxy{Director: func(*http.Request) {}, Transport: tr, FlushInterval: -1}})
}

func fail(msg string) {
	fmt.Println("FAIL " + strings.ReplaceAll(msg, "\n", " "))
	os.Exit(1)
}

func load(path string) (*account, error) {
	b, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}
	var a account
	if err := json.Unmarshal(b, &a); err != nil || a.PrivateKey == "" {
		return nil, errors.New("bad state")
	}
	return &a, nil
}

func save(path string, a *account) {
	b, _ := json.Marshal(a)
	os.WriteFile(path, b, 0o600)
}

// register creates a free WARP device the same way the 1.1.1.1 app does.
func register() (*account, error) {
	priv := make([]byte, 32)
	rand.Read(priv)
	priv[0] &= 248
	priv[31] = priv[31]&127 | 64
	pub, err := curve25519.X25519(priv, curve25519.Basepoint)
	if err != nil {
		return nil, err
	}
	body, _ := json.Marshal(map[string]any{
		"key": base64.StdEncoding.EncodeToString(pub), "install_id": "", "fcm_token": "",
		"tos": time.Now().UTC().Format("2006-01-02T15:04:05.000Z"), "model": "PC", "type": "Android", "locale": "en_US",
	})
	req, _ := http.NewRequest("POST", "https://api.cloudflareclient.com/v0a2158/reg", bytes.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("User-Agent", "okhttp/3.12.1")
	req.Header.Set("CF-Client-Version", "a-6.11-2158")
	resp, err := (&http.Client{Timeout: 20 * time.Second}).Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	raw, _ := io.ReadAll(resp.Body)
	var r struct {
		Config struct {
			Peers []struct {
				PublicKey string `json:"public_key"`
			} `json:"peers"`
			Interface struct {
				Addresses struct{ V4, V6 string } `json:"addresses"`
			} `json:"interface"`
		} `json:"config"`
	}
	if err := json.Unmarshal(raw, &r); err != nil || len(r.Config.Peers) == 0 {
		return nil, fmt.Errorf("HTTP %d: %.200s", resp.StatusCode, raw)
	}
	return &account{
		PrivateKey: base64.StdEncoding.EncodeToString(priv), PeerKey: r.Config.Peers[0].PublicKey,
		V4: r.Config.Interface.Addresses.V4, V6: r.Config.Interface.Addresses.V6,
	}, nil
}

// up starts WireGuard and tries endpoints (last working one first) until traffic flows.
func up(a *account) (*netstack.Net, string, error) {
	addrs := []netip.Addr{netip.MustParseAddr(a.V4)}
	if v6, err := netip.ParseAddr(a.V6); err == nil {
		addrs = append(addrs, v6)
	}
	dns := []netip.Addr{netip.MustParseAddr("1.1.1.1"), netip.MustParseAddr("1.0.0.1")}
	tun, tnet, err := netstack.CreateNetTUN(addrs, dns, 1280)
	if err != nil {
		return nil, "", err
	}
	dev := device.NewDevice(tun, androidBind{conn.NewStdNetBind().(*conn.StdNetBind)}, device.NewLogger(device.LogLevelSilent, ""))
	priv, _ := base64.StdEncoding.DecodeString(a.PrivateKey)
	peer, _ := base64.StdEncoding.DecodeString(a.PeerKey)
	list := endpoints
	if a.Endpoint != "" {
		list = append([]string{a.Endpoint}, endpoints...)
	}
	for _, ep := range list {
		cfg := fmt.Sprintf("private_key=%s\nreplace_peers=true\npublic_key=%s\nendpoint=%s\nallowed_ip=0.0.0.0/0\nallowed_ip=::/0\npersistent_keepalive_interval=25\n",
			hex.EncodeToString(priv), hex.EncodeToString(peer), ep)
		if err := dev.IpcSet(cfg); err != nil {
			return nil, "", err
		}
		if err := dev.Up(); err != nil {
			log("up " + ep + ": " + err.Error())
		}
		if err := works(tnet); err != nil {
			log("check " + ep + ": " + err.Error())
		} else {
			return tnet, ep, nil
		}
	}
	return nil, "", errors.New("no WARP endpoint reachable")
}

func works(tnet *netstack.Net) error {
	c := &http.Client{Timeout: 6 * time.Second, Transport: &http.Transport{DialContext: tnet.DialContext}}
	resp, err := c.Get("https://www.cloudflare.com/cdn-cgi/trace")
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	b, _ := io.ReadAll(resp.Body)
	if !bytes.Contains(b, []byte("warp=on")) {
		return errors.New("not routed through WARP")
	}
	return nil
}

// log lines go to the app's logcat (tag "warp"); only READY/FAIL lines mean anything to it
func log(msg string) {
	fmt.Println("LOG " + msg)
}

type proxy struct {
	tnet *netstack.Net
	rp   *httputil.ReverseProxy
}

func (p *proxy) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodConnect {
		p.rp.ServeHTTP(w, r)
		return
	}
	ctx, cancel := context.WithTimeout(context.Background(), 15*time.Second)
	remote, err := p.tnet.DialContext(ctx, "tcp", r.Host)
	cancel()
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadGateway)
		return
	}
	client, buf, err := w.(http.Hijacker).Hijack()
	if err != nil {
		remote.Close()
		return
	}
	client.Write([]byte("HTTP/1.1 200 Connection established\r\n\r\n"))
	go func() { io.Copy(remote, buf); remote.Close(); client.Close() }()
	io.Copy(client, remote)
	remote.Close()
	client.Close()
}

// androidBind hides the concrete *conn.StdNetBind type so wireguard-go skips its netlink
// route listener, which Android's sandbox refuses ("permission denied") for app processes.
type androidBind struct{ *conn.StdNetBind }
