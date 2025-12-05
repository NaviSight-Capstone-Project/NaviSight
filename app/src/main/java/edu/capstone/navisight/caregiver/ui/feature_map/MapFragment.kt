package edu.capstone.navisight.caregiver.ui.feature_map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import edu.capstone.navisight.BuildConfig
import edu.capstone.navisight.R
import edu.capstone.navisight.caregiver.model.Geofence
import edu.capstone.navisight.caregiver.model.Viu
import edu.capstone.navisight.caregiver.ui.feature_geofence.GeofenceViewModel
import edu.capstone.navisight.caregiver.ui.feature_map.mapScreen.MapScreen
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var composeView: ComposeView

    private val mapState = mutableStateOf<MapLibreMap?>(null)

    private val mapViewModel: MapViewModel by viewModels()
    private val geofenceViewModel: GeofenceViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_map, container, false)
        mapView = root.findViewById(R.id.mapView)
        composeView = root.findViewById(R.id.map_compose_overlay)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        setupComposeOverlay()
        return root
    }

    override fun onMapReady(map: MapLibreMap) {
        this.mapState.value = map

        val styleUrl = "https://api.maptiler.com/maps/satellite/style.json?key=${BuildConfig.MAPTILER_API_KEY}"

        map.setStyle(styleUrl) {
            observeSelectedViuCamera()
        }

        map.addOnMapLongClickListener { point ->
            mapViewModel.onMapLongPress(point)
            true
        }
    }

    private fun setupComposeOverlay() {
        composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val vius by mapViewModel.vius.collectAsState()
                val selectedViu by mapViewModel.selectedViu.collectAsState()
                val isPrimary by mapViewModel.isPrimary.collectAsState()
                val geofences by geofenceViewModel.geofences.collectAsState()
                val longPressedLatLng by mapViewModel.longPressedLatLng.collectAsState()
                val selectedGeofence by mapViewModel.selectedGeofence.collectAsState()

                // State to control Geofence List Dialog visibility
                var showGeofenceList by remember { mutableStateOf(false) }

                LaunchedEffect(selectedViu?.uid) {
                    val uid = selectedViu?.uid
                    mapViewModel.selectViu(uid)

                    if (uid != null) {
                        geofenceViewModel.loadGeofencesForViu(uid)
                    } else {
                        geofenceViewModel.clearGeofences()
                    }
                }

                MapScreen(
                    vius = vius,
                    selectedViu = selectedViu,
                    geofences = geofences,
                    isPrimary = isPrimary,

                    map = mapState.value,

                    mapView = mapView,
                    longPressedLatLng = longPressedLatLng,
                    selectedGeofence = selectedGeofence,
                    onViuSelected = { uid -> mapViewModel.selectViu(uid) },
                    onRecenterClick = { recenterCamera(selectedViu) },

                    // Added: Handle the new list button click
                    onGeofenceListClick = { showGeofenceList = true },

                    onDismissAddDialog = { mapViewModel.dismissAddGeofenceDialog() },
                    onAddGeofence = { name, location, radius ->
                        if (isPrimary) {
                            selectedViu?.uid?.let { viuUid ->
                                val geoPoint = com.google.firebase.firestore.GeoPoint(location.latitude, location.longitude)
                                geofenceViewModel.addGeofence(viuUid, name, geoPoint, radius)
                            }
                        }
                        mapViewModel.dismissAddGeofenceDialog()
                    },
                    onGeofenceSelected = { mapViewModel.selectGeofence(it) },
                    onDismissDetailsDialog = { mapViewModel.dismissGeofenceDetailsDialog() },
                    onDeleteGeofence = { id ->
                        if(isPrimary) {
                            geofenceViewModel.deleteGeofence(id)
                        }
                        mapViewModel.dismissGeofenceDetailsDialog()
                    }
                )

                // Render the Geofence List Dialog when requested
                if (showGeofenceList) {
                    GeofenceListDialog(
                        geofences = geofences,
                        onDismiss = { showGeofenceList = false },
                        onGeofenceClick = { geofence ->
                            showGeofenceList = false
                            recenterToGeofence(geofence)
                        }
                    )
                }
            }
        }
    }

    private fun observeSelectedViuCamera() {
        viewLifecycleOwner.lifecycleScope.launch {
            mapViewModel.selectedViu
                .map { it?.uid }
                .distinctUntilChanged()
                .collect { uid ->
                    if (uid != null) recenterCamera(mapViewModel.selectedViu.value)
                }
        }
    }

    private fun recenterCamera(viu: Viu?) {
        viu?.location?.let {
            mapState.value?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 16.0))
        }
    }

    // New helper to jump to a specific geofence
    private fun recenterToGeofence(geofence: Geofence) {
        // Assuming geofence.location is a GeoPoint or similar object with latitude/longitude
        geofence.location?.let { loc ->
            val lat = loc.latitude
            val lng = loc.longitude
            mapState.value?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 17.0))
        }
    }


    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onStop() { super.onStop(); mapView.onStop() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
    override fun onDestroyView() {
        super.onDestroyView()
        mapState.value = null
        mapView.onDestroy()
    }
}

// --- New Composable for the Geofence List UI ---
@Composable
fun GeofenceListDialog(
    geofences: List<Geofence>,
    onDismiss: () -> Unit,
    onGeofenceClick: (Geofence) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Saved Geofences",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6041EC),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (geofences.isEmpty()) {
                    Text(
                        text = "No geofences found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(geofences) { geofence ->
                            GeofenceListItem(geofence = geofence, onClick = { onGeofenceClick(geofence) })
                            if (geofence != geofences.last()) {
                                Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GeofenceListItem(
    geofence: Geofence,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Place,
            contentDescription = null,
            tint = Color(0xFF6041EC),
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(
                text = geofence.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Radius: ${geofence.radius.toInt()}m",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}