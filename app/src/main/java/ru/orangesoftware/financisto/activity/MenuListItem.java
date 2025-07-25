package ru.orangesoftware.financisto.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.ListAdapter;
import androidx.documentfile.provider.DocumentFile;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.backup.Backup;
import ru.orangesoftware.financisto.bus.GreenRobotBus;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.export.BackupExportTask;
import ru.orangesoftware.financisto.export.BackupImportTask;
import ru.orangesoftware.financisto.export.Export;
import ru.orangesoftware.financisto.export.csv.CsvExportOptions;
import ru.orangesoftware.financisto.export.csv.CsvExportTask;
import ru.orangesoftware.financisto.utils.EntityEnum;
import ru.orangesoftware.financisto.utils.EnumUtils;
import ru.orangesoftware.financisto.utils.IntegrityFix;
import ru.orangesoftware.financisto.utils.SafStorageHelper;
import ru.orangesoftware.financisto.utils.SummaryEntityEnum;

public enum MenuListItem implements SummaryEntityEnum {

    MENU_PREFERENCES(R.string.preferences, R.string.preferences_summary, R.drawable.drawer_action_preferences) {
        @Override
        public void call(Activity activity) {
            activity.startActivityForResult(new Intent(activity, PreferencesActivity.class), ACTIVITY_CHANGE_PREFERENCES);
        }
    },
    MENU_ENTITIES(R.string.entities, R.string.entities_summary, R.drawable.drawer_action_entities) {
        @Override
        public void call(final Activity activity) {
            final MenuEntities[] entities = MenuEntities.values();
            ListAdapter adapter = EnumUtils.createEntityEnumAdapter(activity, entities);
            final AlertDialog d = new AlertDialog.Builder(activity)
                    .setAdapter(adapter, (dialog, which) -> {
                        dialog.dismiss();
                        MenuEntities e = entities[which];
                        activity.startActivity(new Intent(activity, e.getActivityClass()));
                    })
                    .create();
            d.setTitle(R.string.entities);
            d.show();
        }
    },
    MENU_BACKUP(R.string.backup_database, R.string.backup_database_summary, R.drawable.actionbar_db_backup) {
        @Override
        public void call(Activity activity) {
            if (!SafStorageHelper.hasDirectoryAccess(activity)) {
                showSelectFolderMessage(activity);
                return;
            }
            ProgressDialog d = ProgressDialog.show(activity, null, activity.getString(R.string.backup_database_inprogress), true);
            new BackupExportTask(activity, d, true).execute();
        }
    },
    MENU_RESTORE(R.string.restore_database, R.string.restore_database_summary, R.drawable.actionbar_db_restore) {
        @Override
        public void call(final Activity activity) {
            if (!SafStorageHelper.hasDirectoryAccess(activity)) {
                showSelectFolderMessage(activity);
                return;
            }
            final String[] backupFiles = Backup.listBackups(activity);
            final String[] selectedBackupFile = new String[1];
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.restore_database)
                    .setPositiveButton(R.string.restore, (dialog, which) -> {
                        if (selectedBackupFile[0] != null) {
                            ProgressDialog d = ProgressDialog.show(activity, null, activity.getString(R.string.restore_database_inprogress), true);
                            new BackupImportTask(activity, d).execute(selectedBackupFile);
                        }
                    })
                    .setSingleChoiceItems(backupFiles, -1, (dialog, which) -> {
                        if (backupFiles != null && which >= 0 && which < backupFiles.length) {
                            selectedBackupFile[0] = backupFiles[which];
                        }
                    })
                    .show();
        }
    },
    DROPBOX_BACKUP(R.string.backup_database_online_dropbox, R.string.backup_database_online_dropbox_summary, R.drawable.actionbar_dropbox) {
        @Override
        public void call(Activity activity) {
            if (!SafStorageHelper.hasDirectoryAccess(activity)) {
                showSelectFolderMessage(activity);
                return;
            }
            GreenRobotBus.getInstance().post(new MenuListActivity.StartDropboxBackup());
        }
    },
    DROPBOX_RESTORE(R.string.restore_database_online_dropbox, R.string.restore_database_online_dropbox_summary, R.drawable.actionbar_dropbox) {
        @Override
        public void call(Activity activity) {
            if (!SafStorageHelper.hasDirectoryAccess(activity)) {
                showSelectFolderMessage(activity);
                return;
            }
            GreenRobotBus.getInstance().post(new MenuListActivity.StartDropboxRestore());
        }
    },
    MENU_BACKUP_TO(R.string.backup_database_to, R.string.backup_database_to_summary, R.drawable.actionbar_share) {
        @Override
        public void call(final Activity activity) {
            if (!SafStorageHelper.hasDirectoryAccess(activity)) {
                showSelectFolderMessage(activity);
                return;
            }
            ProgressDialog d = ProgressDialog.show(activity, null, activity.getString(R.string.backup_database_inprogress), true);
            final BackupExportTask t = new BackupExportTask(activity, d, false);
            t.setShowResultMessage(false);
            t.setListener(result -> {
                String backupFileName = t.backupFileName;
                DocumentFile documentFile = Export.getBackupFile(activity, backupFileName);
                if (documentFile != null) {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.putExtra(Intent.EXTRA_STREAM, documentFile.getUri());
                    intent.setType(Export.BACKUP_MIME_TYPE);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.backup_database_to_title)));
                }
            });
            t.execute((String[]) null);
        }
    },
    MENU_IMPORT_EXPORT(R.string.csv_export, R.string.csv_export_summary, R.drawable.actionbar_export) {
        @Override
        public void call(Activity activity) {
            if (!SafStorageHelper.hasDirectoryAccess(activity)) {
                showSelectFolderMessage(activity);
                return;
            }
            Intent intent = new Intent(activity, CsvExportActivity.class);
            activity.startActivityForResult(intent, ACTIVITY_CSV_EXPORT);
        }
    },
    MENU_INTEGRITY_FIX(R.string.integrity_fix, R.string.integrity_fix_summary, R.drawable.actionbar_flash) {
        @Override
        public void call(Activity activity) {
            new IntegrityFixTask(activity).execute();
        }
    },
    MENU_ABOUT(R.string.about, R.string.about_summary, R.drawable.ic_action_info) {
        @Override
        public void call(Activity activity) {
            activity.startActivity(new Intent(activity, AboutActivity.class));
        }
    };

    public final int titleId;
    public final int summaryId;
    public final int iconId;

    MenuListItem(int titleId, int summaryId, int iconId) {
        this.titleId = titleId;
        this.summaryId = summaryId;
        this.iconId = iconId;
    }

    @Override
    public int getTitleId() {
        return titleId;
    }

    @Override
    public int getSummaryId() {
        return summaryId;
    }

    @Override
    public int getIconId() {
        return iconId;
    }

    public static final int ACTIVITY_CSV_EXPORT = 2;
    public static final int ACTIVITY_CHANGE_PREFERENCES = 6;

    public abstract void call(Activity activity);

    private static void showSelectFolderMessage(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.select_folder_required)
                .setMessage(R.string.select_folder_required_message)
                .setPositiveButton(R.string.select_folder, (dialog, which) -> {
                    Intent intent = new Intent(activity, PreferencesActivity.class);
                    activity.startActivity(intent);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private enum MenuEntities implements EntityEnum {

        CURRENCIES(R.string.currencies, R.drawable.ic_action_money, CurrencyListActivity.class),
        EXCHANGE_RATES(R.string.exchange_rates, R.drawable.ic_action_line_chart, ExchangeRatesListActivity.class),
        CATEGORIES(R.string.categories, R.drawable.ic_action_category, CategoryListActivity2.class),
        PAYEES(R.string.payees, R.drawable.ic_action_users, PayeeListActivity.class),
        PROJECTS(R.string.projects, R.drawable.ic_action_gear, ProjectListActivity.class);

        private final int titleId;
        private final int iconId;
        private final Class<?> actitivyClass;
        private final String[] permissions;

        MenuEntities(int titleId, int iconId, Class<?> activityClass) {
            this(titleId, iconId, activityClass, (String[]) null);
        }

        MenuEntities(int titleId, int iconId, Class<?> activityClass, String... permissions) {
            this.titleId = titleId;
            this.iconId = iconId;
            this.actitivyClass = activityClass;
            this.permissions = permissions;
        }

        @Override
        public int getTitleId() {
            return titleId;
        }

        @Override
        public int getIconId() {
            return iconId;
        }

        public Class<?> getActivityClass() {
            return actitivyClass;
        }

        public String[] getPermissions() {
            return permissions;
        }
    }

    public static void doCsvExport(Activity activity, CsvExportOptions options) {
        ProgressDialog progressDialog = ProgressDialog.show(activity, null, activity.getString(R.string.csv_export_inprogress), true);
        new CsvExportTask(activity, progressDialog, options).execute();
    }

    private static class IntegrityFixTask extends AsyncTask<Void, Void, Void> {

        private final Activity context;
        private ProgressDialog progressDialog;

        IntegrityFixTask(Activity context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(context, null, context.getString(R.string.integrity_fix_in_progress), true);
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(Void o) {
            if (context instanceof MainActivity) {
                ((MainActivity) context).refreshCurrentTab();
            }
            progressDialog.dismiss();
        }

        @Override
        protected Void doInBackground(Void... objects) {
            DatabaseAdapter db = new DatabaseAdapter(context);
            new IntegrityFix(db).fix();
            return null;
        }
    }

}
