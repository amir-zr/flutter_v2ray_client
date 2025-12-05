package dev.amirzr.flutter_v2ray_client.v2ray.core;

import static dev.amirzr.flutter_v2ray_client.v2ray.utils.Utilities.getUserAssetsPath;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.CountDownTimer;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import dev.amirzr.flutter_v2ray_client.v2ray.interfaces.V2rayServicesListener;
import dev.amirzr.flutter_v2ray_client.v2ray.services.V2rayProxyOnlyService;
import dev.amirzr.flutter_v2ray_client.v2ray.services.V2rayVPNService;
import dev.amirzr.flutter_v2ray_client.v2ray.utils.AppConfigs;
import dev.amirzr.flutter_v2ray_client.v2ray.utils.Utilities;
import dev.amirzr.flutter_v2ray_client.v2ray.utils.V2rayConfig;

import org.json.JSONObject;

import libv2ray.CoreCallbackHandler;
import libv2ray.CoreController;
import libv2ray.Libv2ray;


public final class V2rayCoreManager {
    private static final int NOTIFICATION_ID = 1;
    private volatile static V2rayCoreManager INSTANCE;
    public V2rayServicesListener v2rayServicesListener = null;
    private CoreController coreController;
    public AppConfigs.V2RAY_STATES V2RAY_STATE = AppConfigs.V2RAY_STATES.V2RAY_DISCONNECTED;
    private boolean isLibV2rayCoreInitialized = false;
    private CountDownTimer countDownTimer;
    private int seconds, minutes, hours;
    private long totalDownload, totalUpload, uploadSpeed, downloadSpeed;
    private String SERVICE_DURATION = "00:00:00";

    public static V2rayCoreManager getInstance() {
        if (INSTANCE == null) {
            synchronized (V2rayCoreManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new V2rayCoreManager();
                }
            }
        }
        return INSTANCE;
    }

    private void makeDurationTimer(final Context context, final boolean enable_traffic_statics) {
        countDownTimer = new CountDownTimer(7200, 1000) {
            @RequiresApi(api = Build.VERSION_CODES.M)
            public void onTick(long millisUntilFinished) {

                seconds++;
                if (seconds == 59) {
                    minutes++;
                    seconds = 0;
                }
                if (minutes == 59) {
                    minutes = 0;
                    hours++;
                }
                if (hours == 23) {
                    hours = 0;
                }
                if (enable_traffic_statics) {
                    downloadSpeed = (coreController != null ? coreController.queryStats("block", "downlink") : 0)
                            + (coreController != null ? coreController.queryStats("proxy", "downlink") : 0);
                    uploadSpeed = (coreController != null ? coreController.queryStats("block", "uplink") : 0)
                            + (coreController != null ? coreController.queryStats("proxy", "uplink") : 0);
                    totalDownload = totalDownload + downloadSpeed;
                    totalUpload = totalUpload + uploadSpeed;
                }
                SERVICE_DURATION = Utilities.convertIntToTwoDigit(hours) + ":" + Utilities.convertIntToTwoDigit(minutes)
                        + ":" + Utilities.convertIntToTwoDigit(seconds);
                String packageName = context.getPackageName();
                Intent connection_info_intent = new Intent(packageName + ".V2RAY_CONNECTION_INFO");
                connection_info_intent.setPackage(packageName);
                connection_info_intent.putExtra("STATE", V2rayCoreManager.getInstance().V2RAY_STATE);
                connection_info_intent.putExtra("DURATION", SERVICE_DURATION);
                connection_info_intent.putExtra("UPLOAD_SPEED", uploadSpeed);
                connection_info_intent.putExtra("DOWNLOAD_SPEED", downloadSpeed);
                connection_info_intent.putExtra("UPLOAD_TRAFFIC", totalUpload);
                connection_info_intent.putExtra("DOWNLOAD_TRAFFIC", totalDownload);
                try {
                    context.sendBroadcast(connection_info_intent);
                } catch (Exception e) {
                    Log.w("V2rayCoreManager", "Failed to send connection info broadcast", e);
                }
            }

            public void onFinish() {
                countDownTimer.cancel();
                if (V2rayCoreManager.getInstance().isV2rayCoreRunning())
                    makeDurationTimer(context, enable_traffic_statics);
            }
        }.start();
    }

    public void setUpListener(Service targetService) {
        try {
            v2rayServicesListener = (V2rayServicesListener) targetService;
            Libv2ray.initCoreEnv(getUserAssetsPath(targetService.getApplicationContext()), "");

            
            // Initialize controller with callback handler
            coreController = Libv2ray.newCoreController(new CoreCallbackHandler() {
                @Override
                public long onEmitStatus(long p0, String p1) {
                    // Currently unused; log for debugging
                    Log.d(V2rayCoreManager.class.getSimpleName(), "onEmitStatus => " + p0 + ": " + p1);
                    return 0;
                }

                @Override
                public long shutdown() {
                    if (v2rayServicesListener == null) {
                        Log.e(V2rayCoreManager.class.getSimpleName(), "shutdown failed => can`t find initial service.");
                        return -1;
                    }
                    try {
                        v2rayServicesListener.stopService();
                        v2rayServicesListener = null;
                        return 0;
                    } catch (Exception e) {
                        Log.e(V2rayCoreManager.class.getSimpleName(), "shutdown failed =>", e);
                        return -1;
                    }
                }

                @Override
                public long startup() {
                    if (v2rayServicesListener != null) {
                        try {
                            v2rayServicesListener.startService();
                        } catch (Exception e) {
                            Log.e(V2rayCoreManager.class.getSimpleName(), "startup failed => ", e);
                            return -1;
                        }
                    }
                    return 0;
                }
            });
            isLibV2rayCoreInitialized = true;
            SERVICE_DURATION = "00:00:00";
            seconds = 0;
            minutes = 0;
            hours = 0;
            uploadSpeed = 0;
            downloadSpeed = 0;
            totalDownload = 0;
            totalUpload = 0;
            Log.e(V2rayCoreManager.class.getSimpleName(), "setUpListener => new initialize from "
                    + v2rayServicesListener.getService().getClass().getSimpleName());
        } catch (Exception e) {
            Log.e(V2rayCoreManager.class.getSimpleName(), "setUpListener failed => ", e);
            isLibV2rayCoreInitialized = false;
        }
    }

    public boolean startCore(final V2rayConfig v2rayConfig) {
        makeDurationTimer(v2rayServicesListener.getService().getApplicationContext(),
                v2rayConfig.ENABLE_TRAFFIC_STATICS);
        V2RAY_STATE = AppConfigs.V2RAY_STATES.V2RAY_CONNECTING;
        if (!isLibV2rayCoreInitialized) {
            Log.e(V2rayCoreManager.class.getSimpleName(),
                    "startCore failed => LibV2rayCore should be initialize before start.");
            return false;
        }
        if (isV2rayCoreRunning()) {
            stopCore();
        }
        try {
            if (coreController == null) {
                Log.e(V2rayCoreManager.class.getSimpleName(), "startCore failed => coreController is null.");
                return false;
            }

            coreController.startLoop(v2rayConfig.V2RAY_FULL_JSON_CONFIG);
            V2RAY_STATE = AppConfigs.V2RAY_STATES.V2RAY_CONNECTED;
            if (isV2rayCoreRunning()) {
                showNotification(v2rayConfig);
            }
        } catch (Exception e) {
            Log.e(V2rayCoreManager.class.getSimpleName(), "startCore failed =>", e);
            return false;
        }
        return true;
    }

    public void stopCore() {
        try {
            // Safely cancel notification - handle cases where service might be null
            if (v2rayServicesListener != null && v2rayServicesListener.getService() != null) {
                NotificationManager notificationManager = (NotificationManager) v2rayServicesListener.getService()
                        .getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.cancel(NOTIFICATION_ID);
                }
            }
        } catch (Exception e) {
            Log.w("V2rayCoreManager", "Failed to cancel notification", e);
        }

        try {
            if (isV2rayCoreRunning()) {
                if (coreController != null) {
                    coreController.stopLoop();
                }
                v2rayServicesListener.stopService();
                Log.e(V2rayCoreManager.class.getSimpleName(), "stopCore success => v2ray core stopped.");
            } else {
                Log.e(V2rayCoreManager.class.getSimpleName(), "stopCore failed => v2ray core not running.");
            }
            sendDisconnectedBroadCast();
        } catch (Exception e) {
            Log.e(V2rayCoreManager.class.getSimpleName(), "stopCore failed =>", e);
        }
    }

    private void sendDisconnectedBroadCast() {
        V2RAY_STATE = AppConfigs.V2RAY_STATES.V2RAY_DISCONNECTED;
        SERVICE_DURATION = "00:00:00";
        seconds = 0;
        minutes = 0;
        hours = 0;
        uploadSpeed = 0;
        downloadSpeed = 0;
        if (v2rayServicesListener != null) {
            Context context = v2rayServicesListener.getService().getApplicationContext();
            String packageName = context.getPackageName();
            Intent connection_info_intent = new Intent(packageName + ".V2RAY_CONNECTION_INFO");
            connection_info_intent.setPackage(packageName);
            connection_info_intent.putExtra("STATE", V2rayCoreManager.getInstance().V2RAY_STATE);
            connection_info_intent.putExtra("DURATION", SERVICE_DURATION);
            connection_info_intent.putExtra("UPLOAD_SPEED", uploadSpeed);
            connection_info_intent.putExtra("DOWNLOAD_SPEED", downloadSpeed);
            connection_info_intent.putExtra("UPLOAD_TRAFFIC", totalUpload);
            connection_info_intent.putExtra("DOWNLOAD_TRAFFIC", totalDownload);
            try {
                context.sendBroadcast(connection_info_intent);
            } catch (Exception e) {
                Log.w("V2rayCoreManager", "Failed to send disconnected broadcast", e);
            }
        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    private String createNotificationChannelID(String appName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "A_FLUTTER_V2RAY_SERVICE_CH_ID"; // default and constant ID
            try {
                if (v2rayServicesListener == null || v2rayServicesListener.getService() == null) {
                    return channelId;
                }

                NotificationManager notificationManager = (NotificationManager) v2rayServicesListener.getService()
                        .getSystemService(Context.NOTIFICATION_SERVICE);

                String channelName = appName + " Background Service";
                NotificationChannel channel = new NotificationChannel(channelId, channelName,
                        NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription(channelName);
                channel.setLightColor(Color.DKGRAY);
                channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(channel);
                }
            } catch (Exception e) {
                Log.w("V2rayCoreManager", "Failed to create notification channel", e);
            }

            return channelId;
        }
        return "";
    }

    private void showNotification(final V2rayConfig v2rayConfig) {
        Service context = v2rayServicesListener.getService();
        if (context == null) {
            return;
        }

        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (launchIntent != null) {
            launchIntent.setAction("FROM_DISCONNECT_BTN");
            launchIntent.setFlags(
                    Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        final int flags;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        } else {
            flags = PendingIntent.FLAG_UPDATE_CURRENT;
        }
        PendingIntent notificationContentPendingIntent = PendingIntent.getActivity(
                context, 0, launchIntent, flags);

        String notificationChannelID = createNotificationChannelID(v2rayConfig.APPLICATION_NAME);

        Intent stopIntent;
        if (AppConfigs.V2RAY_CONNECTION_MODE == AppConfigs.V2RAY_CONNECTION_MODES.PROXY_ONLY) {
            stopIntent = new Intent(context, V2rayProxyOnlyService.class);
        } else if (AppConfigs.V2RAY_CONNECTION_MODE == AppConfigs.V2RAY_CONNECTION_MODES.VPN_TUN) {
            stopIntent = new Intent(context, V2rayVPNService.class);
        } else {
            return;
        }
        stopIntent.putExtra("COMMAND", AppConfigs.V2RAY_SERVICE_COMMANDS.STOP_SERVICE);

        PendingIntent pendingIntent = PendingIntent.getService(
                context, 0, stopIntent, flags);

        try {
            // Build the notification
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context,
                    notificationChannelID)
                    .setSmallIcon(v2rayConfig.APPLICATION_ICON)
                    .setContentTitle(v2rayConfig.REMARK)
                    .addAction(0, v2rayConfig.NOTIFICATION_DISCONNECT_BUTTON_NAME, notificationContentPendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setShowWhen(false)
                    .setOnlyAlertOnce(true)
                    .setContentIntent(notificationContentPendingIntent)
                    .setSilent(true)
                    .setOngoing(true);

            context.startForeground(NOTIFICATION_ID, notificationBuilder.build());
        } catch (Exception e) {
            Log.w("V2rayCoreManager", "Failed to show notification, continuing without notification", e);
            // VPN/Proxy continues to work even if notification fails
        }
    }

    public boolean isV2rayCoreRunning() {
        if (coreController != null) {
            return coreController.getIsRunning();
        }
        return false;
    }

    public Long getConnectedV2rayServerDelay() {
        try {
            if (coreController == null)
                return -1L;
            return coreController.measureDelay(AppConfigs.DELAY_URL);
        } catch (Exception e) {
            return -1L;
        }
    }

    public Long getV2rayServerDelay(final String config, final String url) {
        try {
            try {
                JSONObject config_json = new JSONObject(config);
                JSONObject new_routing_json = config_json.getJSONObject("routing");
                new_routing_json.remove("rules");
                config_json.remove("routing");
                config_json.put("routing", new_routing_json);
                return Libv2ray.measureOutboundDelay(config_json.toString(), url);
            } catch (Exception json_error) {
                Log.e("getV2rayServerDelay", json_error.toString());
                return Libv2ray.measureOutboundDelay(config, url);
            }
        } catch (Exception e) {
            Log.e("getV2rayServerDelayCore", e.toString());
            return -1L;
        }
    }

}
