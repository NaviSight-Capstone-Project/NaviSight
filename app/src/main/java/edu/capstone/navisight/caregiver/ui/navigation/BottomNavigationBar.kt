package edu.capstone.navisight.caregiver.ui.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BottomNavigationBar(
    currentIndex: Int,
    onItemSelected: (Int) -> Unit,
    iconSize: Dp = 24.dp
) {
    val items = listOf(
        BottomNavItem.Records,
        BottomNavItem.Stream,
        BottomNavItem.Track,
        BottomNavItem.Notification,
        BottomNavItem.Settings
    )

    val colorBlue = Color(0xFF54AFF2)
    val colorPurple = Color(0xFFAB76F4)
    val unselectedColor = Color(0xFF9E9E9E)

    val iconGradientBrush = Brush.verticalGradient(
        colors = listOf(colorBlue, colorPurple)
    )

    val indicatorGradientBrush = Brush.horizontalGradient(
        colors = listOf(colorBlue, colorPurple)
    )

    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val itemCount = items.size
    val itemWidth = screenWidthDp / itemCount
    val indicatorWidth = 40.dp

    val animatedOffset by animateDpAsState(
        targetValue = itemWidth * currentIndex + (itemWidth - indicatorWidth) / 2,
        label = "indicatorAnimation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Surface(
            shadowElevation = 20.dp,
            color = Color.White,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = 20f
                        ),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
            ) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 0.dp
                ) {
                    items.forEachIndexed { index, item ->
                        val selected = currentIndex == index

                        NavigationBarItem(
                            selected = selected,
                            onClick = { onItemSelected(index) },
                            icon = {
                                if (selected) {
                                    Icon(
                                        painter = painterResource(id = item.filledIcon),
                                        contentDescription = item.label,
                                        modifier = Modifier
                                            .size(iconSize)
                                            .graphicsLayer(alpha = 0.99f)
                                            .drawWithCache {
                                                onDrawWithContent {
                                                    drawContent()
                                                    drawRect(
                                                        brush = iconGradientBrush,
                                                        blendMode = BlendMode.SrcIn
                                                    )
                                                }
                                            },
                                        tint = Color.Unspecified
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = item.outlineIcon),
                                        contentDescription = item.label,
                                        modifier = Modifier.size(iconSize),
                                        tint = unselectedColor
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    fontSize = 11.sp,
                                    color = if (selected) colorPurple else unselectedColor
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .offset(x = animatedOffset)
                .width(indicatorWidth)
                .height(3.dp)
                .background(
                    brush = indicatorGradientBrush,
                    shape = RoundedCornerShape(50)
                )
        )
    }
}