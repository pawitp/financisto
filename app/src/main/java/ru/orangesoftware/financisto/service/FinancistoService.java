/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * <p/>
 * Contributors:
 * Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import static android.app.PendingIntent.FLAG_CANCEL_CURRENT;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import java.util.Date;

import ru.orangesoftware.financisto.activity.AbstractTransactionActivity;
import ru.orangesoftware.financisto.activity.AccountWidget;
import ru.orangesoftware.financisto.backup.DatabaseExport;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.export.Export;
import ru.orangesoftware.financisto.model.TransactionInfo;

import static ru.orangesoftware.financisto.service.DailyAutoBackupScheduler.scheduleNextAutoBackup;
import ru.orangesoftware.financisto.utils.MyPreferences;

public class FinancistoService extends JobIntentService {

    private static final String TAG = "FinancistoService";
    public static final int JOB_ID = 1000;

    public static final String ACTION_SCHEDULE_AUTO_BACKUP = "ru.orangesoftware.financisto.ACTION_SCHEDULE_AUTO_BACKUP";
    public static final String ACTION_AUTO_BACKUP = "ru.orangesoftware.financisto.ACTION_AUTO_BACKUP";

    private DatabaseAdapter db;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, FinancistoService.class, JOB_ID, work);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        db = new DatabaseAdapter(this);
        db.open();
    }

    @Override
    public void onDestroy() {
        if (db != null) {
            db.close();
        }
        super.onDestroy();
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        final String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case ACTION_SCHEDULE_AUTO_BACKUP:
                    scheduleNextAutoBackup(this);
                    break;
                case ACTION_AUTO_BACKUP:
                    doAutoBackup();
                    break;
            }
        }
    }

    private void doAutoBackup() {
        try {
            try {
                long t0 = System.currentTimeMillis();
                Log.e(TAG, "Auto-backup started at " + new Date());
                DatabaseExport export = new DatabaseExport(this, db.db(), true);
                String fileName = export.export();
                boolean successful = true;
                if (MyPreferences.isDropboxUploadAutoBackups(this)) {
                    try {
                        Export.uploadBackupFileToDropbox(this, fileName);
                    } catch (Exception e) {
                        Log.e(TAG, "Unable to upload auto-backup to Dropbox", e);
                        MyPreferences.notifyAutobackupFailed(this, e);
                        successful = false;
                    }
                }
                Log.e(TAG, "Auto-backup completed in " + (System.currentTimeMillis() - t0) + "ms");
                if (successful) {
                    MyPreferences.notifyAutobackupSucceeded(this);
                }
            } catch (Exception e) {
                Log.e(TAG, "Auto-backup unsuccessful", e);
                MyPreferences.notifyAutobackupFailed(this, e);
            }
        } finally {
            scheduleNextAutoBackup(this);
        }
    }
}
