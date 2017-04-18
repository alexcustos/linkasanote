package com.bytesforge.linkasanote.laano.links;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.laano.FilterType;
import com.bytesforge.linkasanote.laano.LaanoFragmentPagerAdapter;
import com.bytesforge.linkasanote.laano.LaanoUiManager;
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

    public static final String SETTING_LINKS_FILTER_TYPE = "LINKS_FILTER_TYPE";

    private final Repository repository;
    private final LinksContract.View view;
    private final LinksContract.ViewModel viewModel;
    private final BaseSchedulerProvider schedulerProvider;
    private final LaanoUiManager laanoUiManager;
    private final Settings settings;

    @NonNull
    private final CompositeDisposable disposable;

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
        disposable = new CompositeDisposable();
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
        disposable.clear();
    }

    @Override
    public void onTabSelected() {
        loadLinks(false);
    }

    @Override
    public void onTabDeselected() {
        view.disableActionMode();
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
        disposable.clear();
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
                        laanoUiManager.setFilterType(
                                LaanoFragmentPagerAdapter.LINKS_TAB, filterType, favorite.getName());
                        return repository.getLinks();
                    });
        } else if (extendedFilter == FilterType.NOTE) {
            loadLinks = repository.getNote(noteFilter)
                    .toObservable()
                    .flatMap(note -> {
                        filterType = FilterType.NOTE;
                        noteFilter = note.getId();
                        String noteNote = note.getNote();
                        if (noteNote != null) {
                            noteNote = noteNote.split(System.lineSeparator(), 2)[0];
                        }
                        laanoUiManager.setFilterType(
                                LaanoFragmentPagerAdapter.LINKS_TAB, filterType, noteNote);
                        return repository.getLinks();
                    });
        }
        if (loadLinks != null) {
            loadLinks = loadLinks.doOnError(throwable -> {
                CommonUtils.logStackTrace(TAG, throwable);
                if (throwable instanceof NoSuchElementException) {
                    filterType = Settings.DEFAULT_FILTER_TYPE;
                    laanoUiManager.setFilterType(
                            LaanoFragmentPagerAdapter.LINKS_TAB, filterType, null);
                } else {
                    viewModel.showDatabaseErrorSnackbar();
                }
            }).onErrorResumeNext(repository.getLinks());
        } else {
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
                            return true; // TODO: search in link.getNotes
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
                    laanoUiManager.updateTitle(LaanoFragmentPagerAdapter.LINKS_TAB);
                })
                .subscribe(links -> {
                    view.showLinks(links);
                    selectLinkFilter();
                }, throwable -> {
                    // NullPointerException
                    CommonUtils.logStackTrace(TAG, throwable);
                    viewModel.showDatabaseErrorSnackbar();
                });
        this.disposable.add(disposable);
    }

    @Override
    public void onLinkClick(String linkId, boolean isConflicted) {
        if (viewModel.isActionMode()) {
            onLinkSelected(linkId);
        } else if (isConflicted) {
            view.showConflictResolution(linkId);
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

    private void selectLinkFilter() {
        if (viewModel.isActionMode()) return;

        String linkFilter = settings.getLinkFilter();
        if (linkFilter != null) {
            int position = getPosition(linkFilter);
            if (position >= 0) {
                viewModel.setSingleSelection(position, true);
                view.scrollToPosition(position);
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
    public void onDeleteClick() {
        int[] selectedIds = viewModel.getSelectedIds();
        view.confirmLinksRemoval(selectedIds);
    }

    @Override
    public void onSelectAllClick() {
        viewModel.toggleSelection();
    }

    @Override
    public void deleteLinks(int[] selectedIds) {
        for (int selectedId : selectedIds) {
            viewModel.removeSelection(selectedId);
            String linkId = view.removeLink(selectedId);
            try {
                repository.deleteLink(linkId);
            } catch (NullPointerException e) {
                viewModel.showDatabaseErrorSnackbar();
            }
        }
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
        String prevNoteFilter = this.noteFilter;
        this.noteFilter = settings.getNoteFilter();
        switch (filterType) {
            case ALL:
            case CONFLICTED:
            case NO_TAGS:
                if (this.filterType == filterType) return null;
                this.filterType = filterType;
                laanoUiManager.setFilterType(LaanoFragmentPagerAdapter.LINKS_TAB, filterType, null);
                break;
            case FAVORITE:
                if (this.filterType == filterType
                        && this.favoriteFilter != null
                        && this.favoriteFilter.equals(prevFavoriteFilter)) {
                    return null;
                }
                if (prevFavoriteFilter == null) {
                    this.filterType = Settings.DEFAULT_FILTER_TYPE;
                    laanoUiManager.setFilterType(
                            LaanoFragmentPagerAdapter.LINKS_TAB, this.filterType, null);
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
                if (prevNoteFilter == null) {
                    this.filterType = Settings.DEFAULT_FILTER_TYPE;
                    laanoUiManager.setFilterType(
                            LaanoFragmentPagerAdapter.LINKS_TAB, this.filterType, null);
                    return null;
                }
                this.filterType = filterType;
                return filterType;
            default:
                this.filterType = Settings.DEFAULT_FILTER_TYPE;
                laanoUiManager.setFilterType(
                        LaanoFragmentPagerAdapter.LINKS_TAB, this.filterType, null);
        }
        return null;
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
    public void updateTabNormalState() {
        laanoUiManager.setTabNormalState(LaanoFragmentPagerAdapter.LINKS_TAB, isConflicted());
    }
}
