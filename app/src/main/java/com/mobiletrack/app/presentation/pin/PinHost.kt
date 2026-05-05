package com.mobiletrack.app.presentation.pin

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.mobiletrack.app.data.preferences.UserPreferences
import com.mobiletrack.app.security.PinHasher
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

enum class PinKind { AppEntry, AppLock }

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PinPrefsEntryPoint {
    fun userPreferences(): UserPreferences
}

@Composable
fun userPreferences(): UserPreferences {
    val ctx = LocalContext.current.applicationContext
    return remember {
        EntryPointAccessors.fromApplication(ctx, PinPrefsEntryPoint::class.java).userPreferences()
    }
}

@Composable
fun PinVerifyHost(
    kind: PinKind,
    title: String,
    subtitle: String,
    onSuccess: () -> Unit
) {
    val prefs = userPreferences()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    PinEntryScreen(
        title = title,
        subtitle = subtitle,
        error = error,
        pin = pin,
        onDigit = { d ->
            if (pin.length < PIN_MAX_LEN) {
                pin += d.toString()
                error = null
                if (pin.length == PIN_MAX_LEN) {
                    val attempt = pin
                    scope.launch {
                        val hash = (if (kind == PinKind.AppEntry) prefs.appEntryPinHash else prefs.appLockPinHash).firstOrNull()
                        val salt = (if (kind == PinKind.AppEntry) prefs.appEntryPinSalt else prefs.appLockPinSalt).firstOrNull()
                        if (hash != null && salt != null && PinHasher.verify(attempt, hash, salt)) {
                            onSuccess()
                        } else {
                            error = "Wrong PIN"
                            pin = ""
                        }
                    }
                }
            }
        },
        onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
        modifier = Modifier
    )
}

@Composable
fun PinSetHost(
    kind: PinKind,
    onDone: () -> Unit
) {
    val prefs = userPreferences()
    var firstPin by remember { mutableStateOf<String?>(null) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val title = if (firstPin == null) "Set PIN" else "Confirm PIN"
    val subtitle = if (firstPin == null) "Enter a 4-digit PIN" else "Re-enter the same PIN"

    PinEntryScreen(
        title = title,
        subtitle = subtitle,
        error = error,
        pin = pin,
        onDigit = { d ->
            if (pin.length < PIN_MAX_LEN) {
                pin += d.toString()
                error = null
                if (pin.length == PIN_MAX_LEN) {
                    val entered = pin
                    if (firstPin == null) {
                        firstPin = entered
                        pin = ""
                    } else if (firstPin != entered) {
                        error = "PINs don't match"
                        firstPin = null
                        pin = ""
                    } else {
                        scope.launch {
                            val salt = PinHasher.newSalt()
                            val hash = PinHasher.hash(entered, salt)
                            if (kind == PinKind.AppEntry) prefs.setAppEntryPin(hash, salt)
                            else prefs.setAppLockPin(hash, salt)
                            onDone()
                        }
                    }
                }
            }
        },
        onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
    )
}
