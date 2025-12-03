package edu.capstone.navisight.caregiver.ui.navigation

import androidx.annotation.DrawableRes
import edu.capstone.navisight.R

sealed class BottomNavItem(
    val index: Int,
    val label: String,
    @DrawableRes val filledIcon: Int,
    @DrawableRes val outlineIcon: Int
) {
    object Records : BottomNavItem(0, "Records", R.drawable.ic_records_filled, R.drawable.ic_records_outline)
    object Stream : BottomNavItem(1, "Live Call", R.drawable.ic_stream_filled, R.drawable.ic_stream_outline)
    object Track : BottomNavItem(2, "Track", R.drawable.ic_track_filled, R.drawable.ic_track_outline)
    object Notification : BottomNavItem(3, "Notification", R.drawable.ic_notification_filled, R.drawable.ic_notification_outline)
    object Settings : BottomNavItem(4, "Settings", R.drawable.ic_settings_filled, R.drawable.ic_settings_outline)
}