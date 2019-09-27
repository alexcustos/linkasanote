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

package com.bytesforge.linkasanote.sync.operations.nextcloud;

import android.content.Context;
import android.support.annotation.NonNull;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.status.GetRemoteStatusOperation;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;

import java.util.ArrayList;

import static com.google.common.base.Preconditions.checkNotNull;


public class GetServerInfoOperation extends RemoteOperation {

    private String serverUrl;
    private ServerInfo serverInfo = new ServerInfo();

    @NonNull
    private Context context;

    public GetServerInfoOperation(String url, @NonNull Context context) {
        serverUrl = url;
        this.context = checkNotNull(context);
    }

    @Override
    protected RemoteOperationResult run(@NonNull OwnCloudClient ocClient) {
        checkNotNull(ocClient);

        GetRemoteStatusOperation statusOperation = new GetRemoteStatusOperation(context);
        RemoteOperationResult result = statusOperation.execute(ocClient);
        if (result.isSuccess()) {
            serverInfo.version = (OwnCloudVersion) result.getData().get(0);
            serverInfo.isSecure = (result.getCode() == RemoteOperationResult.ResultCode.OK_SSL);
            serverInfo.baseUrl = serverUrl;

            ArrayList<Object> data = new ArrayList<>();
            data.add(serverInfo);
            result.setData(data);
        }
        return result;
    }

    public static class ServerInfo {

        public OwnCloudVersion version = null;
        public String baseUrl = null;
        public boolean isSecure = false;

        public boolean isSet() {
            return version != null && baseUrl != null;
        }
    }
}
