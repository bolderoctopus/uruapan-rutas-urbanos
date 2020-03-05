package com.rico.omarw.rutasuruapan

object Constants {
    const val DEBUG_TAG = "debug_tag"
    /**
     * In meters
     */
    const val WALK_DIST_LIMIT_DEFAULT = 1000
    const val METER_IN_ANGULAR_LAT_LNG = 0.000009229349583

    const val COMPLETION_THRESHOLD = 3
    const val CAMERA_PADDING_MARKER = 150
    const val WD_WEIGHT = 100

    const val INITIAL_ZOOM = 13f

    const val BOUNCE_DURATION = 2500f

    object PreferenceKeys{
        const val DIALOG_1_SHOWN = "has_inf_dialog1_been_shown"
        const val DIALOG_2_SHOWN = "has_inf_dialog2_been_shown"
        const val DISCLAIMER_SHOWN = "has_disclaimer_been_shown"

        //Debug preferences
        const val DRAW_SQUARES = "draw_squares"
        const val DRAW_STARTEND_POINTS = "draw_startend_points"
        const val DRAW_ROUTE_POINTS = "draw_route_points"
        const val WALK_DIST_LIMIT = "walk_dist_limit"
        const val RESOLVE_LOCATIONS_TO_ADDRESSES = "resolve_locations_to_addresses"
    }

}