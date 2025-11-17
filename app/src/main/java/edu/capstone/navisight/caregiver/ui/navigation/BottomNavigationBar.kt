package edu.capstone.navisight.caregiver.ui.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.sp

@Composable
fun BottomNavigationBar(
    currentIndex: Int,
    onItemSelected: (Int) -> Unit,
    iconSize: Dp = 24.dp
) {
    val items = listOf(
        BottomNavItem.Track,
        BottomNavItem.Records,
        BottomNavItem.Stream,
        BottomNavItem.Notification,
        BottomNavItem.Settings
    )

    val selectedColor = Color(0xFF6041EC)
    val unselectedColor = Color(0xFF9E9E9E)

    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val itemCount = items.size
    val itemWidth = screenWidthDp / itemCount
    val indicatorWidth = 40.dp

    val animatedOffset by animateDpAsState(
        targetValue = itemWidth * currentIndex + (itemWidth - indicatorWidth) / 2
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
                                Icon(
                                    painter = painterResource(id = item.iconRes),
                                    contentDescription = item.label,
                                    modifier = Modifier.size(iconSize)
                                )
                            },
                            label = { Text(text = item.label, fontSize=11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = selectedColor,
                                unselectedIconColor = unselectedColor,
                                selectedTextColor = selectedColor,
                                unselectedTextColor = unselectedColor,
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
                    color = selectedColor,
                    shape = RoundedCornerShape(50)
                )
        )
    }
}
