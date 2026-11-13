package com.erns.alertauni.screen.post

import android.util.Log
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
import kotlinx.coroutines.delay
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

/**
 * =========================================================================================
 * ESTUDIANTE 4: rememberUpdatedState
 * =========================================================================================
 * Aspecto central:
 *   Acceso a valores y lambdas actualizados dentro de un efecto de larga duración
 *   (temporizador de 4s) SIN reiniciar innecesariamente dicho efecto ante recomposiciones.
 *
 * 1. ¿Qué problema resuelve?
 *    Cuando el muro se actualiza con éxito, mostramos un banner informativo que debe
 *    auto-ocultarse tras 4 segundos invocando `onDismiss`. Durante esos 4 segundos, el usuario
 *    suele interactuar con la pantalla (escribir en el buscador, filtrar cursos o navegar),
 *    lo que genera múltiples recomposiciones por segundo en el composable padre (PostScreen).
 *
 * 2. ¿Cómo funciona?
 *    `val currentOnDismiss by rememberUpdatedState(onDismiss)`
 *    `rememberUpdatedState` envuelve la lambda en un objeto `State` interno de Compose.
 *    En cada recomposición de WallRefreshBanner, Compose actualiza la referencia interna (.value),
 *    pero el objeto State en sí se mantiene idéntico. Así, el LaunchedEffect puede leer
 *    `currentOnDismiss()` y ejecutar siempre la versión más reciente del callback.
 *
 * 3. ¿Por qué se utiliza y qué pasaría sin él?
 *    - Si colocáramos `LaunchedEffect(onDismiss)`: Cada recomposición pasa una nueva instancia
 *      de la función lambda. Compose detectaría que la clave cambió, CANCELANDO la corrutina en
 *      curso y REINICIANDO el delay de 4 segundos a 0. ¡El banner jamás se ocultaría mientras
 *      el docente o estudiante esté tecleando en el buscador!
 *    - Si colocáramos `LaunchedEffect(state.completedAt)` SIN rememberUpdatedState: La corrutina
 *      capturaría la versión vieja de `onDismiss` (stale closure) al momento de nacer, ejecutando
 *      código o estados desactualizados al expirar el tiempo.
 *    - Con `rememberUpdatedState`: El temporizador de 4 segundos corre sin interrupción alguna
 *      y, al finalizar, invoca garantizadamente la última versión de la lambda con sus datos frescos.
 *
 * 4. ¿Cómo se comprueba?
 *    Se comprueba en Logcat con el tag "RememberUpdatedStateDemo". Al actualizar el muro y
 *    teclear letras en la barra de búsqueda, se observa que el temporizador continúa su cuenta
 *    regresiva y se completa exitosamente a los 4s exactos ejecutando la lambda actualizada.
 * =========================================================================================
 */
@Composable
fun WallRefreshBanner(
    state: WallRefreshState,
    onDismiss: () -> Unit = {},
    onRetry: () -> Unit
) {
    if (state == WallRefreshState.Idle) return

    // 1. Capturamos la versión más reciente de la lambda 'onDismiss' sin reiniciar el efecto.
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    // 2. Temporizador de auto-ocultado: la clave es únicamente 'state.completedAt' para que
    //    el efecto solo se inicie cuando realmente haya una nueva actualización exitosa.
    if (state is WallRefreshState.Success) {
        LaunchedEffect(state.completedAt) {
            Log.d(
                "RememberUpdatedStateDemo",
                "[rememberUpdatedState] INICIO EFECTO: Temporizador de 4s iniciado para auto-ocultar banner. (Timestamp: ${state.completedAt})"
            )
            // Esperamos 4 segundos. Durante este lapso el usuario puede escribir en el buscador
            // provocando recomposiciones, pero este efecto NO se cancela ni se reinicia.
            delay(4000L)

            Log.d(
                "RememberUpdatedStateDemo",
                "[rememberUpdatedState] FIN EFECTO: 4s completados sin interrupciones. Invocando currentOnDismiss() actualizado..."
            )
            // Invocamos la lambda más reciente gracias a rememberUpdatedState
            currentOnDismiss()
        }
    }

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
                    Column(modifier = Modifier.weight(1f)) {
                        Text("✓ Muro actualizado · $time")
                        if (state.newPosts > 0) {
                            val label = if (state.newPosts == 1) "anuncio nuevo" else "anuncios nuevos"
                            Text("+${state.newPosts} $label${if (state.isDemo) " · SIMULACIÓN DEMO" else ""}")
                        }
                    }
                    // Opción de cierre manual
                    TextButton(onClick = currentOnDismiss) {
                        Text("Ocultar")
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
