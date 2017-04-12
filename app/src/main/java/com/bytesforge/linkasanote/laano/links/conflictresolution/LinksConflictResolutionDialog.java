package com.bytesforge.linkasanote.laano.links.conflictresolution;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;

import com.bytesforge.linkasanote.LaanoApplication;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.databinding.DialogLinkConflictResolutionBinding;

import java.util.NoSuchElementException;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;

public class LinksConflictResolutionDialog extends DialogFragment implements
        LinksConflictResolutionContract.View {

    public static final String ARGUMENT_LINK_ID = "LINK_ID";

    public static final String DIALOG_TAG = "CONFLICT_RESOLUTION";
    public static final int RESULT_OK = Activity.RESULT_OK;
    public static final int RESULT_FAILED = Activity.RESULT_FIRST_USER;

    @Inject
    LinksConflictResolutionPresenter presenter;

    LinksConflictResolutionContract.ViewModel viewModel;

    @Override
    public void onResume() {
        super.onResume();
        presenter.subscribe();
    }

    @Override
    public void onPause() {
        super.onPause();
        presenter.unsubscribe();
    }

    @Override
    public void setPresenter(@NonNull LinksConflictResolutionContract.Presenter presenter) {
        // NOTE: presenter is injected directly to this dialogFragment
    }

    @Override
    public void setViewModel(@NonNull LinksConflictResolutionContract.ViewModel viewModel) {
        this.viewModel = checkNotNull(viewModel);
    }

    @Override
    public boolean isActive() {
        return isAdded();
    }

    @Override
    public void finishActivity() {
        dismiss();
        getTargetFragment().onActivityResult(getTargetRequestCode(), RESULT_OK, null);
    }

    @Override
    public void cancelActivity() {
        dismiss();
        getTargetFragment().onActivityResult(getTargetRequestCode(), RESULT_FAILED, null);
    }

    public static LinksConflictResolutionDialog newInstance(@NonNull String linkId) {
        checkNotNull(linkId);

        Bundle args = new Bundle();
        args.putString(ARGUMENT_LINK_ID, linkId);
        LinksConflictResolutionDialog dialog = new LinksConflictResolutionDialog();
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String linkId = getArguments().getString(ARGUMENT_LINK_ID);
        // Presenter
        LaanoApplication application = (LaanoApplication) getActivity().getApplication();
        application.getApplicationComponent()
                .getLinksConflictResolutionComponent(
                        new LinksConflictResolutionPresenterModule(getContext(), this, linkId))
                .inject(this);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // TODO: move this check outside of the dialog creation
        try {
            boolean conflictResolved = presenter.autoResolve().blockingGet();
            if (conflictResolved) {
                setShowsDialog(false);
                finishActivity();
                return new Dialog(getContext());
            }
        } catch (NullPointerException | NoSuchElementException e) {
            cancelActivity();
            return new Dialog(getContext());
        }
        Context context = getActivity();
        LayoutInflater inflater = LayoutInflater.from(context);
        DialogLinkConflictResolutionBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.dialog_link_conflict_resolution, null, false);
        viewModel.setInstanceState(savedInstanceState);
        binding.setPresenter(presenter);
        binding.setViewModel((LinksConflictResolutionViewModel) viewModel);
        return new AlertDialog.Builder(getContext())
                .setView(binding.getRoot())
                .create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        viewModel.saveInstanceState(outState);
    }
}
