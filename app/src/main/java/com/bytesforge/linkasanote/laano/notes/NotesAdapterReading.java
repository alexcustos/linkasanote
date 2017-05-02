package com.bytesforge.linkasanote.laano.notes;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.databinding.ItemNotesReadingBinding;

import java.util.List;

public class NotesAdapterReading extends NotesAdapterBase<NotesAdapterReading.ViewHolder> {

    private static final String TAG = NotesAdapterReading.class.getSimpleName();

    public NotesAdapterReading(
            @NonNull List<Note> notes,
            @NonNull NotesContract.Presenter presenter,
            @NonNull NotesViewModel viewModel) {
        super(notes, presenter, viewModel);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemNotesReadingBinding binding;

        public ViewHolder(ItemNotesReadingBinding binding) {
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
        ItemNotesReadingBinding binding = ItemNotesReadingBinding.inflate(inflater, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Note note = notes.get(position);
        holder.bind(note, presenter, viewModel);
    }
}
