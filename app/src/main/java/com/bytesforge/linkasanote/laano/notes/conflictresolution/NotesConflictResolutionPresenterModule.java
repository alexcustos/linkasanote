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

package com.bytesforge.linkasanote.laano.notes.conflictresolution;

import android.content.Context;

import com.bytesforge.linkasanote.laano.notes.NoteId;

import dagger.Module;
import dagger.Provides;

@Module
public class NotesConflictResolutionPresenterModule {

    private final Context context;
    private final NotesConflictResolutionContract.View view;
    private String noteId;

    public NotesConflictResolutionPresenterModule(
            Context context, NotesConflictResolutionContract.View view, String noteId) {
        this.context = context;
        this.view = view;
        this.noteId = noteId;
    }

    @Provides
    public NotesConflictResolutionContract.View provideNotesConflictResolutionContractView() {
        return view;
    }

    @Provides
    public NotesConflictResolutionContract.ViewModel provideNotesConflictResolutionContractViewModel() {
        return new NotesConflictResolutionViewModel(context);
    }

    @Provides
    @NoteId
    public String provideNoteId() {
        return noteId;
    }
}
