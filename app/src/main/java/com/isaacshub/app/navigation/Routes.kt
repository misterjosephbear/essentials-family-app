package com.isaacshub.app.navigation

object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val EDIT_SESSION_BASE = "edit_session"
    const val EDIT_SESSION_ARG = "sessionId"
    const val EDIT_SESSION_PATTERN = "$EDIT_SESSION_BASE/{$EDIT_SESSION_ARG}"
    private const val NEW_SESSION_TOKEN = "new"

    fun editSession(sessionId: Long?): String =
        "$EDIT_SESSION_BASE/${sessionId ?: NEW_SESSION_TOKEN}"

    fun parseSessionId(arg: String?): Long? = arg?.takeIf { it != NEW_SESSION_TOKEN }?.toLongOrNull()
}
