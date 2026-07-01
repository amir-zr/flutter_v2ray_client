import 'dart:convert';

import 'package:flutter_v2ray_client/url/url.dart';

/// Hysteria URL parser and adapter to produce V2Ray (Xray) configuration pieces.
///
/// It parses a `hysteria2://` / `hy2://` (version 2) or `hysteria://` /
/// `hy://` (version 1) share link into structured fields and exposes outbound
/// and stream settings compatible with the Xray core.
class HysteriaURL extends V2RayURL {
  /// Creates a HysteriaURL by parsing the provided hysteria share link string.
  ///
  /// Throws [ArgumentError] if the url does not use a hysteria scheme or
  /// cannot be decoded into a valid URI.
  HysteriaURL({required super.url}) {
    final scheme = url.split('://').first.toLowerCase();
    if (!const ['hysteria', 'hysteria2', 'hy', 'hy2'].contains(scheme)) {
      throw ArgumentError('url is invalid');
    }
    final temp = Uri.tryParse(url);
    if (temp == null) {
      throw ArgumentError('url is invalid');
    }
    uri = temp;
    // hysteria2 / hy2 are protocol version 2, hysteria / hy are version 1.
    version = (scheme == 'hysteria2' || scheme == 'hy2') ? 2 : 1;
    auth = uri.userInfo;

    // The `fm` (final mask) parameter carries a JSON object describing the
    // obfuscation/masking applied to the underlying UDP transport.
    final fm = uri.queryParameters['fm'];
    if (fm != null && fm.isNotEmpty) {
      try {
        finalmask = jsonDecode(fm);
      } catch (_) {
        finalmask = null;
      }
    }

    // Idle timeout (seconds) for UDP connections. Optional; Xray defaults to 60.
    final idle = uri.queryParameters['udpIdleTimeout'] ??
        uri.queryParameters['udp_idle_timeout'];
    udpIdleTimeout = idle == null ? null : int.tryParse(idle);

    // HTTP/3 masquerade config, supplied as a JSON object (like `fm`).
    final mq = uri.queryParameters['masquerade'];
    if (mq != null && mq.isNotEmpty) {
      try {
        masquerade = jsonDecode(mq);
      } catch (_) {
        masquerade = null;
      }
    }
  }

  /// The parsed URI object from the hysteria URL.
  late final Uri uri;

  /// Hysteria protocol version (1 or 2).
  late final int version;

  /// Authentication string extracted from the URI user info.
  late final String auth;

  /// Parsed `fm` (final mask) object, or null when absent/invalid.
  Map<String, dynamic>? finalmask;

  /// Idle timeout (seconds) for UDP connections, or null to use Xray's default.
  int? udpIdleTimeout;

  /// Parsed HTTP/3 `masquerade` object, or null when absent/invalid.
  Map<String, dynamic>? masquerade;

  /// Server address extracted from the URI host.
  @override
  String get address => uri.host;

  /// Server port parsed from the URI. Falls back to [super.port] if absent.
  @override
  int get port => uri.hasPort ? uri.port : super.port;

  /// Human-readable remark decoded from the URI fragment.
  @override
  String get remark => Uri.decodeFull(uri.fragment.replaceAll('+', '%20'));

  /// Outbound configuration map for the hysteria protocol used by Xray core.
  @override
  Map<String, dynamic> get outbound1 {
    final sni = uri.queryParameters['sni'];
    return {
      'tag': 'proxy',
      'protocol': 'hysteria',
      'settings': {
        'address': address,
        'port': port,
        'version': version,
      },
      'streamSettings': {
        'network': 'hysteria',
        'security': uri.queryParameters['security'] ?? 'tls',
        'tlsSettings': {
          'allowInsecure': allowInsecure,
          'serverName': (sni == null || sni.isEmpty) ? null : sni,
        },
        'hysteriaSettings': {
          'version': version,
          'auth': auth,
          'udpIdleTimeout': udpIdleTimeout,
          'masquerade': masquerade,
        },
        'finalmask': finalmask,
      },
      'mux': {'enabled': false},
    };
  }
}
