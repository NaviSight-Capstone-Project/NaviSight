package edu.capstone.navisight.caregiver.ui.feature_map

import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.IntOffset
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import edu.capstone.navisight.R
import edu.capstone.navisight.caregiver.model.Geofence
import edu.capstone.navisight.caregiver.model.Viu
import edu.capstone.navisight.caregiver.ui.feature_geofence.GeofenceViewModel
import edu.capstone.navisight.caregiver.ui.feature_geofence.PositionedGeofencePin
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback
import kotlin.math.roundToInt

import edu.capstone.navisight.BuildConfig

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.unit.dp
import org.maplibre.android.geometry.LatLng

import androidx.compose.material3.Slider

import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.text.font.FontWeight

private data class GeofenceScreenState(
    val offset: IntOffset = IntOffset.Zero,
    val radiusInPixels: Float = 0f,
    val alpha: Float = 1.0f
)

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
        val apiKey = BuildConfig.MAPTILER_API_KEY
        val styleUrl =
            "https://api.maptiler.com/maps/satellite/style.json?key=$apiKey"

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
                val geofences by geofenceViewModel.geofences.collectAsState()

                val longPressedLatLng by mapViewModel.longPressedLatLng.collectAsState()
                val selectedGeofence by mapViewModel.selectedGeofence.collectAsState()

                MapDataObserver(
                    selectedViu = selectedViu,
                    onViuSelected = { uid ->
                        mapViewModel.selectViu(uid)
                        if (uid != null) {
                            geofenceViewModel.loadGeofencesForViu(uid)
                        } else {
                            geofenceViewModel.clearGeofences()
                        }
                    }
                )

                MapOverlayUi(
                    vius = vius,
                    selectedViu = selectedViu,
                    geofences = geofences,
                    onViuSelected = { uid -> mapViewModel.selectViu(uid) },
                    onRecenterClick = { recenterCamera(selectedViu) },

                    longPressedLatLng = longPressedLatLng,
                    onDismissDialog = { mapViewModel.dismissAddGeofenceDialog() },
                    onAddGeofence = { name, location, radius ->
                        selectedViu?.uid?.let { viuUid ->
                            val geoPoint = com.google.firebase.firestore.GeoPoint(location.latitude, location.longitude)
                            geofenceViewModel.addGeofence(viuUid, name, geoPoint, radius)
                        }
                        mapViewModel.dismissAddGeofenceDialog()
                    },
                    selectedGeofence = selectedGeofence,
                    onGeofenceSelected = { geofence ->
                        mapViewModel.selectGeofence(geofence)
                    },
                    onDismissDetailsDialog = {
                        mapViewModel.dismissGeofenceDetailsDialog()
                    },
                    onDeleteGeofence = { geofenceId ->
                        geofenceViewModel.deleteGeofence(geofenceId)
                        mapViewModel.dismissGeofenceDetailsDialog()
                    }


                )
            }
        }
    }

    @Composable
    private fun MapOverlayUi(
        vius: List<Viu>,
        selectedViu: Viu?,
        geofences: List<Geofence>,
        onViuSelected: (String) -> Unit,
        onRecenterClick: () -> Unit,

        longPressedLatLng: LatLng?,
        onDismissDialog: () -> Unit,
        onAddGeofence: (String, LatLng, Double) -> Unit,
        selectedGeofence: Geofence?,
        onGeofenceSelected: (Geofence) -> Unit,
        onDismissDetailsDialog: () -> Unit,
        onDeleteGeofence: (String) -> Unit

    ) {
        MaterialTheme {
            Box(modifier = Modifier.fillMaxSize()) {

                ViuAndGeofencePins(
                    selectedViu = selectedViu,
                    geofences = geofences,
                    onGeofenceSelected = onGeofenceSelected
                )

                MapScreen(
                    vius = vius,
                    selectedViu = selectedViu,
                    onViuSelected = onViuSelected
                )

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    RecenterButton(onClick = onRecenterClick)
                }

                if (longPressedLatLng != null) {
                    AddGeofenceDialog(
                        latLng = longPressedLatLng,
                        onDismiss = onDismissDialog,
                        onAdd = onAddGeofence
                    )
                }

                if (selectedGeofence != null) {
                    GeofenceDetailsDialog(
                        geofence = selectedGeofence,
                        onDismiss = onDismissDetailsDialog,
                        onDelete = { onDeleteGeofence(selectedGeofence.id) }
                    )
                }
            }
        }
    }

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)

    @Composable
    private fun AddGeofenceDialog(
        latLng: LatLng,
        onDismiss: () -> Unit,
        onAdd: (name: String, location: LatLng, radius: Double) -> Unit
    ) {
        var name by remember { mutableStateOf("") }

        var sliderValue by remember { mutableStateOf(50f) }

        val radiusRange = 30f..100f

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add Geofence") },
            text = {
                Column {
                    Text("Location: ${latLng.latitude.format(4)}, ${latLng.longitude.format(4)}")
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Geofence Name") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Radius: ${sliderValue.roundToInt()} meters",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Slider(
                        value = sliderValue,
                        onValueChange = { newValue ->
                            sliderValue = (newValue.roundToInt() / 10 * 10).toFloat()
                        },
                        valueRange = radiusRange,
                        steps = 6
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onAdd(name, latLng, sliderValue.roundToInt().toDouble())
                        }
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    @Composable
    private fun MapDataObserver(
        selectedViu: Viu?,
        onViuSelected: (String?) -> Unit
    ) {

        LaunchedEffect(selectedViu?.uid) {
            onViuSelected(selectedViu?.uid)
        }
    }


    private fun observeSelectedViuCamera() {
        viewLifecycleOwner.lifecycleScope.launch {
            mapViewModel.selectedViu
                .map { it?.uid }
                .distinctUntilChanged()
                .collect { uid ->
                    if (uid != null) {
                        recenterCamera(mapViewModel.selectedViu.value)
                    }
                }
        }
    }

    @Composable
    private fun ViuAndGeofencePins(
        selectedViu: Viu?,
        geofences: List<Geofence>,
        onGeofenceSelected: (Geofence) -> Unit
    ) {
        var viuScreenOffset by remember { mutableStateOf(IntOffset.Zero) }

        val geofenceStates = remember { mutableStateMapOf<String, GeofenceScreenState>() }

        val mapLoc = remember { IntArray(2) }
        val composeLoc = remember { IntArray(2) }

        val maxZoomForFade: Double = 14.0
        val minZoomForFade: Double = 11.0

        fun PointF.toComposeOffset(): IntOffset {
            val absoluteX = mapLoc[0] + this.x
            val absoluteY = mapLoc[1] + this.y
            val relativeX = absoluteX - composeLoc[0]
            val relativeY = absoluteY - composeLoc[1]
            return IntOffset(relativeX.roundToInt(), relativeY.roundToInt())
        }

        DisposableEffect(map, geofences, selectedViu) {
            val updatePositions = {
                mapView.getLocationOnScreen(mapLoc)
                composeView.getLocationOnScreen(composeLoc)

                val currentZoom = map?.cameraPosition?.zoom ?: maxZoomForFade

                val alpha = ((currentZoom - minZoomForFade) / (maxZoomForFade - minZoomForFade))
                    .coerceIn(0.0, 1.0).toFloat()


                selectedViu?.location?.let { loc ->
                    map?.projection?.toScreenLocation(LatLng(loc.latitude, loc.longitude))
                        ?.let { viuScreenOffset = it.toComposeOffset() }
                }

                geofences.forEach { geo ->
                    geo.location?.let { loc ->
                        val metersPerPixel = map?.projection?.getMetersPerPixelAtLatitude(loc.latitude) ?: 1.0
                        val radiusInPixels = (geo.radius / metersPerPixel).toFloat()

                        val offset = map?.projection?.toScreenLocation(LatLng(loc.latitude, loc.longitude))
                            ?.toComposeOffset() ?: IntOffset.Zero

                        geofenceStates[geo.id] = GeofenceScreenState(
                            offset = offset,
                            radiusInPixels = radiusInPixels,
                            alpha = alpha
                        )
                    }
                }
            }

            val moveListener = MapLibreMap.OnCameraMoveListener { updatePositions() }
            val idleListener = MapLibreMap.OnCameraIdleListener { updatePositions() }

            map?.addOnCameraMoveListener(moveListener)
            map?.addOnCameraIdleListener(idleListener)
            updatePositions()

            onDispose {
                map?.removeOnCameraMoveListener(moveListener)
                map?.removeOnCameraIdleListener(idleListener)
            }
        }

        geofences.forEach { geofence ->
            val state = geofenceStates[geofence.id] ?: GeofenceScreenState()

            Box(modifier = Modifier.alpha(state.alpha)) {
                PositionedGeofencePin(
                    offset = state.offset,
                    radius = state.radiusInPixels,
                    onClick = { onGeofenceSelected(geofence) }
                )
            }
        }

        Box(
            modifier = Modifier
                .offset { viuScreenOffset.copy(y = viuScreenOffset.y - 30) },
            contentAlignment = Alignment.Center
        ) {
            LocatorPin()
        }
    }

    @Composable
    private fun GeofenceDetailsDialog(
        geofence: Geofence,
        onDismiss: () -> Unit,
        onDelete: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(geofence.name, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    // Display its information
                    Text("Radius: ${geofence.radius.format(0)} meters")
                    Spacer(modifier = Modifier.height(8.dp))
                    geofence.location?.let {
                        Text("Location: ${it.latitude.format(4)}, ${it.longitude.format(4)}")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
    private fun recenterCamera(viu: Viu?) {
        viu?.location?.let {
            map?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(it.latitude, it.longitude), 16.0
                )
            )
        }
    }

    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onStop() { super.onStop(); mapView.onStop() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
    override fun onDestroyView() {
        super.onDestroyView()
        map?.clear()
        mapView.onDestroy()
        map = null
    }
}