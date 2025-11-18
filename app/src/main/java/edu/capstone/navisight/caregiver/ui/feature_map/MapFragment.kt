package edu.capstone.navisight.caregiver.ui.feature_map

import edu.capstone.navisight.BuildConfig
import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.IntOffset
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import edu.capstone.navisight.R
import edu.capstone.navisight.caregiver.domain.connectionUseCase.GetAllPairedViusUseCase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback
import kotlin.math.roundToInt

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private var map: MapLibreMap? = null

    private val viewModel = MapViewModel(GetAllPairedViusUseCase())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_map, container, false)

        mapView = root.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        val composeContainer =
            root.findViewById<androidx.compose.ui.platform.ComposeView>(R.id.map_compose_overlay)

        val mapViewRef = mapView
        val composeViewRef = composeContainer

        composeContainer.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val vius by viewModel.vius.collectAsState()
                val selectedViu by viewModel.selectedViu.collectAsState()

                var viuScreenOffset by remember { mutableStateOf(IntOffset.Zero) }
                val currentViu by rememberUpdatedState(newValue = selectedViu)

                // Converts screen coordinates from MapLibre to Compose offsets
                fun PointF.toComposeOffset(): IntOffset {
                    val mapLoc = IntArray(2)
                    val composeLoc = IntArray(2)
                    mapViewRef.getLocationOnScreen(mapLoc)
                    composeViewRef.getLocationOnScreen(composeLoc)

                    val absoluteX = mapLoc[0] + this.x
                    val absoluteY = mapLoc[1] + this.y

                    val relativeX = absoluteX - composeLoc[0]
                    val relativeY = absoluteY - composeLoc[1]

                    return IntOffset(relativeX.roundToInt(), relativeY.roundToInt())
                }

                // Keeps locator pin in correct screen position while moving camera
                DisposableEffect(map) {
                    val moveListener = MapLibreMap.OnCameraMoveListener {
                        val viu = currentViu ?: return@OnCameraMoveListener
                        viu.location?.let { loc ->
                            map?.projection?.toScreenLocation(LatLng(loc.latitude, loc.longitude))
                                ?.let { pointF ->
                                    viuScreenOffset = pointF.toComposeOffset()
                                }
                        }
                    }

                    val idleListener = MapLibreMap.OnCameraIdleListener {
                        val viu = currentViu ?: return@OnCameraIdleListener
                        viu.location?.let { loc ->
                            map?.projection?.toScreenLocation(LatLng(loc.latitude, loc.longitude))
                                ?.let { pointF ->
                                    viuScreenOffset = pointF.toComposeOffset()
                                }
                        }
                    }

                    map?.addOnCameraMoveListener(moveListener)
                    map?.addOnCameraIdleListener(idleListener)

                    map?.let { m ->
                        val viu = currentViu
                        viu?.location?.let {
                            m.projection?.toScreenLocation(LatLng(it.latitude, it.longitude))
                                ?.let { p -> viuScreenOffset = p.toComposeOffset() }
                        }
                    }

                    onDispose {
                        map?.removeOnCameraMoveListener(moveListener)
                        map?.removeOnCameraIdleListener(idleListener)
                    }
                }


                MaterialTheme {
                    Box(modifier = Modifier.fillMaxSize()) {

                        // Main capsule selector UI
                        MapScreen(
                            vius = vius,
                            selectedViu = selectedViu,
                            onViuSelected = { uid -> viewModel.selectViu(uid) }
                        )

                        // locator pin
                        Box(
                            modifier = Modifier
                                .offset { viuScreenOffset.copy(y = viuScreenOffset.y - 30) },
                            contentAlignment = Alignment.Center
                        ) {
                            LocatorPin()
                        }

                        // recenter Button
                        Box(
                            modifier = Modifier
                                .fillMaxSize(),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            RecenterButton {
                                viewModel.selectedViu.value?.location?.let {
                                    map?.animateCamera(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(it.latitude, it.longitude), 16.0
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        return root
    }

    override fun onMapReady(map: MapLibreMap) {
        this.map = map
        val apiKey = BuildConfig.MAPTILER_API_KEY
        val styleUrl =
            "https://api.maptiler.com/maps/satellite/style.json?key=$apiKey"

        map.setStyle(styleUrl) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.selectedViu.collectLatest { viu ->
                    viu?.location?.let {
                        val latLng = LatLng(it.latitude, it.longitude)
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16.0))
                    }
                }
            }
        }
    }

    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onStop() { super.onStop(); mapView.onStop() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDestroy()
    }
}