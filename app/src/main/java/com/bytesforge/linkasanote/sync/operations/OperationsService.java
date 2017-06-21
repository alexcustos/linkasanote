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

package com.bytesforge.linkasanote.sync.operations;

import android.accounts.Account;
import android.accounts.AccountsException;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.bytesforge.linkasanote.data.source.cloud.CloudDataSource;
import com.bytesforge.linkasanote.sync.operations.nextcloud.CheckCredentialsOperation;
import com.bytesforge.linkasanote.sync.operations.nextcloud.GetServerInfoOperation;
import com.google.common.base.Objects;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;

public class OperationsService extends Service {

    private static final String TAG = OperationsService.class.getSimpleName();
    private AtomicInteger operationId = new AtomicInteger(0);

    public static final String ACTION_GET_SERVER_INFO = "GET_SERVER_INFO";
    public static final String ACTION_CHECK_CREDENTIALS = "CHECK_CREDENTIALS";
    public static final String EXTRA_ACCOUNT = "ACCOUNT";
    public static final String EXTRA_CREDENTIALS = "CREDENTIALS";
    public static final String EXTRA_SERVER_URL = "SERVER_URL";
    public static final String EXTRA_SERVER_VERSION = "SERVER_VERSION";

    private final IBinder binder = new OperationsBinder();
    private OperationsHandler operationsHandler;

    private OwnCloudClient ocClient; // Cache
    private final ConcurrentLinkedQueue<OperationItem> pendingOperations =
            new ConcurrentLinkedQueue<>();

    private static class Target {
        public Account account = null;
        public Uri serverUrl = null;

        public Target(Account account, Uri serverUrl) {
            this.account = account;
            this.serverUrl = serverUrl;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;

            Target target = (Target) obj;
            return Objects.equal(account, target.account)
                    && Objects.equal(serverUrl, target.serverUrl);
        }
    }

    private static class OperationItem {
        public final Target target;
        public final RemoteOperation operation;
        public final OnRemoteOperationListener listener;
        public final Handler handler;

        public OperationItem(
                @NonNull Target target, @NonNull RemoteOperation operation,
                OnRemoteOperationListener listener, Handler handler) {
            this.target = checkNotNull(target);
            this.operation = checkNotNull(operation);
            this.listener = listener;
            this.handler = handler;
        }

        private boolean hasListener() {
            return (listener != null && handler != null);
        }

        public void dispatchResult(@NonNull final RemoteOperationResult result) {
            if (hasListener()) {
                handler.post(() -> listener.onRemoteOperationFinish(operation, result));
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread(
                "Operations thread", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        operationsHandler = new OperationsHandler(thread.getLooper(), this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class OperationsBinder extends Binder {

        public OperationsService getService() {
            return OperationsService.this;
        }
    }

    private class OperationsHandler extends Handler {

        private OperationsService service;
        private Target lastTarget;

        public OperationsHandler(Looper looper, @NonNull OperationsService service) {
            super(looper);
            this.service = checkNotNull(service);
            lastTarget = null;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            OperationItem operationItem;
            synchronized (pendingOperations) {
                operationItem = pendingOperations.poll();
            }
            if (operationItem != null) {
                RemoteOperationResult result = executeOperationItem(operationItem);
                operationItem.dispatchResult(result);
            }
            Log.d(TAG, "End of processing message with id " + msg.arg1);
        }

        private RemoteOperationResult executeOperationItem(OperationItem item) {
            RemoteOperationResult result;
            try {
                RemoteOperation operation = item.operation;
                OwnCloudAccount account;
                if (lastTarget == null || !lastTarget.equals(item.target)) {
                    lastTarget = item.target;
                    if (lastTarget.account != null) {
                        account = new OwnCloudAccount(lastTarget.account, service);
                    } else {
                        account = new OwnCloudAccount(lastTarget.serverUrl, null);
                    }
                    ocClient = OwnCloudClientManagerFactory.getDefaultSingleton()
                            .getClientFor(account, service);
                }
                result = CloudDataSource.executeRemoteOperation(operation, ocClient).blockingGet();
            } catch (AccountsException | IOException e) {
                result = new RemoteOperationResult(e);
            }
            return result;
        }
    }

    public long queueOperation(Intent intent, OnRemoteOperationListener listener, Handler handler) {
        Log.d(TAG, "Queuing message with id " + operationId);

        OperationItem operationItem = buildOperation(intent, listener, handler);
        pendingOperations.add(operationItem);

        Message msg = operationsHandler.obtainMessage();
        msg.arg1 = operationId.getAndIncrement();
        operationsHandler.sendMessage(msg);

        return operationItem.operation.hashCode();
    }

    @NonNull
    private OperationItem buildOperation(
            Intent intent, OnRemoteOperationListener listener, Handler handler) {
        RemoteOperation operation;

        if (!intent.hasExtra(EXTRA_ACCOUNT) && !intent.hasExtra(EXTRA_SERVER_URL)) {
            throw new InvalidParameterException(
                    "At least one of the following EXTRA must be specified: ACCOUNT, SERVER_URL");
        }
        Account account = intent.getParcelableExtra(EXTRA_ACCOUNT);
        String serverUrl = intent.getStringExtra(EXTRA_SERVER_URL);
        Target target = new Target(
                account, (serverUrl == null) ? null : Uri.parse(serverUrl));

        String action = intent.getAction();
        switch (action) {
            case ACTION_GET_SERVER_INFO:
                operation = new GetServerInfoOperation(serverUrl, OperationsService.this);
                break;
            case ACTION_CHECK_CREDENTIALS:
                Bundle credentials = intent.getBundleExtra(EXTRA_CREDENTIALS);
                OwnCloudVersion serverVersion = new OwnCloudVersion(
                        intent.getStringExtra(EXTRA_SERVER_VERSION));
                operation = new CheckCredentialsOperation(credentials, serverVersion);
                break;
            default:
                throw new UnsupportedOperationException("OperationItem not supported: " + action);
        }
        return new OperationItem(target, operation, listener, handler);
    }

    @VisibleForTesting
    int getPendingOperationsQueueSize() {
        return pendingOperations.size();
    }
}
