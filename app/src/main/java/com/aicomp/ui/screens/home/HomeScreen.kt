package com.aicomp.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aicomp.data.repository.CompanionRepository
import com.aicomp.ui.components.BannerAdView
import com.aicomp.ui.components.CoinBadge
import com.aicomp.ui.components.CompanionGlowHeader
import com.aicomp.ui.components.CompanionGridCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCompanionClick: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    val companions by CompanionRepository.companions.collectAsState()

    LaunchedEffect(Unit) {
        CompanionRepository.refresh()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CompanionGlowHeader(height = 200.dp)

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Ishqana",
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    actions = {
                        CoinBadge(modifier = Modifier.padding(end = 8.dp))
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent,
            bottomBar = { BannerAdView() }
        ) { padding ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "Pick who you want to talk to",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    )
                }
                items(companions, key = { it.id }) { companion ->
                    CompanionGridCard(
                        companion = companion,
                        onClick = { onCompanionClick(companion.id) }
                    )
                }
            }
        }
    }
}
