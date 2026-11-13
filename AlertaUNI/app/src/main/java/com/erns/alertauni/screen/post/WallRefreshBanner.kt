package com.erns.alertauni.screen.post

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface WallRefreshState {
    data object Idle : WallRefreshState
    data object Loading : WallRefreshState
    data class Success(
        val completedAt: Long,
        val newPosts: Int = 0,
        val isDemo: Boolean = false
    ) : WallRefreshState
    data object Error : WallRefreshState
}

@Composable
fun WallRefreshBanner(state: WallRefreshState, onRetry: () -> Unit) {
    if (state == WallRefreshState.Idle) return
    val background = when (state) {
        WallRefreshState.Loading -> Color(0xFFE3F2FD)
        is WallRefreshState.Success -> Color(0xFFE8F5E9)
        else -> Color(0xFFFFF3E0)
    }
    Surface(color = background, contentColor = Color(0xFF17212B)) {
        Row(
            modifier = Modifier.fillMaxWidth().semantics {
                liveRegion = LiveRegionMode.Polite
            }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (state) {
                WallRefreshState.Loading -> {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Actualizando anuncios…")
                }
                is WallRefreshState.Success -> {
                    val time = remember(state.completedAt) {
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            .format(Date(state.completedAt))
                    }
                    Column {
                        Text("✓ Muro actualizado · $time")
                        if (state.newPosts > 0) {
                            val label = if (state.newPosts == 1) "anuncio nuevo" else "anuncios nuevos"
                            Text("+${state.newPosts} $label${if (state.isDemo) " · SIMULACIÓN DEMO" else ""}")
                        }
                    }
                }
                WallRefreshState.Error -> {
                    Text("No se pudo actualizar", modifier = Modifier.weight(1f))
                    TextButton(onClick = onRetry) { Text("Reintentar") }
                }
                WallRefreshState.Idle -> Unit
            }
        }
    }
}
