package com.bytesforge.linkasanote.laano.notes;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.bytesforge.linkasanote.data.Note;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class NotesAdapterBase<VH extends RecyclerView.ViewHolder> extends
        RecyclerView.Adapter<VH> {

    private static final String TAG = NotesAdapterBase.class.getSimpleName();

    protected final NotesContract.Presenter presenter;
    protected final NotesViewModel viewModel;

    @NonNull
    // TODO: switch Note to generic variable and move adapter class to BaseItemAdapter
    protected List<Note> notes;
    private List<String> noteIds;

    public NotesAdapterBase(
            @NonNull List<Note> notes,
            @NonNull NotesContract.Presenter presenter,
            @NonNull NotesViewModel viewModel) {
        this.notes = checkNotNull(notes);
        this.presenter = checkNotNull(presenter);
        this.viewModel = checkNotNull(viewModel);
        updateIds();
        setHasStableIds(true);
    }

    // Items

    @NonNull
    // NOTE: this return is not type safe, but inspection is missing this fact, so it's OK
    public List<Note> getNotes() {
        return notes;
    }

    @NonNull
    public String[] getIds() {
        // NOTE: adapter will be a raw type, so make a copy is only way to make it type safe
        return noteIds.toArray(new String[noteIds.size()]);
    }

    public void swapItems(@NonNull List<Note> notes) {
        checkNotNull(notes);
        this.notes = notes;
        updateIds();
        notifyDataSetChanged();
    }

    @Nullable
    public synchronized Note removeItem(int position) {
        Note note;
        try {
            note = notes.remove(position);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
        noteIds.remove(position);
        notifyItemRemoved(position);
        return note;
    }

    public synchronized int removeItem(@NonNull String id) {
        checkNotNull(id);
        int position = getPosition(id);
        if (position < 0) return -1;

        notes.remove(position);
        noteIds.remove(position);
        notifyItemRemoved(position);
        return position;
    }

    public int getPosition(@Nullable String noteId) {
        if (noteId == null) {
            return -1;
        }
        return noteIds.indexOf(noteId);
    }

    public void notifyItemChanged(String noteId) {
        int position = getPosition(noteId);
        if (position < 0) return;

        notifyItemChanged(position);
    }

    @Nullable
    public String getId(int position) {
        try {
            return notes.get(position).getId();
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    private synchronized void updateIds() {
        int size = notes.size();
        if (noteIds == null) {
            noteIds = new ArrayList<>(size);
        } else {
            noteIds.clear();
        }
        for (int i = 0; i < size; i++) {
            Note note = notes.get(i);
            noteIds.add(i, note.getId());
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
