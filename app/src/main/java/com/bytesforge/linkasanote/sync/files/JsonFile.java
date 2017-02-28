package com.bytesforge.linkasanote.sync.files;

import android.accounts.Account;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.data.source.local.LocalContract;
import com.bytesforge.linkasanote.sync.SyncState;
import com.google.common.base.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

public class JsonFile extends SyncState implements Parcelable, Comparable<JsonFile> {

    private static final String MIME_TYPE = "application/json";

    public static final String PATH_SEPARATOR = "/";

    private Uri uri;
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
        uri = null;
        length = 0;
        localPath = null;
        this.remotePath = remotePath;
        eTag = null;
    }

    protected JsonFile(Parcel in) {
        uri = Uri.parse(in.readString());
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
        dest.writeString(uri.toString());
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
        return Objects.hashCode(remotePath, length);
    }

    // Getters & Setters

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

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

    public String getKey(@NonNull Account account) {
        checkNotNull(account);
        return account.name + getRemotePath();
    }

    @NonNull
    public ContentValues getUpdateValues() {
        ContentValues values = getSyncStateValues();
        values.put(LocalContract.COMMON_NAME_ETAG, getETag());
        return values;
    }
}
