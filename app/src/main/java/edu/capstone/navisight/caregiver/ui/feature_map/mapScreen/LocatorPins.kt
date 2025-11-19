package edu.capstone.navisight.caregiver.ui.feature_map.mapScreen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import edu.capstone.navisight.caregiver.model.Geofence
import edu.capstone.navisight.caregiver.model.Viu
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import kotlin.math.roundToInt


data class GeofenceScreenState(
    val offset: IntOffset = IntOffset.Zero,
    val radiusInPixels: Float = 0f,
    val alpha: Float = 1.0f
)


@Composable
fun ViuAndGeofencePins(
    selectedViu: Viu?,
    geofences: List<Geofence>,
    map: MapLibreMap?,
    mapView: MapView?,
    onGeofenceSelected: (Geofence) -> Unit
) {
    var viuScreenOffset by remember { mutableStateOf(IntOffset.Zero) }
    val geofenceStates = remember { mutableStateMapOf<String, GeofenceScreenState>() }
    val mapLoc = remember { IntArray(2) }

    val updatePositions = {
        if (map != null && mapView != null) {
            mapView.getLocationOnScreen(mapLoc)

            val currentZoom = map.cameraPosition.zoom
            val maxZoomForFade = 14.0
            val minZoomForFade = 11.0
            val alpha = ((currentZoom - minZoomForFade) / (maxZoomForFade - minZoomForFade))
                .coerceIn(0.0, 1.0).toFloat()

            // Update VIU Pin
            selectedViu?.location?.let { loc ->
                val screenPoint = map.projection.toScreenLocation(LatLng(loc.latitude, loc.longitude))
                viuScreenOffset = IntOffset(screenPoint.x.roundToInt(), screenPoint.y.roundToInt())
            }

            // Update Geofence Pins
            geofences.forEach { geo ->
                geo.location?.let { loc ->
                    val metersPerPixel = map.projection.getMetersPerPixelAtLatitude(loc.latitude)
                    val radiusInPixels = (geo.radius / metersPerPixel).toFloat()
                    val screenPoint = map.projection.toScreenLocation(LatLng(loc.latitude, loc.longitude))

                    geofenceStates[geo.id] = GeofenceScreenState(
                        offset = IntOffset(screenPoint.x.roundToInt(), screenPoint.y.roundToInt()),
                        radiusInPixels = radiusInPixels,
                        alpha = alpha
                    )
                }
            }
        }
    }

    DisposableEffect(map, geofences, selectedViu) {
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

    // Render Geofences
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

    // Render VIU Pin
    if (selectedViu != null) {
        Box(
            modifier = Modifier
                .offset { viuScreenOffset }
                .offset(x = (-25).dp, y = (-25).dp),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(50.dp), contentAlignment = Alignment.Center) {
                LocatorPin()
            }
        }
    }
}



@Composable
fun PositionedGeofencePin(
    offset: IntOffset,
    radius: Float,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)

                val x = offset.x - placeable.width / 2
                val y = offset.y - placeable.height / 2

                layout(placeable.width, placeable.height) {
                    placeable.placeRelative(x, y)
                }
            }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        GeofenceRadiusPin(radiusInPixels = radius)
    }
}

@Composable
fun GeofenceRadiusPin(radiusInPixels: Float) {
    val outerCircleFillColor = Color(0x330060FF)
    val innerCircleColor = Color(0xFF0060FF)

    val infiniteTransition = rememberInfiniteTransition(label = "GeofencePulse")

    val pulseStrokeWidth by infiniteTransition.animateValue(
        initialValue = 2.dp,
        targetValue = 6.dp,
        typeConverter = Dp.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GeofenceStroke"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GeofenceAlpha"
    )

    val diameterInDp = with(LocalDensity.current) {
        (radiusInPixels * 2).toDp()
    }

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(diameterInDp)
                .background(outerCircleFillColor, CircleShape)
                .border(
                    width = pulseStrokeWidth,
                    color = Color.Black.copy(alpha = pulseAlpha),
                    shape = CircleShape
                )
        )

        // Inner solid circle
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(innerCircleColor, CircleShape)
        )
    }
}


private val PinVioletColor = Color(0xFF6041EC)
private val OuterPinSize = 17.dp
private val MiddlePinSize = 16.5.dp
private val InnerPinSize = 14.dp

@Composable
fun LocatorPin() {
    Box(contentAlignment = Alignment.Center) {
        PulsingGlow(modifier = Modifier.size(OuterPinSize))
        StaticPin(modifier = Modifier.size(OuterPinSize))
    }
}

@Composable
private fun PulsingGlow(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    Box(
        modifier = modifier
            .scale(pulseScale)
            .background(color = PinVioletColor.copy(alpha = pulseAlpha), shape = CircleShape)
    )
}

@Composable
private fun StaticPin(modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(PinVioletColor, CircleShape), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(MiddlePinSize).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(InnerPinSize).background(PinVioletColor, CircleShape))
        }
    }
}