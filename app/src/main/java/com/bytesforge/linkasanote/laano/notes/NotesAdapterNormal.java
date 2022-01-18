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

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
