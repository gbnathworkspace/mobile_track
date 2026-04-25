package com.mobiletrack.app.presentation.unlock

const val EXTRA_PURPOSE = "purpose"

val PURPOSE_PACKAGE_HINTS = mapOf(
    "Check messages" to listOf("whatsapp", "telegram", "signal", "messages", "instagram", "snapchat"),
    "Make a call" to listOf("dialer", "phone", "contacts", "whatsapp", "telegram"),
    "Scan & Pay" to listOf("gpay", "tez", "phonepe", "paytm", "bhim", "amazonpay", "cred", "scanner", "qr"),
    "Navigation / Maps" to listOf("maps", "waze", "uber", "ola"),
    "Music / Podcast" to listOf("spotify", "youtube", "music", "podcast", "gaana", "jiosaavn"),
    "Work / Email" to listOf("gmail", "outlook", "slack", "teams", "notion", "drive"),
    "Read / Books" to listOf("kindle", "play.books", "moon.reader", "kobo", "libby", "audible", "pocket", "instapaper", "readera", "lithium", "bookmate"),
    "Camera" to listOf("camera"),
    "Alarm / Timer" to listOf("clock", "alarm", "timer"),
    "Calendar / Tasks" to listOf("calendar", "tasks", "todoist", "keep"),
    "Stocks / Finance" to listOf("kite", "zerodha", "tickertape", "groww", "upstox", "angelone", "mstock"),
)

/**
 * Resolve hints for a purpose, preferring custom overrides from user preferences.
 */
fun getHintsForPurpose(purpose: String, customHints: Map<String, List<String>>): List<String> {
    return customHints[purpose] ?: PURPOSE_PACKAGE_HINTS[purpose].orEmpty()
}

fun matchesPurpose(packageName: String, purpose: String, customHints: Map<String, List<String>> = emptyMap()): Boolean {
    val hints = getHintsForPurpose(purpose, customHints)
    if (hints.isEmpty()) return true

    val normalizedPackageName = packageName.lowercase()
    return hints.any { hint -> normalizedPackageName.contains(hint.lowercase()) }
}
