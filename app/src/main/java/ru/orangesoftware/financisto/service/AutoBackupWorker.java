package ru.orangesoftware.financisto.service;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Date;

import ru.orangesoftware.financisto.backup.DatabaseExport;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.export.Export;
import ru.orangesoftware.financisto.utils.MyPreferences;

public class AutoBackupWorker extends Worker {

    private static final String TAG = "AutoBackupWorker";

    public AutoBackupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        DatabaseAdapter db = null;
        try {
            long t0 = System.currentTimeMillis();
            Log.i(TAG, "Auto-backup started at " + new Date());

            db = new DatabaseAdapter(getApplicationContext());
            db.open();

            DatabaseExport export = new DatabaseExport(getApplicationContext(), db.db(), true);
            String fileName = export.export();
            
            if (MyPreferences.isDropboxUploadAutoBackups(getApplicationContext())) {
                Export.uploadBackupFileToDropbox(getApplicationContext(), fileName);
            }

            Log.i(TAG, "Auto-backup completed in " + (System.currentTimeMillis() - t0) + "ms");

            MyPreferences.notifyAutobackupSucceeded(getApplicationContext());
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Auto-backup failed", e);
            MyPreferences.notifyAutobackupFailed(getApplicationContext(), e);
            return Result.retry();
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

}