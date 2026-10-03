package dev.amirzr.flutter_v2ray_client.v2ray.services;

import android.app.Service;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.content.pm.PackageManager;

import dev.amirzr.flutter_v2ray_client.v2ray.core.V2rayCoreManager;
import dev.amirzr.flutter_v2ray_client.v2ray.interfaces.V2rayServicesListener;
import dev.amirzr.flutter_v2ray_client.v2ray.utils.AppConfigs;
import dev.amirzr.flutter_v2ray_client.v2ray.utils.Utilities;
import dev.amirzr.flutter_v2ray_client.v2ray.utils.V2rayConfig;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * VPN mode using LibXrayLite native TUN ({@code startLoop(config, tunFd)}).
 * <p>
 * Modern AndroidLibXrayLite no longer relies on external tun2socks for the default
 * VPN path. The VpnService FD is passed into Xray's built-in TUN inbound.
 */
public class V2rayVPNService extends VpnService implements V2rayServicesListener {
    private static final int VPN_MTU = 1500;

    private ParcelFileDescriptor mInterface;
    private V2rayConfig v2rayConfig;
    private boolean isRunning = false;
    /** True after VPN interface is established and before/while core owns the FD. */
    private boolean vpnInterfaceReady = false;

    @Override
    public void onCreate() {
        super.onCreate();
        V2rayCoreManager.getInstance().setUpListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.w("V2rayVPNService", "onStartCommand called with null intent, stopping service");
            this.onDestroy();
            return START_NOT_STICKY;
        }

        AppConfigs.V2RAY_SERVICE_COMMANDS startCommand = (AppConfigs.V2RAY_SERVICE_COMMANDS) intent
                .getSerializableExtra("COMMAND");

        if (startCommand == null) {
            Log.w("V2rayVPNService", "No command found in intent, stopping service");
            this.onDestroy();
            return START_NOT_STICKY;
        }

        if (startCommand.equals(AppConfigs.V2RAY_SERVICE_COMMANDS.START_SERVICE)) {
            v2rayConfig = (V2rayConfig) intent.getSerializableExtra("V2RAY_CONFIG");
            if (v2rayConfig == null) {
                Log.w("V2rayVPNService", "V2RAY_CONFIG is null, cannot start service");
                this.onDestroy();
                return START_NOT_STICKY;
            }
            if (V2rayCoreManager.getInstance().isV2rayCoreRunning()) {
                V2rayCoreManager.getInstance().stopCore();
            }

            // 1) Establish VpnService TUN first (v2rayNG order).
            if (!establishVpnInterface()) {
                Log.e("V2rayVPNService", "Failed to establish VPN interface");
                this.onDestroy();
                return START_NOT_STICKY;
            }

            // 2) Inject tun inbound and start core with FD.
            try {
                v2rayConfig.V2RAY_FULL_JSON_CONFIG = Utilities.ensureTunInbound(
                        v2rayConfig.V2RAY_FULL_JSON_CONFIG, VPN_MTU);
                int tunFd = mInterface.getFd();
                Log.i("V2rayVPNService", "Starting core with native tunFd=" + tunFd);
                if (!V2rayCoreManager.getInstance().startCore(v2rayConfig, tunFd)) {
                    Log.e("V2rayVPNService", "Failed to start v2ray core with tunFd");
                    this.onDestroy();
                    return START_NOT_STICKY;
                }
                Log.i("V2rayVPNService", "onStartCommand success => native TUN core started.");
            } catch (Exception e) {
                Log.e("V2rayVPNService", "Failed to start native TUN core", e);
                this.onDestroy();
                return START_NOT_STICKY;
            }
        } else if (startCommand.equals(AppConfigs.V2RAY_SERVICE_COMMANDS.STOP_SERVICE)) {
            V2rayCoreManager.getInstance().stopCore();
            AppConfigs.V2RAY_CONFIG = null;
        } else if (startCommand.equals(AppConfigs.V2RAY_SERVICE_COMMANDS.MEASURE_DELAY)) {
            new Thread(() -> {
                try {
                    String packageName = getPackageName();
                    Intent sendB = new Intent(packageName + ".CONNECTED_V2RAY_SERVER_DELAY");
                    sendB.setPackage(packageName);
                    sendB.putExtra("DELAY",
                            String.valueOf(V2rayCoreManager.getInstance().getConnectedV2rayServerDelay()));
                    sendBroadcast(sendB);
                } catch (Exception e) {
                    Log.w("V2rayVPNService", "Failed to send delay broadcast", e);
                }
            }, "MEASURE_CONNECTED_V2RAY_SERVER_DELAY").start();
        } else {
            Log.w("V2rayVPNService", "Unknown command received, stopping service");
            this.onDestroy();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    private boolean establishVpnInterface() {
        Intent prepareIntent = prepare(this);
        if (prepareIntent != null) {
            Log.e("V2rayVPNService", "VPN permission not granted");
            return false;
        }

        Builder builder = new Builder();
        builder.setSession(v2rayConfig.REMARK != null ? v2rayConfig.REMARK : "V2ray");
        builder.setMtu(VPN_MTU);
        builder.addAddress("26.26.26.1", 30);

        // Force IPv4 path: sinkhole IPv6 so apps don't bypass a broken dual-stack route.
        try {
            builder.addAddress("fd00:1:fd00:1:fd00:1:fd00:1", 126);
            builder.addRoute("::", 0);
        } catch (Exception e) {
            Log.w("V2rayVPNService", "IPv6 sinkhole not supported", e);
        }

        if (v2rayConfig.BYPASS_SUBNETS == null || v2rayConfig.BYPASS_SUBNETS.isEmpty()) {
            builder.addRoute("0.0.0.0", 0);
        } else {
            for (String subnet : v2rayConfig.BYPASS_SUBNETS) {
                String[] parts = subnet.split("/");
                if (parts.length == 2) {
                    try {
                        builder.addRoute(parts[0], Integer.parseInt(parts[1]));
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        // Keep this process outside the VPN so outbound sockets don't loop.
        try {
            builder.addDisallowedApplication(getPackageName());
        } catch (Exception e) {
            Log.w("V2rayVPNService", "Failed to exclude self from VPN", e);
        }
        if (v2rayConfig.BLOCKED_APPS != null) {
            for (String app : v2rayConfig.BLOCKED_APPS) {
                try {
                    builder.addDisallowedApplication(app);
                } catch (Exception ignored) {
                }
            }
        }

        boolean addedDns = false;
        try {
            JSONObject json = new JSONObject(v2rayConfig.V2RAY_FULL_JSON_CONFIG);
            JSONObject dnsObject = json.optJSONObject("dns");
            if (dnsObject != null) {
                JSONArray serversArray = dnsObject.optJSONArray("servers");
                if (serversArray != null) {
                    for (int i = 0; i < serversArray.length(); i++) {
                        Object entry = serversArray.get(i);
                        String candidate = null;
                        if (entry instanceof String) {
                            candidate = (String) entry;
                        } else if (entry instanceof JSONObject) {
                            candidate = ((JSONObject) entry).optString("address", null);
                        }
                        if (candidate != null && isPlainIpAddress(candidate)) {
                            builder.addDnsServer(candidate);
                            addedDns = true;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        if (!addedDns) {
            builder.addDnsServer("1.1.1.1");
            builder.addDnsServer("8.8.8.8");
        }

        try {
            if (mInterface != null) {
                mInterface.close();
            }
        } catch (Exception ignored) {
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false);
        }
            // add this part then your app wont go to vpn tunnel  
        try {
            builder.addDisallowedApplication(getPackageName());
            } catch (PackageManager.NameNotFoundException ignored) {}
            // now remove the protect parts in coremanager and use pure libv2ray.aar you can download it from "https://github.com/2dust/AndroidLibXrayLite/releases"

        try {
            mInterface = builder.establish();
            if (mInterface == null) {
                return false;
            }
            isRunning = true;
            vpnInterfaceReady = true;
            return true;
        } catch (Exception e) {
            Log.e("V2rayVPNService", "Failed to establish VPN interface", e);
            return false;
        }
    }

    private static boolean isPlainIpAddress(String value) {
        if (value == null) {
            return false;
        }
        String host = value.trim();
        if (host.isEmpty() || host.contains("/") || host.contains("://")) {
            return false;
        }
        if (host.contains(":")) {
            return host.matches("^[0-9a-fA-F:]+$") && host.split(":").length >= 3;
        }
        String[] parts = host.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        try {
            for (String part : parts) {
                int n = Integer.parseInt(part);
                if (n < 0 || n > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void stopAllProcess() {
        stopForeground(true);
        isRunning = false;
        vpnInterfaceReady = false;
        try {
            stopSelf();
        } catch (Exception e) {
            Log.e("V2rayVPNService", "stopSelf failed", e);
        }
        try {
            if (mInterface != null) {
                mInterface.close();
                mInterface = null;
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onDestroy() {
        Log.i("V2rayVPNService", "onDestroy called - cleaning up resources");
        isRunning = false;
        vpnInterfaceReady = false;

        try {
            if (V2rayCoreManager.getInstance().isV2rayCoreRunning()) {
                V2rayCoreManager.getInstance().stopCore();
            }
        } catch (Exception e) {
            Log.e("V2rayVPNService", "Error stopping V2ray core in onDestroy", e);
        }

        try {
            stopForeground(true);
        } catch (Exception e) {
            Log.e("V2rayVPNService", "Error stopping foreground in onDestroy", e);
        }

        try {
            if (mInterface != null) {
                mInterface.close();
                mInterface = null;
            }
        } catch (Exception e) {
            Log.e("V2rayVPNService", "Error closing VPN interface in onDestroy", e);
        }

        super.onDestroy();
    }

    @Override
    public void onRevoke() {
        try {
            V2rayCoreManager.getInstance().stopCore();
        } catch (Exception e) {
            stopAllProcess();
        }
    }

    @Override
    public boolean onProtect(int socket) {
        return protect(socket);
    }

    @Override
    public Service getService() {
        return this;
    }

    @Override
    public void startService() {
        // Core Startup callback: VPN interface is already established before startLoop.
        if (vpnInterfaceReady) {
            Log.i("V2rayVPNService", "startService callback ignored (native TUN already ready)");
            return;
        }
        Log.w("V2rayVPNService", "startService callback with VPN not ready");
    }

    @Override
    public void stopService() {
        stopAllProcess();
    }
}
