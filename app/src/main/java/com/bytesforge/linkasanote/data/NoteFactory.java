package com.bytesforge.linkasanote.data;

import java.util.List;

public interface NoteFactory<T> extends ItemFactory<T> {

    T build(T item, List<Tag> tags);
    T buildOrphaned(T item);
}
