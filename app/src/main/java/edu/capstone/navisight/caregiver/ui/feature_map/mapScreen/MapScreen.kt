package edu.capstone.navisight.caregiver.ui.feature_map.mapScreen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.capstone.navisight.caregiver.model.Geofence
import edu.capstone.navisight.caregiver.model.Viu
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

@Composable
fun MapScreen(
    vius: List<Viu>,
    selectedViu: Viu?,
    geofences: List<Geofence>,
    isPrimary: Boolean,
    map: MapLibreMap?,
    mapView: MapView?,
    longPressedLatLng: LatLng?,
    selectedGeofence: Geofence?,
    onViuSelected: (String) -> Unit,
    onRecenterClick: () -> Unit,
    onDismissAddDialog: () -> Unit,
    onAddGeofence: (String, LatLng, Double) -> Unit,
    onGeofenceSelected: (Geofence) -> Unit,
    onDismissDetailsDialog: () -> Unit,
    onDeleteGeofence: (String) -> Unit
) {
    val navBarInsets = WindowInsets.navigationBars.asPaddingValues()
    val bottomPadding = navBarInsets.calculateBottomPadding() + 100.dp

    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {

            ViuAndGeofencePins(
                selectedViu = selectedViu,
                geofences = geofences,
                map = map,
                mapView = mapView,
                onGeofenceSelected = onGeofenceSelected
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = bottomPadding),
                contentAlignment = Alignment.BottomStart
            ) {
                ViuSelector(
                    vius = vius,
                    selectedViu = selectedViu,
                    onViuSelected = onViuSelected
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = bottomPadding, end = 16.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                RecenterButton(onClick = onRecenterClick)
            }

            if (longPressedLatLng != null) {
                AddGeofenceDialog(
                    latLng = longPressedLatLng,
                    onDismiss = onDismissAddDialog,
                    onAdd = onAddGeofence
                )
            }

            if (selectedGeofence != null) {
                GeofenceDetailsDialog(
                    geofence = selectedGeofence,
                    canEdit = isPrimary,
                    onDismiss = onDismissDetailsDialog,
                    onDelete = onDeleteGeofence
                )
            }
        }
    }
}