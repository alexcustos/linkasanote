package com.bytesforge.linkasanote;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;

import com.bytesforge.linkasanote.addeditaccount.AddEditAccountComponent;
import com.bytesforge.linkasanote.addeditaccount.nextcloud.NextcloudPresenterModule;
import com.bytesforge.linkasanote.addeditfavorite.AddEditFavoriteComponent;
import com.bytesforge.linkasanote.addeditfavorite.AddEditFavoritePresenterModule;
import com.bytesforge.linkasanote.data.source.ProviderModule;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.data.source.RepositoryModule;
import com.bytesforge.linkasanote.laano.LaanoComponent;
import com.bytesforge.linkasanote.laano.favorites.FavoritesPresenterModule;
import com.bytesforge.linkasanote.laano.links.LinksPresenterModule;
import com.bytesforge.linkasanote.laano.notes.NotesPresenterModule;
import com.bytesforge.linkasanote.manageaccounts.ManageAccountsComponent;
import com.bytesforge.linkasanote.manageaccounts.ManageAccountsPresenterModule;
import com.bytesforge.linkasanote.settings.SettingsModule;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.bytesforge.linkasanote.utils.schedulers.SchedulerProviderModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        ApplicationModule.class,
        SettingsModule.class,
        RepositoryModule.class,
        ProviderModule.class,
        SchedulerProviderModule.class})
public interface ApplicationComponent {

    Context getContext();
    SharedPreferences getSharedPreferences();
    Repository getRepository();
    ContentResolver getContentResolver();
    BaseSchedulerProvider getSchedulerProvider();

    // Subcomponents
    AddEditAccountComponent getAddEditAccountComponent(NextcloudPresenterModule module);
    AddEditFavoriteComponent getAddEditFavoriteComponent(AddEditFavoritePresenterModule module);
    LaanoComponent getLaanoComponent(
            LinksPresenterModule linksPresenterModule,
            FavoritesPresenterModule favoritesPresenterModule,
            NotesPresenterModule notesPresenterModule);
    ManageAccountsComponent getManageAccountsComponent(ManageAccountsPresenterModule module);
}
