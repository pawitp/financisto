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
package ru.orangesoftware.financisto.export;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import androidx.documentfile.provider.DocumentFile;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.export.dropbox.Dropbox;
import ru.orangesoftware.financisto.utils.SafStorageHelper;

public abstract class Export {

    public static final String BACKUP_MIME_TYPE = "application/x-gzip";

    private final Context context;
    private final boolean useGzip;

    protected Export(Context context, boolean useGzip) {
        this.context = context;
        this.useGzip = useGzip;
    }

    public String export() throws Exception {
        if (!SafStorageHelper.hasDirectoryAccess(context)) {
            throw new ImportExportException(R.string.select_folder_required);
        }
        
        String fileName = generateFilename();
        String mimeType = useGzip ? BACKUP_MIME_TYPE : "text/plain";
        DocumentFile documentFile = SafStorageHelper.createFileInDirectory(context, fileName, mimeType);
        if (documentFile == null) {
            throw new ImportExportException(R.string.cannot_create_backup_file);
        }
        
        OutputStream outputStream = SafStorageHelper.openOutputStream(context, documentFile);
        if (outputStream == null) {
            throw new ImportExportException(R.string.cannot_create_backup_file);
        }
        
        try {
            if (useGzip) {
                export(new GZIPOutputStream(outputStream));
            } else {
                export(outputStream);
            }
        } finally {
            outputStream.flush();
            outputStream.close();
        }
        return fileName;
    }

    protected void export(OutputStream outputStream) throws Exception {
        generateBackup(outputStream);
    }

    public String generateFilename() {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd'_'HHmmss'_'SSS");
        return df.format(new Date()) + getExtension();
    }

    public byte[] generateBackupBytes() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OutputStream out = new BufferedOutputStream(new GZIPOutputStream(outputStream));
        generateBackup(out);
        return outputStream.toByteArray();
    }

    private void generateBackup(OutputStream outputStream) throws Exception {
        OutputStreamWriter osw = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        try (BufferedWriter bw = new BufferedWriter(osw, 65536)) {
            writeHeader(bw);
            writeBody(bw);
            writeFooter(bw);
        }
    }

    protected abstract void writeHeader(BufferedWriter bw) throws IOException, NameNotFoundException;

    protected abstract void writeBody(BufferedWriter bw) throws IOException;

    protected abstract void writeFooter(BufferedWriter bw) throws IOException;

    protected abstract String getExtension();

    public static DocumentFile getBackupFile(Context context, String backupFileName) {
        Uri directoryUri = SafStorageHelper.getPersistedDirectoryUri(context);
        if (directoryUri == null) return null;
        
        DocumentFile directory = DocumentFile.fromTreeUri(context, directoryUri);
        if (directory == null || !directory.exists()) {
            return null;
        }
        
        return directory.findFile(backupFileName);
    }

    public static void uploadBackupFileToDropbox(Context context, String backupFileName) throws Exception {
        DocumentFile documentFile = getBackupFile(context, backupFileName);
        if (documentFile == null) {
            throw new ImportExportException(R.string.cannot_create_backup_file);
        }
        
        String fileName = documentFile.getName();
        if (fileName == null) {
            throw new ImportExportException(R.string.cannot_create_backup_file);
        }
        
        try (InputStream inputStream = SafStorageHelper.openInputStream(context, documentFile)) {
            if (inputStream == null) {
                throw new ImportExportException(R.string.cannot_create_backup_file);
            }
            Dropbox dropbox = new Dropbox(context);
            dropbox.uploadFile(inputStream, fileName);
        }
    }

}
