package local.tvhome.netspeed;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Puts the line back after a restart or an update of this app, if it was on. */
public final class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String a = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(a) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)) SpeedService.apply(context);
    }
}
