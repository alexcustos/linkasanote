package com.bytesforge.linkasanote;

import android.support.annotation.VisibleForTesting;

import com.bytesforge.linkasanote.about.AboutComponent;
import com.bytesforge.linkasanote.about.AboutFragment;
import com.bytesforge.linkasanote.about.AboutPresenterModule;
import com.bytesforge.linkasanote.addeditaccount.AddEditAccountComponent;
import com.bytesforge.linkasanote.addeditaccount.nextcloud.NextcloudPresenterModule;
import com.bytesforge.linkasanote.data.source.ProviderModule;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.data.source.RepositoryModule;
import com.bytesforge.linkasanote.laano.ClipboardService;
import com.bytesforge.linkasanote.laano.LaanoComponent;
import com.bytesforge.linkasanote.laano.LaanoUiManagerModule;
import com.bytesforge.linkasanote.laano.favorites.FavoritesPresenterModule;
import com.bytesforge.linkasanote.laano.favorites.addeditfavorite.AddEditFavoriteComponent;
import com.bytesforge.linkasanote.laano.favorites.addeditfavorite.AddEditFavoritePresenterModule;
import com.bytesforge.linkasanote.laano.favorites.conflictresolution.FavoritesConflictResolutionComponent;
import com.bytesforge.linkasanote.laano.favorites.conflictresolution.FavoritesConflictResolutionPresenterModule;
import com.bytesforge.linkasanote.laano.links.LinksPresenterModule;
import com.bytesforge.linkasanote.laano.links.addeditlink.AddEditLinkComponent;
import com.bytesforge.linkasanote.laano.links.addeditlink.AddEditLinkPresenterModule;
import com.bytesforge.linkasanote.laano.links.conflictresolution.LinksConflictResolutionComponent;
import com.bytesforge.linkasanote.laano.links.conflictresolution.LinksConflictResolutionPresenterModule;
import com.bytesforge.linkasanote.laano.notes.NotesPresenterModule;
import com.bytesforge.linkasanote.laano.notes.addeditnote.AddEditNoteComponent;
import com.bytesforge.linkasanote.laano.notes.addeditnote.AddEditNotePresenterModule;
import com.bytesforge.linkasanote.laano.notes.conflictresolution.NotesConflictResolutionComponent;
import com.bytesforge.linkasanote.laano.notes.conflictresolution.NotesConflictResolutionPresenterModule;
import com.bytesforge.linkasanote.manageaccounts.ManageAccountsComponent;
import com.bytesforge.linkasanote.manageaccounts.ManageAccountsPresenterModule;
import com.bytesforge.linkasanote.settings.SettingsFragment;
import com.bytesforge.linkasanote.settings.SettingsModule;
import com.bytesforge.linkasanote.sync.SyncService;
import com.bytesforge.linkasanote.synclog.SyncLogComponent;
import com.bytesforge.linkasanote.synclog.SyncLogPresenterModule;
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

    @VisibleForTesting
    Repository getRepository();

    void inject(SyncService syncService);
    void inject(ClipboardService clipboardService);
    void inject(SettingsFragment settingsFragment);
    void inject(AboutFragment.LicenseTermsDialog licenseTermsDialog);

    // Subcomponents
    AddEditAccountComponent getAddEditAccountComponent(NextcloudPresenterModule module);
    AddEditFavoriteComponent getAddEditFavoriteComponent(AddEditFavoritePresenterModule module);
    AddEditLinkComponent getAddEditLinkComponent(AddEditLinkPresenterModule module);
    AddEditNoteComponent getAddEditNoteComponent(AddEditNotePresenterModule module);
    LaanoComponent getLaanoComponent(
            LinksPresenterModule linksPresenterModule,
            FavoritesPresenterModule favoritesPresenterModule,
            NotesPresenterModule notesPresenterModule,
            LaanoUiManagerModule laanoUiManagerModule);
    ManageAccountsComponent getManageAccountsComponent(ManageAccountsPresenterModule module);
    FavoritesConflictResolutionComponent getFavoritesConflictResolutionComponent(
            FavoritesConflictResolutionPresenterModule favoritesConflictResolutionPresenterModule);
    LinksConflictResolutionComponent getLinksConflictResolutionComponent(
            LinksConflictResolutionPresenterModule favoritesConflictResolutionPresenterModule);
    NotesConflictResolutionComponent getNotesConflictResolutionComponent(
            NotesConflictResolutionPresenterModule notesConflictResolutionPresenterModule);
    AboutComponent getAboutComponent(AboutPresenterModule module);
    SyncLogComponent getSyncLogComponent(SyncLogPresenterModule module);
}
