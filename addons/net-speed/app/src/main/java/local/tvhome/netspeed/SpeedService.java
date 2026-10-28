package local.tvhome.netspeed;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;

/**
 * The line "↓ 1.2 MB/s  ↑ 35 KB/s" in the bottom right corner, 8 px from both edges, over every app. It reads the
 * whole device's counters (TrafficStats) every interval; nothing is stored or sent anywhere.
 */
public final class SpeedService extends Service {
    private static final String CHANNEL = "speed";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager windows;
    private TextView line;
    private long rx, tx, at;
    private int intervalMs;
    private boolean bits;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            long nowRx = TrafficStats.getTotalRxBytes(), nowTx = TrafficStats.getTotalTxBytes();
            long now = SystemClock.elapsedRealtime();
            long down = Rate.perSecond(rx, nowRx, now - at), up = Rate.perSecond(tx, nowTx, now - at);
            rx = nowRx;
            tx = nowTx;
            at = now;
            line.setText("↓ " + Rate.format(down, bits) + "   ↑ " + Rate.format(up, bits));
            handler.postDelayed(this, intervalMs);
        }
    };

    /** Starts or stops the line to match the settings. */
    static void apply(Context ctx) {
        Intent i = new Intent(ctx, SpeedService.class);
        if (Prefs.on(ctx) && Settings.canDrawOverlays(ctx)) ctx.startForegroundService(i);
        else ctx.stopService(i);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundQuietly();
        intervalMs = Prefs.intervalMs(this);
        bits = Prefs.bits(this);
        if (line == null) addLine();
        handler.removeCallbacks(tick);
        rx = TrafficStats.getTotalRxBytes();
        tx = TrafficStats.getTotalTxBytes();
        at = SystemClock.elapsedRealtime();
        handler.postDelayed(tick, intervalMs);
        return START_STICKY;
    }

    private void startForegroundQuietly() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, getString(R.string.channel),
                NotificationManager.IMPORTANCE_MIN));
        Notification n = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.icon).setContentTitle(getString(R.string.app_name)).build();
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(1, n);
    }

    private void addLine() {
        float d = getResources().getDisplayMetrics().density;
        line = new TextView(this);
        line.setTextColor(0xFFFFFFFF);
        line.setTextSize(13);
        line.setFontFeatureSettings("tnum");   // digits keep their width, the line does not jitter
        line.setPadding(Math.round(8 * d), Math.round(3 * d), Math.round(8 * d), Math.round(3 * d));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x99000000);
        bg.setCornerRadius(6 * d);
        line.setBackground(bg);
        line.setText("↓ 0 B/s   ↑ 0 B/s");
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.BOTTOM | Gravity.END;
        lp.x = 8;   // px from the right edge, as asked
        lp.y = 8;   // px from the bottom edge
        windows = getSystemService(WindowManager.class);
        windows.addView(line, lp);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(tick);
        if (line != null) windows.removeView(line);
        line = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
