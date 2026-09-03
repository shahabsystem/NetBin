# DNS Eye

DNS Eye (`Ir.hamed.dnseye`) is a modern Android local-VPN DNS/Hosts tool based on the original VHosts engine.

## DNS management
- Add unlimited custom IPv4 DNS servers.
- Built-in DNS profiles for Cloudflare, Google, Quad9, AdGuard and OpenDNS.
- Manual primary/backup selection.
- Automatic DNS benchmarking at connection time; the fastest two reachable servers are applied.
- One-tap DNS speed test from Settings.
- Default online Hosts source: `https://raw.githubusercontent.com/shahabsystem/VhostchizPn/main/hosts.txt`.

## Features
- Local VPN DNS interception without root
- Hosts and wildcard DNS support
- Built-in ad/tracker blocking
- Optional remote blocklist
- Primary and backup custom DNS
- Persian UI
- Light / dark / system theme
- JSON settings import/export
- Startup support, Quick Settings tile and widget
- Developer support links

## Build
```bash
./gradlew assembleGithubDebug
```

GitHub Actions automatically builds the GitHub Debug APK on pushes and pull requests.

## Package
`Ir.hamed.dnseye`

## Support
- GitHub: https://github.com/shahabsystem
- Email: hamedmohammadinikche@gmail.com
- Coffee: https://coffeebede.com/shahabsystem
- Reymit: https://reymit.ir/shahabsystem
