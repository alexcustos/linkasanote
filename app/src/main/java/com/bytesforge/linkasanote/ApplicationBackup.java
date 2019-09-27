/*
 * LaaNo Android application
 *
 * @author Aleksandr Borisenko <developer@laano.net>
 * Copyright (C) 2017 Aleksandr Borisenko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.bytesforge.linkasanote;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.DateFormat;

import com.bytesforge.linkasanote.data.source.local.DatabaseHelper;
import com.bytesforge.linkasanote.settings.Settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import static com.bytesforge.linkasanote.utils.CommonUtils.logStackTrace;
import static com.google.common.base.Preconditions.checkNotNull;

public final class ApplicationBackup {

    private static final String TAG = ApplicationBackup.class.getSimpleName();
    private static final String TAG_E = ApplicationBackup.class.getCanonicalName();

    private static final String BACKUP_DIRECTORY = File.separator + "backups";
    public static final String BACKUP_EXTENSION_FORMAT = ".yyyy-MM-dd_HH-mm";
    private static final Pattern BACKUP_EXTENSION_PATTERN =
            Pattern.compile("\\.\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}");

    private ApplicationBackup() {
    }

    @Nullable
    private static File getExternalDir(@NonNull Context context) {
        File externalDir;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            externalDir = context.getExternalFilesDir(null);
        else
            externalDir = Environment.getExternalStorageDirectory();

        return externalDir;
    }

    @NonNull
    private static File getBackupDir(File externalDir) {
        File backupDir;

        // NOTE: for backward compatibility
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            backupDir = new File(externalDir + BACKUP_DIRECTORY);
        else
            backupDir = new File(externalDir +
                    Settings.GLOBAL_APPLICATION_DIRECTORY + BACKUP_DIRECTORY);

        return backupDir;
    }

    public static String backupDB(@NonNull Context context) {
        checkNotNull(context);

        File externalDir = getExternalDir(context);
        if (externalDir == null || !externalDir.canWrite())
            return null;

        File backupDir = getBackupDir(externalDir);
        if (!backupDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            backupDir.mkdirs();
        }
        String backupExtension = DateFormat.format(BACKUP_EXTENSION_FORMAT, new Date()).toString();
        String backupFile = DatabaseHelper.DATABASE_NAME + backupExtension;
        File srcPath = context.getDatabasePath(DatabaseHelper.DATABASE_NAME);
        File dstPath = new File(backupDir, backupFile);
        try {
            FileChannel src = new FileInputStream(srcPath).getChannel();
            FileChannel dst = new FileOutputStream(dstPath).getChannel();
            src.transferTo(0, src.size(), dst);
            src.close();
            dst.close();
        } catch (IOException e) {
            logStackTrace(TAG_E, e);
            return null;
        }
        return backupFile;
    }

    public static boolean restoreDB(@NonNull Context context, @NonNull String backupFile) {
        checkNotNull(context);
        checkNotNull(backupFile);
        File externalDir = getExternalDir(context);
        if (externalDir == null || !externalDir.canRead())
            return false;

        File backupDir = getBackupDir(externalDir);
        File srcPath = new File(backupDir, backupFile);
        if (!srcPath.exists()) return false;

        File dstPath = context.getDatabasePath(DatabaseHelper.DATABASE_NAME);
        try {
            FileChannel src = new FileInputStream(srcPath).getChannel();
            FileChannel dst = new FileOutputStream(dstPath).getChannel();
            src.transferTo(0, src.size(), dst);
            src.close();
            dst.close();
        } catch (IOException e) {
            logStackTrace(TAG_E, e);
            return false;
        }
        return true;
    }

    public static List<String> getBackupFileNames(@NonNull Context context) {
        File externalDir = getExternalDir(context);
        if (externalDir == null || !externalDir.canRead())
            return null;

        File backupDir = getBackupDir(externalDir);
        if (!backupDir.exists()) {
            return new ArrayList<>(0);
        }
        File[] backupFiles = backupDir.listFiles();
        if (backupFiles == null) return null;

        List<String> backupList = new ArrayList<>(backupFiles.length);
        for (File backupFile : backupFiles) {
            String fileName = backupFile.getName();
            String fileExtension = fileName.replace(DatabaseHelper.DATABASE_NAME, "");
            if (BACKUP_EXTENSION_PATTERN.matcher(fileExtension).matches()) {
                backupList.add(fileName);
            }
        }
        Collections.sort(backupList, Collections.reverseOrder());
        return backupList;
    }
}
