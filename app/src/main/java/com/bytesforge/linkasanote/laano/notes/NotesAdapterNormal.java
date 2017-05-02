package com.bytesforge.linkasanote.laano.notes;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.databinding.ItemNotesNormalBinding;

import java.util.List;

public class NotesAdapterNormal extends NotesAdapterBase<NotesAdapterNormal.ViewHolder> {

    private static final String TAG = NotesAdapterNormal.class.getSimpleName();

    public NotesAdapterNormal(
            @NonNull List<Note> notes,
            @NonNull NotesContract.Presenter presenter,
            @NonNull NotesViewModel viewModel) {
        super(notes, presenter, viewModel);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemNotesNormalBinding binding;

        public ViewHolder(ItemNotesNormalBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(
                Note note, NotesContract.Presenter presenter,
                NotesViewModel viewModel) {
            binding.setNote(note);
            binding.setPresenter(presenter);
            binding.setViewModel(viewModel); // NOTE: global viewModel for fragment and all items

            binding.executePendingBindings();
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemNotesNormalBinding binding = ItemNotesNormalBinding.inflate(inflater, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Note note = notes.get(position);
        holder.bind(note, presenter, viewModel);
    }
}
