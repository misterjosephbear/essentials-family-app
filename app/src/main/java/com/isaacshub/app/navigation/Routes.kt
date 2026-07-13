package com.isaacshub.app.navigation

object Routes {
    const val LANDING = "landing"

    const val SLEEP_HOME = "sleep_home"
    const val SLEEP_HISTORY = "sleep_history"
    const val SLEEP_SETTINGS = "sleep_settings"
    const val EDIT_SESSION_BASE = "edit_session"
    const val EDIT_SESSION_ARG = "sessionId"
    const val EDIT_SESSION_PATTERN = "$EDIT_SESSION_BASE/{$EDIT_SESSION_ARG}"

    const val TIME_HOME = "time_home"
    const val TIME_ROUTES = "time_routes"
    const val TIME_SETTINGS = "time_settings"
    const val TIME_SCHEDULE = "time_schedule"
    const val EDIT_TIME_ENTRY_BASE = "edit_time_entry"
    const val EDIT_TIME_ENTRY_ARG = "entryId"
    const val EDIT_TIME_ENTRY_PATTERN = "$EDIT_TIME_ENTRY_BASE/{$EDIT_TIME_ENTRY_ARG}"
    const val EDIT_ROUTE_BASE = "edit_route"
    const val EDIT_ROUTE_ARG = "routeId"
    const val EDIT_ROUTE_PATTERN = "$EDIT_ROUTE_BASE/{$EDIT_ROUTE_ARG}"

    private const val NEW_TOKEN = "new"

    fun editSession(sessionId: Long?): String = "$EDIT_SESSION_BASE/${sessionId ?: NEW_TOKEN}"
    fun editTimeEntry(entryId: Long?): String = "$EDIT_TIME_ENTRY_BASE/${entryId ?: NEW_TOKEN}"
    fun editRoute(routeId: Long?): String = "$EDIT_ROUTE_BASE/${routeId ?: NEW_TOKEN}"

    fun parseId(arg: String?): Long? = arg?.takeIf { it != NEW_TOKEN }?.toLongOrNull()
}
