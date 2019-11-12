package com.example.wifi.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.example.wifi.enums.ServiceAction;
import com.example.wifi.enums.ServiceState;

public class WifiService extends Service {

    private final static String TAG = WifiService.class.getSimpleName();
    private boolean isServiceStarted = false;
    private PowerManager.WakeLock wakeLock;

    private com.example.wifi.utils.Service ServiceUtils;



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
                case ServiceAction.START.name():
                    this.startTheService();
                    break;
                case ServiceAction.STOP.name():
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
        val notification = createNotification();
        startForeground(1, notification);
    }




    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.i(TAG, "\n\nonDestroy: The service has been destroyed");
    }



    public void startTheService() {

        if (isServiceStarted) return;

        Log.i(TAG, "\n\nstartTheService: Starting the foreground service task");

        isServiceStarted = true;

        ServiceUtils.setState(this, ServiceState.STARTED);

        // we need this lock so our service gets not affected by Doze Mode
        wakeLock =
                (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EndlessService::lock").apply {
                acquire()
            }
        }

        if (SerialPortService.WAKELOCK == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            SerialPortService.WAKELOCK = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, SerialPortService.WL_TAG);
            SerialPortService.WAKELOCK.acquire();
            startService(new Intent(getApplicationContext(), SerialPortService.class));
        }

        // we're starting a loop in a coroutine
        GlobalScope.launch(Dispatchers.IO) {
            while (isServiceStarted) {
                launch(Dispatchers.IO) {
                    pingFakeServer()
                }
                delay(1 * 60 * 1000)
            }
            log("End of the loop for the service")
        }
    }




    private fun stopService() {
        log("Stopping the foreground service")
        Toast.makeText(this, "Service stopping", Toast.LENGTH_SHORT).show()
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            log("Service stopped without being started: ${e.message}")
        }
        isServiceStarted = false
        setServiceState(this, ServiceState.STOPPED)
    }

    private fun pingFakeServer() {
        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.mmmZ")
        val gmtTime = df.format(Date())

        val deviceId = Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)

        val json =
                """
        {
            "deviceId": "$deviceId",
                "createdAt": "$gmtTime"
        }
        """
        try {
            Fuel.post("https://jsonplaceholder.typicode.com/posts")
                    .jsonBody(json)
                    .response { _, _, result ->
                    val (bytes, error) = result
                if (bytes != null) {
                    log("[response bytes] ${String(bytes)}")
                } else {
                    log("[response error] ${error?.message}")
                }
            }
        } catch (e: Exception) {
            log("Error making the request: ${e.message}")
        }
    }

    private fun createNotification(): Notification {
        val notificationChannelId = "ENDLESS SERVICE CHANNEL"

        // depending on the Android API that we're dealing with we will have
        // to use a specific method to create the notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                    notificationChannelId,
                    "Endless Service notifications channel",
                    NotificationManager.IMPORTANCE_HIGH
            ).let {
                it.description = "Endless Service channel"
                it.enableLights(true)
                it.lightColor = Color.RED
                it.enableVibration(true)
                it.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
                it
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
        }

        val builder: Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(
                this,
                notificationChannelId
        ) else Notification.Builder(this)

        return builder
                .setContentTitle("Endless Service")
                .setContentText("This is your favorite endless service working")
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker("Ticker text")
                .setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
                .build()
    }
}
