# Actividad práctica: Análisis y mejora de interfaces móviles

Evidencias de la actividad sobre **layouts y adaptación de pantalla** en Jetpack Compose.

**Pantalla analizada:** "Comentarios" (`PostCommentScreen.kt`), el chat de cada publicación.
Se llega desde *Anuncios → N Comentarios* (modo demo: "Entrar como Estudiante").

## Contenido

| Carpeta | Descripción |
|---|---|
| `capturas/antes_original/` | Pantalla original (commit `d8f9edb`) en las 6 pruebas, más una con el teclado abierto |
| `capturas/despues_mejorado/` | Pantalla con las mejoras en las mismas 6 pruebas y con el teclado abierto |
| `comparativas/` | Imágenes antes → después con recuadros y flechas numeradas que señalan cada cambio |
| `codigo_mejorado/` | Los 4 archivos modificados (misma ruta que en el proyecto) y `cambios.patch` con el detalle exacto |

Para aplicar las mejoras sobre el proyecto original, desde la raíz del repo:

```bash
git apply Actividad-Analisis-y-Mejora-de-Interfaces-Moviles/codigo_mejorado/cambios.patch
```

o copiar los archivos de `codigo_mejorado/AlertaUNI/...` encima de los del proyecto.

## Escenarios de prueba

| Tamaño | Configuración | Vertical | Horizontal |
|---|---|---|---|
| Pequeño | 720×1280, 320 dpi | `prueba1` | `prueba2` |
| Mediano | 1080×2400, 420 dpi | `prueba3` | `prueba4` |
| Grande | 1440×3120, 560 dpi | `prueba5` | `prueba6` |

Los tamaños se simularon en un único emulador (`Pixel_36`) con `adb shell wm size` y `wm density`.

## Problemas detectados y mejoras

1. **Teclado abierto (vertical):** el encabezado desaparecía porque Android desplazaba toda la ventana.
   Se resolvió con `windowSoftInputMode="adjustResize"` y `consumeWindowInsets` en el `Scaffold`.
2. **Horizontal:** el encabezado de 100 dp, la barra de entrada y la barra inferior dejaban casi sin espacio la lista de mensajes.
   Se compactó el encabezado (64 dp) y se ocultó la barra inferior dentro del chat en horizontal.
3. **Ancho de las burbujas:** ocupaban casi todo el ancho. Ahora tienen un máximo del 80 % con tope de 480 dp (`BoxWithConstraints` + `widthIn`).
4. **Pantallas anchas:** el chat se centra con un ancho máximo de 720 dp.
5. **Barra de entrada:** el texto sobrevive a la rotación (`rememberSaveable`), Enter envía el mensaje y el botón de enviar se deshabilita sin texto.

Archivos modificados: `PostCommentScreen.kt`, `SearchBoxComponent.kt`, `MainAppNavigation.kt` y `AndroidManifest.xml`.
