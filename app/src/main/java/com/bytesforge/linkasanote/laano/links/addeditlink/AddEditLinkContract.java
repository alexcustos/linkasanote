package com.bytesforge.linkasanote.laano.links.addeditlink;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.BasePresenter;
import com.bytesforge.linkasanote.BaseView;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.laano.ClipboardService;
import com.bytesforge.linkasanote.laano.TagsCompletionView;

import java.util.List;

public interface AddEditLinkContract {

    interface View extends BaseView<Presenter> {

        void setViewModel(@NonNull AddEditLinkContract.ViewModel viewModel);
        boolean isActive();
        void finishActivity(String linkId);

        void swapTagsCompletionViewItems(List<Tag> tags);
        void setLinkPaste(int clipboardType);
    }

    interface ViewModel extends BaseView<Presenter> {

        void setInstanceState(@Nullable Bundle savedInstanceState);
        void saveInstanceState(Bundle outState);
        void applyInstanceState(@NonNull Bundle state);
        Bundle getDefaultInstanceState();

        void setTagsCompletionView(@NonNull TagsCompletionView completionView);
        void showEmptyLinkSnackbar();
        void showLinkNotFoundSnackbar();
        void showDuplicateKeyError();

        boolean isValid();
        void checkAddButton();
        void enableAddButton();
        void disableAddButton();
        void hideLinkError();
        void afterLinkChanged();

        void populateLink(@NonNull Link link);

        void setLinkLink(String linkLink);
        void setLinkTags(String[] tags);
        void setLinkName(String linkName);
        void setStateLinkDisabled(boolean disabled);
    }

    interface Presenter extends BasePresenter, ClipboardService.Callback {

        boolean isNewLink();
        void loadTags();
        void saveLink(String link, String name, boolean disabled, List<Tag> tags);
        void populateLink();

        void setShowFillInFormInfo(boolean show);
        boolean isShowFillInFormInfo();
    }
}
