package com.bytesforge.linkasanote.laano.links;

import android.support.annotation.NonNull;
import android.util.Log;

import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.laano.LaanoFragmentPagerAdapter;
import com.bytesforge.linkasanote.laano.LaanoUiManager;
import com.bytesforge.linkasanote.utils.EspressoIdlingResource;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.google.common.base.Strings;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.inject.Inject;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public final class LinksPresenter implements LinksContract.Presenter {

    private static final String TAG = LinksPresenter.class.getSimpleName();

    private final Repository repository;
    private final LinksContract.View view;
    private final LinksContract.ViewModel viewModel;
    private final BaseSchedulerProvider schedulerProvider;
    private final LaanoUiManager laanoUiManager;

    @NonNull
    private final CompositeDisposable disposable;

    private boolean firstLoad = true;

    @Inject
    LinksPresenter(
            Repository repository, LinksContract.View view,
            LinksContract.ViewModel viewModel, BaseSchedulerProvider schedulerProvider,
            LaanoUiManager laanoUiManager) {
        this.repository = repository;
        this.view = view;
        this.viewModel = viewModel;
        this.schedulerProvider = schedulerProvider;
        this.laanoUiManager = laanoUiManager;
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
        loadLinks(false);
    }

    @Override
    public void unsubscribe() {
        disposable.clear();
    }

    @Override
    public void onTabSelected() {
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
        Disposable disposable = repository.getLinks()
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
                    switch (viewModel.getFilterType()) {
                        case LINKS_CONFLICTED:
                            return link.isConflicted();
                        case LINKS_ALL:
                        default:
                            Log.i(TAG, "Filter ALL");
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
                })
                .subscribe(view::showLinks, throwable -> {
                    // NullPointerException
                    StringWriter sw = new StringWriter();
                    throwable.printStackTrace(new PrintWriter(sw));
                    Log.e(TAG, throwable.toString());
                    viewModel.showDatabaseErrorSnackbar();
                });
        this.disposable.add(disposable);
    }

    @Override
    public void onLinkClick(String linkId, boolean isConflicted) {
        // TODO: normal mode selection must highlight current link filter
        if (viewModel.isActionMode()) {
            onLinkSelected(linkId);
        } else if (isConflicted){
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

    @Override
    public void onEditClick(@NonNull String linkId) {
        view.showEditLink(linkId);
    }

    @Override
    public void onLinkOpenClick(@NonNull String linkId) {
    }

    @Override
    public void onToNotesClick(@NonNull String linkId) {
    }

    @Override
    public void onAddNoteClick(@NonNull String linkId) {
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
    public void setFilterType(@NonNull LinksFilterType filterType) {
        viewModel.setFilterType(filterType);
        laanoUiManager.updateTitle(LaanoFragmentPagerAdapter.LINKS_TAB);
    }

    @Override
    public void updateTabNormalState() {
        laanoUiManager.setTabNormalState(
                LaanoFragmentPagerAdapter.LINKS_TAB, isConflicted());
    }
}
