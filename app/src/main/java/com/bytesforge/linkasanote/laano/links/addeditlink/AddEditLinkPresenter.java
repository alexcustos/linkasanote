package com.bytesforge.linkasanote.laano.links.addeditlink;

import android.database.sqlite.SQLiteConstraintException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.data.source.Repository;
import com.bytesforge.linkasanote.laano.ClipboardService;
import com.bytesforge.linkasanote.laano.links.LinkId;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.utils.CommonUtils;
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider;
import com.tokenautocomplete.TokenCompleteTextView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

import static com.google.common.base.Preconditions.checkNotNull;

public final class AddEditLinkPresenter implements
        AddEditLinkContract.Presenter, TokenCompleteTextView.TokenListener<Tag> {

    private static final String TAG = AddEditLinkPresenter.class.getSimpleName();

    private final Repository repository;
    private final AddEditLinkContract.View view;
    private final AddEditLinkContract.ViewModel viewModel;
    private final BaseSchedulerProvider schedulerProvider;
    private final Settings settings;

    private String linkId; // NOTE: can be reset to null if NoSuchElementException

    @NonNull
    private final CompositeDisposable tagsDisposable;

    @NonNull
    private final CompositeDisposable linkDisposable;

    @Inject
    AddEditLinkPresenter(
            Repository repository, AddEditLinkContract.View view,
            AddEditLinkContract.ViewModel viewModel, BaseSchedulerProvider schedulerProvider,
            Settings settings, @Nullable @LinkId String linkId) {
        this.repository = repository;
        this.view = view;
        this.viewModel = viewModel;
        this.schedulerProvider = schedulerProvider;
        this.settings = settings;
        this.linkId = linkId;
        tagsDisposable = new CompositeDisposable();
        linkDisposable = new CompositeDisposable();
    }

    @Inject
    void setupView() {
        view.setPresenter(this);
        view.setViewModel(viewModel);
        viewModel.setPresenter(this);
    }

    @Override
    public void subscribe() {
        loadTags();
    }

    @Override
    public void unsubscribe() {
        tagsDisposable.clear();
        linkDisposable.clear();
    }

    @Override
    public void loadTags() {
        tagsDisposable.clear(); // stop previous requests

        Disposable disposable = repository.getTags()
                .subscribeOn(schedulerProvider.computation())
                .toList()
                .observeOn(schedulerProvider.ui())
                .subscribe(view::swapTagsCompletionViewItems, throwable -> {
                    CommonUtils.logStackTrace(TAG, throwable);
                    view.swapTagsCompletionViewItems(new ArrayList<>());
                });
        tagsDisposable.add(disposable);
    }

    @Override
    public boolean isNewLink() {
        return linkId == null;
    }

    @Override
    public void populateLink() {
        if (linkId == null) {
            throw new RuntimeException("populateLink() was called but linkId is null");
        }
        linkDisposable.clear();

        Disposable disposable = repository.getLink(linkId)
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .subscribe(viewModel::populateLink, throwable -> {
                    linkId = null;
                    viewModel.showLinkNotFoundSnackbar();
                });
        linkDisposable.add(disposable);
    }

    @Override
    public void saveLink(
            String linkLink, String linkName, boolean linkDisabled, List<Tag> linkTags) {
        if (isNewLink()) {
            createLink(linkLink, linkName, linkDisabled, linkTags);
        } else {
            updateLink(linkLink, linkName, linkDisabled, linkTags);
        }
    }

    private void createLink(
            String linkLink, String linkName, boolean linkDisabled, List<Tag> linkTags) {
        saveLink(new Link(linkLink, linkName, linkDisabled, linkTags));
    }

    private void updateLink(
            String linkLink, String linkName, boolean linkDisabled, List<Tag> linkTags) {
        if (linkId == null) {
            throw new RuntimeException("updateLink() was called but linkId is null");
        }
        // NOTE: state eTag will NOT be overwritten if null
        saveLink(new Link(linkId, linkLink, linkName, linkDisabled, linkTags)); // UNSYNCED
    }

    private void saveLink(@NonNull final Link link) {
        checkNotNull(link);
        if (link.isEmpty()) {
            viewModel.showEmptyLinkSnackbar();
            return;
        }
        final String linkId = link.getId();
        repository.saveLink(link, false) // sync after save
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribe(itemState -> {
                    switch (itemState) {
                        case DEFERRED:
                            view.finishActivity(linkId);
                            break;
                    }
                }, throwable -> {
                    if (throwable instanceof SQLiteConstraintException) {
                        viewModel.showDuplicateKeyError();
                    } else {
                        CommonUtils.logStackTrace(TAG, throwable);
                        viewModel.showDatabaseErrorSnackbar();
                    }
                });
    }

    @Override
    public void onClipboardChanged(int clipboardType) {
        view.setLinkPaste(clipboardType);
        // NOTE: if data is ready either this method or linkExtraReady is called
        if (viewModel.isEmpty() && settings.isClipboardFillInForms()) {
            view.fillInForm();
        }
    }

    @Override
    public void onClipboardLinkExtraReady() {
        view.setLinkPaste(ClipboardService.CLIPBOARD_EXTRA);
        if (viewModel.isEmpty() && settings.isClipboardFillInForms()) {
            view.fillInForm();
        }
    }

    @Override
    public void setShowFillInFormInfo(boolean show) {
        settings.setShowFillInFormInfo(show);
    }

    @Override
    public boolean isShowFillInFormInfo() {
        return settings.isShowFillInFormInfo();
    }

    // Tags

    @Override
    public void onTokenAdded(Tag token) {
    }

    @Override
    public void onTokenRemoved(Tag token) {
    }

    @Override
    public void onDuplicateRemoved(Tag token) {
        viewModel.showTagsDuplicateRemovedToast();
    }
}
