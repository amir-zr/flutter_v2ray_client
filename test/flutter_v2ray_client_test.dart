import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_v2ray_client/flutter_v2ray.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('V2ray URL Parsing Tests', () {
    test('should parse vmess URL correctly', () {
      const vmessUrl =
          'vmess://eyJ2IjoiMiIsInBzIjoiVGVzdCBTZXJ2ZXIiLCJhZGQiOiIxMC4wLjAuMSIsInBvcnQiOiI0NDMiLCJpZCI6IjEyMzQ1Njc4LWFiY2QtMTIzNC1hYmNkLTEyMzQ1Njc4YWJjZCIsImFpZCI6IjAiLCJuZXQiOiJ0Y3AiLCJ0eXBlIjoibm9uZSIsImhvc3QiOiIiLCJwYXRoIjoiIiwidGxzIjoiIn0=';

      expect(() => V2ray.parseFromURL(vmessUrl), returnsNormally);
      final parsed = V2ray.parseFromURL(vmessUrl);
      expect(parsed, isA<V2RayURL>());
      expect(parsed.remark, equals('Test Server'));
    });

    test('should parse vless URL correctly', () {
      const vlessUrl =
          'vless://12345678-abcd-1234-abcd-12345678abcd@10.0.0.1:443?type=tcp&security=tls&sni=example.com#Test VLESS';

      expect(() => V2ray.parseFromURL(vlessUrl), returnsNormally);
      final parsed = V2ray.parseFromURL(vlessUrl);
      expect(parsed, isA<V2RayURL>());
      expect(parsed.remark, equals('Test VLESS'));
    });

    test('should throw ArgumentError for invalid URL', () {
      const invalidUrl = 'invalid://url';

      expect(() => V2ray.parseFromURL(invalidUrl), throwsArgumentError);
    });

    test('should throw ArgumentError for unsupported protocol', () {
      const unsupportedUrl = 'unsupported://example.com';

      expect(() => V2ray.parseFromURL(unsupportedUrl), throwsArgumentError);
    });
  });

  group('V2ray Configuration Validation Tests', () {
    const channel = MethodChannel('flutter_v2ray_client');
    final messenger =
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
    late V2ray v2ray;
    late List<MethodCall> calls;

    setUp(() {
      v2ray = V2ray(onStatusChanged: (_) {});
      calls = [];
      messenger.setMockMethodCallHandler(channel, (call) async {
        calls.add(call);
        return call.method == 'getServerDelay' ? 42 : null;
      });
    });

    tearDown(() {
      messenger.setMockMethodCallHandler(channel, null);
    });

    test('forwards a valid configuration to the Android start channel',
        () async {
      const validConfig = '{"inbounds": [], "outbounds": []}';

      await v2ray.startV2Ray(
        remark: 'Test',
        config: validConfig,
        proxyOnly: true,
      );

      expect(calls.single.method, 'startV2Ray');
      expect(calls.single.arguments['config'], validConfig);
    });

    test('rejects invalid JSON before starting Android', () async {
      await expectLater(
        v2ray.startV2Ray(
          remark: 'Test',
          config: 'invalid json',
          proxyOnly: true,
        ),
        throwsArgumentError,
      );

      expect(calls, isEmpty);
    });

    test('returns the delay from the Android channel', () async {
      const validConfig = '{"inbounds": [], "outbounds": []}';

      expect(await v2ray.getServerDelay(config: validConfig), 42);
      expect(calls.single.method, 'getServerDelay');
      expect(calls.single.arguments['config'], validConfig);
    });

    test('rejects invalid JSON before measuring Android delay', () async {
      await expectLater(
        v2ray.getServerDelay(config: 'invalid json'),
        throwsArgumentError,
      );

      expect(calls, isEmpty);
    });
  });
}
