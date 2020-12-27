package shalev.apps.internetchecker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class IMAlarmService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        InternetManager manager = InternetManager.get();
        manager.doWork();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public static void addAlarm(Context context) {
        // Get alarm manager.
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        long startTime = System.currentTimeMillis();
        long intervalTime = Conf.IM_ALARM_SERVICE_INTERVAL_MS;

        // Create intent to invoke the background service.
        Intent intent = new Intent(context, IMAlarmService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Create repeat alarm.
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, startTime, intervalTime, pendingIntent);
    }
}