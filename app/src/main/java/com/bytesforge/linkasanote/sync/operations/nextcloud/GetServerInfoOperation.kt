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
package com.bytesforge.linkasanote.sync.operations.nextcloud

import android.content.Context
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.status.GetRemoteStatusOperation
import com.owncloud.android.lib.resources.status.OwnCloudVersion

class GetServerInfoOperation(
    private val serverUrl: String?, private val context: Context) : RemoteOperation()
{
    private val serverInfo = ServerInfo()
    override fun run(ocClient: OwnCloudClient): RemoteOperationResult {
        val statusOperation = GetRemoteStatusOperation(context)
        val result = statusOperation.execute(ocClient)
        if (result.isSuccess) {
            serverInfo.version = result.data[0] as OwnCloudVersion
            serverInfo.isSecure = result.code == RemoteOperationResult.ResultCode.OK_SSL
            serverInfo.baseUrl = serverUrl
            val data = ArrayList<Any>()
            data.add(serverInfo)
            result.data = data
        }
        return result
    }

    class ServerInfo {
        @JvmField
        var version: OwnCloudVersion? = null
        @JvmField
        var baseUrl: String? = null
        @JvmField
        var isSecure = false
        val isSet: Boolean
            get() = version != null && baseUrl != null
    }
}