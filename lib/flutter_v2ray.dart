import 'dart:convert';
import 'dart:io';

import 'package:flutter_v2ray_client/url/shadowsocks.dart';
import 'package:flutter_v2ray_client/url/socks.dart';
import 'package:flutter_v2ray_client/url/trojan.dart';
import 'package:flutter_v2ray_client/url/url.dart';
import 'package:flutter_v2ray_client/url/vless.dart';
import 'package:flutter_v2ray_client/url/vmess.dart';

import 'flutter_v2ray_platform_interface.dart';
import 'model/v2ray_status.dart';

export 'model/v2ray_status.dart';
export 'url/url.dart';

/// A class for managing V2Ray connections and operations.
/// Provides methods to initialize, start, stop, and query V2Ray services.
class V2ray {
  /// Creates a new V2ray instance.
  /// [onStatusChanged] is a callback function that will be called whenever the V2Ray status changes.
  V2ray({required this.onStatusChanged});

  /// Callback function invoked when the V2Ray status changes.
  /// It receives a [V2RayStatus] object containing details like duration, speeds, and state.
  final void Function(V2RayStatus status) onStatusChanged;

  /// Requests permission to use V2Ray features, such as VPN access on Android.
  /// Returns a [Future] that completes with true if permission is granted, otherwise false.
  /// On non-Android platforms, it defaults to granting permission.
  Future<bool> requestPermission() async {
    if (Platform.isAndroid) {
      return FlutterV2rayPlatform.instance.requestPermission();
    }
    return true;
  }

  /// Initializes the V2Ray client with notification settings and a status change callback.
  /// [notificationIconResourceType] specifies the type of the notification icon (e.g., 'mipmap').
  /// [notificationIconResourceName] specifies the name of the notification icon (e.g., 'ic_launcher').
  /// Returns a [Future] that completes when initialization is done.
  Future<void> initialize({
    String notificationIconResourceType = 'mipmap',
    String notificationIconResourceName = 'ic_launcher',
  }) async {
    await FlutterV2rayPlatform.instance.initializeV2Ray(
      onStatusChanged: onStatusChanged,
      notificationIconResourceType: notificationIconResourceType,
      notificationIconResourceName: notificationIconResourceName,
    );
  }

  /// Starts the V2Ray service with the given configuration and settings.
  /// [remark] is a string identifier for the connection.
  /// [config] is the V2Ray configuration in JSON format.
  /// [blockedApps] is an optional list of app package names to block.
  /// [bypassSubnets] is an optional list of subnets to bypass the VPN.
  /// [proxyOnly] is a boolean indicating whether to run in proxy-only mode.
  /// [notificationDisconnectButtonName] is the text for the disconnect button in notifications.
  /// Throws an [ArgumentError] if the config is not valid JSON.
  /// Returns a [Future] that completes when the service starts.
  Future<void> startV2Ray({
    required String remark,
    required String config,
    List<String>? blockedApps,
    List<String>? bypassSubnets,
    bool proxyOnly = false,
    String notificationDisconnectButtonName = 'DISCONNECT',
  }) async {
    try {
      if (jsonDecode(config) == null) {
        throw ArgumentError('The provided string is not valid JSON');
      }
    } catch (_) {
      throw ArgumentError('The provided string is not valid JSON');
    }

    await FlutterV2rayPlatform.instance.startV2Ray(
      remark: remark,
      config: config,
      blockedApps: blockedApps,
      proxyOnly: proxyOnly,
      bypassSubnets: bypassSubnets,
      notificationDisconnectButtonName: notificationDisconnectButtonName,
    );
  }

  /// Stops the V2Ray service.
  /// Returns a [Future] that completes when the service is stopped.
  Future<void> stopV2Ray() async {
    await FlutterV2rayPlatform.instance.stopV2Ray();
  }

  /// Measures the delay to a V2Ray server using the provided configuration.
  /// [config] is the V2Ray configuration in JSON format.
  /// [url] is the server URL to test for delay (default is 'https://clients3.google.com/generate_204').
  /// Throws an [ArgumentError] if the config is not valid JSON.
  /// Returns a [Future] that completes with the delay in milliseconds.
  Future<int> getServerDelay({
    required String config,
    String url = 'https://clients3.google.com/generate_204',
  }) async {
    try {
      if (jsonDecode(config) == null) {
        throw ArgumentError('The provided string is not valid JSON');
      }
    } catch (_) {
      throw ArgumentError('The provided string is not valid JSON');
    }
    return FlutterV2rayPlatform.instance
        .getServerDelay(config: config, url: url);
  }

  /// Measures the delay to the currently connected V2Ray server.
  /// [url] is the server URL to test for delay (default is 'https://clients3.google.com/generate_204').
  /// Returns a [Future] that completes with the delay in milliseconds.
  Future<int> getConnectedServerDelay({
    String url = 'https://clients3.google.com/generate_204',
  }) async {
    return FlutterV2rayPlatform.instance.getConnectedServerDelay(url);
  }

  /// Retrieves the version of the V2Ray core.
  /// Returns a [Future] that completes with a [String] representing the core version.
  Future<String> getCoreVersion() async {
    return FlutterV2rayPlatform.instance.getCoreVersion();
  }

  /// Parses a V2Ray URL string and returns the corresponding V2RayURL object.
  /// [url] is the V2Ray share link (e.g., 'vmess://', 'vless://', etc.).
  /// Throws an [ArgumentError] if the URL scheme is invalid.
  /// Returns a [V2RayURL] instance based on the scheme (e.g., VmessURL, VlessURL).
  static V2RayURL parseFromURL(String url) {
    switch (url.split('://')[0].toLowerCase()) {
      case 'vmess':
        return VmessURL(url: url);
      case 'vless':
        return VlessURL(url: url);
      case 'trojan':
        return TrojanURL(url: url);
      case 'ss':
        return ShadowSocksURL(url: url);
      case 'socks':
        return SocksURL(url: url);
      default:
        throw ArgumentError('url is invalid');
    }
  }
}
