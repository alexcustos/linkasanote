package com.bytesforge.linkasanote.laano.links;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.DataSource;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.laano.BaseItemPresenter;
import com.bytesforge.linkasanote.laano.FilterType;
import com.bytesforge.linkasanote.laano.LaanoFragmentPagerAdapter;
import com.bytesforge.linkasanote.laano.LaanoUiManager;
import com.bytesforge.linkasanote.laano.links.conflictresolution.LinksConflictResolutionDialog;
import com.bytesforge.linkasanote.laano.notes.NotesPresenter;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.sync.SyncAdapter;
import com.bytesforge.linkasanote.utils.CommonUtils;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

import static com.google.common.base.Preconditions.checkNotNull;

public final class LinksPresenter extends BaseItemPresenter implements
        LinksContract.Presenter, DataSource.Callback {

    private static final String TAG = LinksPresenter.class.getSimpleName();
    private static final int TAB = LaanoFragmentPagerAdapter.LINKS_TAB;

    public static final String SETTING_LINKS_FILTER_TYPE = "LINKS_FILTER_TYPE";

    private final Repository repository;
    private final LinksContract.View view;
    private final LinksContract.ViewModel viewModel;
    private final BaseSchedulerProvider schedulerProvider;
    private final LaanoUiManager laanoUiManager;
    private final Settings settings;

    @NonNull
    private final CompositeDisposable compositeDisposable;

    private String favoriteFilter;
    private String noteFilter;
    private List<Tag> favoriteFilterTags;
    private FilterType filterType;
    private boolean firstLoad = true;

    @Inject
    LinksPresenter(
            Repository repository, LinksContract.View view,
            LinksContract.ViewModel viewModel, BaseSchedulerProvider schedulerProvider,
            LaanoUiManager laanoUiManager, Settings settings) {
        this.repository = repository;
        this.view = view;
        this.viewModel = viewModel;
        this.schedulerProvider = schedulerProvider;
        this.laanoUiManager = laanoUiManager;
        this.settings = settings;
        compositeDisposable = new CompositeDisposable();
    }

    @Inject
    void setupView() {
        view.setPresenter(this);
        view.setViewModel(viewModel);
    }

    @Override
    public void subscribe() {
        repository.addLinksCallback(this);
    }

    @Override
    public void unsubscribe() {
        compositeDisposable.clear();
        repository.removeLinksCallback(this);
    }

    @Override
    public void onTabSelected() {
        loadLinks(false);
    }

    @Override
    public void onTabDeselected() {
        view.finishActionMode();
    }

    @Override
    public void showAddLink() {
        view.startAddLinkActivity();
    }

    @Override
    public void loadLinks(final boolean forceUpdate) {
        loadLinks(forceUpdate || firstLoad, true);
        firstLoad = false;
    }

    private void loadLinks(boolean forceUpdate, final boolean showLoading) {
        compositeDisposable.clear();
        if (forceUpdate) {
            repository.refreshLinks();
        }
        if (showLoading) {
            viewModel.showProgressOverlay();
        }
        FilterType extendedFilter = updateFilter();
        Observable<Link> loadLinks = null;
        if (extendedFilter == FilterType.FAVORITE) {
            loadLinks = repository.getFavorite(favoriteFilter)
                    .toObservable()
                    .flatMap(favorite -> {
                        // NOTE: just to be sure we are still in sync
                        filterType = FilterType.FAVORITE;
                        favoriteFilter = favorite.getId();
                        favoriteFilterTags = favorite.getTags();
                        laanoUiManager.setFilterType(TAB, filterType, favorite.getName());
                        return repository.getLinks();
                    }).doOnError(throwable -> {
                        if (throwable instanceof NoSuchElementException) {
                            setDefaultLinksFilterType();
                            favoriteFilter = null;
                            settings.setFavoriteFilter(null);
                        } else {
                            CommonUtils.logStackTrace(TAG, throwable);
                        }
                    }).onErrorResumeNext(throwable -> {
                        if (throwable instanceof NoSuchElementException) {
                            return repository.getLinks();
                        } else {
                            return Observable.empty();
                        }
                    });
        } else if (extendedFilter == FilterType.NOTE) {
            loadLinks = repository.getNote(noteFilter)
                    .toObservable()
                    .flatMap(note -> {
                        filterType = FilterType.NOTE;
                        noteFilter = note.getId();
                        laanoUiManager.setFilterType(TAB, filterType, note.getNote());
                        return repository.getLinks();
                    }).doOnError(throwable -> {
                        if (throwable instanceof NoSuchElementException) {
                            setDefaultLinksFilterType();
                            noteFilter = null;
                            settings.setNoteFilter(null);
                        } else {
                            CommonUtils.logStackTrace(TAG, throwable);
                        }
                    }).onErrorResumeNext(throwable -> {
                        if (throwable instanceof NoSuchElementException) {
                            return repository.getLinks();
                        } else {
                            return Observable.empty();
                        }
                    });
        }
        if (loadLinks == null) {
            loadLinks = repository.getLinks();
        }
        Disposable disposable = loadLinks
                .subscribeOn(schedulerProvider.computation())
                .filter(link -> {
                    String searchText = viewModel.getSearchText();
                    if (!Strings.isNullOrEmpty(searchText)) {
                        boolean found = false;
                        searchText = searchText.toLowerCase();
                        String linkName = link.getName();
                        if (linkName != null && linkName.toLowerCase().contains(searchText)) {
                            found = true;
                        }
                        String linkLink = link.getLink();
                        if (linkLink != null && linkLink.toLowerCase().contains(searchText)) {
                            found = true;
                        }
                        if (!found) return false;
                    }
                    // OPTIMIZATION: query the filtered data set will be more efficient
                    switch (filterType) {
                        case CONFLICTED:
                            return link.isConflicted();
                        case FAVORITE:
                            List<Tag> linkTags = link.getTags();
                            if (favoriteFilter == null) {
                                return true; // No filter
                            } else if (linkTags == null) {
                                return false; // No tags
                            }
                            return !Collections.disjoint(favoriteFilterTags, linkTags);
                        case NOTE:
                            List<Note> notes = link.getNotes();
                            if (notes == null) return false;
                            for (Note note : notes) {
                                if (note.getId().equals(noteFilter)) return true;
                            }
                            return false;
                        case NO_TAGS:
                            return link.getTags() == null;
                        case ALL:
                        default:
                            return true;
                    }
                })
                .toList()
                .observeOn(schedulerProvider.ui())
                .doFinally(() -> {
                    if (showLoading) {
                        viewModel.hideProgressOverlay();
                    }
                    laanoUiManager.updateTitle(TAB);
                })
                .subscribe(links -> {
                    view.showLinks(links);
                    selectLinkFilter();
                }, throwable -> {
                    // NullPointerException
                    CommonUtils.logStackTrace(TAG, throwable);
                    viewModel.showDatabaseErrorSnackbar();
                });
        compositeDisposable.add(disposable);
    }

    @Override
    public void onLinkClick(String linkId, boolean isConflicted) {
        if (viewModel.isActionMode()) {
            onLinkSelected(linkId);
        } else if (isConflicted) {
            repository.autoResolveLinkConflict(linkId)
                    .subscribeOn(schedulerProvider.computation())
                    .observeOn(schedulerProvider.ui())
                    .subscribe(success -> {
                        if (success) {
                            view.onActivityResult(
                                    LinksFragment.REQUEST_LINK_CONFLICT_RESOLUTION,
                                    LinksConflictResolutionDialog.RESULT_OK, null);
                        } else {
                            if (settings.isShowConflictResolutionWarning()) {
                                view.showConflictResolutionWarning(linkId);
                            } else {
                                view.showConflictResolution(linkId);
                            }
                        }
                    }, throwable -> {
                        if (throwable instanceof NullPointerException) {
                            viewModel.showDatabaseErrorSnackbar();
                        } else {
                            // NOTE: if there is any problem show it in the dialog
                            view.showConflictResolution(linkId);
                        }
                    });
        } else if (Settings.GLOBAL_ITEM_CLICK_SELECT_FILTER) {
            boolean selected = viewModel.toggleSingleSelection(linkId);
            // NOTE: filterType will be updated accordingly on the tab
            if (selected) {
                settings.setLinkFilter(linkId);
            } else {
                settings.setLinkFilter(null);
            }
        }
    }

    @Override
    public boolean onLinkLongClick(String linkId) {
        view.enableActionMode();
        onLinkSelected(linkId);
        return true;
    }

    @Override
    public void onCheckboxClick(String linkId) {
        onLinkSelected(linkId);
    }

    private void onLinkSelected(String linkId) {
        viewModel.toggleSelection(linkId);
        view.selectionChanged(linkId);
    }

    @Override
    public void selectLinkFilter() {
        if (viewModel.isActionMode()) return;

        String linkFilter = settings.getLinkFilter();
        if (linkFilter != null) {
            int position = getPosition(linkFilter);
            if (position >= 0) {
                viewModel.setSingleSelection(linkFilter, true);
                //view.scrollToPosition(position);
            }
        }
    }

    @Override
    public void onEditClick(@NonNull String linkId) {
        view.showEditLink(linkId);
    }

    @Override
    public void onLinkOpenClick(@NonNull String linkId) {
        repository.getLink(checkNotNull(linkId))
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .subscribe(link -> {
                    Uri uri = Uri.parse(link.getLink());
                    view.openLink(uri);
                }, throwable -> {
                    CommonUtils.logStackTrace(TAG, throwable);
                    viewModel.showOpenLinkErrorSnackbar();
                });
    }

    @Override
    public void onToNotesClick(@NonNull String linkId) {
        checkNotNull(linkId);
        viewModel.setSingleSelection(linkId, true);
        settings.setFilterType(NotesPresenter.SETTING_NOTES_FILTER_TYPE, FilterType.LINK);
        settings.setLinkFilter(linkId);
        laanoUiManager.setCurrentTab(LaanoFragmentPagerAdapter.NOTES_TAB);
    }

    @Override
    public void onAddNoteClick(@NonNull String linkId) {
        view.showAddNote(linkId);
    }

    @Override
    public void onToggleClick(@NonNull String linkId) {
        checkNotNull(linkId);
        viewModel.toggleVisibility(linkId);
        view.visibilityChanged(linkId);
    }

    @Override
    public void onDeleteClick() {
        ArrayList<String> selectedIds = viewModel.getSelectedIds();
        view.confirmLinksRemoval(selectedIds);
    }

    @Override
    public void onSelectAllClick() {
        String[] ids = view.getIds();
        int listSize = ids.length;
        if (listSize <= 0) return;

        int selectedCount = viewModel.getSelectedCount();
        if (selectedCount > listSize / 2) {
            viewModel.setSelection(null);
        } else {
            viewModel.setSelection(ids);
        }
    }

    @Override
    public void syncSavedLink(@NonNull final String linkId) {
        checkNotNull(linkId);
        boolean sync = settings.isSyncable() && settings.isOnline();
        if (!sync) {
            if (settings.isSyncable()) {
                settings.setSyncStatus(SyncAdapter.SYNC_STATUS_UNSYNCED);
                laanoUiManager.updateSyncStatus();
            }
            return;
        }
        repository.syncSavedLink(linkId)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .doFinally(this::updateSyncStatus)
                .subscribe(itemState -> {
                    Log.d(TAG, "syncSavedLink() -> subscribe(): [" + itemState.name() + "]");
                    switch (itemState) {
                        case CONFLICTED:
                            updateTabNormalState();
                            loadLinks(false);
                            laanoUiManager.showLongToast(R.string.toast_sync_conflict);
                            break;
                        case ERROR_CLOUD:
                            settings.setSyncStatus(SyncAdapter.SYNC_STATUS_ERROR);
                            laanoUiManager.showLongToast(R.string.toast_sync_error);
                            break;
                        case SAVED:
                            updateTabNormalState();
                            loadLinks(false);
                            laanoUiManager.showShortToast(R.string.toast_sync_success);
                            break;
                    }
                }, throwable -> CommonUtils.logStackTrace(TAG, throwable));
    }

    @Override
    public void syncSavedNote(@NonNull final String linkId, @NonNull final String noteId) {
        checkNotNull(linkId);
        checkNotNull(noteId);
        // NOTE: repository do not control other Item's cache
        repository.refreshLink(linkId); // saved
        boolean sync = settings.isSyncable() && settings.isOnline();
        if (!sync) {
            if (settings.isSyncable()) {
                settings.setSyncStatus(SyncAdapter.SYNC_STATUS_UNSYNCED);
                laanoUiManager.updateSyncStatus();
            }
            return;
        }
        repository.syncSavedNote(noteId)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .doFinally(this::updateSyncStatus)
                .subscribe(itemState -> {
                    Log.d(TAG, "syncSavedNote() -> subscribe(): [" + itemState.name() + "]");
                    switch (itemState) {
                        case CONFLICTED:
                            updateNotesTabNormalState();
                            repository.refreshLink(linkId); // synced
                            // NOTE: it is needless to force loadNotes, because conflicted state is practically impossible (UUID constraint)
                            loadLinks(false);
                            laanoUiManager.showLongToast(R.string.toast_sync_conflict);
                            break;
                        case ERROR_CLOUD:
                            settings.setSyncStatus(SyncAdapter.SYNC_STATUS_ERROR);
                            laanoUiManager.showLongToast(R.string.toast_sync_error);
                            break;
                        case SAVED:
                            updateNotesTabNormalState();
                            repository.refreshLink(linkId); // synced
                            loadLinks(false);
                            laanoUiManager.showShortToast(R.string.toast_sync_success);
                            break;
                    }
                }, throwable -> CommonUtils.logStackTrace(TAG, throwable));
    }

    @Override
    public void deleteLinks(
            @NonNull final ArrayList<String> selectedIds, final boolean deleteNotes) {
        checkNotNull(selectedIds);
        boolean sync = settings.isSyncable() && settings.isOnline();
        Observable.fromIterable(selectedIds)
                .flatMap(linkId -> {
                    Log.d(TAG, "deleteLinks(): [" + linkId + "]");
                    return repository.deleteLink(linkId, sync, deleteNotes)
                            .subscribeOn(schedulerProvider.io())
                            .observeOn(schedulerProvider.ui())
                            .doOnNext(itemState -> {
                                Log.d(TAG, "deleteLink() -> doOnNext(): [" + itemState.name() + "]");
                                if (itemState == DataSource.ItemState.DELETED
                                        || itemState == DataSource.ItemState.DEFERRED) {
                                    // NOTE: can be called twice
                                    view.removeLink(linkId);
                                    settings.resetLinkFilter(linkId);
                                }
                            });
                })
                .filter(itemState -> itemState == DataSource.ItemState.CONFLICTED
                        || itemState == DataSource.ItemState.ERROR_LOCAL
                        || itemState == DataSource.ItemState.ERROR_CLOUD
                        || itemState == DataSource.ItemState.ERROR_EXTRA)
                .toList()
                .doFinally(() -> {
                    if (sync) {
                        this.updateSyncStatus();
                    } else if (settings.isSyncable()) {
                        settings.setSyncStatus(SyncAdapter.SYNC_STATUS_UNSYNCED);
                        laanoUiManager.updateSyncStatus();
                    }
                })
                .subscribe(itemStates -> {
                    Log.d(TAG, "deleteLinks(): Completed [" + itemStates.toString() + "]");
                    if (itemStates.isEmpty()) {
                        // DELETED or DEFERRED if sync is disabled
                        //viewModel.showDeleteSuccessSnackbar();
                        if (sync) {
                            laanoUiManager.showShortToast(R.string.toast_sync_success);
                        }
                    } else if (itemStates.contains(DataSource.ItemState.CONFLICTED)) {
                        laanoUiManager.showLongToast(R.string.toast_sync_conflict);
                    } else if (itemStates.contains(DataSource.ItemState.ERROR_LOCAL)) {
                        viewModel.showDatabaseErrorSnackbar();
                    } else if (itemStates.contains(DataSource.ItemState.ERROR_CLOUD)) {
                        settings.setSyncStatus(SyncAdapter.SYNC_STATUS_ERROR);
                        laanoUiManager.showLongToast(R.string.toast_sync_error);
                    } else if (itemStates.contains(DataSource.ItemState.ERROR_EXTRA)) {
                        // TODO: remove error_extra and treat the extra as a normal Link
                        laanoUiManager.showLongToast(R.string.toast_error);
                    }
                    loadLinks(false);
                    updateTabNormalState();
                }, throwable -> CommonUtils.logStackTrace(TAG, throwable));
    }

    @Override
    public void updateSyncStatus() {
        int status = settings.getSyncStatus();
        if (status == SyncAdapter.SYNC_STATUS_ERROR
                || status == SyncAdapter.SYNC_STATUS_UNSYNCED) {
            // NOTE: only SyncAdapter can reset these statuses
            laanoUiManager.updateSyncStatus();
            return;
        }
        repository.getSyncStatus()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe(syncStatus -> {
                    settings.setSyncStatus(syncStatus);
                    laanoUiManager.updateSyncStatus();
                }, throwable -> {
                    CommonUtils.logStackTrace(TAG, throwable);
                    viewModel.showDatabaseErrorSnackbar();
                });
    }

    @Override
    public int getPosition(String linkId) {
        return view.getPosition(linkId);
    }

    @Override
    public void setFilterType(@NonNull FilterType filterType) {
        checkNotNull(filterType);
        settings.setFilterType(SETTING_LINKS_FILTER_TYPE, filterType);
        if (this.filterType != filterType) {
            loadLinks(false);
        }
    }

    /**
     * @return Return null if there is no additional data is required
     */
    @Nullable
    private FilterType updateFilter() {
        FilterType filterType = settings.getFilterType(SETTING_LINKS_FILTER_TYPE);
        String prevFavoriteFilter = this.favoriteFilter;
        this.favoriteFilter = settings.getFavoriteFilter();
        String prevNoteFilter = this.noteFilter;
        this.noteFilter = settings.getNoteFilter();
        switch (filterType) {
            case ALL:
            case CONFLICTED:
            case NO_TAGS:
                if (this.filterType == filterType) return null;
                this.filterType = filterType;
                laanoUiManager.setFilterType(TAB, filterType, null);
                break;
            case FAVORITE:
                if (this.filterType == filterType
                        && this.favoriteFilter != null
                        && this.favoriteFilter.equals(prevFavoriteFilter)) {
                    return null;
                }
                if (this.favoriteFilter == null) {
                    setDefaultLinksFilterType();
                    return null;
                }
                this.filterType = filterType;
                return filterType;
            case NOTE:
                if (this.filterType == filterType
                        && this.noteFilter != null
                        && this.noteFilter.equals(prevNoteFilter)) {
                    return null;
                }
                if (this.noteFilter == null) {
                    setDefaultLinksFilterType();
                    return null;
                }
                this.filterType = filterType;
                return filterType;
            default:
                setDefaultLinksFilterType();
        }
        return null;
    }

    private void setDefaultLinksFilterType() {
        filterType = Settings.DEFAULT_FILTER_TYPE;
        laanoUiManager.setFilterType(TAB, filterType, null);
        settings.setFilterType(SETTING_LINKS_FILTER_TYPE, filterType);
    }

    @Override
    public boolean isFavoriteFilter() {
        return favoriteFilter != null;
    }

    @Override
    public boolean isNoteFilter() {
        return noteFilter != null;
    }

    @Override
    public boolean isExpandLinks() {
        return settings.isExpandLinks();
    }

    @Override
    public void updateTabNormalState() {
        repository.isConflictedLinks()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe(
                        conflicted -> laanoUiManager.setTabNormalState(TAB, conflicted),
                        throwable -> {
                            CommonUtils.logStackTrace(TAG, throwable);
                            viewModel.showDatabaseErrorSnackbar();
                        });
    }

    private void updateNotesTabNormalState() {
        repository.isConflictedNotes()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe(
                        conflicted -> laanoUiManager.setTabNormalState(
                                LaanoFragmentPagerAdapter.NOTES_TAB, conflicted),
                        throwable -> {
                            CommonUtils.logStackTrace(TAG, throwable);
                            viewModel.showDatabaseErrorSnackbar();
                        });
    }

    @Override
    public void setShowConflictResolutionWarning(boolean show) {
        settings.setShowConflictResolutionWarning(show);
    }

    @Override
    public void onRepositoryDelete(@NonNull String id, @NonNull DataSource.ItemState itemState) {
        if (itemState == DataSource.ItemState.SAVED) {
            throw new IllegalStateException("SAVED state is not allowed to Delete operation");
        }
    }

    @Override
    public void onRepositorySave(@NonNull String id, @NonNull DataSource.ItemState itemState) {
        if (itemState == DataSource.ItemState.DELETED) {
            throw new IllegalStateException("DELETED state is not allowed to Save operation");
        }
    }
}
