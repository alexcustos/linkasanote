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
import com.bytesforge.linkasanote.sync.SyncState;

import java.util.List;

public interface AddEditLinkContract {

    interface View extends BaseView<Presenter> {

        void setViewModel(@NonNull AddEditLinkContract.ViewModel viewModel);
        boolean isActive();
        void finishActivity(String linkId);

        void swapTagsCompletionViewItems(List<Tag> tags);
        void setLinkPaste(int clipboardType);
        void fillInForm();
    }

    interface ViewModel extends BaseView<Presenter> {

        void setInstanceState(@Nullable Bundle savedInstanceState);
        void saveInstanceState(Bundle outState);
        void applyInstanceState(@NonNull Bundle state);
        Bundle getDefaultInstanceState();

        void setTagsCompletionView(@NonNull TagsCompletionView completionView);
        void showDatabaseErrorSnackbar();
        void showEmptyLinkSnackbar();
        void showLinkNotFoundSnackbar();
        void showDuplicateKeyError();
        void showTagsDuplicateRemovedToast();

        boolean isValid();
        boolean isEmpty();
        void checkAddButton();
        void enableAddButton();
        void disableAddButton();
        void hideLinkError();

        void populateLink(@NonNull Link link);

        void setLinkLink(String linkLink);
        void setLinkTags(String[] tags);
        void setLinkName(String linkName);
        void setStateLinkDisabled(boolean disabled);
        SyncState getLinkSyncState();
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
