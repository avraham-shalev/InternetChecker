package shalev.apps.internetchecker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.os.Build;
import android.util.Log;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class IMBroadcastReceiver extends BroadcastReceiver {

    private static boolean _wasRegistered = false;
    private static final Lock _lock = new ReentrantLock();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(Conf.LOGCAT_TAG_NAME, "BR:onRcv_" + intent.getAction());
        registerService(context);
    }

    public static void registerService(Context context) {
        Log.d(Conf.LOGCAT_TAG_NAME, "BR:rs");
        if (_wasRegistered) return;

        Log.d(Conf.LOGCAT_TAG_NAME, "BR:Locking");
        _lock.lock();
        Log.d(Conf.LOGCAT_TAG_NAME, "BR:Locked");

        try {
            if (_wasRegistered) return;

            Log.d(Conf.LOGCAT_TAG_NAME, "BR:NOT REGISTERED");
            initIManager();
            Log.d(Conf.LOGCAT_TAG_NAME, "BR:Inited IManager");

            if (Build.VERSION.SDK_INT < 21) {
                registerServiceByAlarm(context);
            } else {
                registerServiceByJobService(context);
            }
            Log.d(Conf.LOGCAT_TAG_NAME, "BR:REGISTERED");
            _wasRegistered = true;
        } finally {
            Log.d(Conf.LOGCAT_TAG_NAME, "BR:Unlocking");
            _lock.unlock();
            Log.d(Conf.LOGCAT_TAG_NAME, "BR:Unlocked");
        }
    }

    private static void initIManager() {
        InternetManager.get();
    }

    private static void registerServiceByAlarm(Context context) {
        IMAlarmService.addAlarm(context);
    }

    private static void registerServiceByJobService(Context context) {
        IMJobService.enqueueJob(context);
    }
}