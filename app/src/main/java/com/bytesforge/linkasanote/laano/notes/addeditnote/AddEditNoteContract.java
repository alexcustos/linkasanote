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

package com.bytesforge.linkasanote.laano.notes.addeditnote;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bytesforge.linkasanote.BasePresenter;
import com.bytesforge.linkasanote.BaseView;
import com.bytesforge.linkasanote.data.Link;
import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.laano.ClipboardService;
import com.bytesforge.linkasanote.laano.TagsCompletionView;
import com.bytesforge.linkasanote.sync.SyncState;

import java.util.List;

public interface AddEditNoteContract {

    interface View extends BaseView<Presenter> {

        void setViewModel(@NonNull AddEditNoteContract.ViewModel viewModel);
        boolean isActive();
        void finishActivity(String noteId, String linkId);

        void swapTagsCompletionViewItems(List<Tag> tags);
        void setBoundTitle(boolean newNote);
        void setUnboundTitle(boolean newNote);
        void setNotePaste(int clipboardState);
        void fillInForm();
    }

    interface ViewModel extends BaseView<Presenter> {

        void setInstanceState(@Nullable Bundle savedInstanceState);
        void saveInstanceState(Bundle outState);
        void applyInstanceState(@NonNull Bundle state);
        Bundle getDefaultInstanceState();

        void setTagsCompletionView(@NonNull TagsCompletionView completionView);
        void showDatabaseErrorSnackbar();
        void showEmptyNoteSnackbar();
        void showNoteNotFoundSnackbar();
        void showTagsDuplicateRemovedToast();
        void showLinkStatusNoteWillBeUnbound();
        void showLinkStatusLoading();
        void hideLinkStatus();

        boolean isValid();
        boolean isEmpty();
        void checkAddButton();
        void enableAddButton();
        void disableAddButton();
        void hideNoteError();

        void populateNote(@NonNull Note note);
        void populateLink(@NonNull Link link);

        void setNoteNote(String noteNote);
        void setNoteTags(String[] tags);
        SyncState getNoteSyncState();
    }

    interface Presenter extends BasePresenter, ClipboardService.Callback {

        boolean isNewNote();
        void loadTags();
        void saveNote(String name, List<Tag> tags);
        void populateNote();

        void setShowFillInFormInfo(boolean show);
        boolean isShowFillInFormInfo();
    }
}
