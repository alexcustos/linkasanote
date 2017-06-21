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
    protected List<Note> notes;

    @NonNull
    private List<String> noteIds;

    public NotesAdapterBase(
            @NonNull List<Note> notes,
            @NonNull NotesContract.Presenter presenter,
            @NonNull NotesViewModel viewModel) {
        this.notes = checkNotNull(notes);
        this.presenter = checkNotNull(presenter);
        this.viewModel = checkNotNull(viewModel);
        noteIds = new ArrayList<>(notes.size());
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
        this.notes = checkNotNull(notes);
        updateIds();
        notifyDataSetChanged();
    }

    public synchronized  void addItem(@NonNull Note note) {
        checkNotNull(note);
        notes.add(note);
        noteIds.add(note.getId());
        notifyItemInserted(notes.size() - 1);
    }

    public synchronized void addItems(@NonNull List<Note> notes) {
        checkNotNull(notes);
        final int start = this.notes.size();
        this.notes.addAll(notes);
        for (Note note : notes) {
            noteIds.add(note.getId());
        }
        notifyItemRangeInserted(start, notes.size());
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

    public synchronized void clear() {
        notes.clear();
        noteIds.clear();
        notifyDataSetChanged();
    }

    public int getPosition(@Nullable String noteId) {
        if (noteId == null) return -1;

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
        noteIds.clear();
        for (int i = 0; i < notes.size(); i++) {
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
