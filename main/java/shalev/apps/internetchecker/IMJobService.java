package shalev.apps.internetchecker;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

@TargetApi(21)
public class IMJobService extends JobService {
    private static Context _ctx = null;

    public static void enqueueJob(Context context) {
        if(_ctx == null) _ctx = context;

        ComponentName serviceComponent = new ComponentName(_ctx, IMJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(1337, serviceComponent);
//        builder.setMinimumLatency(28 * 60 * 1000); // wait at least 10 minutes
//        builder.setOverrideDeadline(30 * 60 * 1000); // maximum delay 15 minutes
        builder.setMinimumLatency(Conf.IM_ALARM_SERVICE_INTERVAL_MS); //DELETE THOSE 2 lines
        builder.setOverrideDeadline(Conf.IM_ALARM_SERVICE_INTERVAL_MS + 2000);
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);

        JobScheduler jobScheduler = (JobScheduler)_ctx.getSystemService(context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(builder.build());
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        InternetManager manager = InternetManager.get();
        manager.doWork();
        enqueueJob(getApplicationContext());
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
