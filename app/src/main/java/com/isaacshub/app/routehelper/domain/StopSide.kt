package com.isaacshub.app.routehelper.domain

/** Which of the four one-tap buttons was used to route a stop, and the canned note it produces. */
enum class StopSide {
    LEFT,
    RIGHT,
    IN_DRIVE,
    NONE;

    val note: String?
        get() = when (this) {
            LEFT -> "House on the left side of the road"
            RIGHT -> "House on the right side of the road"
            IN_DRIVE -> "Mailbox in driveway"
            NONE -> null
        }
}
