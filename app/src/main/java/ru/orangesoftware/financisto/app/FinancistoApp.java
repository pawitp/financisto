package ru.orangesoftware.financisto.app;

import android.content.Context;
import android.content.res.Configuration;
import androidx.multidex.MultiDexApplication;

import ru.orangesoftware.financisto.bus.GreenRobotBus;
import ru.orangesoftware.financisto.utils.MyPreferences;

public class FinancistoApp extends MultiDexApplication {

    public GreenRobotBus bus = GreenRobotBus.getInstance();

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(MyPreferences.switchLocale(base));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        MyPreferences.switchLocale(this);
    }
}
