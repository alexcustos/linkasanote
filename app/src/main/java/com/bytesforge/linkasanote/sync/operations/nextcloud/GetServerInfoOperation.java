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
    protected RemoteOperationResult run(OwnCloudClient client) {
        GetRemoteStatusOperation status = new GetRemoteStatusOperation(context);
        RemoteOperationResult result = status.execute(client);

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
