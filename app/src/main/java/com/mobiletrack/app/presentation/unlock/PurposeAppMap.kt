package com.mobiletrack.app.presentation.unlock

const val EXTRA_PURPOSE = "purpose"

val PURPOSE_PACKAGE_HINTS = mapOf(
    "Check messages" to listOf("whatsapp", "telegram", "signal", "messages", "instagram", "snapchat"),
    "Make a call" to listOf("dialer", "phone", "contacts", "whatsapp", "telegram"),
    "Navigation / Maps" to listOf("maps", "waze", "uber", "ola"),
    "Music / Podcast" to listOf("spotify", "youtube", "music", "podcast", "gaana", "jiosaavn"),
    "Work / Email" to listOf("gmail", "outlook", "slack", "teams", "notion", "drive"),
    "Camera" to listOf("camera"),
    "Alarm / Timer" to listOf("clock", "alarm", "timer"),
    "Calendar / Tasks" to listOf("calendar", "tasks", "todoist", "keep"),
    "Stocks / Finance" to listOf("kite", "zerodha", "tickertape", "groww", "upstox", "angelone", "mstock"),
    // PURPOSE_SEARCH_APP is handled separately — no hint mapping needed
)

fun matchesPurpose(packageName: String, purpose: String): Boolean {
    val hints = PURPOSE_PACKAGE_HINTS[purpose].orEmpty()
    if (hints.isEmpty()) return true

    val normalizedPackageName = packageName.lowercase()
    return hints.any { hint -> normalizedPackageName.contains(hint.lowercase()) }
}
