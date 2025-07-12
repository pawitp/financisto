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
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.activity.AbstractTransactionActivity;
import ru.orangesoftware.financisto.activity.AccountWidget;
import ru.orangesoftware.financisto.backup.DatabaseExport;
import ru.orangesoftware.financisto.blotter.BlotterFilter;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.export.Export;
import ru.orangesoftware.financisto.filter.WhereFilter;
import ru.orangesoftware.financisto.model.TransactionInfo;
import ru.orangesoftware.financisto.model.TransactionStatus;
import ru.orangesoftware.financisto.recur.NotificationOptions;
import static ru.orangesoftware.financisto.service.DailyAutoBackupScheduler.scheduleNextAutoBackup;
import ru.orangesoftware.financisto.utils.MyPreferences;

public class FinancistoService extends JobIntentService {

    private static final String TAG = "FinancistoService";
    public static final int JOB_ID = 1000;

    public static final String ACTION_SCHEDULE_ALL = "ru.orangesoftware.financisto.SCHEDULE_ALL";
    public static final String ACTION_SCHEDULE_ONE = "ru.orangesoftware.financisto.SCHEDULE_ONE";
    public static final String ACTION_SCHEDULE_AUTO_BACKUP = "ru.orangesoftware.financisto.ACTION_SCHEDULE_AUTO_BACKUP";
    public static final String ACTION_AUTO_BACKUP = "ru.orangesoftware.financisto.ACTION_AUTO_BACKUP";

    private static final int RESTORED_NOTIFICATION_ID = 0;

    private DatabaseAdapter db;
    private RecurrenceScheduler scheduler;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, FinancistoService.class, JOB_ID, work);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        db = new DatabaseAdapter(this);
        db.open();
        scheduler = new RecurrenceScheduler(db);
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
                case ACTION_SCHEDULE_ALL:
                    scheduleAll();
                    break;
                case ACTION_SCHEDULE_ONE:
                    scheduleOne(intent);
                    break;
                case ACTION_SCHEDULE_AUTO_BACKUP:
                    scheduleNextAutoBackup(this);
                    break;
                case ACTION_AUTO_BACKUP:
                    doAutoBackup();
                    break;
            }
        }
    }


    private void scheduleAll() {
        scheduler.scheduleAll(this);
    }

    private void scheduleOne(Intent intent) {
        long scheduledTransactionId = intent.getLongExtra(RecurrenceScheduler.SCHEDULED_TRANSACTION_ID, -1);
        if (scheduledTransactionId > 0) {
            TransactionInfo transaction = scheduler.scheduleOne(this, scheduledTransactionId);
            if (transaction != null) {
                notifyUser(transaction);
                AccountWidget.updateWidgets(this);
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

    private void notifyUser(TransactionInfo transaction) {
        Notification notification = createScheduledNotification(transaction);
        notifyUser(notification, (int) transaction.id);
    }

    private void notifyUser(Notification notification, int id) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, notification);
    }

    private Notification createScheduledNotification(TransactionInfo t) {
        String tickerText = t.getNotificationTickerText(this);
        String contentTitle = t.getNotificationContentTitle(this);
        String text = t.getNotificationContentText(this);

        return generateNotification(t, tickerText, contentTitle, text);
    }

    private Notification generateNotification(TransactionInfo t, String tickerText, String contentTitle, String text) {
        Intent notificationIntent = new Intent(this, t.getActivity());
        notificationIntent.putExtra(AbstractTransactionActivity.TRAN_ID_EXTRA, t.id);
        PendingIntent contentIntent = PendingIntent.getActivity(this, (int) t.id, notificationIntent, FLAG_CANCEL_CURRENT); /* https://stackoverflow.com/a/3730394/365675 */

        Notification notification = new NotificationCompat.Builder(this, "transactions")
                .setContentIntent(contentIntent)
                .setSmallIcon(t.getNotificationIcon())
                .setWhen(System.currentTimeMillis())
                .setTicker(tickerText)
                .setContentText(text)
                .setContentTitle(contentTitle)
                .setAutoCancel(true)
                .build();

        applyNotificationOptions(notification, t.notificationOptions);

        return notification;
    }

    private void applyNotificationOptions(Notification notification, String notificationOptions) {
        if (notificationOptions == null) {
            notification.defaults = Notification.DEFAULT_ALL;
        } else {
            NotificationOptions options = NotificationOptions.parse(notificationOptions);
            options.apply(notification);
        }
    }

}
