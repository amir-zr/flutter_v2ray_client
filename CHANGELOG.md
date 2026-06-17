# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.3.0]

### Changed
- **Core**: Updated embedded Xray core to **v26.6.1** (from v26.4.17)
- **Android**: Updated `libv2ray` (AAR) and `V2rayCoreManager` to match the new core

## [3.2.0]

This release updates the embedded **Xray core to v26.4.17** and refreshes the Android `libv2ray` AAR and `V2rayCoreManager` integration. The items below follow upstream Xray changes since the **v25.10.15** line.

### Core — architecture
- **TUN inbound (Android VPN mode)**: Native integration with `VpnService`, with full-device VPN via a TUN interface, reducing the need for tun2socks in many deployments.
- **Process-based routing**: Route traffic by application or process where Android allows it, for finer control in VPN scenarios.

### Core — protocols & networking
- **Hysteria2**: High-throughput, UDP-based transport with better performance on unstable mobile links.
- **UDPhop**: Dynamic UDP port hopping to keep sessions working when ports are filtered or throttled.
- **Salamander obfuscation layer**: Optional traffic obfuscation to reduce visibility to mobile DPI.

### Core — obfuscation (FinalMask)
- Custom HTTP/TCP headers, TCP fragmentation, and UDP noise; randomized traffic patterns to resist fingerprinting and DPI.

### Core — security
- Replaces the old insecure TLS option with **certificate pinning** via `pinnedPeerCertSha256`.
- **REALITY**: Tighter handling of invalid or intercepted certificates, with stronger MITM protection on untrusted and mobile networks.

### Core — DNS & routing
- **XDNS**: More stable mobile DNS; routing behavior aligned with TUN mode; fewer surprises when the device switches between WiFi and mobile data.

### Core — performance & stability (mobile)
- Lower memory use (improved geodata handling); fewer Android runtime crash paths; more stable performance on poor networks; more reliable long-lived background sessions.

### Android — TUN
- Stabler TUN path through the VPN `Service` stack; less packet loss on high-latency links; better behavior under background execution limits; fewer stalls and race conditions in typical mobile conditions.

### Changed
- **Android**: Updated `libv2ray` (AAR) and `V2rayCoreManager` to use the new core and APIs above.

## [3.1.0]

### Changed
- **Core**: Updated xray-core to v25.12.2
- **Build**: Updated build configurations for better compatibility
- **Dependencies**: Resolved conflicts with `openvpn_flutter` package
- **Compatibility**: Added support for using alongside OpenVPN in the same application

## [3.0.1]

### Changed
- Updated README.md with iOS screenshot.

## [3.0.0]

### Added
- **Log Management**: Added V2Ray log viewing and management functionality
- **Log Features**: Implemented log filtering, copying, and clearing
- **Example App**: Added "View Logs" page to the example app
- **Documentation**: Updated with new log viewing feature details and usage examples

### Changed
- **Build**: Updated Gradle to 8.14 and Kotlin to 2.1.0
- **Build**: Updated Android Gradle Plugin to 8.13.0
- **Build**: Updated compiled SDK to 35
- **Core**: Updated xray-core to v25.10.15 (b69a376)
- **Compatibility**: Improved compatibility with latest Flutter versions

### Fixed
- **Android**: Fixed VPN permission race conditions
- **Android**: Added null safety in V2rayVPNService.onStartCommand
- **Service**: Improved service reliability with better error handling
- **Connection**: Fixed mode switching between VPN and proxy modes
- **Broadcast**: Fixed broadcast receiver registration issues
- **Activity**: Added proper activity lifecycle handling
- **Notifications**: Added silent mode when notification permissions are denied
- **Memory**: Fixed potential memory leaks in service lifecycle

### Performance
- **Service**: Improved service stability with better error handling
- **Logging**: Enhanced logging for debugging service lifecycle issues
- **Error Handling**: Added comprehensive error handling throughout the codebase

## [2.0.0]

### Added
- **Security**: Implemented app-specific broadcast isolation for V2Ray connections

### Changed
- **Build**: Enabled legacy JNI library packaging for better compatibility
- **Build**: Pinned NDK version to 27.0.12077973 for consistent builds
- **Performance**: Optimized server delay checks with parallel execution

## [1.1.0]

### Added
- **Enhanced VPN Protection**: Implemented robust Android VPN socket protector to prevent connection loops
- **IPv6 Support**: Added IPv6 preference option for server connections
- **Socket Management**: Introduced outbound socket management with automatic IP failover
- **Cross-Platform Builds**: Android-only build tags for seamless development across platforms

### Changed
- **Dependencies**: Updated to `golang.org/x/sys` for Android unix syscalls
- **Core Integration**: Improved Java/Go integration for better reliability
- **Documentation**: Added comprehensive guide for VPN protector implementation

### Fixed
- **Connection Stability**: Resolved issues with VPN socket routing
- **Build System**: Ensured compatibility with non-Android platforms

## [1.0.0]

### Added
- **Initial Release**: Comprehensive Flutter plugin for V2Ray/Xray client functionality
- **VPN & Proxy Modes**: Support for both VPN tunneling and proxy-only connections on Android
- **Server Delay Testing**: Built-in functionality to measure outbound and connected server delays
- **URL Parsing**: Robust parsers for VMess, VLess, Trojan, Socks, and ShadowSocks protocols
- **Configuration Editing**: Flexible API to modify V2Ray inbound/outbound settings, DNS, and routing
- **Live Status Monitoring**: Real-time updates for connection state, upload/download speeds, traffic data, and duration
- **Socket Protection**: Built-in Android VPN socket protection for secure tunneling
- **App Exclusion**: Ability to exclude specific Android apps from VPN traffic (blockedApps)
- **LAN Traffic Bypass**: Support for bypassing local network traffic with custom subnet lists
- **Notification Integration**: Customizable VPN notifications with icon resources
- **Permission Handling**: Automatic Android VPN permission requests
- **Event-Based Updates**: Callback system for status changes and connection events
- **Modernized API**: Clean, documented API with comprehensive Dartdoc comments
- **Example Application**: Complete Flutter app demonstrating all features and usage patterns
- **Android Compatibility**: Full support for Android 16 KB page size (required for Google Play)
- **Cross-Platform Foundation**: Platform interface architecture ready for iOS/Windows/Linux/macOS expansion

### Changed
- **Flutter Compatibility**: Upgraded to Flutter 3.16.0+ and Dart 3.1.0+
- **Code Quality**: Improved lint compliance with flutter_lints and comprehensive documentation
- **Build System**: Gradle 7.4 integration with Android NDK 26.3 support

### Technical Details
- **Android Implementation**: Xray 25.9.11 integration with method channels
- **Plugin Architecture**: Extends plugin_platform_interface for platform abstraction
- **Dependencies**: Minimal external dependencies for stability
- **Testing**: flutter_test integration for unit and widget testing
- **Licensing**: Open-source MIT license for community adoption

### Roadmap Highlights
- **Performance**: Planned integration with hev-socks5-tunnel for enhanced speed and efficiency
- **Multi-Platform**: Foundation laid for iOS, Windows, Linux, and macOS support (community-driven via donations)
- **Advanced Features**: Traffic routing, filtering, and UI components in development

### Attribution
This project builds upon third-party libraries and open-source contributions. See [ATTRIBUTION.md](./ATTRIBUTION.md) for detailed credits.

### Notes
- Android is the primary supported platform; other platforms marked as "Coming Soon"
- Community contributions welcome for accelerating multi-platform development
