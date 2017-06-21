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

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;

import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.data.Tag;
import com.bytesforge.linkasanote.databinding.DialogDoNotShowCheckboxBinding;
import com.bytesforge.linkasanote.databinding.FragmentAddEditNoteBinding;
import com.bytesforge.linkasanote.laano.ClipboardService;
import com.bytesforge.linkasanote.laano.TagsCompletionView;
import com.bytesforge.linkasanote.settings.Settings;
import com.bytesforge.linkasanote.utils.CommonUtils;
import com.tokenautocomplete.FilteredArrayAdapter;
import com.tokenautocomplete.TokenCompleteTextView;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class AddEditNoteFragment extends Fragment implements AddEditNoteContract.View {

    public static final String TAG = AddEditNoteFragment.class.getSimpleName();

    public static final String ARGUMENT_NOTE_ID = "NOTE_ID";
    public static final String ARGUMENT_RELATED_LINK_ID = "RELATED_LINK_ID";

    private Context context;
    private Resources resources;
    private AddEditNoteContract.Presenter presenter;
    private AddEditNoteContract.ViewModel viewModel;
    private FragmentAddEditNoteBinding binding;
    private ClipboardService clipboardService;

    private MenuItem notePasteMenuItem;
    private List<Tag> tags;

    public static AddEditNoteFragment newInstance() {
        return new AddEditNoteFragment();
    }

    @Override
    public void onStart() {
        super.onStart();
        bindClipboardService();
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.subscribe();
    }

    @Override
    public void onPause() {
        presenter.unsubscribe();
        super.onPause();
    }

    @Override
    public void onStop() {
        unbindClipboardService();
        super.onStop();
    }

    private void bindClipboardService() {
        Intent intent = new Intent(context, ClipboardService.class);
        context.bindService(intent, clipboardServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindClipboardService() {
        if (clipboardServiceConnection != null) {
            context.unbindService(clipboardServiceConnection);
        }
    }

    @Override
    public boolean isActive() {
        return isAdded();
    }

    @Override
    public void setPresenter(@NonNull AddEditNoteContract.Presenter presenter) {
        this.presenter = checkNotNull(presenter);
    }

    @Override
    public void setViewModel(@NonNull AddEditNoteContract.ViewModel viewModel) {
        this.viewModel = checkNotNull(viewModel);
    }

    @Override
    public void finishActivity(String noteId, String linkId) {
        Intent data = new Intent();
        if (noteId != null) {
            data.putExtra(ARGUMENT_NOTE_ID, noteId);
        }
        if (linkId != null) {
            data.putExtra(ARGUMENT_RELATED_LINK_ID, linkId);
        }
        FragmentActivity activity = getActivity();
        activity.setResult(Activity.RESULT_OK, data);
        activity.finish();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
        resources = context.getResources();
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_add_edit_note, container, false);
        viewModel.setInstanceState(savedInstanceState);
        binding.setViewModel((AddEditNoteViewModel) viewModel);
        if (savedInstanceState == null && !presenter.isNewNote()) {
            presenter.populateNote();
        }
        // NoteTags
        final TagsCompletionView completionView = binding.noteTags;
        if (completionView != null) {
            setupTagsCompletionView(completionView);
            viewModel.setTagsCompletionView(completionView);
        }
        return binding.getRoot();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.toolbar_add_edit_note, menu);
        notePasteMenuItem = menu.findItem(R.id.toolbar_note_paste);
        // NOTE: menu is created after onStart so there is a race condition here
        if (clipboardService != null) {
            setNotePaste(clipboardService.getClipboardState());
        } else {
            setNotePaste(ClipboardService.CLIPBOARD_EMPTY);
        }
    }

    @Override
    public void setNotePaste(int clipboardState) {
        if (notePasteMenuItem == null) return;

        switch (clipboardState) {
            case ClipboardService.CLIPBOARD_TEXT:
                notePasteMenuItem.setIcon(R.drawable.ic_content_paste_text_white);
                setNotePasteEnabled(true);
                break;
            case ClipboardService.CLIPBOARD_LINK:
                notePasteMenuItem.setIcon(R.drawable.ic_content_paste_link_white);
                setNotePasteEnabled(true);
                break;
            case ClipboardService.CLIPBOARD_EXTRA:
                notePasteMenuItem.setIcon(R.drawable.ic_content_paste_add_white);
                setNotePasteEnabled(true);
                break;
            case ClipboardService.CLIPBOARD_EMPTY:
            default:
                setNotePasteEnabled(false);
        }
    }

    private void setNotePasteEnabled(boolean enabled) {
        notePasteMenuItem.setEnabled(enabled);
        if (enabled) {
            notePasteMenuItem.getIcon().setAlpha(255);
        } else {
            notePasteMenuItem.getIcon().setAlpha((int) (255.0 * Settings.GLOBAL_ICON_ALPHA_DISABLED));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.toolbar_note_paste:
                if (clipboardService == null) {
                    setNotePaste(ClipboardService.CLIPBOARD_EMPTY);
                } else if (presenter.isShowFillInFormInfo()) {
                    showFillInFormInfo();
                } else {
                    fillInForm();
                }
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void fillInForm() {
        if (clipboardService == null) return;

        int clipboardState = clipboardService.getClipboardState();
        switch (clipboardState) {
            case ClipboardService.CLIPBOARD_TEXT:
            case ClipboardService.CLIPBOARD_LINK:
                viewModel.setNoteNote(clipboardService.getNormalizedClipboard());
                break;
            case ClipboardService.CLIPBOARD_EXTRA:
                viewModel.setNoteNote(clipboardService.getLinkDescription());
                viewModel.setNoteTags(clipboardService.getLinkKeywords());
                break;
            case ClipboardService.CLIPBOARD_EMPTY:
            default:
                setNotePaste(ClipboardService.CLIPBOARD_EMPTY);
        }
    }

    private void showFillInFormInfo() {
        FillInFormInfoDialog dialog = FillInFormInfoDialog.newInstance();
        dialog.setTargetFragment(this, FillInFormInfoDialog.DIALOG_REQUEST_CODE);
        dialog.show(getFragmentManager(), FillInFormInfoDialog.DIALOG_TAG);
    }

    // NOTE: callback from AlertDialog
    public void setFillInFormInfo(boolean show) {
        presenter.setShowFillInFormInfo(show);
    }

    private void setupTagsCompletionView(TagsCompletionView completionView) {
        // Options
        completionView.setTokenClickStyle(TokenCompleteTextView.TokenClickStyle.Select);
        completionView.setDeletionStyle(TokenCompleteTextView.TokenDeleteStyle.SelectThenDelete);
        completionView.allowCollapse(false);
        char[] splitChars = {','};
        completionView.setSplitChar(splitChars);
        completionView.allowDuplicates(false);
        completionView.performBestGuess(false);
        int threshold = Settings.GLOBAL_TAGS_AUTOCOMPLETE_THRESHOLD;
        completionView.setThreshold(threshold);
        completionView.setTokenListener((AddEditNotePresenter) presenter);
        // Adapter
        tags = new ArrayList<>();
        ArrayAdapter<Tag> adapter = new FilteredArrayAdapter<Tag>(
                getContext(), android.R.layout.simple_list_item_1, tags) {
            @Override
            protected boolean keepObject(Tag tag, String mask) {
                String name = tag.getName();
                return name != null
                        && name.toLowerCase().startsWith(mask.trim().toLowerCase())
                        && !completionView.getObjects().contains(tag);
            }
        };
        completionView.setAdapter(adapter);
        // Input filter
        InputFilter alphanumericFilter = (source, start, end, dest, dStart, dEnd) -> {
            if (source instanceof SpannableStringBuilder) {
                binding.noteTagsLayout.setError(null);
                return source;
            } else {
                StringBuilder filteredStringBuilder = new StringBuilder();
                for (int i = start; i < end; i++) {
                    char currentChar = source.charAt(i);
                    boolean spaceChar = Character.isSpaceChar(currentChar);
                    if (Character.isLetterOrDigit(currentChar) || spaceChar) {
                        filteredStringBuilder.append(currentChar);
                        binding.noteTagsLayout.setError(null);
                    } else {
                        binding.noteTagsLayout.setError(getResources().getString(
                                R.string.validation_error_tags_invalid_char));
                    }
                }
                return filteredStringBuilder.toString();
            }
        };
        InputFilter[] inputFilters = CommonUtils.arrayAdd(
                completionView.getFilters(), alphanumericFilter);
        completionView.setFilters(inputFilters);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        viewModel.saveInstanceState(outState);
    }

    @Override
    public void swapTagsCompletionViewItems(List<Tag> tags) {
        if (this.tags != null) {
            this.tags.clear();
            this.tags.addAll(tags);
        }
    }

    @Override
    public void setBoundTitle(boolean newNote) {
        ActionBar actionBar = ((AddEditNoteActivity) context).getSupportActionBar();
        if (actionBar == null) return;

        String title;
        if (newNote) {
            title = resources.getString(R.string.actionbar_title_new_note,
                    resources.getString(R.string.add_edit_note_message_link_bound_new));
        } else {
            title = resources.getString(R.string.actionbar_title_edit_note,
                    resources.getString(R.string.add_edit_note_message_link_bound_edit));
        }
        actionBar.setTitle(title);
    }

    @Override
    public void setUnboundTitle(boolean newNote) {
        ActionBar actionBar = ((AddEditNoteActivity) context).getSupportActionBar();
        if (actionBar == null) return;

        String title;
        if (newNote) {
            title = resources.getString(R.string.actionbar_title_new_note,
                    resources.getString(R.string.add_edit_note_message_link_unbound_new));
        } else {
            title = resources.getString(R.string.actionbar_title_edit_note,
                    resources.getString(R.string.add_edit_note_message_link_unbound_edit));
        }
        actionBar.setTitle(title);
    }

    private void startClipboardService() {
        Intent intent = new Intent(context.getApplicationContext(), ClipboardService.class);
        context.startService(intent);
    }

    private ServiceConnection clipboardServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ClipboardService.ClipboardBinder binder = (ClipboardService.ClipboardBinder) service;
            clipboardService = binder.getService();
            if (!clipboardService.isStartedByCommand()) {
                startClipboardService();
            }
            clipboardService.setCallback(presenter);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG, "Clipboard service has been unexpectedly disconnected");
            if (clipboardService != null) {
                clipboardService.setCallback(null);
                clipboardService = null;
            }
        }
    };

    public static class FillInFormInfoDialog extends DialogFragment {

        public static final String DIALOG_TAG = "NOTE_FILL_IN_FORM_INFO";
        public static final int DIALOG_REQUEST_CODE = 0;

        public static FillInFormInfoDialog newInstance() {
            return new FillInFormInfoDialog();
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = getContext();
            LayoutInflater inflater = LayoutInflater.from(context);
            DialogDoNotShowCheckboxBinding binding =
                    DialogDoNotShowCheckboxBinding.inflate(inflater, null, false);
            CheckBox checkBox = binding.doNotShowCheckbox;

            // NOTE: settings dialog destroy the application, so it will be hard to get back to the addEdit dialog
            return new AlertDialog.Builder(context)
                    .setView(binding.getRoot())
                    .setTitle(R.string.fill_in_form_info_title)
                    .setMessage(R.string.fill_in_form_info_message)
                    .setIcon(R.drawable.ic_info)
                    .setPositiveButton(R.string.dialog_button_continue, (dialog, which) -> {
                        AddEditNoteFragment fragment = (AddEditNoteFragment) getTargetFragment();
                        fragment.setFillInFormInfo(!checkBox.isChecked());
                        fragment.fillInForm();
                    })
                    .setNegativeButton(R.string.dialog_button_cancel, (dialog, which) -> {
                        AddEditNoteFragment fragment = (AddEditNoteFragment) getTargetFragment();
                        fragment.setFillInFormInfo(!checkBox.isChecked());
                    })
                    .create();
        }
    }
}
