package com.mobiletrack.app.presentation.applimits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobiletrack.app.data.local.dao.AppRuleDao
import com.mobiletrack.app.data.local.entity.AppRule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppLimitsViewModel @Inject constructor(
    private val appRuleDao: AppRuleDao
) : ViewModel() {

    val allRules = appRuleDao.getAllRules()

    fun setLimit(packageName: String, appName: String, limitMinutes: Int) {
        viewModelScope.launch {
            val existing = appRuleDao.getRuleForApp(packageName)
            appRuleDao.upsert(
                (existing ?: AppRule(packageName, appName)).copy(
                    dailyLimitMinutes = limitMinutes
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
