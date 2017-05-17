package com.bytesforge.linkasanote.data.source;

import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;

import com.bytesforge.linkasanote.data.Favorite;
import com.bytesforge.linkasanote.data.FavoriteFactory;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.LinkFactory;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.NoteFactory;
import com.bytesforge.linkasanote.data.source.cloud.CloudDataSource;
import com.bytesforge.linkasanote.data.source.cloud.CloudItem;
import com.bytesforge.linkasanote.data.source.local.LocalDataSource;
import com.bytesforge.linkasanote.data.source.local.LocalFavorites;
import com.bytesforge.linkasanote.data.source.local.LocalLinks;
import com.bytesforge.linkasanote.data.source.local.LocalNotes;
import com.bytesforge.linkasanote.data.source.local.LocalTags;
import com.bytesforge.linkasanote.settings.Settings;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class RepositoryModule {

    private Repository repository;

    @Provides
    @Singleton
    public LocalTags provideLocalTags(ContentResolver contentResolver) {
        return new LocalTags(contentResolver);
    }

    @Provides
    @Singleton
    public LocalLinks<Link> provideLocalLinks(
            Context context, ContentResolver contentResolver,
            LocalTags localTags, LocalNotes<Note> localNotes) {
        LinkFactory<Link> linkFactory = Link.getFactory();
        return new LocalLinks<>(context, contentResolver, localTags, localNotes, linkFactory);
    }

    @Provides
    @Singleton
    public CloudItem<Link> provideCloudLinks(
            Context context, AccountManager accountManager, Settings settings) {
        LinkFactory<Link> linkFactory = Link.getFactory();
        return new CloudItem<>(context, accountManager, settings,
                Link.CLOUD_DIRECTORY_NAME, Link.SETTING_LAST_SYNCED_ETAG, linkFactory);
    }

    @Provides
    @Singleton
    public LocalFavorites<Favorite> provideLocalFavorites(
            Context context, ContentResolver contentResolver, LocalTags localTags) {
        FavoriteFactory<Favorite> favoriteFactory = Favorite.getFactory();
        return new LocalFavorites<>(context, contentResolver, localTags, favoriteFactory);
    }

    @Provides
    @Singleton
    public CloudItem<Favorite> provideCloudFavorites(
            Context context, AccountManager accountManager, Settings settings) {
        FavoriteFactory<Favorite> favoriteFactory = Favorite.getFactory();
        return new CloudItem<>(context, accountManager, settings,
                Favorite.CLOUD_DIRECTORY_NAME, Favorite.SETTING_LAST_SYNCED_ETAG, favoriteFactory);
    }

    @Provides
    @Singleton
    public LocalNotes<Note> provideLocalNotes(
            Context context, ContentResolver contentResolver, LocalTags localTags) {
        NoteFactory<Note> noteFactory = Note.getFactory();
        return new LocalNotes<>(contentResolver, localTags, noteFactory);
    }

    @Provides
    @Singleton
    public CloudItem<Note> provideCloudNotes(
            Context context, AccountManager accountManager, Settings settings) {
        NoteFactory<Note> noteFactory = Note.getFactory();
        return new CloudItem<>(context, accountManager, settings,
                Note.CLOUD_DIRECTORY_NAME, Note.SETTING_LAST_SYNCED_ETAG, noteFactory);
    }

    @Provides
    @Singleton
    public LocalDataSource provideLocalDataSource(
            LocalLinks<Link> localLinks, LocalFavorites<Favorite> localFavorites,
            LocalNotes<Note> localNotes, LocalTags localTags) {
        return new LocalDataSource(localLinks, localFavorites, localNotes, localTags);
    }

    @Provides
    @Singleton
    public CloudDataSource provideCloudDataSource(
            LocalLinks<Link> localLinks, CloudItem<Link> cloudLinks,
            LocalFavorites<Favorite> localFavorites, CloudItem<Favorite> cloudFavorites,
            LocalNotes<Note> localNotes, CloudItem<Note> cloudNotes) {
        return new CloudDataSource(localLinks, cloudLinks,
                localFavorites, cloudFavorites,
                localNotes, cloudNotes);
    }

    @Provides
    @Singleton
    public Repository provideRepository(
            LocalDataSource localDataSource, CloudDataSource cloudDataSource) {
        if (repository == null) {
            repository = new Repository(localDataSource, cloudDataSource);
        }
        return repository;
    }
}
