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

package com.bytesforge.linkasanote.sync.files;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.utils.CloudUtils;
import com.bytesforge.linkasanote.utils.UuidUtils;
import com.google.common.base.Objects;

import java.io.File;

import static com.google.common.base.Preconditions.checkNotNull;

public class JsonFile implements Parcelable, Comparable<JsonFile> {

    private static final String MIME_TYPE = "application/json";
    private static final String FILE_EXTENSION = ".json";

    public static final String PATH_SEPARATOR = "/";

    private long length;
    private String localPath;
    private String remotePath;
    private String eTag;

    public static final Creator<JsonFile> CREATOR = new Creator<JsonFile>() {

        @Override
        public JsonFile createFromParcel(Parcel in) {
            return new JsonFile(in);
        }

        @Override
        public JsonFile[] newArray(int size) {
            return new JsonFile[size];
        }
    };

    public JsonFile(@NonNull String remotePath) {
        checkNotNull(remotePath);

        if (!remotePath.startsWith(PATH_SEPARATOR)) {
            throw new IllegalArgumentException(
                    "Remote path must be absolute [" + remotePath + "]");
        }
        length = 0;
        localPath = null;
        this.remotePath = remotePath;
        eTag = null;
    }

    public JsonFile(@NonNull String localPath, @NonNull String remotePath) {
        this(remotePath);
        checkNotNull(localPath);

        this.localPath = localPath;
        File localFile = new File(localPath);
        length = localFile.length();
    }

    protected JsonFile(Parcel in) {
        length = in.readLong();
        localPath = in.readString();
        remotePath = in.readString();
        eTag = in.readString();
    }

    @Override
    public int describeContents() {
        return super.hashCode();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(length);
        dest.writeString(localPath);
        dest.writeString(remotePath);
        dest.writeString(eTag);
    }

    @Override
    public int compareTo(@NonNull JsonFile obj) {
        return getRemotePath().compareToIgnoreCase(obj.getRemotePath());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        JsonFile file = (JsonFile) obj;
        return Objects.equal(remotePath, file.remotePath)
                && Objects.equal(length, file.length);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(length, localPath, remotePath, eTag);
    }

    // Getters & Setters

    public String getRemotePath() {
        return remotePath;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public String getMimeType() {
        return MIME_TYPE;
    }

    public String getETag() {
        return eTag;
    }

    public void setETag(String eTag) {
        this.eTag = eTag;
    }

    @NonNull
    public static String getFileName(@NonNull String id) {
        return checkNotNull(id) + FILE_EXTENSION;
    }

    @NonNull
    public static String getTempFileName(@NonNull String id) {
        return checkNotNull(id) + "." + CloudUtils.getApplicationId();
    }

    @Nullable
    public static String getId(String mimeType, String filePath) {
        if (mimeType == null || filePath == null) return null;
        if (!mimeType.equals(MIME_TYPE)) return null;

        String id = Uri.parse(filePath).getLastPathSegment();
        if (id != null && id.endsWith(FILE_EXTENSION)) {
            id = id.substring(0, id.length() - FILE_EXTENSION.length());
        } else {
            return null;
        }
        if (UuidUtils.isKeyValidUuid(id)) {
            return id;
        }
        return null;
    }
}
