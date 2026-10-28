package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Remote-friendly settings owned by the launcher, in three panes (SettingsScreen) built from SettingsCatalog;
 * system settings stay one row away.
 */
public final class LauncherSettingsActivity extends Activity implements SettingsCatalog.Host {
    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));   // locale
    }

    static final String EXTRA_REORDER = "com.gathin.tvhome.extra.REORDER_FROM_SETTINGS";
    /** Back on Home, open the module cards list (the add-ons). */
    static final String EXTRA_CARDS = "com.gathin.tvhome.extra.CARDS_FROM_SETTINGS";
    private SharedPreferences choices;
    private Config config;
    private SettingsScreen screen;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_launcher_settings);
        choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        config = Config.load(this);
        screen = new SettingsScreen(this, new SettingsCatalog(this, this).build());
        screen.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        View content = findViewById(android.R.id.content);
        if (content.getVisibility() != View.VISIBLE) content.setVisibility(View.VISIBLE);
        screen.refresh();   // a picker or a dialog may have changed a value
    }

    @Override
    public void onBackPressed() {
        if (!screen.back()) super.onBackPressed();
    }

    // ---- SettingsCatalog.Host ----------------------------------------------------------------------

    /**
     * A choice was saved. Home rebuilds when it next resumes because the change count moved, whether these
     * settings are left with Back or with the remote's Home button (which cancels the activity result).
     */
    @Override
    public void changed() {
        setResult(RESULT_OK);
        choices.edit().putLong(Config.SETTINGS_REVISION_KEY,
                choices.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).apply();
    }

    @Override
    public void openGallery() {
        new WallpaperGallery(this, () -> {
            changed();
            screen.refresh();
        }).show();
    }

    @Override
    public void reorder() {
        setResult(RESULT_OK, new Intent().putExtra(EXTRA_REORDER, true));
        finish();
    }

    @Override
    public void openCards() {
        setResult(RESULT_OK, new Intent().putExtra(EXTRA_CARDS, true));
        finish();
    }

    @Override
    public void widgetPlaces() {
        // Read when opened: the dialog may have changed it since this screen was built.
        new WidgetPlacementDialog(this, Config.tvOverrides(choices).optString("widget_layout", config.widgetLayout),
                this::changed).show();
    }

    @Override
    public void preview() {
        startActivity(new Intent(this, CelebrationActivity.class));
    }

    @Override
    public boolean openOnStartAvailable() {
        return config.openOnStartChoice;
    }

    @Override
    public void openOnStart() {
        showOpenOnStartPicker();
    }

    @Override
    public String openOnStartLabel() {
        return openOnStartName();
    }

    @Override
    public void defaultHome() {
        // The system's Default home screen page is see-through on some TVs (Android 14's permission
        // controller): with these rows behind it, neither is readable. Show plain black behind it instead.
        getWindow().getDecorView().setBackgroundColor(0xFF000000);
        findViewById(android.R.id.content).setVisibility(View.INVISIBLE);
        Toast.makeText(this, R.string.home_settings_help, Toast.LENGTH_LONG).show();
        open(new Intent(Settings.ACTION_HOME_SETTINGS));
    }

    @Override
    public void tvSettings() {
        open(SystemSettings.intent(this, Settings.ACTION_SETTINGS));
    }

    @Override
    public void followTheme() {
        // It undoes every choice made here at once (the dock, wallpaper timing, widgets): ask first.
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.follow_theme)
                .setMessage(R.string.follow_theme_confirm)
                .setPositiveButton(R.string.follow_theme_reset, (d, which) -> {
                    choices.edit().remove("columns").remove(Config.ROWS_BELOW_DOCK_KEY).remove("animations")
                            .remove("advanced_effects").remove(Config.WALLPAPER_CHOICE_KEY)
                            .remove(Config.DOCK_OPACITY_KEY).remove(OpenOnStart.KEY).remove(Config.TV_OVERRIDES_KEY)
                            .commit();
                    changed();
                    recreate();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();
    }

    @Override
    public void confirmAdvanced(Runnable yes) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.advanced_effects)
                .setMessage(R.string.advanced_effects_warning)
                .setPositiveButton(R.string.enable, (dialog, which) -> {
                    yes.run();
                    screen.refresh();
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> recreate())   // shows Off again
                .show();
    }

    // ---- Open on start -------------------------------------------------------------------------------

    /** Off, then every app with a TV entry point by name. Saved at once; it is read on the next boot. */
    private void showOpenOnStartPicker() {
        PackageManager pm = getPackageManager();
        List<String[]> apps = new ArrayList<>();
        Intent tv = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        for (ResolveInfo r : pm.queryIntentActivities(tv, 0)) {
            String pkg = r.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            boolean seen = false;
            for (String[] a : apps) seen |= a[1].equals(pkg);
            if (!seen) apps.add(new String[]{r.loadLabel(pm).toString(), pkg});
        }
        Collections.sort(apps, (a, b) -> a[0].compareToIgnoreCase(b[0]));
        String[] labels = new String[apps.size() + 1];
        labels[0] = getString(R.string.off);
        String current = choices.getString(OpenOnStart.KEY, "");
        int checked = 0;
        for (int i = 0; i < apps.size(); i++) {
            labels[i + 1] = apps.get(i)[0];
            if (apps.get(i)[1].equals(current)) checked = i + 1;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.open_on_start_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == 0) choices.edit().remove(OpenOnStart.KEY).apply();
                    else {
                        OpenOnStart.skipThisBoot(this);
                        choices.edit().putString(OpenOnStart.KEY, apps.get(which - 1)[1]).apply();
                    }
                    screen.refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** The picked app's name, its package when it is gone, or Off. */
    private String openOnStartName() {
        String pkg = choices.getString(OpenOnStart.KEY, "");
        if (pkg.isEmpty()) return getString(R.string.off);
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
        }
    }
}
