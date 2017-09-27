package org.tasks.injection;

import android.app.Activity;
import android.content.Context;

import org.tasks.R;
import org.tasks.fragments.TaskEditControlSetFragmentManager;
import org.tasks.gtasks.SyncAdapterHelper;
import org.tasks.preferences.Preferences;
import org.tasks.themes.ThemeAccent;
import org.tasks.themes.ThemeBase;
import org.tasks.themes.ThemeCache;
import org.tasks.themes.ThemeColor;

import dagger.Module;
import dagger.Provides;

@Module
public class ActivityModule {

    private final Activity activity;

    public ActivityModule(Activity activity) {
        this.activity = activity;
    }

    @Provides
    public Activity getActivity() {
        return activity;
    }

    @Provides
    @ForActivity
    public Context getActivityContext() {
        return activity;
    }

    @Provides
    @ActivityScope
    public ThemeBase getThemeBase(ThemeCache themeCache, Preferences preferences) {
        return themeCache.getThemeBase(preferences.getInt(R.string.p_theme, 0));
    }

    @Provides
    @ActivityScope
    public ThemeColor getThemeColor(ThemeCache themeCache, Preferences preferences) {
        return themeCache.getThemeColor(preferences.getInt(R.string.p_theme_color, 0));
    }

    @Provides
    @ActivityScope
    public ThemeAccent getThemeAccent(ThemeCache themeCache, Preferences preferences) {
        return themeCache.getThemeAccent(preferences.getInt(R.string.p_theme_accent, 1));
    }

    @Provides
    @ActivityScope
    public TaskEditControlSetFragmentManager getTaskEditControlSetFragmentManager(Preferences preferences, SyncAdapterHelper syncAdapterHelper) {
        return new TaskEditControlSetFragmentManager(activity, preferences, syncAdapterHelper);
    }
}
