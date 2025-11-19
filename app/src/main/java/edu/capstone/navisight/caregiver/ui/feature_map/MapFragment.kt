package edu.capstone.navisight.caregiver.ui.feature_map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import edu.capstone.navisight.BuildConfig
import edu.capstone.navisight.R
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
    private var map: MapLibreMap? = null

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
        this.map = map
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
                    map = map,
                    mapView = mapView,

                    longPressedLatLng = longPressedLatLng,
                    selectedGeofence = selectedGeofence,

                    onViuSelected = { uid -> mapViewModel.selectViu(uid) },
                    onRecenterClick = { recenterCamera(selectedViu) },

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
            map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 16.0))
        }
    }

    // Lifecycle Forwarding
    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onStop() { super.onStop(); mapView.onStop() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
    override fun onDestroyView() {
        super.onDestroyView()
        map = null
        mapView.onDestroy()
    }
}