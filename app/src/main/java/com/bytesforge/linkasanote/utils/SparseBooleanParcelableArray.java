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
