package com.mobiletrack.app.presentation.applimits

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mobiletrack.app.data.local.dao.AppRuleDao
import com.mobiletrack.app.data.local.entity.AppRule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InstalledApp(val appName: String, val packageName: String)

@HiltViewModel
class AppLimitsViewModel @Inject constructor(
    private val appRuleDao: AppRuleDao,
    application: Application
) : AndroidViewModel(application) {

    val allRules = appRuleDao.getAllRules()

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = getApplication<Application>().packageManager
            val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            }
            val apps = pm.queryIntentActivities(intent, 0)
                .map { ri ->
                    InstalledApp(
                        appName = ri.loadLabel(pm).toString(),
                        packageName = ri.activityInfo.packageName
                    )
                }
                .distinctBy { it.packageName }
                .sortedBy { it.appName }
            _installedApps.value = apps
        }
    }

    fun setLimit(packageName: String, appName: String, limitMinutes: Int, scrollLimit: Int = 0) {
        viewModelScope.launch {
            val existing = appRuleDao.getRuleForApp(packageName)
            appRuleDao.upsert(
                (existing ?: AppRule(packageName, appName)).copy(
                    dailyLimitMinutes = limitMinutes,
                    dailyScrollLimit = scrollLimit
                )
            )
        }
    }

    fun toggleBlock(rule: AppRule) {
        viewModelScope.launch {
            appRuleDao.upsert(rule.copy(isBlocked = !rule.isBlocked))
        }
    }

    fun deleteRule(packageName: String) {
        viewModelScope.launch {
            appRuleDao.deleteByPackage(packageName)
        }
    }
}
