package com.erns.alertauni.screen.proposal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Selector de alternativa para la demo del informe (Examen 2).
 *  Problema 1 (--es alt A|B|C): A = entradas separadas, B = formulario unico, C = FAB desplegable.
 *  Problema 2 (--es p2 A|B|C):  A = busqueda simple, B = muro compacto con filtros, C = busqueda avanzada.
 * Se fija al arrancar: adb shell am start -n com.erns.alertauni/.MainActivity --es p2 B
 */
object ProposalConfig {
    var variant by mutableStateOf("A")

    // Alternativa C: FAB desplegado y formulario abierto desde el (null = ninguno)
    var cMenuOpen by mutableStateOf(false)
    var cForm by mutableStateOf<Audience?>(null)

    // Problema 2: null = muro original
    var p2 by mutableStateOf<String?>(null)
    var hideFab by mutableStateOf(false)

    // Estado inicial opcional para generar capturas reproducibles
    var p2Query = ""
    var p2Course = ""      // codigo de curso
    var p2Vis = "all"      // all | public | private
    var p2Detail = 0       // id de anuncio
    var p2Adv = 0          // 0 = muro, 1 = pantalla de criterios, 2 = resultados
    var p2Sort = "recent"  // recent | priority | comments
    var p2Search = false   // A: campo de busqueda abierto
}
