package local.tvhome.wallfetch;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;

/** Runs Fetcher on the schedule (job 1, every Store.days) or once now (job 2), only with a network. */
public final class FetchJob extends JobService {
    private static final int PERIODIC = 1, NOW = 2;
    private Thread worker;

    /** Sets the periodic job to the chosen days; 0 cancels it. Safe to call again with the same value. */
    static void schedule(Context ctx) {
        JobScheduler js = ctx.getSystemService(JobScheduler.class);
        int days = Store.days(ctx);
        if (days <= 0) {
            js.cancel(PERIODIC);
            return;
        }
        js.schedule(new JobInfo.Builder(PERIODIC, new ComponentName(ctx, FetchJob.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(Schedule.periodMs(days))
                .setPersisted(true)
                .build());
    }

    static void runNow(Context ctx) {
        ctx.getSystemService(JobScheduler.class).schedule(new JobInfo.Builder(NOW, new ComponentName(ctx, FetchJob.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .build());
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        // A periodic run that comes early (the TV was off, the job was rescheduled) waits for the period.
        if (params.getJobId() == PERIODIC
                && !Schedule.due(Store.last(this), System.currentTimeMillis(), Store.days(this))) return false;
        worker = new Thread(() -> {
            Store.prefs(this).edit().putBoolean("running", true).remove("error").apply();
            try {
                int got = Fetcher.run(this, Store.count(this), (n, of) ->
                        Store.prefs(this).edit().putInt("got", n).putInt("of", of).apply());
                Store.prefs(this).edit().putLong("last", System.currentTimeMillis()).putInt("have", got).apply();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                Store.prefs(this).edit().putString("error", String.valueOf(e.getMessage())).apply();
            } finally {
                Store.prefs(this).edit().putBoolean("running", false).apply();
                jobFinished(params, false);
            }
        }, "wallfetch");
        worker.start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (worker != null) worker.interrupt();
        Store.prefs(this).edit().putBoolean("running", false).apply();
        return true;   // try again later
    }
}
