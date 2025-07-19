package ru.orangesoftware.financisto.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for Storage Access Framework (SAF) operations
 * Replaces deprecated external storage permissions with modern SAF approach
 */
public class SafStorageHelper {

    public static final int REQUEST_CODE_OPEN_DIRECTORY = 1001;

    /**
     * Opens the SAF directory picker to let user select a folder
     */
    public static void openDirectoryPicker(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        activity.startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY);
    }

    /**
     * Persists URI permissions so they survive app restarts
     */
    public static void persistUriPermissions(Context context, Uri uri) {
        try {
            context.getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (SecurityException e) {
            // Handle case where permissions cannot be persisted
        }
    }

    /**
     * Gets the persisted directory URI from preferences
     */
    public static Uri getPersistedDirectoryUri(Context context) {
        String uriString = MyPreferences.getDatabaseBackupFolderUri(context);
        if (uriString != null && !uriString.isEmpty()) {
            return Uri.parse(uriString);
        }
        return null;
    }

    /**
     * Checks if we have persistent access to the directory
     */
    public static boolean hasDirectoryAccess(Context context) {
        Uri uri = getPersistedDirectoryUri(context);
        if (uri == null) return false;

        List<UriPermission> permissions = context.getContentResolver().getPersistedUriPermissions();
        for (UriPermission permission : permissions) {
            if (permission.getUri().equals(uri) && permission.isWritePermission()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a document file in the selected directory
     */
    public static DocumentFile createFileInDirectory(Context context, String fileName, String mimeType) {
        Uri directoryUri = getPersistedDirectoryUri(context);
        if (directoryUri == null) return null;

        DocumentFile directory = DocumentFile.fromTreeUri(context, directoryUri);
        if (directory == null || !directory.exists() || !directory.canWrite()) {
            return null;
        }

        // Check if file already exists and delete it
        DocumentFile existingFile = directory.findFile(fileName);
        if (existingFile != null && existingFile.exists()) {
            existingFile.delete();
        }

        return directory.createFile(mimeType, fileName);
    }

    /**
     * Lists all files in the selected directory
     */
    public static DocumentFile[] listFiles(Context context) {
        Uri directoryUri = getPersistedDirectoryUri(context);
        if (directoryUri == null) return new DocumentFile[0];

        DocumentFile directory = DocumentFile.fromTreeUri(context, directoryUri);
        if (directory == null || !directory.exists()) {
            return new DocumentFile[0];
        }

        DocumentFile[] files = directory.listFiles();

        // Only return actual files, not directories
        List<DocumentFile> fileList = new ArrayList<>();
        for (DocumentFile file : files) {
            if (file.isFile()) {
                fileList.add(file);
            }
        }

        return fileList.toArray(new DocumentFile[0]);
    }

    /**
     * Opens an input stream for reading from a document
     */
    public static InputStream openInputStream(Context context, DocumentFile documentFile) throws FileNotFoundException {
        return context.getContentResolver().openInputStream(documentFile.getUri());
    }

    /**
     * Opens an output stream for writing to a document
     */
    public static OutputStream openOutputStream(Context context, DocumentFile documentFile) throws FileNotFoundException {
        return context.getContentResolver().openOutputStream(documentFile.getUri());
    }

    /**
     * Gets display name for the selected directory
     */
    public static String getDirectoryDisplayName(Context context) {
        Uri directoryUri = getPersistedDirectoryUri(context);
        if (directoryUri == null) {
            return context.getString(ru.orangesoftware.financisto.R.string.no_folder_selected);
        }

        DocumentFile directory = DocumentFile.fromTreeUri(context, directoryUri);
        if (directory != null && directory.exists()) {
            String displayName = directory.getName();
            if (displayName != null && !displayName.isEmpty()) {
                return displayName;
            }
        }

        // Fallback to showing part of the URI
        String path = directoryUri.getPath();
        if (path != null) {
            String[] segments = path.split("/");
            if (segments.length > 0) {
                return segments[segments.length - 1];
            }
        }

        return directoryUri.toString();
    }
}