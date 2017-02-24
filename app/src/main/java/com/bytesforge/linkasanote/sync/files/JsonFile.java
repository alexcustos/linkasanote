package com.bytesforge.linkasanote.sync.files;

import android.accounts.Account;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.bytesforge.linkasanote.data.source.local.LocalContract;
import com.google.common.base.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

public class JsonFile implements Parcelable, Comparable<JsonFile> {

    private static final String MIME_TYPE = "application/json";

    public static final String PATH_SEPARATOR = "/";

    private Uri uri;
    private long length;
    private String localPath;
    private String remotePath;
    private String eTag;
    private boolean conflicted;
    private boolean deleted;
    private boolean synced;

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
        conflicted = false;
        deleted = false;
        synced = false;
    }

    protected JsonFile(Parcel in) {
        uri = Uri.parse(in.readString());
        length = in.readLong();
        localPath = in.readString();
        remotePath = in.readString();
        eTag = in.readString();
        conflicted = in.readInt() == 1;
        deleted = in.readInt() == 1;
        synced = in.readInt() == 1;
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
        dest.writeInt(conflicted ? 1 : 0);
        dest.writeInt(deleted ? 1 : 0);
        dest.writeInt(synced ? 1 : 0);
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

    // States

    public void setState(boolean conflicted, boolean deleted, boolean synced) {
        this.conflicted = conflicted;
        this.deleted = deleted;
        this.synced = synced;
    }

    public void setUnsyncedState() {
        setState(false, false, false); // cds
    }

    public void setSyncedState() {
        setState(false, false, true); // cdS
    }

    public void setLocalDeletedState() {
        setState(false, true, false); // cDs, cDS successfully deleted (delete record)
    }

    public void setConflictedUpdate() {
        // NOTE: Local record was updated and Cloud one was modified or deleted
        // TODO: Conflicted record must preload Cloud copy and check if conflict still exists
        // TODO: Cloud copy: empty & cDs - deleted, cds - updated
        setState(true, false, false); // Cds, CdS successfully resolved (syncedState)
    }

    public void setConflictedDelete() {
        // NOTE: Local record was deleted and Cloud one was modified
        setState(true, true, false); // CDs, CDS successfully resolved (syncedState)
    }

    public String getKey(@NonNull Account account) {
        checkNotNull(account);
        return account.name + getRemotePath();
    }

    @NonNull
    public ContentValues getUpdateValues() {
        ContentValues values = new ContentValues();
        values.put(LocalContract.COMMON_NAME_ETAG, getETag());
        values.put(LocalContract.COMMON_NAME_CONFLICTED, conflicted);
        values.put(LocalContract.COMMON_NAME_DELETED, deleted);
        values.put(LocalContract.COMMON_NAME_SYNCED, synced);

        return values;
    }
}
