package org.tvheadend.tvhclient.ui.features.dvr.recordings

import android.app.Application
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.switchMap
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.data.service.HtspService
import org.tvheadend.tvhclient.domain.entity.Channel
import org.tvheadend.tvhclient.domain.entity.Recording
import org.tvheadend.tvhclient.domain.entity.ServerProfile
import org.tvheadend.tvhclient.ui.base.BaseViewModel
import timber.log.Timber

class RecordingViewModel(application: Application) : BaseViewModel(application), SharedPreferences.OnSharedPreferenceChangeListener {

    val completedRecordings: LiveData<List<Recording>>
    val scheduledRecordings: LiveData<List<Recording>>
    val failedRecordings: LiveData<List<Recording>>
    val removedRecordings: LiveData<List<Recording>>

    var recording = Recording()
    var recordingProfileNameId: Int = 0

    private var hideDuplicateScheduledRecordings: MutableLiveData<Boolean> = MutableLiveData()

    fun getIntentData(recording: Recording): Intent {
        val intent = Intent(appContext, HtspService::class.java)
        intent.putExtra("title", recording.title)
        intent.putExtra("subtitle", recording.subtitle)
        intent.putExtra("summary", recording.summary)
        intent.putExtra("description", recording.description)
        intent.putExtra("stop", recording.stop / 1000)
        intent.putExtra("stopExtra", recording.stopExtra)

        if (!recording.isRecording) {
            intent.putExtra("channelId", recording.channelId)
            intent.putExtra("start", recording.start / 1000)
            intent.putExtra("startExtra", recording.startExtra)
            intent.putExtra("priority", recording.priority)
            intent.putExtra("enabled", if (recording.isEnabled) 1 else 0)
        }
        return intent
    }

    init {
        onSharedPreferenceChanged(sharedPreferences, "hide_duplicate_scheduled_recordings_enabled")

        val trigger = ScheduledRecordingLiveData(hideDuplicateScheduledRecordings)
        scheduledRecordings = switchMap(trigger) { value ->
            Timber.d("Loading scheduled recordings because the duplicate setting has changed")
            if (value == null) {
                Timber.d("Skipping loading of scheduled recordings because the duplicate setting is not set")
                return@switchMap null
            }
            return@switchMap appRepository.recordingData.getScheduledRecordings(value)
        }

        completedRecordings = appRepository.recordingData.getCompletedRecordings()
        failedRecordings = appRepository.recordingData.getFailedRecordings()
        removedRecordings = appRepository.recordingData.getRemovedRecordings()

        Timber.d("Registering shared preference change listener")
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onCleared() {
        Timber.d("Unregistering shared preference change listener")
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onCleared()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Timber.d("Shared preference $key has changed")
        if (sharedPreferences == null) return
        when (key) {
            "hide_duplicate_scheduled_recordings_enabled" -> {
                hideDuplicateScheduledRecordings.value = sharedPreferences.getBoolean(key, appContext.resources.getBoolean(R.bool.pref_default_hide_duplicate_scheduled_recordings_enabled))
            }
        }
    }

    fun getRecordingById(id: Int): LiveData<Recording>? {
        return appRepository.recordingData.getLiveDataItemById(id)
    }

    fun loadRecordingByIdSync(id: Int) {
        recording = appRepository.recordingData.getItemById(id) ?: Recording()
    }

    fun getChannelList(): List<Channel> {
        val defaultChannelSortOrder = appContext.resources.getString(R.string.pref_default_channel_sort_order)
        val channelSortOrder = Integer.valueOf(sharedPreferences.getString("channel_sort_order", defaultChannelSortOrder)
                ?: defaultChannelSortOrder)
        return appRepository.channelData.getChannels(channelSortOrder)
    }

    fun getRecordingProfileNames(): Array<String> {
        return appRepository.serverProfileData.recordingProfileNames
    }

    fun getRecordingProfile(): ServerProfile? {
        return appRepository.serverProfileData.getItemById(appRepository.serverStatusData.activeItem.recordingServerProfileId)
    }

    internal inner class ScheduledRecordingLiveData(hideDuplicates: LiveData<Boolean>) : MediatorLiveData<Boolean>() {
        init {
            addSource(hideDuplicates) { hide ->
                value = hide
            }
        }
    }
}
