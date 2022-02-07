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
package com.bytesforge.linkasanote.about

import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.bytesforge.linkasanote.BuildConfig
import com.bytesforge.linkasanote.LaanoApplication
import com.bytesforge.linkasanote.R
import com.bytesforge.linkasanote.about.AboutFragment
import com.bytesforge.linkasanote.databinding.DialogAboutLicenseTermsBinding
import com.bytesforge.linkasanote.databinding.FragmentAboutBinding
import com.bytesforge.linkasanote.utils.ActivityUtils
import com.bytesforge.linkasanote.utils.CommonUtils
import com.bytesforge.linkasanote.utils.schedulers.BaseSchedulerProvider
import com.google.common.base.Charsets
import com.google.common.base.Preconditions
import io.reactivex.Single
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import javax.inject.Inject

class AboutFragment : Fragment(), AboutContract.View {
    private var presenter: AboutContract.Presenter? = null
    private var viewModel: AboutContract.ViewModel? = null
    override fun onResume() {
        super.onResume()
        presenter!!.subscribe()
    }

    override fun onPause() {
        super.onPause()
        presenter!!.unsubscribe()
    }

    override val isActive: Boolean
        get() = isAdded

    override fun setPresenter(presenter: AboutContract.Presenter) {
        this.presenter = Preconditions.checkNotNull(presenter)
    }

    override fun setViewModel(viewModel: AboutContract.ViewModel) {
        this.viewModel = Preconditions.checkNotNull(viewModel)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentAboutBinding.inflate(inflater, container, false)
        viewModel!!.setInstanceState(savedInstanceState)
        binding.presenter = presenter
        binding.viewModel = viewModel as AboutViewModel?
        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel!!.saveInstanceState(outState)
    }

    override fun showGooglePlay() {
        val resources = resources
        val uri = Uri.parse(
            resources.getString(R.string.google_market) + BuildConfig.APPLICATION_ID
        )
        val intent = Intent(Intent.ACTION_VIEW, uri)
        val apps = requireContext().packageManager.queryIntentActivities(intent, 0)
        var found = false
        for (app in apps) {
            if (app.activityInfo.applicationInfo.packageName == GOOGLE_PLAY_PACKAGE_NAME) {
                val activityInfo = app.activityInfo
                val componentName = ComponentName(
                    activityInfo.applicationInfo.packageName, activityInfo.name
                )
                intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                            or Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
                intent.component = componentName
                startActivity(intent)
                found = true
                break
            }
        }
        if (!found) {
            val webUri = Uri.parse(
                resources.getString(R.string.google_play) + BuildConfig.APPLICATION_ID
            )
            val webIntent = Intent(Intent.ACTION_VIEW, webUri)
            try {
                startActivity(webIntent)
            } catch (e: ActivityNotFoundException) {
                viewModel!!.showLaunchGooglePlayErrorSnackbar()
            }
        }
    }

    override fun showLicenseTermsAlertDialog(licenseAsset: String) {
        Preconditions.checkNotNull(licenseAsset)
        val dialog = LicenseTermsDialog.newInstance(licenseAsset)
        dialog.show(requireFragmentManager(), LicenseTermsDialog.DIALOG_TAG)
    }

    class LicenseTermsDialog : DialogFragment() {
        //private var context: Context? = null
        //private var resources: Resources? = null
        var binding: DialogAboutLicenseTermsBinding? = null
        private var licenseAsset: String? = null

        @JvmField
        @Inject
        var schedulerProvider: BaseSchedulerProvider? = null
        override fun onStart() {
            super.onStart()
            binding!!.licenseTerms.movementMethod = LinkMovementMethod.getInstance()
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            licenseAsset = requireArguments().getString(ARGUMENT_LICENSE_ASSET)
            //context = getContext()
            //resources = requireContext().resources
            val application = requireActivity().application as LaanoApplication
            application.applicationComponent.inject(this)
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val inflater = LayoutInflater.from(context)
            binding = DialogAboutLicenseTermsBinding.inflate(inflater, null, false)
            Single.fromCallable {
                getLicenseText(
                    licenseAsset!!
                )
            }
                .subscribeOn(schedulerProvider!!.computation())
                .map { source: String? ->
                    ActivityUtils.fromHtmlCompat(
                        source!!
                    )
                }
                .observeOn(schedulerProvider!!.ui())
                .subscribe(
                    { text: Spanned? -> binding!!.licenseTerms.text = text }
                ) { throwable: Throwable? -> CommonUtils.logStackTrace(TAG_E, throwable!!) }
            return AlertDialog.Builder(context)
                .setView(binding!!.root)
                .setPositiveButton(resources!!.getString(R.string.dialog_button_ok), null)
                .create()
        }

        private fun getLicenseText(assetName: String): String {
            Preconditions.checkNotNull(assetName)
            val resources = requireContext().resources
            var licenseText: String
            try {
                var line: String?
                val builder = StringBuilder()
                val stream = resources.assets.open(assetName)
                val `in` = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
                while (`in`.readLine().also { line = it } != null) {
                    builder.append(line)
                    builder.append('\n')
                }
                `in`.close()
                licenseText = builder.toString()
            } catch (e: IOException) {
                licenseText = resources.getString(R.string.about_fragment_error_license, assetName)
            }
            return licenseText
        }

        companion object {
            private const val ARGUMENT_LICENSE_ASSET = "LICENSE_ASSET"
            const val DIALOG_TAG = "LICENSE_TERMS"
            fun newInstance(licenseAsset: String?): LicenseTermsDialog {
                val args = Bundle()
                args.putString(ARGUMENT_LICENSE_ASSET, licenseAsset)
                val dialog = LicenseTermsDialog()
                dialog.arguments = args
                return dialog
            }
        }
    }

    companion object {
        private val TAG = AboutFragment::class.java.simpleName
        private val TAG_E = AboutFragment::class.java.canonicalName
        private const val GOOGLE_PLAY_PACKAGE_NAME = "com.android.vending"
        fun newInstance(): AboutFragment {
            return AboutFragment()
        }
    }
}