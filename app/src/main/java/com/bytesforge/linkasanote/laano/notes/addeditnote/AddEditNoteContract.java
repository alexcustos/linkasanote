package com.bytesforge.linkasanote.laano.notes.addeditnote;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.BasePresenter;
import com.bytesforge.linkasanote.BaseView;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.laano.TagsCompletionView;

import java.util.List;

public interface AddEditNoteContract {

    interface View extends BaseView<Presenter> {

        void setViewModel(@NonNull AddEditNoteContract.ViewModel viewModel);
        boolean isActive();
        void finishActivity();

        void swapTagsCompletionViewItems(List<Tag> tags);
    }

    interface ViewModel extends BaseView<Presenter> {

        void setInstanceState(@Nullable Bundle savedInstanceState);
        void saveInstanceState(Bundle outState);
        void applyInstanceState(@NonNull Bundle state);
        Bundle getDefaultInstanceState();

        void setTagsCompletionView(@NonNull TagsCompletionView completionView);
        void showEmptyNoteSnackbar();
        void showNoteNotFoundSnackbar();
        void showDuplicateKeyError();

        boolean isValid();
        void checkAddButton();
        void enableAddButton();
        void disableAddButton();
        void hideNoteError();
        void afterNoteChanged();

        void populateNote(@NonNull Note note);
    }

    interface Presenter extends BasePresenter {

        boolean isNewNote();
        void loadTags();
        void saveNote(String name, List<Tag> tags);
        void populateNote();
    }
}
