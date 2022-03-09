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

import android.os.Bundle
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.lib.resources.users.GetRemoteUserInfoOperation

class CheckCredentialsOperation(
    credentials: Bundle?, private val serverVersion: OwnCloudVersion) : RemoteOperation()
{
    private val username: String? = credentials!!.getString(ACCOUNT_USERNAME)
    private val password: String? = credentials!!.getString(ACCOUNT_PASSWORD)

    override fun run(ocClient: OwnCloudClient): RemoteOperationResult {
        val credentials = OwnCloudCredentialsFactory.newBasicCredentials(username, password)
        ocClient.credentials = credentials
        ocClient.ownCloudVersion = serverVersion
        ocClient.isFollowRedirects = true
        val checkOperation = ExistenceCheckRemoteOperation(ROOT_PATH, false)
        var result = checkOperation.execute(ocClient)
        if (checkOperation.wasRedirected()) {
            val path = checkOperation.redirectionPath
            val location = path.lastPermanentLocation
            result.lastPermanentLocation = location
        }
        if (result.isSuccess) {
            // NOTE: user display name is updated during synchronization
            val infoOperation = GetRemoteUserInfoOperation()
            result = infoOperation.execute(ocClient)
        }
        if (result.isSuccess) {
            result.data.add(credentials)
        }
        return result
    }

    companion object {
        const val ACCOUNT_USERNAME = "USERNAME"
        const val ACCOUNT_PASSWORD = "PASSWORD"
        private const val ROOT_PATH = "/"
    }
}