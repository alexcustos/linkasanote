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
package com.bytesforge.linkasanote.sync.operations

import android.accounts.Account
import android.accounts.AccountsException
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.*
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.bytesforge.linkasanote.data.source.cloud.CloudDataSource
import com.bytesforge.linkasanote.sync.operations.nextcloud.CheckCredentialsOperation
import com.bytesforge.linkasanote.sync.operations.nextcloud.GetServerInfoOperation
import com.google.common.base.Objects
import com.owncloud.android.lib.common.OwnCloudAccount
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import java.io.IOException
import java.security.InvalidParameterException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

class OperationsService : Service() {
    private val operationId = AtomicInteger(0)
    private val binder: IBinder = OperationsBinder()
    private var operationsHandler: OperationsHandler? = null
    private var ocClient // Cache
            : OwnCloudClient? = null
    private val pendingOperations = ConcurrentLinkedQueue<OperationItem>()

    private class Target(account: Account?, serverUrl: Uri?) {
        var account: Account? = null
        var serverUrl: Uri? = null

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val target = other as Target
            return (Objects.equal(account, target.account)
                    && Objects.equal(serverUrl, target.serverUrl))
        }

        override fun hashCode(): Int {
            return Objects.hashCode(account, serverUrl)
        }

        init {
            this.account = account
            this.serverUrl = serverUrl
        }
    }

    private class OperationItem(
        val target: Target, val operation: RemoteOperation,
        val listener: OnRemoteOperationListener?, val handler: Handler?
    ) {
        fun hasListener(): Boolean {
            return listener != null && handler != null
        }

        fun dispatchResult(result: RemoteOperationResult) {
            if (hasListener()) {
                handler!!.post { listener!!.onRemoteOperationFinish(operation, result) }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val thread = HandlerThread(
            "Operations thread", Process.THREAD_PRIORITY_BACKGROUND
        )
        thread.start()
        operationsHandler = OperationsHandler(thread.looper, this)
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    inner class OperationsBinder : Binder() {
        val service: OperationsService
            get() = this@OperationsService
    }

    private inner class OperationsHandler(
        looper: Looper?, private val service: OperationsService) : Handler(
        looper!!
    ) {
        private var lastTarget: Target?
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            var operationItem: OperationItem?
            synchronized(pendingOperations) { operationItem = pendingOperations.poll() }
            if (operationItem != null) {
                val result = executeOperationItem(operationItem!!)
                operationItem!!.dispatchResult(result)
            }
            Log.d(TAG, "End of processing message with id " + msg.arg1)
        }

        private fun executeOperationItem(item: OperationItem): RemoteOperationResult {
            var result: RemoteOperationResult
            try {
                val operation = item.operation
                val account: OwnCloudAccount
                if (lastTarget == null || lastTarget != item.target) {
                    lastTarget = item.target
                    account = if (lastTarget!!.account != null) {
                        OwnCloudAccount(lastTarget!!.account, service)
                    } else {
                        OwnCloudAccount(lastTarget!!.serverUrl, null)
                    }
                    ocClient = OwnCloudClientManagerFactory.getDefaultSingleton()
                        .getClientFor(account, service)
                }
                result = CloudDataSource.executeRemoteOperation(operation, ocClient!!).blockingGet()
            } catch (e: AccountsException) {
                result = RemoteOperationResult(e)
            } catch (e: IOException) {
                result = RemoteOperationResult(e)
            }
            return result
        }

        init {
            lastTarget = null
        }
    }

    fun queueOperation(
        intent: Intent,
        listener: OnRemoteOperationListener?,
        handler: Handler?
    ): Long {
        Log.d(TAG, "Queuing message with id $operationId")
        val operationItem = buildOperation(intent, listener, handler)
        pendingOperations.add(operationItem)
        val msg = operationsHandler!!.obtainMessage()
        msg.arg1 = operationId.getAndIncrement()
        if (operationItem.hasListener()) {
            operationsHandler!!.sendMessage(msg)
        }
        return operationItem.operation.hashCode().toLong()
    }

    private fun buildOperation(
        intent: Intent, listener: OnRemoteOperationListener?, handler: Handler?
    ): OperationItem {
        val operation: RemoteOperation
        if (!intent.hasExtra(EXTRA_ACCOUNT) && !intent.hasExtra(EXTRA_SERVER_URL)) {
            throw InvalidParameterException(
                "At least one of the following EXTRA must be specified: ACCOUNT, SERVER_URL"
            )
        }
        val account = intent.getParcelableExtra<Account>(EXTRA_ACCOUNT)
        val serverUrl = intent.getStringExtra(EXTRA_SERVER_URL)
        val target = Target(
            account, if (serverUrl == null) null else Uri.parse(serverUrl)
        )
        val action = intent.action
        operation = when (action) {
            ACTION_GET_SERVER_INFO -> GetServerInfoOperation(
                serverUrl,
                this@OperationsService
            )
            ACTION_CHECK_CREDENTIALS -> {
                val credentials = intent.getBundleExtra(EXTRA_CREDENTIALS)
                val serverVersion = OwnCloudVersion(
                    intent.getStringExtra(EXTRA_SERVER_VERSION)
                )
                CheckCredentialsOperation(credentials, serverVersion)
            }
            else -> throw UnsupportedOperationException("OperationItem not supported: $action")
        }
        return OperationItem(target, operation, listener, handler)
    }

    @get:VisibleForTesting
    val pendingOperationsQueueSize: Int
        get() = pendingOperations.size

    companion object {
        private val TAG = OperationsService::class.java.simpleName
        const val ACTION_GET_SERVER_INFO = "GET_SERVER_INFO"
        const val ACTION_CHECK_CREDENTIALS = "CHECK_CREDENTIALS"
        const val EXTRA_ACCOUNT = "ACCOUNT"
        const val EXTRA_CREDENTIALS = "CREDENTIALS"
        const val EXTRA_SERVER_URL = "SERVER_URL"
        const val EXTRA_SERVER_VERSION = "SERVER_VERSION"
    }
}