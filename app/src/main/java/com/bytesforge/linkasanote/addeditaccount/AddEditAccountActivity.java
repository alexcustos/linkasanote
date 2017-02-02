package com.bytesforge.linkasanote.addeditaccount;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.bytesforge.linkasanote.LaanoApplication;
import com.bytesforge.linkasanote.R;
import com.bytesforge.linkasanote.addeditaccount.nextcloud.NextcloudFragment;
import com.bytesforge.linkasanote.addeditaccount.nextcloud.NextcloudPresenter;
import com.bytesforge.linkasanote.addeditaccount.nextcloud.NextcloudPresenterModule;
import com.bytesforge.linkasanote.addeditaccount.nextcloud.NextcloudViewModel;
import com.bytesforge.linkasanote.sync.operations.OperationsService;
import com.bytesforge.linkasanote.utils.ActivityUtils;

import javax.inject.Inject;

public class AddEditAccountActivity extends AppCompatActivity {

    private static final String TAG = AddEditAccountActivity.class.getSimpleName();

    private OperationsService operationsService = null;

    @Inject
    NextcloudPresenter presenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DataBindingUtil.setContentView(this, R.layout.activity_add_edit_account);

        // Fragment (View)
        NextcloudFragment nextcloudFragment = (NextcloudFragment) getSupportFragmentManager()
                .findFragmentById(R.id.content_frame);
        if (nextcloudFragment == null) {
            nextcloudFragment = NextcloudFragment.newInstance();

            ActivityUtils.addFragmentToActivity(
                    getSupportFragmentManager(), nextcloudFragment, R.id.content_frame);
        }
        NextcloudViewModel nextcloudViewModel = new NextcloudViewModel(getApplicationContext());

        // Presenter
        DaggerAddEditAccountComponent.builder()
                .applicationComponent(((LaanoApplication) getApplication()).getApplicationComponent())
                .nextcloudPresenterModule(new NextcloudPresenterModule(nextcloudFragment, nextcloudViewModel))
                .build().inject(this);

        // Service binding
        Intent intent = new Intent(this, OperationsService.class);
        bindService(intent, operationsServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (operationsService != null) {
            unbindService(operationsServiceConnection);
        }
    }

    private ServiceConnection operationsServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "Service connected [" + className.getShortClassName() + "]");
            OperationsService.OperationsBinder binder =
                    (OperationsService.OperationsBinder) service;
            operationsService = binder.getService();
            presenter.setOperationsService(operationsService);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "Service disconnected [" + className.getShortClassName() + "]");
            operationsService = null;
        }
    };
}
