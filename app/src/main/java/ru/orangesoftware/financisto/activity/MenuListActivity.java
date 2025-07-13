/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * <p/>
 * Contributors:
 * Denis Solonenko - initial API and implementation
 * Abdsandryk Souza - implementing 2D chart reports
 ******************************************************************************/
package ru.orangesoftware.financisto.activity;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.adapter.SummaryEntityListAdapter;
import ru.orangesoftware.financisto.bus.GreenRobotBus;
import ru.orangesoftware.financisto.export.csv.CsvExportOptions;
import ru.orangesoftware.financisto.export.dropbox.DropboxFileList;
import ru.orangesoftware.financisto.export.dropbox.DropboxBackupTask;
import ru.orangesoftware.financisto.export.dropbox.DropboxListFilesTask;
import ru.orangesoftware.financisto.export.dropbox.DropboxRestoreTask;

import static ru.orangesoftware.financisto.activity.MenuListItem.ACTIVITY_CHANGE_PREFERENCES;
import static ru.orangesoftware.financisto.activity.MenuListItem.ACTIVITY_CSV_EXPORT;
import static ru.orangesoftware.financisto.service.DailyAutoBackupScheduler.scheduleNextAutoBackup;

import ru.orangesoftware.financisto.utils.MyPreferences;

public class MenuListActivity extends ListActivity {

    private GreenRobotBus bus;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.bus = GreenRobotBus.getInstance();
        setContentView(R.layout.activity_menu_list);
        setListAdapter(new SummaryEntityListAdapter(this, MenuListItem.values()));
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(MyPreferences.switchLocale(base));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        MenuListItem.values()[position].call(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ACTIVITY_CSV_EXPORT:
                onCsvExportResult(resultCode, data);
                break;
            case ACTIVITY_CHANGE_PREFERENCES:
                onChangePreferences();
                break;
        }
    }

    public void onCsvExportResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            CsvExportOptions options = CsvExportOptions.fromIntent(data);
            MenuListItem.doCsvExport(this, options);
        }
    }

    public void onChangePreferences() {
        scheduleNextAutoBackup(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        bus.unregister(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bus.register(this);
    }

    // dropbox
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void doImportFromDropbox(DropboxFileList event) {
        final String[] backupFiles = event.files;
        if (backupFiles != null) {
            final String[] selectedDropboxFile = new String[1];
            new AlertDialog.Builder(this)
                    .setTitle(R.string.restore_database_online_dropbox)
                    .setPositiveButton(R.string.restore, (dialog, which) -> {
                        if (selectedDropboxFile[0] != null) {
                            ProgressDialog d = ProgressDialog.show(MenuListActivity.this, null, getString(R.string.restore_database_inprogress_dropbox), true);
                            new DropboxRestoreTask(MenuListActivity.this, d, selectedDropboxFile[0]).execute();
                        }
                    })
                    .setSingleChoiceItems(backupFiles, -1, (dialog, which) -> {
                        if (which >= 0 && which < backupFiles.length) {
                            selectedDropboxFile[0] = backupFiles[which];
                        }
                    })
                    .show();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void doDropboxBackup(StartDropboxBackup e) {
        ProgressDialog d = ProgressDialog.show(this, null, this.getString(R.string.backup_database_dropbox_inprogress), true);
        new DropboxBackupTask(this, d).execute();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void doDropboxRestore(StartDropboxRestore e) {
        ProgressDialog d = ProgressDialog.show(this, null, this.getString(R.string.dropbox_loading_files), true);
        new DropboxListFilesTask(this, d).execute();
    }

    public static class StartDropboxBackup {
    }

    public static class StartDropboxRestore {
    }


}
