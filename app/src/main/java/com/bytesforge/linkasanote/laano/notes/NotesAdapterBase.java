package com.bytesforge.linkasanote.laano.notes;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ViewGroup;

import com.bytesforge.linkasanote.data.Note;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class NotesAdapterBase<VH extends RecyclerView.ViewHolder> extends
        RecyclerView.Adapter<VH> {

    private static final String TAG = NotesAdapterBase.class.getSimpleName();

    protected final NotesContract.Presenter presenter;
    protected final NotesViewModel viewModel;

    @NonNull
    protected List<Note> notes;

    private Map<String, Integer> positionMap;

    public NotesAdapterBase(
            @NonNull List<Note> notes,
            @NonNull NotesContract.Presenter presenter,
            @NonNull NotesViewModel viewModel) {
        this.notes = checkNotNull(notes);
        this.presenter = checkNotNull(presenter);
        this.viewModel = checkNotNull(viewModel);
        updatePositionMap();
        setHasStableIds(true);
    }

    // Items

    @NonNull
    public List<Note> getNotes() {
        return notes;
    }

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
    }

    @Override
    public abstract VH onCreateViewHolder(ViewGroup parent, int viewType);

    @Override
    public abstract void onBindViewHolder(VH holder, int position);

    @Override
    public int getItemCount() {
        return notes.size();
    }

    @Override
    public long getItemId(int position) {
        return notes.get(position).getRowId();
    }
}
