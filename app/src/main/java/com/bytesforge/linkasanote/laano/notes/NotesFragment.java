package com.bytesforge.linkasanote.laano.notes;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bytesforge.linkasanote.BaseFragment;
import com.bytesforge.linkasanote.databinding.FragmentLaanoNotesBinding;

import static com.google.common.base.Preconditions.checkNotNull;

public class NotesFragment extends BaseFragment implements NotesContract.View {

    private NotesContract.Presenter presenter;

    public NotesFragment() {
        // Requires empty public constructor
    }

    public static NotesFragment newInstance() {
        return new NotesFragment();
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        FragmentLaanoNotesBinding binding =
                FragmentLaanoNotesBinding.inflate(inflater, container, false);

        binding.textView.setText(getTitle());

        return binding.getRoot();
    }

    @Override
    public void setPresenter(NotesContract.Presenter presenter) {
        this.presenter = checkNotNull(presenter);
    }

    @Override
    public boolean isActive() {
        return isAdded();
    }
}