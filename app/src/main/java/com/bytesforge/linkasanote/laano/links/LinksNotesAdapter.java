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

package com.bytesforge.linkasanote.laano.links;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.bytesforge.linkasanote.data.Note;
import com.bytesforge.linkasanote.databinding.ItemLinksNotesBinding;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class LinksNotesAdapter extends RecyclerView.Adapter<LinksNotesAdapter.ViewHolder> {

    private static final String TAG = LinksNotesAdapter.class.getSimpleName();

    private final LinksContract.Presenter presenter;

    private List<Note> notes;
    private Map<String, Integer> positionMap;

    public LinksNotesAdapter(List<Note> notes, @NonNull LinksContract.Presenter presenter) {
        if (notes == null) {
           this.notes = new ArrayList<>(0);
        } else {
            this.notes = notes;
        }
        updatePositionMap();
        this.presenter = checkNotNull(presenter);
        setHasStableIds(true);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemLinksNotesBinding binding;

        public ViewHolder(ItemLinksNotesBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Note note, LinksContract.Presenter presenter, LinksNotesAdapter adapter) {
            binding.setNote(note);
            binding.setPresenter(presenter);
            binding.setAdapter(adapter);
            binding.executePendingBindings();
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemLinksNotesBinding binding = ItemLinksNotesBinding.inflate(inflater, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Note note = notes.get(position);
        holder.bind(note, presenter, this);
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
        positionMap.remove(note.getId());
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
            Log.e(TAG, "No position is found for Note on Link [" + noteId + "]");
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
    }
}
