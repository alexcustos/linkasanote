package com.bytesforge.linkasanote.utils;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseBooleanArray;

public class SparseBooleanParcelableArray extends SparseBooleanArray implements Parcelable {

    public static final Creator<SparseBooleanParcelableArray> CREATOR =
            new Creator<SparseBooleanParcelableArray>() {
        @Override
        public SparseBooleanParcelableArray createFromParcel(Parcel in) {
            SparseBooleanParcelableArray read = new SparseBooleanParcelableArray();
            int size = in.readInt();

            int[] keys = new int[size];
            boolean[] values = new boolean[size];
            in.readIntArray(keys);
            in.readBooleanArray(values);
            for (int i = 0; i < size; i++) {
                read.put(keys[i], values[i]);
            }
            return read;
        }

        @Override
        public SparseBooleanParcelableArray[] newArray(int size) {
            return new SparseBooleanParcelableArray[size];
        }
    };

    public SparseBooleanParcelableArray() {
    }

    public SparseBooleanParcelableArray(SparseBooleanArray sparseBooleanArray) {
        for (int i = 0; i < sparseBooleanArray.size(); i++) {
            this.put(sparseBooleanArray.keyAt(i), sparseBooleanArray.valueAt(i));
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        int size = size();

        int[] keys = new int[size];
        boolean[] values = new boolean[size];
        for (int i = 0; i < size; i++) {
            keys[i] = keyAt(i);
            values[i] = valueAt(i);
        }
        dest.writeInt(size);
        dest.writeIntArray(keys);
        dest.writeBooleanArray(values);
    }
}
