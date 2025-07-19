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
package ru.orangesoftware.financisto.backup;

import android.content.Context;
import androidx.documentfile.provider.DocumentFile;
import ru.orangesoftware.financisto.utils.SafStorageHelper;

import java.util.Arrays;
import java.util.Comparator;

import static ru.orangesoftware.financisto.db.DatabaseHelper.*;

public final class Backup {

    public static final String[] BACKUP_TABLES = {
            ACCOUNT_TABLE, ATTRIBUTES_TABLE, CATEGORY_ATTRIBUTE_TABLE,
            TRANSACTION_ATTRIBUTE_TABLE, BUDGET_TABLE, CATEGORY_TABLE,
            CURRENCY_TABLE, LOCATIONS_TABLE, PROJECT_TABLE, TRANSACTION_TABLE,
            PAYEE_TABLE, CCARD_CLOSING_DATE_TABLE, SMS_TEMPLATES_TABLE,
            "split", /* todo: seems not used, found only in old 20110422_0051_create_split_table.sql, should be removed then */
            EXCHANGE_RATES_TABLE};

    public static final String[] BACKUP_TABLES_WITH_SYSTEM_IDS = {
            ATTRIBUTES_TABLE, CATEGORY_TABLE, PROJECT_TABLE, LOCATIONS_TABLE};

    public static final String[] BACKUP_TABLES_WITH_SORT_ORDER = {
            ACCOUNT_TABLE, SMS_TEMPLATES_TABLE, PROJECT_TABLE, PAYEE_TABLE, BUDGET_TABLE, CURRENCY_TABLE, LOCATIONS_TABLE, ATTRIBUTES_TABLE};

    public static final String[] RESTORE_SCRIPTS = {
            "20100114_1158_alter_accounts_types.sql",
            "20110903_0129_alter_template_splits.sql",
            "20171230_1852_alter_electronic_account_type.sql"
    };

    private Backup() {
    }

    public static String[] listBackups(Context context) {
        return Arrays.stream(SafStorageHelper.listFiles(context))
                .map(DocumentFile::getName)
                .filter(name -> name != null && name.endsWith(".backup"))
                .sorted(Comparator.reverseOrder())
                .toArray(String[]::new);
    }

    public static boolean tableHasSystemIds(String tableName) {
        for (String table : BACKUP_TABLES_WITH_SYSTEM_IDS) {
            if (table.equalsIgnoreCase(tableName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean tableHasOrder(String tableName) {
        for (String table : BACKUP_TABLES_WITH_SORT_ORDER) {
            if (table.equalsIgnoreCase(tableName)) {
                return true;
            }
        }
        return false;
    }

}
