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
package com.bytesforge.linkasanote.synclog

import com.bytesforge.linkasanote.synclog.SyncLogViewModel
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import android.view.LayoutInflater
import com.bytesforge.linkasanote.synclog.SyncLogAdapter
import androidx.appcompat.app.AppCompatActivity
import javax.inject.Inject
import com.bytesforge.linkasanote.synclog.SyncLogPresenter
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.bytesforge.linkasanote.R
import com.bytesforge.linkasanote.synclog.SyncLogFragment
import com.bytesforge.linkasanote.utils.ActivityUtils
import com.bytesforge.linkasanote.LaanoApplication
import com.bytesforge.linkasanote.synclog.SyncLogPresenterModule
import com.bytesforge.linkasanote.BaseView
import com.bytesforge.linkasanote.BasePresenter
import androidx.recyclerview.widget.LinearLayoutManager
import android.os.Parcelable
import com.bytesforge.linkasanote.FragmentScoped
import dagger.Subcomponent
import com.bytesforge.linkasanote.synclog.SyncLogActivity
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import com.bytesforge.linkasanote.utils.CommonUtils
import androidx.databinding.BaseObservable
import androidx.databinding.ObservableInt
import com.bytesforge.linkasanote.BR
import androidx.databinding.BindingAdapter
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar
import dagger.Provides

@FragmentScoped
@Subcomponent(modules = [SyncLogPresenterModule::class])
interface SyncLogComponent {
    fun inject(syncLogActivity: SyncLogActivity?)
}