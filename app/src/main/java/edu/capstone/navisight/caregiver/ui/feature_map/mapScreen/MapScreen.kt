package edu.capstone.navisight.caregiver.ui.feature_map.mapScreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import edu.capstone.navisight.R
import edu.capstone.navisight.caregiver.model.Geofence
import edu.capstone.navisight.caregiver.model.Viu
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

private val PrimaryViolet = Color(0xFF6041EC)
private val LightViolet = Color(0xFFB644F1)
private val SharedGradient = Brush.horizontalGradient(
    colors = listOf(LightViolet, PrimaryViolet)
)

private val DialogBackground = Color.White
private val SelectedBorderColor = PrimaryViolet
private val UnselectedBorderColor = Color(0xFFE0E0E0)

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
    onGeofenceListClick: () -> Unit,
    currentStyleIndex: Int,
    onMapStyleChange: (Int) -> Unit,
    onDismissAddDialog: () -> Unit,
    onAddGeofence: (String, LatLng, Double) -> Unit,
    onGeofenceSelected: (Geofence) -> Unit,
    onDismissDetailsDialog: () -> Unit,
    onDeleteGeofence: (String) -> Unit
) {
    val navBarInsets = WindowInsets.navigationBars.asPaddingValues()
    val bottomPadding = navBarInsets.calculateBottomPadding() + 100.dp

    var showStyleDialog by remember { mutableStateOf(false) }

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
                    .padding(top = 48.dp, end = 16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                MapStyleButton(onClick = { showStyleDialog = true })
            }

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
                MapControlButtons(
                    onRecenterClick = onRecenterClick,
                    onGeofenceListClick = onGeofenceListClick
                )
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

            if (showStyleDialog) {
                MapStyleSelectorDialog(
                    selectedIndex = currentStyleIndex,
                    onStyleSelected = { index ->
                        onMapStyleChange(index)
                    },
                    onDismiss = { showStyleDialog = false }
                )
            }
        }
    }
}

@Composable
fun MapStyleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val buttonShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .size(50.dp)
            .shadow(
                elevation = 8.dp,
                shape = buttonShape,
                ambientColor = PrimaryViolet.copy(alpha = 0.3f),
                spotColor = PrimaryViolet.copy(alpha = 0.5f)
            )
            .clip(buttonShape)
            .background(SharedGradient)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color.White.copy(alpha = 0.3f)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_mapstyle),
            contentDescription = "Change map style",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun MapStyleSelectorDialog(
    selectedIndex: Int,
    onStyleSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DialogBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SharedGradient)
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Map Style",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }


                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MapStyleOption(
                            label = "Default",
                            imageResId = R.drawable.mapstyle_default,
                            isSelected = selectedIndex == 0,
                            modifier = Modifier.weight(1f),
                            onClick = { onStyleSelected(0) }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        MapStyleOption(
                            label = "Satellite",
                            imageResId = R.drawable.mapstyle_satellite,
                            isSelected = selectedIndex == 1,
                            modifier = Modifier.weight(1f),
                            onClick = { onStyleSelected(1) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MapStyleOption(
                            label = "Hybrid",
                            imageResId = R.drawable.mapstyle_hybrid,
                            isSelected = selectedIndex == 2,
                            modifier = Modifier.weight(1f),
                            onClick = { onStyleSelected(2) }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        MapStyleOption(
                            label = "Basic",
                            imageResId = R.drawable.mapstyle_basic,
                            isSelected = selectedIndex == 3,
                            modifier = Modifier.weight(1f),
                            onClick = { onStyleSelected(3) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MapStyleOption(
    label: String,
    imageResId: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) SelectedBorderColor else UnselectedBorderColor
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val textColor = if (isSelected) SelectedBorderColor else Color.Gray
    val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1.3f)
                .fillMaxWidth()
                .border(BorderStroke(borderWidth, borderColor), RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF1F3F4))
        ) {
            Image(
                painter = painterResource(id = imageResId),
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PrimaryViolet.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = PrimaryViolet,
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.White, CircleShape)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            fontWeight = fontWeight,
            textAlign = TextAlign.Center
        )
    }
}