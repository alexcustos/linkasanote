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
package com.bytesforge.linkasanote.sync.files

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import com.bytesforge.linkasanote.utils.CloudUtils
import com.bytesforge.linkasanote.utils.UuidUtils
import com.google.common.base.Objects
import java.io.File

class JsonFile : Parcelable, Comparable<JsonFile> {
    val mimeType: String
        get() = MIME_TYPE

    private var length: Long
    var localPath: String?

    // Getters & Setters
    var remotePath: String?
        private set
    var eTag: String?

    constructor(remotePath: String) {
        require(remotePath.startsWith(PATH_SEPARATOR)) { "Remote path must be absolute [$remotePath]" }

        length = 0
        localPath = null
        this.remotePath = remotePath
        eTag = null
    }

    constructor(localPath: String, remotePath: String) : this(remotePath) {
        this.localPath = localPath
        val localFile = File(localPath)
        length = localFile.length()
    }

    private constructor(`in`: Parcel) {
        length = `in`.readLong()
        localPath = `in`.readString()
        remotePath = `in`.readString()
        eTag = `in`.readString()
    }

    override fun describeContents(): Int {
        return super.hashCode()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(length)
        dest.writeString(localPath)
        dest.writeString(remotePath)
        dest.writeString(eTag)
    }

    override fun compareTo(other: JsonFile): Int {
        return remotePath!!.compareTo(other.remotePath!!, ignoreCase = true)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val file = other as JsonFile
        return (Objects.equal(remotePath, file.remotePath)
                && Objects.equal(length, file.length))
    }

    override fun hashCode(): Int {
        return Objects.hashCode(length, localPath, remotePath, eTag)
    }

    fun setLength(length: Long) {
        this.length = length
    }

    companion object {
        private const val MIME_TYPE = "application/json"
        private const val FILE_EXTENSION = ".json"
        const val PATH_SEPARATOR = "/"

        @JvmField
        val CREATOR: Parcelable.Creator<JsonFile> = object : Parcelable.Creator<JsonFile> {
            override fun createFromParcel(`in`: Parcel): JsonFile {
                return JsonFile(`in`)
            }

            override fun newArray(size: Int): Array<JsonFile?> {
                return arrayOfNulls(size)
            }
        }

        @JvmStatic
        fun getFileName(id: String): String {
            return id + FILE_EXTENSION
        }

        @JvmStatic
        fun getTempFileName(id: String): String {
            return id + "." + CloudUtils.getApplicationId()
        }

        @JvmStatic
        fun getId(mimeType: String?, filePath: String?): String? {
            if (mimeType == null || filePath == null) return null
            if (mimeType != mimeType) return null

            var id = Uri.parse(filePath).lastPathSegment
            if (id != null && id.endsWith(FILE_EXTENSION)) {
                id = id.substring(0, id.length - FILE_EXTENSION.length)
            } else {
                return null
            }
            return if (UuidUtils.isKeyValidUuid(id)) {
                id
            } else null
        }
    }
}