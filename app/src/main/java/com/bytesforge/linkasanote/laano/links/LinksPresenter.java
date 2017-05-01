package com.bytesforge.linkasanote.laano.links;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.laano.FilterType;
import com.bytesforge.linkasanote.laano.LaanoFragmentPagerAdapter;
import com.bytesforge.linkasanote.laano.LaanoUiManager;
import com.bytesforge.linkasanote.laano.links.conflictresolution.LinksConflictResolutionDialog;
import com.bytesforge.linkasanote.laano.notes.NotesPresenter;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.utils.CommonUtils;
import com.bytesforge.linkasanote.utils.EspressoIdlingResource;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.google.common.base.Strings;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

import static com.google.common.base.Preconditions.checkNotNull;

public final class LinksPresenter implements LinksContract.Presenter {

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
        viewModel.setPresenter(this);
        viewModel.setLaanoUiManager(laanoUiManager);
    }

    @Override
    public void subscribe() {
    }

    @Override
    public void unsubscribe() {
        compositeDisposable.clear();
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
    public void addLink() {
        view.showAddLink();
    }

    @Override
    public void loadLinks(final boolean forceUpdate) {
        loadLinks(forceUpdate || firstLoad, true);
        firstLoad = false;
    }

    private void loadLinks(boolean forceUpdate, final boolean showLoading) {
        EspressoIdlingResource.increment();
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
                        CommonUtils.logStackTrace(TAG, throwable);
                        if (throwable instanceof NoSuchElementException) {
                            setDefaultLinksFilterType();
                            favoriteFilter = null;
                            settings.setFavoriteFilter(null);
                        } else {
                            viewModel.showDatabaseErrorSnackbar();
                        }
                    }).onErrorResumeNext(repository.getLinks());
        } else if (extendedFilter == FilterType.NOTE) {
            loadLinks = repository.getNote(noteFilter)
                    .toObservable()
                    .flatMap(note -> {
                        filterType = FilterType.NOTE;
                        noteFilter = note.getId();
                        laanoUiManager.setFilterType(TAB, filterType, note.getNote());
                        return repository.getLinks();
                    }).doOnError(throwable -> {
                        CommonUtils.logStackTrace(TAG, throwable);
                        if (throwable instanceof NoSuchElementException) {
                            setDefaultLinksFilterType();
                            noteFilter = null;
                            settings.setNoteFilter(null);
                        } else {
                            viewModel.showDatabaseErrorSnackbar();
                        }
                    }).onErrorResumeNext(repository.getLinks());
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
                    if (!EspressoIdlingResource.getIdlingResource().isIdleNow()) {
                        EspressoIdlingResource.decrement();
                    }
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
            int position = getPosition(linkId);
            boolean selected = viewModel.toggleSingleSelection(position);
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
        int position = view.getPosition(linkId);
        viewModel.toggleSelection(position);
        view.selectionChanged(position);
    }

    @Override
    public void selectLinkFilter() {
        if (viewModel.isActionMode()) return;

        String linkFilter = settings.getLinkFilter();
        if (linkFilter != null) {
            int position = getPosition(linkFilter);
            if (position >= 0) {
                viewModel.setSingleSelection(position, true);
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
                }, throwable -> viewModel.showOpenLinkErrorSnackbar());
    }

    @Override
    public void onToNotesClick(@NonNull String linkId) {
        checkNotNull(linkId);
        int position = getPosition(linkId);
        viewModel.setSingleSelection(position, true);
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
        int position = getPosition(linkId);
        viewModel.toggleLinkVisibility(position);
        view.linkVisibilityChanged(position);
    }

    @Override
    public void onDeleteClick() {
        int[] selectedIds = viewModel.getSelectedIds();
        view.confirmLinksRemoval(selectedIds);
    }

    @Override
    public void onSelectAllClick() {
        viewModel.toggleSelection();
    }

    @Override
    public void deleteLinks(int[] selectedIds, boolean deleteNotes) {
        for (int selectedId : selectedIds) {
            viewModel.removeSelection(selectedId);
            String linkId = view.removeLink(selectedId);
            deleteLink(linkId, deleteNotes);
        }
    }

    private void deleteLink(final @NonNull String linkId, final boolean deleteNotes) {
        checkNotNull(linkId);
        repository.getLink(linkId)
                .subscribeOn(schedulerProvider.computation())
                .map(link -> {
                    if (deleteNotes) {
                        List<Note> notes = link.getNotes();
                        if (notes != null) {
                            for (Note note : notes) {
                                repository.deleteNote(note.getId());
                            }
                        }
                    }
                    return link;
                })
                .observeOn(schedulerProvider.ui())
                .subscribe(
                        link -> {
                            String id = link.getId();
                            repository.deleteLink(id);
                            settings.resetLinkFilter(id);
                        }, throwable -> {
                            if (throwable instanceof NullPointerException) {
                                viewModel.showDatabaseErrorSnackbar();
                            } // TODO: force Notes cleanup by linkId if Link has not been found
                        });
    }

    @Override
    public int getPosition(String linkId) {
        return view.getPosition(linkId);
    }

    @Override
    public boolean isConflicted() {
        return repository.isConflictedLinks().blockingGet();
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
        // NOTE: there may be some concurrency who actually will reset the filter, but it OK
        if (this.favoriteFilter != null) {
            repository.getFavorite(this.favoriteFilter)
                    .subscribeOn(schedulerProvider.computation())
                    .subscribe(favorite -> { /* OK */ }, throwable -> {
                        this.favoriteFilter = null;
                        settings.setFavoriteFilter(null);
                    });
        }
        String prevNoteFilter = this.noteFilter;
        this.noteFilter = settings.getNoteFilter();
        if (this.noteFilter != null) {
            repository.getNote(this.noteFilter)
                    .subscribeOn(schedulerProvider.computation())
                    .subscribe(favorite -> { /* OK */ }, throwable -> {
                        this.noteFilter = null;
                        settings.setNoteFilter(null);
                    });
        }
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
        laanoUiManager.setTabNormalState(TAB, isConflicted());
    }

    @Override
    public void setShowConflictResolutionWarning(boolean show) {
        settings.setShowConflictResolutionWarning(show);
    }
}
