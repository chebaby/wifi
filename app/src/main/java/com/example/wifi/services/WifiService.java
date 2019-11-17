package com.example.wifi.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.wifi.R;
import com.example.wifi.enums.ServiceState;

public class WifiService extends Service {

    private final static String TAG = WifiService.class.getSimpleName();
    private boolean isServiceStarted = false;
    private PowerManager.WakeLock wakeLock;

    private com.example.wifi.utils.Service ServiceUtils;

    private static final int NOTIFICATION_ID = 1;
    private static final String PRIMARY_CHANNEL = "default";



    public WifiService() {
        ServiceUtils = new com.example.wifi.utils.Service();
    }




    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "\n\n onStartCommand executed with startId " + startId);

        if (intent != null) {
            String action = intent.getAction();
            Log.d(TAG, "\n\nonStartCommand using an intent with action " + action);

            switch (action) {
                case "START":
                    this.startTheService();
                    break;
                case "STOP":
                    this.stopTheService();
                    break;
                default:
                    Log.i(TAG, "\n\nonStartCommand: This should never happen. No action in the received intent");
                    break;
            }
        } else {
            Log.i(TAG, "\n\nonStartCommand: with a null intent. It has been probably restarted by the system.");
        }
        // by returning this we make sure the service is restarted if the system kills the service
        return START_STICKY;
    }




    @Override
    public IBinder onBind(Intent intent) {
        // The system invokes this method by calling bindService()
        // when another component wants to bind with the service
        // (such as to perform RPC).
        // We don't provide binding, so return null
        return null;
    }




    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "\n\nonCreate : The service has been created");

        startForeground(NOTIFICATION_ID, createNotification(this));
    }




    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.i(TAG, "\n\nonDestroy: The service has been destroyed");

        stopForeground(true);

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
//        if (trackingController != null) {
//            trackingController.stop();
//        }
    }




    public void startTheService() {

        if (isServiceStarted) return;

        startForeground(NOTIFICATION_ID, createNotification(this));

        Log.i(TAG, "\n\nstartTheService: Starting the foreground service task");

        isServiceStarted = true;

        // TODO: 11/17/2019 probably i sould name "ServiceUtils" something like "PersistServiceState" or "StoreServiceState"
        ServiceUtils.setState(this, ServiceState.STARTED);

        // we need this lock so our service gets not affected by Doze Mode
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        wakeLock.acquire();

//        trackingController = new TrackingController(this);
//        trackingController.start();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, new Intent(this, HideNotificationService.class));
        }
    }



    public void stopTheService() {

        Log.i(TAG, "\n\nstopTheService: Stopping the foreground service");

        stopForeground(true);

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

//        if (trackingController != null) {
//            trackingController.stop();
//        }

        isServiceStarted = false;
        ServiceUtils.setState(this, ServiceState.STOPPED);
    }




    private static Notification createNotification(Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, PRIMARY_CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE);

        Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);

        builder.setContentIntent(PendingIntent.getActivity(context, 0, intent, 0));
        return builder.build();
    }




    public static class HideNotificationService extends Service {
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public void onCreate() {
            startForeground(NOTIFICATION_ID, createNotification(this));
            stopForeground(true);
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            stopSelfResult(startId);
            return START_NOT_STICKY;
        }
    }
}
