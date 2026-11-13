package com.erns.alertauni.screen.proposal

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erns.alertauni.R
import com.erns.alertauni.data.model.CatalogCourseEntity
import com.erns.alertauni.data.model.DraftEntity
import com.erns.alertauni.data.model.PostEntity
import com.erns.alertauni.screen.common.UserInitialCircle
import com.erns.alertauni.ui.theme.MyBorderColor
import com.erns.alertauni.ui.theme.MyMutedColor
import com.erns.alertauni.ui.theme.MyMutedForegroundColor
import com.erns.alertauni.ui.theme.MyPrimaryColor
import com.erns.alertauni.ui.theme.MyPrimaryForegroundColor
import com.erns.alertauni.ui.theme.MySecondaryColor
import com.erns.alertauni.ui.theme.MySelectedColor
import com.erns.alertauni.ui.theme.MySurfaceColor

private val PRIVATE_COLOR = Color(0xFF6A1B9A)
private val SORTS = listOf("recent" to "Más recientes", "oldest" to "Más antiguos")
private val VISIBILITY = listOf("all" to "Todos", "public" to "Públicos", "private" to "Privados")

private fun dateKey(s: String): Int {
    val p = s.trim().split("/")
    return if (p.size == 3) (p[2].toIntOrNull() ?: 0) * 10000 + (p[1].toIntOrNull() ?: 0) * 100 + (p[0].toIntOrNull() ?: 0) else 0
}

private fun norm(s: String) = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
    .replace(Regex("\\p{Mn}+"), "").lowercase()

internal fun applyCriteria(
    posts: List<PostEntity>, query: String, course: String, vis: String, sort: String, from: String, to: String
): List<PostEntity> {
    val q = norm(query.trim())
    val f = dateKey(from)
    val t = dateKey(to)
    return posts.filter {
        (q.isEmpty() || norm(it.title).contains(q) || norm(it.content).contains(q) ||
                norm(it.courseCode).contains(q) || norm(it.courseName ?: "").contains(q)) &&
                (course.isEmpty() || it.courseCode == course) &&
                (vis == "all" || (vis == "private") == it.isPrivate) &&
                (f == 0 || dateKey(it.date) >= f) && (t == 0 || dateKey(it.date) <= t)
    }.let { l ->
        when (sort) {
            "oldest" -> l.sortedBy { dateKey(it.date) }
            else -> l.sortedByDescending { dateKey(it.date) }
        }
    }
}

/**
 * Prototipo del Problema 2 (Examen 2). Las tres alternativas comparten criterios de filtrado;
 * cambian la presentacion y la interaccion. Datos: los de la API/demo (sin hora de publicacion).
 */
@Composable
fun Problem2Board(
    variant: String,
    username: String,
    posts: List<PostEntity>,
    courses: List<CatalogCourseEntity>,
    vm: com.erns.alertauni.screen.post.PostViewModel,
    drafts: List<DraftEntity>,
    onOpenDraft: (DraftEntity) -> Unit,
    onDeleteDraft: (DraftEntity) -> Unit,
    onClickComment: (String) -> Unit
) {
    val cfg = ProposalConfig
    // Criterios de busqueda/filtro en el ViewModel (MVVM): sobreviven a la navegacion a Comentarios
    val query by vm.query.collectAsState()
    val course by vm.course.collectAsState()
    val vis by vm.vis.collectAsState()
    val sort by vm.sort.collectAsState()
    val highlightedPostId by vm.highlightedPostId.collectAsState()
    var showDrafts by remember { mutableStateOf(false) }
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    var detailId by remember { mutableStateOf(cfg.p2Detail) }
    var adv by remember { mutableStateOf(cfg.p2Adv) }
    var searchOpen by remember { mutableStateOf(cfg.p2Search || cfg.p2Query.isNotEmpty()) }

    val filtered = remember(posts, query, course, vis, sort, from, to, highlightedPostId) {
        applyCriteria(posts, query, course, vis, sort, from, to)
            .sortedBy { it.id != highlightedPostId } // El último anuncio simulado se fija primero.
    }
    val setQuery: (String) -> Unit = vm::setQuery
    val setCourse: (String) -> Unit = vm::setCourse
    val setVis: (String) -> Unit = vm::setVis
    val setSort: (String) -> Unit = vm::setSort
    val clearAll: () -> Unit = vm::clearFilters
    val courseOptions = remember(posts) { posts.map { it.courseCode to (it.courseName ?: it.courseCode) }.distinct() }
    val courseLabel: (String) -> String = { code -> courseOptions.firstOrNull { it.first == code }?.second ?: "Todos" }

    LaunchedEffect(detailId, adv, showDrafts) { cfg.hideFab = detailId != 0 || adv != 0 || showDrafts }
    DisposableEffect(Unit) { onDispose { cfg.hideFab = false } }
    BackHandler(enabled = detailId != 0 || adv != 0 || showDrafts) {
        if (showDrafts) showDrafts = false else if (detailId != 0) detailId = 0 else adv = if (adv == 2) 1 else 0
    }

    if (showDrafts) {
        DraftsScreen(drafts, courseLabel = { id -> courses.firstOrNull { it.id == id }?.let { "${it.courseCode} · ${it.courseName}" } ?: "" },
            onBack = { showDrafts = false }, onOpen = { showDrafts = false; onOpenDraft(it) }, onDelete = onDeleteDraft)
        return
    }
    val detail = posts.firstOrNull { it.id == detailId }
    if (detail != null) {
        DetailScreen(detail, onBack = { detailId = 0 }, onComments = { onClickComment(detail.id.toString()) })
        return
    }

    when (variant) {
        "A" -> AltA(username, filtered, query, searchOpen, setQuery, { searchOpen = it }, onClickComment)
        "B" -> AltB(
            username, filtered, posts.size, query, course, vis, sort, courseOptions, courseLabel,
            onQuery = setQuery, onCourse = setCourse, onVis = setVis, onSort = setSort,
            onClear = clearAll, draftCount = drafts.size, onDrafts = { showDrafts = true },
            onDetail = { detailId = it.id }, onComments = { onClickComment(it.id.toString()) },
            highlightedPostId = highlightedPostId
        )
        else -> if (adv == 1) {
            AdvancedForm(
                query, course, vis, sort, from, to, courseOptions, courseLabel,
                setQuery, setCourse, setVis, setSort, { from = it }, { to = it },
                onBack = { adv = 0 }, onApply = { adv = 2 },
                onReset = { clearAll(); from = ""; to = "" }
            )
        } else if (adv == 2) {
            AdvancedResults(filtered, query, course, vis, sort, from, to, courseLabel,
                onModify = { adv = 1 }, onBack = { adv = 0 }, onClickComment = onClickComment)
        } else {
            Column(Modifier.fillMaxSize().background(MySecondaryColor)) {
                PrimaryHeader(username, "Anuncios") {
                    TextButton(onClick = { adv = 1 }) {
                        Icon(Icons.Default.Search, null, tint = Color.White)
                        Text("Búsqueda avanzada", color = Color.White, modifier = Modifier.padding(start = 4.dp))
                    }
                }
                CardList(filtered, onClickComment)
            }
        }
    }
}

// ---------------------------------------------------------------- Alternativa A
@Composable
private fun AltA(
    username: String, list: List<PostEntity>, query: String, open: Boolean,
    onQuery: (String) -> Unit, onOpen: (Boolean) -> Unit, onClickComment: (String) -> Unit
) {
    Column(Modifier.fillMaxSize().background(MySecondaryColor)) {
        Box(Modifier.fillMaxWidth().height(100.dp).background(MyPrimaryColor).padding(10.dp)) {
            if (open) {
                TextField(
                    value = query, onValueChange = onQuery,
                    placeholder = { Text("Buscar...") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().align(Alignment.CenterStart),
                    trailingIcon = {
                        IconButton(onClick = { if (query.isNotEmpty()) onQuery("") else onOpen(false) }) {
                            Icon(Icons.Default.Close, contentDescription = "Borrar búsqueda")
                        }
                    }
                )
            } else {
                Column(Modifier.align(Alignment.CenterStart)) {
                    Text(username, color = MySecondaryColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Anuncios", color = MyPrimaryForegroundColor, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                }
                IconButton(onClick = { onOpen(true) }, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Icon(Icons.Default.Search, "Buscar", tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
        }
        if (query.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${list.size} resultado(s) para “$query”", color = MyPrimaryColor, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButton(onClick = { onQuery("") }) { Text("Borrar búsqueda") }
            }
        }
        if (list.isEmpty()) {
            Text("Sin resultados", modifier = Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center, color = MyMutedForegroundColor)
        } else CardList(list, onClickComment)
    }
}

// ---------------------------------------------------------------- Alternativa B (elegida)
// 1. Objeto singleton externo (fuera del control del árbol de Compose)
object BoardTelemetryTracker {
    var lastRenderedCount: Int = 0
        private set
    var lastActiveQuery: String = ""
        private set
    var totalRecompositions: Int = 0
        private set

    fun recordSuccessfulRender(renderedCount: Int, total: Int, query: String, course: String, vis: String) {
        lastRenderedCount = renderedCount
        lastActiveQuery = query
        totalRecompositions++
        Log.d(
            "SideEffectDemo",
            "[SideEffect] Recomposición EXITOSA #$totalRecompositions en AltB -> " +
                    "Query: '$query' | Curso: '$course' | Visibilidad: '$vis' | " +
                    "Renderizando $renderedCount de $total anuncios en pantalla"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AltB(
    username: String, list: List<PostEntity>, total: Int,
    query: String, course: String, vis: String, sort: String,
    courseOptions: List<Pair<String, String>>, courseLabel: (String) -> String,
    onQuery: (String) -> Unit, onCourse: (String) -> Unit, onVis: (String) -> Unit, onSort: (String) -> Unit,
    onClear: () -> Unit, draftCount: Int, onDrafts: () -> Unit,
    onDetail: (PostEntity) -> Unit, onComments: (PostEntity) -> Unit,
    highlightedPostId: Int? = null
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(highlightedPostId, list.firstOrNull()?.id) {
        if (highlightedPostId != null && list.firstOrNull()?.id == highlightedPostId) {
            listState.animateScrollToItem(0)
        }
    }
    var courseMenu by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    val filtersActive = query.isNotBlank() || course.isNotEmpty() || vis != "all"
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MySelectedColor, selectedLabelColor = MyPrimaryColor, selectedTrailingIconColor = MyPrimaryColor
    )

    // SideEffect: Sincronización post-recomposición exitosa
    SideEffect {
        BoardTelemetryTracker.recordSuccessfulRender(
            renderedCount = list.size,
            total = total,
            query = query,
            course = course,
            vis = vis
        )
    }

    Column(Modifier.fillMaxSize().background(MySecondaryColor)) {
        // Cabecera compacta
        Row(Modifier.fillMaxWidth().background(MyPrimaryColor).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Anuncios", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = onDrafts) {
                Text("Borradores ($draftCount)", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
        Column(Modifier.background(MySurfaceColor).padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                value = query, onValueChange = onQuery, singleLine = true,
                placeholder = { Text("Buscar anuncios") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }) { Icon(Icons.Default.Close, "Borrar búsqueda") } },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.weight(1f)) {
                    FilterChip(
                        selected = course.isNotEmpty(), onClick = { courseMenu = true }, colors = chipColors,
                        label = { Text(if (course.isEmpty()) "Curso: Todos" else courseLabel(course), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(expanded = courseMenu, onDismissRequest = { courseMenu = false }) {
                        DropdownMenuItem(text = { Text("Todos los cursos") }, onClick = { onCourse(""); courseMenu = false })
                        courseOptions.forEach { (code, name) ->
                            DropdownMenuItem(text = { Text("$code · $name") }, onClick = { onCourse(code); courseMenu = false })
                        }
                    }
                }
                Box {
                    FilterChip(
                        selected = sort != "recent", onClick = { sortMenu = true }, colors = chipColors,
                        label = { Text(SORTS.first { it.first == sort }.second) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
                    )
                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        SORTS.forEach { (k, l) -> DropdownMenuItem(text = { Text(l) }, onClick = { onSort(k); sortMenu = false }) }
                    }
                }
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                VISIBILITY.forEachIndexed { i, (k, l) ->
                    SegmentedButton(
                        selected = vis == k, onClick = { onVis(k) },
                        shape = SegmentedButtonDefaults.itemShape(i, VISIBILITY.size),
                        colors = SegmentedButtonDefaults.colors(activeContainerColor = MySelectedColor, activeContentColor = MyPrimaryColor)
                    ) { Text(l) }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (filtersActive) "${list.size} de $total anuncios" else "$total anuncios",
                color = MyMutedForegroundColor, fontSize = 14.sp, modifier = Modifier.weight(1f)
            )
            if (filtersActive) TextButton(onClick = onClear) { Text("Limpiar filtros") }
        }
        if (list.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Search, null, tint = MyMutedForegroundColor, modifier = Modifier.size(56.dp))
                Text("No se encontraron anuncios", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(top = 8.dp))
                Text(
                    "Prueba con otras palabras o quita algún filtro.",
                    color = MyMutedForegroundColor, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )
                Button(onClick = onClear) { Text("Limpiar filtros") }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 88.dp)) {
                items(list, key = { it.id }) {
                    CompactCard(it, onDetail, onComments, isNew = it.id == highlightedPostId)
                }
            }
        }
    }
}

@Composable
private fun CompactCard(post: PostEntity, onDetail: (PostEntity) -> Unit, onComments: (PostEntity) -> Unit, isNew: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp).clickable { onDetail(post) },
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = if (isNew) Color(0xFFE8F5E9) else MySurfaceColor)
    ) {
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 2.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (isNew) {
                Text("NUEVO · DEMO · FIJADO AL INICIO", color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CodeChip(post.courseCode)
                VisibilityBadge(post.isPrivate)
                PriorityPill(post.priority)
                Spacer(Modifier.weight(1f))
                Text(post.date, color = MyMutedForegroundColor, fontSize = 13.sp)
            }
            Text(post.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (post.isPrivate && !post.receiverPrivate.isNullOrBlank()) {
                Text("Para: ${post.receiverPrivate}", color = PRIVATE_COLOR, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            Text(post.content, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = Color(0xFF334155))
            Spacer(Modifier.fillMaxWidth().height(1.dp).background(MyBorderColor))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onDetail(post) }) { Text("Ver detalle", fontWeight = FontWeight.Bold) }
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = { onComments(post) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)) {
                    Icon(painterResource(R.drawable.outline_chat_bubble_outline_24), null, modifier = Modifier.size(18.dp))
                    Text("${post.countComments} Comentarios", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailScreen(post: PostEntity, onBack: () -> Unit, onComments: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MySurfaceColor)) {
        Row(Modifier.fillMaxWidth().background(MyPrimaryColor).padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White) }
            Text("Detalle del anuncio", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CodeChip(post.courseCode)
                VisibilityBadge(post.isPrivate)
                PriorityPill(post.priority)
            }
            Text(post.courseName ?: "", color = MyMutedForegroundColor, fontSize = 15.sp)
            Text(post.title, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UserInitialCircle(post.authorName ?: "X")
                Column {
                    Text(post.authorName ?: "Desconocido", fontWeight = FontWeight.SemiBold)
                    Text("Publicado el ${post.date}", color = MyMutedForegroundColor, fontSize = 14.sp)
                }
            }
            if (post.isPrivate && !post.receiverPrivate.isNullOrBlank()) {
                Row(
                    Modifier.fillMaxWidth().background(MySecondaryColor, RoundedCornerShape(8.dp)).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, null, tint = PRIVATE_COLOR, modifier = Modifier.padding(end = 6.dp))
                    Text("Mensaje privado para ${post.receiverPrivate}", color = PRIVATE_COLOR, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.fillMaxWidth().height(1.dp).background(MyBorderColor))
            Text(post.content, fontSize = 18.sp, lineHeight = 26.sp)
        }
        Column(Modifier.padding(16.dp)) {
            Button(onClick = onComments, modifier = Modifier.fillMaxWidth()) {
                Icon(painterResource(R.drawable.outline_chat_bubble_outline_24), null, modifier = Modifier.size(20.dp))
                Text("Comentarios (${post.countComments})", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

// ---------------------------------------------------------------- Alternativa C
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvancedForm(
    query: String, course: String, vis: String, sort: String, from: String, to: String,
    courseOptions: List<Pair<String, String>>, courseLabel: (String) -> String,
    onQuery: (String) -> Unit, onCourse: (String) -> Unit, onVis: (String) -> Unit, onSort: (String) -> Unit,
    onFrom: (String) -> Unit, onTo: (String) -> Unit,
    onBack: () -> Unit, onApply: () -> Unit, onReset: () -> Unit
) {

    var courseMenu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(MySurfaceColor)) {
        Row(Modifier.fillMaxWidth().background(MyPrimaryColor).padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White) }
            Text("Búsqueda avanzada", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(query, onQuery, label = { Text("Texto a buscar") }, singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth())
            Box {
                OutlinedTextField(
                    value = if (course.isEmpty()) "Todos los cursos" else courseLabel(course), onValueChange = {}, readOnly = true,
                    label = { Text("Curso") }, singleLine = true, trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                    modifier = Modifier.fillMaxWidth()
                )
                Box(Modifier.matchParentSize().clickable { courseMenu = true })
                DropdownMenu(expanded = courseMenu, onDismissRequest = { courseMenu = false }) {
                    DropdownMenuItem(text = { Text("Todos los cursos") }, onClick = { onCourse(""); courseMenu = false })
                    courseOptions.forEach { (code, name) ->
                        DropdownMenuItem(text = { Text("$code · $name") }, onClick = { onCourse(code); courseMenu = false })
                    }
                }
            }
            Text("Visibilidad", fontWeight = FontWeight.SemiBold, color = MyMutedForegroundColor)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                VISIBILITY.forEachIndexed { i, (k, l) ->
                    SegmentedButton(selected = vis == k, onClick = { onVis(k) }, shape = SegmentedButtonDefaults.itemShape(i, VISIBILITY.size),
                        colors = SegmentedButtonDefaults.colors(activeContainerColor = MySelectedColor, activeContentColor = MyPrimaryColor)) { Text(l) }
                }
            }
            Text("Rango de fechas", fontWeight = FontWeight.SemiBold, color = MyMutedForegroundColor)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(from, onFrom, label = { Text("Desde") }, placeholder = { Text("dd/mm/aaaa") }, singleLine = true,
                    trailingIcon = { Icon(Icons.Default.DateRange, null) }, modifier = Modifier.weight(1f))
                OutlinedTextField(to, onTo, label = { Text("Hasta") }, placeholder = { Text("dd/mm/aaaa") }, singleLine = true,
                    trailingIcon = { Icon(Icons.Default.DateRange, null) }, modifier = Modifier.weight(1f))
            }
            Text("Ordenar por", fontWeight = FontWeight.SemiBold, color = MyMutedForegroundColor)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SORTS.forEach { (k, l) ->
                    FilterChip(selected = sort == k, onClick = { onSort(k) }, label = { Text(l) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MySelectedColor, selectedLabelColor = MyPrimaryColor))
                }
            }
        }
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) { Text("Limpiar") }
            Button(onClick = onApply, modifier = Modifier.weight(2f)) { Text("Ver resultados") }
        }
    }
}

@Composable
private fun AdvancedResults(
    list: List<PostEntity>, query: String, course: String, vis: String, sort: String, from: String, to: String,
    courseLabel: (String) -> String, onModify: () -> Unit, onBack: () -> Unit, onClickComment: (String) -> Unit
) {
    val criteria = buildList {
        if (query.isNotBlank()) add("Texto: “$query”")
        if (course.isNotEmpty()) add("Curso: ${courseLabel(course)}")
        if (vis != "all") add(VISIBILITY.first { it.first == vis }.second)
        if (from.isNotBlank()) add("Desde $from")
        if (to.isNotBlank()) add("Hasta $to")
        add(SORTS.first { it.first == sort }.second)
    }
    Column(Modifier.fillMaxSize().background(MySecondaryColor)) {
        Row(Modifier.fillMaxWidth().background(MyPrimaryColor).padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White) }
            Text("Resultados (${list.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Column(Modifier.background(MySurfaceColor).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                criteria.forEach {
                    Text(it, color = MyPrimaryColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.background(MySelectedColor, RoundedCornerShape(16.dp)).padding(horizontal = 10.dp, vertical = 5.dp))
                }
            }
            OutlinedButton(onClick = onModify) { Text("Modificar criterios") }
        }
        if (list.isEmpty()) {
            Text("No se encontraron anuncios", modifier = Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center, color = MyMutedForegroundColor)
        } else CardList(list, onClickComment)
    }
}

// ---------------------------------------------------------------- Componentes comunes
@Composable
private fun PrimaryHeader(username: String, title: String, action: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(100.dp).background(MyPrimaryColor).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(username, color = MySecondaryColor, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(title, color = MyPrimaryForegroundColor, fontWeight = FontWeight.Bold, fontSize = 26.sp)
        }
        action()
    }
}

@Composable
private fun CardList(list: List<PostEntity>, onClickComment: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 88.dp)) {
        items(list, key = { it.id }) { AdaptiveCard(it, onClickComment) }
    }
}

/** Tarjeta del muro original, pero sin altura fija: el contenido completo siempre es legible. */
@Composable
private fun AdaptiveCard(post: PostEntity, onClickComment: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp).clickable { onClickComment(post.id.toString()) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MySurfaceColor)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CodeChip(post.courseCode)
                Spacer(Modifier.size(8.dp))
                VisibilityBadge(post.isPrivate)
                Spacer(Modifier.size(8.dp))
                PriorityPill(post.priority)
                Spacer(Modifier.weight(1f))
                Text(post.date, fontSize = 14.sp)
            }
            Text(post.title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(post.content, fontSize = 17.sp)
            Spacer(Modifier.fillMaxWidth().height(1.dp).background(MyBorderColor))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                UserInitialCircle(post.authorName ?: "X")
                Text(post.authorName ?: "Desconocido", color = MyMutedForegroundColor, fontSize = 15.sp, modifier = Modifier.weight(1f).padding(start = 4.dp))
                Icon(painterResource(R.drawable.outline_chat_bubble_outline_24), null, modifier = Modifier.size(20.dp))
                Text("${post.countComments} Comentarios", color = MyPrimaryColor, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

@Composable
private fun CodeChip(code: String) {
    Text(code, color = MyMutedForegroundColor, fontSize = 13.sp,
        modifier = Modifier.background(MyMutedColor, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 3.dp))
}

@Composable
private fun VisibilityBadge(isPrivate: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(if (isPrivate) Icons.Default.Lock else Icons.Outlined.Person, null,
            tint = if (isPrivate) PRIVATE_COLOR else MyMutedForegroundColor, modifier = Modifier.size(16.dp))
        Text(if (isPrivate) "Privado" else "Público", fontSize = 13.sp,
            color = if (isPrivate) PRIVATE_COLOR else MyMutedForegroundColor,
            fontWeight = if (isPrivate) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(start = 3.dp))
    }
}

@Composable
private fun PriorityPill(priority: String) {
    Text(priorityLabel(priority).uppercase(), color = priorityColor(priority), fontWeight = FontWeight.Bold, fontSize = 11.sp,
        modifier = Modifier.background(priorityColor(priority).copy(alpha = 0.15f), RoundedCornerShape(10.dp)).padding(horizontal = 7.dp, vertical = 2.dp))
}

@Composable
private fun DraftsScreen(
    drafts: List<DraftEntity>, courseLabel: (String) -> String,
    onBack: () -> Unit, onOpen: (DraftEntity) -> Unit, onDelete: (DraftEntity) -> Unit
) {
    Column(Modifier.fillMaxSize().background(MySecondaryColor)) {
        Row(Modifier.fillMaxWidth().background(MyPrimaryColor).padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White) }
            Text("Borradores (${drafts.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Text("Los borradores se guardan solo en este dispositivo y no son visibles para los estudiantes.",
            color = MyMutedForegroundColor, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
        if (drafts.isEmpty()) {
            Text("No tienes borradores guardados", modifier = Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center, color = MyMutedForegroundColor)
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(drafts, key = { it.id }) { d ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp).clickable { onOpen(d) },
                    colors = CardDefaults.cardColors(containerColor = MySurfaceColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB45309))
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("BORRADOR", color = Color(0xFFB45309), fontWeight = FontWeight.Bold, fontSize = 11.sp,
                                    modifier = Modifier.background(Color(0xFFFEF3C7), RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 2.dp))
                                PriorityPill(d.priority)
                            }
                            Text(d.title.ifBlank { "(sin título)" }, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(d.content.ifBlank { "(sin contenido)" }, maxLines = 2, overflow = TextOverflow.Ellipsis, color = Color(0xFF334155))
                            Text(courseLabel(d.courseId) + if (d.audience == "STUDENT") " · Para: ${d.studentFirstname} ${d.studentSurname}" else " · Todo el curso",
                                color = MyMutedForegroundColor, fontSize = 13.sp)
                        }
                        IconButton(onClick = { onDelete(d) }) { Icon(Icons.Default.Close, "Eliminar borrador") }
                    }
                }
            }
        }
    }
}
