package com.mobiletrack.app.service

/**
 * In-memory screen state shared between TrackingService (writer) and UnlockHandler (reader).
 * Persists across the foreground service lifetime.
 */
object ScreenState {
    /** Timestamp of the last ACTION_SCREEN_OFF event. */
    @Volatile
    var lastScreenOffAt: Long = 0L
}
