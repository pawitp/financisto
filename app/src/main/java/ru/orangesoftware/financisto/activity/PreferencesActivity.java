/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.export.dropbox.Dropbox;
import ru.orangesoftware.financisto.rates.ExchangeRateProviderFactory;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.SafStorageHelper;

public class PreferencesActivity extends PreferenceActivity {

    Preference pOpenExchangeRatesAppId;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(MyPreferences.switchLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        PreferenceScreen preferenceScreen = getPreferenceScreen();
        Preference pLocale = preferenceScreen.findPreference("ui_language");
        pLocale.setOnPreferenceChangeListener((preference, newValue) -> {
            String locale = (String) newValue;
            MyPreferences.switchLocale(PreferencesActivity.this, locale);
            return true;
        });
        Preference pDatabaseBackupFolder = preferenceScreen.findPreference("database_backup_folder");
        pDatabaseBackupFolder.setOnPreferenceClickListener(arg0 -> {
            selectDatabaseBackupFolder();
            return true;
        });
        Preference pAuthDropbox = preferenceScreen.findPreference("dropbox_authorize");
        pAuthDropbox.setOnPreferenceClickListener(arg0 -> {
            authDropbox();
            return true;
        });
        Preference pDeauthDropbox = preferenceScreen.findPreference("dropbox_unlink");
        pDeauthDropbox.setOnPreferenceClickListener(arg0 -> {
            deAuthDropbox();
            return true;
        });
        Preference pExchangeProvider = preferenceScreen.findPreference("exchange_rate_provider");
        pOpenExchangeRatesAppId = preferenceScreen.findPreference("openexchangerates_app_id");
        pExchangeProvider.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                pOpenExchangeRatesAppId.setEnabled(isOpenExchangeRatesProvider((String) newValue));
                return true;
            }

            private boolean isOpenExchangeRatesProvider(String provider) {
                return ExchangeRateProviderFactory.openexchangerates.name().equals(provider);
            }
        });
        linkToDropbox();
        setCurrentDatabaseBackupFolder();
        enableOpenExchangeApp();
    }

    private void linkToDropbox() {
        boolean dropboxAuthorized = MyPreferences.isDropboxAuthorized(this);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.findPreference("dropbox_unlink").setEnabled(dropboxAuthorized);
        preferenceScreen.findPreference("dropbox_upload_backup").setEnabled(dropboxAuthorized);
        preferenceScreen.findPreference("dropbox_upload_autobackup").setEnabled(dropboxAuthorized);
    }

    private void selectDatabaseBackupFolder() {
        SafStorageHelper.openDirectoryPicker(this);
    }

    private void enableOpenExchangeApp() {
        pOpenExchangeRatesAppId.setEnabled(MyPreferences.isOpenExchangeRatesProviderSelected(this));
    }

    private String getDatabaseBackupFolder() {
        if (SafStorageHelper.hasDirectoryAccess(this)) {
            return SafStorageHelper.getDirectoryDisplayName(this);
        } else {
            return getString(R.string.no_folder_selected);
        }
    }

    private void setCurrentDatabaseBackupFolder() {
        Preference pDatabaseBackupFolder = getPreferenceScreen().findPreference("database_backup_folder");
        String summary = getString(R.string.database_backup_folder_summary, getDatabaseBackupFolder());
        pDatabaseBackupFolder.setSummary(summary);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case SafStorageHelper.REQUEST_CODE_OPEN_DIRECTORY:
                    if (data != null) {
                        Uri uri = data.getData();
                        if (uri != null) {
                            SafStorageHelper.persistUriPermissions(this, uri);
                            MyPreferences.setDatabaseBackupFolderUri(this, uri.toString());
                            setCurrentDatabaseBackupFolder();
                        }
                    }
                    break;
            }
        }
    }

    Dropbox dropbox = new Dropbox(this);

    private void authDropbox() {
        dropbox.startAuth();
    }

    private void deAuthDropbox() {
        dropbox.deAuth();
        linkToDropbox();
    }

    @Override
    protected void onResume() {
        super.onResume();
        dropbox.completeAuth();
        linkToDropbox();
    }

}
