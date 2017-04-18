package com.bytesforge.linkasanote.laano.notes;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.databinding.ItemNotesBinding;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.ViewHolder> {

    private static final String TAG = NotesAdapter.class.getSimpleName();

    private final NotesContract.Presenter presenter;
    private final NotesViewModel viewModel;

    private List<Note> notes;
    private Map<String, Integer> positionMap;

    public NotesAdapter(
            @NonNull List<Note> notes,
            @NonNull NotesContract.Presenter presenter,
            @NonNull NotesViewModel viewModel) {
        this.notes = checkNotNull(notes);
        this.presenter = checkNotNull(presenter);
        this.viewModel = checkNotNull(viewModel);
        setHasStableIds(true);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemNotesBinding binding;

        public ViewHolder(ItemNotesBinding binding) {
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
        ItemNotesBinding binding = ItemNotesBinding.inflate(inflater, parent, false);

        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Note note = notes.get(position);
        holder.bind(note, presenter, viewModel);
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    @Override
    public long getItemId(int position) {
        return notes.get(position).getRowId();
    }

    // Items

    @NonNull
    public Note removeItem(int position) {
        Note note = notes.remove(position);
        updatePositionMap();
        notifyItemRemoved(position);
        return note;
    }

    public void swapItems(@NonNull List<Note> notes) {
        checkNotNull(notes);

        this.notes = notes;
        updatePositionMap();
        notifyDataSetChanged();
    }

    public int getPosition(@Nullable String noteId) {
        if (noteId == null) return -1;

        Integer position = positionMap.get(noteId);
        if (position == null) {
            Log.e(TAG, "No position is found for Note [" + noteId + "]");
            return -1;
        }
        return position;
    }

    private void updatePositionMap() {
        if (notes == null) return;

        int size = notes.size();
        if (positionMap == null) {
            positionMap = new LinkedHashMap<>(size);
        } else {
            positionMap.clear();
        }
        for (int i = 0; i < size; i++) {
            Note note = notes.get(i);
            positionMap.put(note.getId(), i);
        }
    } // updatePositionMap
}
