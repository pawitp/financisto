/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.service;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

import ru.orangesoftware.financisto.utils.MyPreferences;

public class DailyAutoBackupScheduler {

    private static final String AUTO_BACKUP_WORK_NAME = "daily_auto_backup";
    private static final String TAG = "DailyAutoBackupScheduler";

    public static void scheduleNextAutoBackup(Context context) {
        if (MyPreferences.isAutoBackupEnabled(context)) {
            int hhmm = MyPreferences.getAutoBackupTime(context);
            int hh = hhmm/100;
            int mm = hhmm - 100*hh;
            new DailyAutoBackupScheduler().scheduleBackup(context, hh, mm);
        } else {
            cancelAutoBackup(context);
        }
    }

    public static void cancelAutoBackup(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(AUTO_BACKUP_WORK_NAME);
        Log.i(TAG, "Auto-backup cancelled");
    }

    private void scheduleBackup(Context context, int hh, int mm) {
        Duration initialDelay = calculateInitialDelay(hh, mm);
        
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();

        PeriodicWorkRequest backupWorkRequest =
                new PeriodicWorkRequest.Builder(AutoBackupWorker.class, 24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(initialDelay)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                AUTO_BACKUP_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                backupWorkRequest
        );

        Log.i(TAG, "Auto-backup scheduled with initial delay of " + initialDelay.toMinutes() + " minutes");
    }

    Duration calculateInitialDelay(int hh, int mm) {
        LocalDateTime now = LocalDateTime.now();
        LocalTime targetTime = LocalTime.of(hh, mm);
        LocalDateTime scheduledTime = now.toLocalDate().atTime(targetTime);
        
        if (scheduledTime.isBefore(now) || scheduledTime.isEqual(now)) {
            scheduledTime = scheduledTime.plusDays(1);
        }
        
        return Duration.between(now, scheduledTime);
    }
}