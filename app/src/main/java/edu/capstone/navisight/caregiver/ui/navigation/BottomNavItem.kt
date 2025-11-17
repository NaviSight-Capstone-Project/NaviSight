package edu.capstone.navisight.caregiver.ui.navigation

import androidx.annotation.DrawableRes
import edu.capstone.navisight.R

sealed class BottomNavItem(
    val index: Int,
    val label: String,
    @DrawableRes val iconRes: Int
) {
    object Track : BottomNavItem(0, "Track", R.drawable.ic_track)
    object Records : BottomNavItem(1, "Records", R.drawable.ic_records)
    object Stream : BottomNavItem(2, "Live Call", R.drawable.ic_camera_on)
    object Notification : BottomNavItem(3, "Notifications", R.drawable.ic_notification)
    object Settings : BottomNavItem(4, "Settings", R.drawable.ic_settings)
}
