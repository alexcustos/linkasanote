package com.bytesforge.linkasanote.sync.files;

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.bytesforge.linkasanote.sync.operations.nextcloud.UploadFileOperation;
import com.bytesforge.linkasanote.utils.CloudUtils;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;

import java.io.IOException;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.base.Preconditions.checkNotNull;

public class FilesService extends Service {

    private static final String TAG = FilesService.class.getSimpleName();

    public static final String EXTRA_ACCOUNT = "ACCOUNT";
    public static final String EXTRA_LOCAL_PATHS = "LOCAL_PATHS";
    public static final String EXTRA_REMOTE_PATHS = "REMOTE_PATHS";
    public static final String EXTRA_URIS = "URIS";

    private FilesHandler filesHandler;
    ConcurrentMap<String, UploadFileOperation> pendingOperations = new ConcurrentHashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();

        HandlerThread thread = new HandlerThread("Files thread", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        filesHandler = new FilesHandler(thread.getLooper(), this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Account account = intent.getParcelableExtra(EXTRA_ACCOUNT);
        if (account == null) {
            Log.e(TAG, "An account extra is required to start operation");
            return Service.START_NOT_STICKY;
        }
        String[] uris = intent.getStringArrayExtra(EXTRA_URIS);
        String[] localPaths = intent.getStringArrayExtra(EXTRA_LOCAL_PATHS);
        String[] remotePaths = intent.getStringArrayExtra(EXTRA_REMOTE_PATHS);
        if (uris == null || localPaths == null || remotePaths == null
                || uris.length <= 0 || localPaths.length <= 0 || remotePaths.length <= 0
                || uris.length != localPaths.length
                || uris.length != remotePaths.length) {
            Log.e(TAG, "Arrays of URIs, local and remote files must not be empty and must have the equal size");
            return Service.START_NOT_STICKY;
        }
        if (!CloudUtils.isAccountExists(this, account)) {
            Log.e(TAG, "Account does not exist anymore [" + account.name + "]");
            cancelOperationsForAccount(account);
            return Service.START_NOT_STICKY;
        }

        JsonFile[] files = new JsonFile[localPaths.length];
        for (int i = 0; i < localPaths.length; i++) {
            files[i] = UploadFileOperation.createJsonFile(
                    Uri.parse(uris[i]), localPaths[i], remotePaths[i]);
        }
        List<String> uploadKeys = new Vector<>();
        for (JsonFile file : files) {
            UploadFileOperation operation =
                    new UploadFileOperation(account, file, getContentResolver());
            String uploadKey = file.getKey(account);
            UploadFileOperation previousOperation =
                    pendingOperations.putIfAbsent(uploadKey, operation);
            if (previousOperation == null) {
                uploadKeys.add(uploadKey);
            }
        }
        Message msg = filesHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = uploadKeys;
        filesHandler.sendMessage(msg);

        return Service.START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final class FilesHandler extends Handler {

        private FilesService service;

        public FilesHandler(Looper looper, @NonNull FilesService service) {
            super(looper);
            this.service = checkNotNull(service);
        }

        @Override
        public void handleMessage(Message msg) {
            @SuppressWarnings("unchecked")
            List<String> uploadKeys = (List<String>) msg.obj;
            if (uploadKeys != null) {
                for (String uploadKey : uploadKeys) {
                    service.uploadFile(uploadKey);
                }
            }
            service.stopSelf(msg.arg1);
        }
    } // class FilesHandler

    public void uploadFile(@NonNull String uploadKey) {
        checkNotNull(uploadKey);

        UploadFileOperation operation = pendingOperations.get(uploadKey);
        checkNotNull(operation);

        Account account = operation.getAccount();
        OwnCloudAccount ownCloudAccount;
        try {
            ownCloudAccount = new OwnCloudAccount(account, this);
        } catch (AccountUtils.AccountNotFoundException e) {
            cancelOperationsForAccount(account);
            return;
        }
        RemoteOperationResult result = null;
        try {
            OwnCloudClient ownCloudClient = OwnCloudClientManagerFactory
                    .getDefaultSingleton().getClientFor(ownCloudAccount, this);
            result = operation.execute(ownCloudClient);
        } catch (AccountUtils.AccountNotFoundException
                | OperationCanceledException | AuthenticatorException | IOException e) {
            result = new RemoteOperationResult(e);
        } finally {
            pendingOperations.remove(uploadKey);
            if (result != null && result.isSuccess()) {
                // TODO: notify listeners about progress: size of pendingOperations
                Log.d(TAG, "Upload operation successfully completed");
            }
        }
    } // uploadFile

    private void cancelOperationsForAccount(Account account) {
        for (String key : pendingOperations.keySet()) {
            if (key.startsWith(account.name)) {
                pendingOperations.remove(key);
            }
        }
    } // cancelOperationsForAccount
}
