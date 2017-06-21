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
import com.bytesforge.linkasanote.data.source.local.LocalSyncResults;
import com.bytesforge.linkasanote.data.source.local.LocalTags;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;

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
            ContentResolver contentResolver, LocalSyncResults localSyncResults,
            LocalTags localTags, LocalNotes<Note> localNotes) {
        LinkFactory<Link> linkFactory = Link.getFactory();
        return new LocalLinks<>(contentResolver, localSyncResults,
                localTags, localNotes, linkFactory);
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
            ContentResolver contentResolver,
            LocalSyncResults localSyncResults, LocalTags localTags) {
        FavoriteFactory<Favorite> favoriteFactory = Favorite.getFactory();
        return new LocalFavorites<>(contentResolver, localSyncResults, localTags, favoriteFactory);
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
            ContentResolver contentResolver,
            LocalSyncResults localSyncResults, LocalTags localTags) {
        NoteFactory<Note> noteFactory = Note.getFactory();
        return new LocalNotes<>(contentResolver, localSyncResults, localTags, noteFactory);
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
    public LocalSyncResults provideLocalSyncResults(ContentResolver contentResolver) {
        return new LocalSyncResults(contentResolver);
    }

    @Provides
    @Singleton
    public LocalDataSource provideLocalDataSource(
            LocalSyncResults localSyncResults,
            LocalLinks<Link> localLinks, LocalFavorites<Favorite> localFavorites,
            LocalNotes<Note> localNotes, LocalTags localTags) {
        return new LocalDataSource(
                localSyncResults, localLinks, localFavorites, localNotes, localTags);
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
            LocalDataSource localDataSource, CloudDataSource cloudDataSource,
            BaseSchedulerProvider schedulerProvider) {
        if (repository == null) {
            repository = new Repository(localDataSource, cloudDataSource, schedulerProvider);
        }
        return repository;
    }
}
