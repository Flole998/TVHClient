package org.tvheadend.tvhclient.ui.base

import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import org.tvheadend.tvhclient.MainApplication
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.data.repository.AppRepository
import org.tvheadend.tvhclient.domain.entity.Connection
import org.tvheadend.tvhclient.ui.common.callbacks.ToolbarInterface
import org.tvheadend.tvhclient.ui.common.showConfirmationToReconnectToServer
import org.tvheadend.tvhclient.util.extensions.gone
import org.tvheadend.tvhclient.util.extensions.visible
import timber.log.Timber
import javax.inject.Inject

abstract class BaseFragment : Fragment() {

    @Inject
    lateinit var appRepository: AppRepository
    @Inject
    lateinit var sharedPreferences: SharedPreferences

    protected lateinit var baseViewModel: BaseViewModel
    protected lateinit var toolbarInterface: ToolbarInterface
    protected var isDualPane: Boolean = false
    protected var isUnlocked: Boolean = false
    protected var htspVersion: Int = 13
    protected var isConnectionToServerAvailable: Boolean = false
    protected lateinit var connection: Connection

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        MainApplication.component.inject(this)

        if (activity is ToolbarInterface) {
            toolbarInterface = activity as ToolbarInterface
        }

        baseViewModel = ViewModelProviders.of(activity as BaseActivity).get(BaseViewModel::class.java)
        baseViewModel.connectionToServerAvailable.observe(viewLifecycleOwner, Observer { isAvailable ->
            Timber.d("Received live data, connection to server availability changed to $isAvailable")
            isConnectionToServerAvailable = isAvailable
        })

        baseViewModel.isUnlocked.observe(viewLifecycleOwner, Observer { unlocked ->
            Timber.d("Received live data, unlocked changed to $unlocked")
            isUnlocked = unlocked
        })

        connection = baseViewModel.connection
        htspVersion = baseViewModel.htspVersion

        // Check if we have a frame in which to embed the details fragment.
        // Make the frame layout visible and set the weights again in case
        // it was hidden by the call to forceSingleScreenLayout()
        isDualPane = resources.getBoolean(R.bool.isDualScreen)
        if (isDualPane) {
            enableDualScreenLayout()
        } else {
            enableSingleScreenLayout()
        }

        setHasOptionsMenu(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val ctx = context ?: return super.onOptionsItemSelected(item)
        return when (item.itemId) {
            android.R.id.home -> {
                activity?.finish()
                true
            }
            R.id.menu_reconnect_to_server -> showConfirmationToReconnectToServer(ctx, baseViewModel)
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun enableSingleScreenLayout() {
        Timber.d("Dual pane is not active, hiding details layout")
        val mainFrameLayout: FrameLayout = activity!!.findViewById(R.id.main)
        val detailsFrameLayout: FrameLayout? = activity!!.findViewById(R.id.details)
        detailsFrameLayout?.gone()
        mainFrameLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.0f)
    }

    private fun enableDualScreenLayout() {
        Timber.d("Dual pane is active, showing details layout")
        val mainFrameLayout: FrameLayout = activity!!.findViewById(R.id.main)
        val detailsFrameLayout: FrameLayout? = activity!!.findViewById(R.id.details)
        detailsFrameLayout?.visible()
        mainFrameLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                0.65f)
    }

    protected fun forceSingleScreenLayout() {
        if (isDualPane) {
            Timber.d("Dual pane is active, forcing single screen layout")
            enableSingleScreenLayout()
        }
    }
}
