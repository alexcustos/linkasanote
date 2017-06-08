package com.bytesforge.linkasanote.sync.operations.nextcloud;

import android.os.Bundle;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudCredentials;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.network.RedirectionPath;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.lib.resources.users.GetRemoteUserInfoOperation;

public class CheckCredentialsOperation extends RemoteOperation {

    public static final String ACCOUNT_USERNAME = "USERNAME";
    public static final String ACCOUNT_PASSWORD = "PASSWORD";

    private static final String ROOT_PATH = "/";

    private final String username;
    private final String password;
    private final OwnCloudVersion serverVersion;

    public CheckCredentialsOperation(Bundle credentials, OwnCloudVersion serverVersion) {
        username = credentials.getString(ACCOUNT_USERNAME);
        password = credentials.getString(ACCOUNT_PASSWORD);
        this.serverVersion = serverVersion;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient ocClient) {
        OwnCloudCredentials credentials =
                OwnCloudCredentialsFactory.newBasicCredentials(username, password);
        ocClient.setCredentials(credentials);
        ocClient.setOwnCloudVersion(serverVersion);
        ocClient.setFollowRedirects(true);

        ExistenceCheckRemoteOperation checkOperation =
                new ExistenceCheckRemoteOperation(ROOT_PATH, false);
        RemoteOperationResult result = checkOperation.execute(ocClient);
        if (checkOperation.wasRedirected()) {
            RedirectionPath path = checkOperation.getRedirectionPath();
            String location = path.getLastPermanentLocation();
            result.setLastPermanentLocation(location);
        }
        if (result.isSuccess()) {
            // NOTE: user display name is updated during synchronization
            GetRemoteUserInfoOperation infoOperation = new GetRemoteUserInfoOperation();
            result = infoOperation.execute(ocClient);
        }
        if (result.isSuccess()) {
            result.getData().add(credentials);
        }
        return result;
    }
}
