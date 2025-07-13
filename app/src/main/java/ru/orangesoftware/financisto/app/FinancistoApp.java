package ru.orangesoftware.financisto.app;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;

import ru.orangesoftware.financisto.utils.MyPreferences;

public class FinancistoApp extends Application {

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
