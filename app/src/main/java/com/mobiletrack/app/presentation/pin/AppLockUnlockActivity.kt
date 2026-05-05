package com.mobiletrack.app.presentation.pin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mobiletrack.app.presentation.theme.MobileTrackTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AppLockUnlockActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE = "package_name"
        const val EXTRA_APP_NAME = "app_name"

        // Set of packages unlocked during the current screen-on session.
        // Cleared by BlockerAccessibilityService on SCREEN_OFF.
        val unlockedPackages: MutableSet<String> = java.util.Collections.synchronizedSet(mutableSetOf())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: run { finish(); return }
        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: pkg

        setContent {
            MobileTrackTheme {
                PinVerifyHost(
                    kind = PinKind.AppLock,
                    title = "Unlock $appName",
                    subtitle = "Enter your App-Lock PIN to continue",
                    onSuccess = {
                        unlockedPackages.add(pkg)
                        finish()
                    }
                )
            }
        }
    }

    override fun onBackPressed() {
        // Send to home instead of dropping back into the locked app
        val home = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(home)
        finish()
    }
}
