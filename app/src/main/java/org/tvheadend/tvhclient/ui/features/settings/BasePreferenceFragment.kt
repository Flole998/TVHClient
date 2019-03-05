package org.tvheadend.tvhclient.ui.features.settings

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.domain.entity.ServerStatus
import org.tvheadend.tvhclient.ui.base.callbacks.ToolbarInterface
import javax.inject.Inject

abstract class BasePreferenceFragment : PreferenceFragmentCompat() {

    @Inject
    lateinit var sharedPreferences: SharedPreferences
    @Inject
    lateinit var appRepository: AppRepository

    lateinit var toolbarInterface: ToolbarInterface

    var isUnlocked: Boolean = false
    var htspVersion: Int = 0
    var serverStatus: ServerStatus? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (activity is ToolbarInterface) {
            toolbarInterface = activity as ToolbarInterface
        }

        MainApplication.getComponent().inject(this)

        serverStatus = appRepository.serverStatusData.activeItem
        htspVersion = serverStatus?.htspVersion ?: 13
        isUnlocked = MainApplication.getInstance().isUnlocked
    }
}