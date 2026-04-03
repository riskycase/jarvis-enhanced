package com.riskycase.jarvisEnhanced.viewModel

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riskycase.jarvisEnhanced.datastore.settingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppInfo(
    val packageName: String,
    val label: String,
    val blocked: Boolean
)

@HiltViewModel
class MusicBlocklistViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packageManager: PackageManager
) : ViewModel() {

    private val _apps = MutableLiveData<List<AppInfo>>(emptyList())
    val apps: LiveData<List<AppInfo>> = _apps

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            val blocked = context.settingsDataStore.data.first().blockedMusicPackagesList.toSet()
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val installed = packageManager.queryIntentActivities(intent, 0)
                .map { it.activityInfo.packageName }
                .distinct()
                .map { pkg ->
                    AppInfo(
                        packageName = pkg,
                        label = packageManager.getApplicationInfo(pkg, 0)
                            .loadLabel(packageManager).toString(),
                        blocked = blocked.contains(pkg)
                    )
                }
                .sortedWith(compareByDescending<AppInfo> { it.blocked }.thenBy { it.label })
            _apps.postValue(installed)
        }
    }

    fun toggleApp(packageName: String) {
        viewModelScope.launch {
            context.settingsDataStore.updateData { current ->
                val blockedList = current.blockedMusicPackagesList.toMutableList()
                if (blockedList.contains(packageName)) blockedList.remove(packageName)
                else blockedList.add(packageName)
                current.toBuilder().clearBlockedMusicPackages()
                    .addAllBlockedMusicPackages(blockedList).build()
            }
            _apps.postValue(_apps.value?.map {
                if (it.packageName == packageName) it.copy(blocked = !it.blocked) else it
            })
        }
    }
}
