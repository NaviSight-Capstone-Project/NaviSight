package edu.capstone.navisight.caregiver.ui.feature_records

import edu.capstone.navisight.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import edu.capstone.navisight.caregiver.model.Viu

@Composable
fun RecordsScreen(
    viewModel: RecordsViewModel,
    onViuClicked: (String) -> Unit,
    onTransferViuClicked: () -> Unit
) {
    val vius by viewModel.vius.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFF0F1))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_records_outline),
                contentDescription = null,
                tint = Color(0xFF6041EC),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Records",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF202833),
                style = TextStyle(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFB644F1), Color(0xFF6041EC))
                    )
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) }
                )
            }
            IconButton(
                onClick = { viewModel.toggleSortOrder() },
                modifier = Modifier
                    .size(50.dp)
                    .shadow(4.dp, CircleShape)
                    .background(Color.White, CircleShape)
            ) {
                Icon(
                    imageVector = if (sortOrder == SortOrder.ASCENDING)
                        Icons.Default.KeyboardArrowDown
                    else
                        Icons.Default.KeyboardArrowUp,
                    contentDescription = "Sort",
                    tint = Color(0xFF6041EC)
                )
            }

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .shadow(4.dp, CircleShape)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF77F7ED), Color(0xFF6041EC))
                        ),
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .clickable { onTransferViuClicked() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add_secondary_pair),
                    contentDescription = "Transfer VIU",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF6041EC))
            }

            error != null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = error ?: "Unknown error", color = Color.Red)
            }

            vius.isEmpty() -> Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = if (searchQuery.isNotEmpty()) "No matching VIUs found" else "No VIU records found",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = Color.Gray
                )
            }

            else -> LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(vius) { viu ->
                    ViuCard(
                        viu = viu,
                        onClick = { onViuClicked(viu.uid) }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .shadow(4.dp, RoundedCornerShape(50)),
        shape = RoundedCornerShape(50),
        placeholder = {
            Text(
                text = "Search...",
                style = TextStyle(color = Color.Gray, fontSize = 14.sp)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color(0xFF6041EC)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = Color.Gray
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = Color(0xFF6041EC),
            unfocusedBorderColor = Color.Transparent,
            cursorColor = Color(0xFF6041EC)
        ),
        singleLine = true
    )
}

@Composable
fun ViuCard(
    viu: Viu,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(horizontal = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            AsyncImage(
                model = viu.profileImageUrl,
                contentDescription = "Profile Image",
                fallback = painterResource(R.drawable.default_profile),
                error = painterResource(R.drawable.default_profile),
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .shadow(10.dp, RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${viu.firstName} ${viu.lastName}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    val hasCategory = !viu.category.isNullOrEmpty()
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .background(
                                color = if (hasCategory) Color(0xFFD2C8FE) else Color(0xFFF5F5F5),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (hasCategory) Color(0xFF6041EC) else Color.Gray,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (hasCategory) viu.category!! else "No Vision Status Set",
                            fontSize = 12.sp,
                            color = if (hasCategory) Color(0xFF6041EC) else Color.Gray
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_phone),
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = viu.phone ?: "N/A", fontSize = 10.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_email),
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = viu.email ?: "N/A", fontSize = 10.sp)
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Click To View Information",
                        fontSize = 9.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = Color.Gray,
                        style = TextStyle(fontWeight = FontWeight.Light)
                    )
                }
            }
        }
    }
}