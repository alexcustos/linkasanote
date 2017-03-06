package com.bytesforge.linkasanote.data.source.cloud;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.util.Log;

import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.DataSource;
import com.bytesforge.linkasanote.data.source.local.LocalContract;
import com.bytesforge.linkasanote.sync.files.FilesService;
import com.bytesforge.linkasanote.sync.files.JsonFile;
import com.bytesforge.linkasanote.utils.CloudUtils;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.List;

import javax.inject.Singleton;

import io.reactivex.Single;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class CloudDataSource implements DataSource {

    private static final String TAG = CloudDataSource.class.getSimpleName();

    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final Resources resources;
    private final AccountManager accountManager;

    public CloudDataSource(Context context, SharedPreferences sharedPreferences) {
        this.context = context;
        this.sharedPreferences = sharedPreferences;
        resources = context.getResources();
        accountManager = AccountManager.get(context);
    }

    @Override
    public Single<List<Link>> getLinks() {
        return null;
    }

    @Override
    public Single<Link> getLink(@NonNull String linkId) {
        return null;
    }

    @Override
    public void saveLink(@NonNull Link link) {
    }

    @Override
    public void deleteAllLinks() {
    }

    // Notes

    @Override
    public Single<List<Note>> getNotes() {
        return null;
    }

    @Override
    public Single<Note> getNote(@NonNull String noteId) {
        return null;
    }

    @Override
    public void saveNote(@NonNull Note note) {
    }

    @Override
    public void deleteAllNotes() {
    }

    // Favorites

    @Override
    public Single<List<Favorite>> getFavorites() {
        return null;
    }

    @Override
    public Single<Favorite> getFavorite(@NonNull String favoriteId) {
        return null;
    }

    @Override
    public void saveFavorite(@NonNull Favorite favorite) {
        checkNotNull(favorite);
        if (!CloudUtils.isApplicationConnected(context)) return;

        JSONObject favoriteJson = favorite.getJsonObject();
        if (favoriteJson == null) {
            throw new InvalidParameterException("Favorite object must not be empty");
        }
        Account[] accounts = CloudUtils.getAccountsWithPermissionCheck(context, accountManager);
        if (accounts == null) {
            Log.e(TAG, "Insufficient permission to access accounts in device");
            return;
        } else if (accounts.length <= 0) {
            return;
        }
        String localPath = context.getCacheDir() + File.separator + favorite.getTempFileName();
        File localFile = new File(localPath);
        try {
            Files.write(favoriteJson.toString(), localFile, Charsets.UTF_8);
        } catch (IOException e) {
            Log.e(TAG, "Cannot create temporary file: ", e);
            return;
        }
        String remotePath = getSyncDirectory() + JsonFile.PATH_SEPARATOR +
                Favorite.CLOUD_DIRECTORY + JsonFile.PATH_SEPARATOR + favorite.getFileName();
        String uri = LocalContract.FavoriteEntry
                .buildFavoritesUriWith(favorite.getId()).toString();
        Intent intent = new Intent(context, FilesService.class);
        // TODO: add multi-accounts support: upload to all accounts but sync with default one
        intent.putExtra(FilesService.EXTRA_ACCOUNT, accounts[0]);
        intent.putExtra(FilesService.EXTRA_URIS, new String[]{uri});
        intent.putExtra(FilesService.EXTRA_LOCAL_PATHS, new String[]{localFile.getPath()});
        intent.putExtra(FilesService.EXTRA_REMOTE_PATHS, new String[]{remotePath});

        context.startService(intent);
    }

    @Override
    public void deleteAllFavorites() {
    }

    @Override
    public void deleteFavorite(@NonNull String favoriteId) {
    }

    // Tags

    @Override
    public Single<List<Tag>> getTags() {
        throw new RuntimeException("getTags() was called but this operation cannot be applied to the cloud");
    }

    @Override
    public Single<Tag> getTag(@NonNull String tagId) {
        throw new RuntimeException("getTag() was called but this operation cannot be applied to the cloud");
    }

    @Override
    public void saveTag(@NonNull Tag tag) {
        throw new RuntimeException("saveTag() was called but this operation cannot be applied to the cloud");
    }

    @Override
    public void deleteAllTags() {
        throw new RuntimeException("deleteAllTags() was called but this operation cannot be applied to the cloud");
    }

    @NonNull
    private String getSyncDirectory() {
        String defaultSyncDirectory = resources.getString(R.string.default_sync_directory);
        return JsonFile.PATH_SEPARATOR + sharedPreferences.getString(
                resources.getString(R.string.pref_key_sync_directory), defaultSyncDirectory);
    }
}
