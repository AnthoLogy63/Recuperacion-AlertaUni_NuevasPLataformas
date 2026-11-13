package com.erns.alertauni.screen.post

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.erns.alertauni.R
import com.erns.alertauni.data.model.PostEntity
import com.erns.alertauni.screen.common.SearchBoxComponent
import com.erns.alertauni.screen.common.UserInitialCircle
import com.erns.alertauni.ui.theme.MyBorderColor
import com.erns.alertauni.ui.theme.MyMutedColor
import com.erns.alertauni.ui.theme.MyMutedForegroundColor
import com.erns.alertauni.ui.theme.MyPrimaryColor
import com.erns.alertauni.ui.theme.MySecondaryColor
import com.erns.alertauni.ui.theme.MySurfaceColor
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Lock
import kotlinx.coroutines.launch

@Composable
fun PostScreen(
    modifier: Modifier = Modifier,
    onClickComment: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    onFabActionReady: (() -> Unit) -> Unit
) {
    val context = LocalContext.current
    val postViewModel: PostViewModel = hiltViewModel()
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val refreshState = postViewModel.wallRefresh.collectAsState()

    // El observador pertenece al muro y se retira al salir de su composición.
    DisposableEffect(lifecycleOwner, postViewModel) {
        var wasStopped = false
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    wasStopped = true
                    android.util.Log.d("MuroDisposableEffect", "ON_STOP: muro detenido; esperando regreso")
                }
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> if (wasStopped) {
                    wasStopped = false
                    android.util.Log.d("MuroDisposableEffect", "ON_RESUME: regreso al muro; solicitando refresco")
                    postViewModel.refreshAfterReturn()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        android.util.Log.d("MuroDisposableEffect", "REGISTRADO: observador del ciclo de vida del muro")
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            android.util.Log.d("MuroDisposableEffect", "ELIMINADO: onDispose retiro el observador del muro")
        }
    }
    val scope = rememberCoroutineScope()
    val announcementState = postViewModel.announcements.collectAsState()
    val catalogCourseState = postViewModel.catalogCourses.collectAsState()
    val showAddDialog = remember { mutableStateOf(false) }
    val searchQuery = remember { mutableStateOf("") }
    val username = remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        postViewModel.username.collect {
            username.value = it
        }
    }

    val onClickNewPost: (String, String, String, String, String, String) -> Unit =
        { courseCatalogId, courseCode, courseName, titlePost, contentPost, priority ->
            showAddDialog.value = false
            postViewModel.sendPost(
                courseCatalogId,
                titlePost,
                contentPost,
                priority
            )
        }

    // --- Propuesta (Examen 2): formulario unico / FAB desplegable
    val contactVm: com.erns.alertauni.screen.contact.ContactViewModel = hiltViewModel()
    val students = contactVm.students.collectAsState()
    val variant = com.erns.alertauni.screen.proposal.ProposalConfig.variant
    val draftVm: DraftViewModel = hiltViewModel()
    val drafts = draftVm.drafts.collectAsState()
    val editingDraft = remember { mutableStateOf<com.erns.alertauni.data.model.DraftEntity?>(null) }
    val cForm = com.erns.alertauni.screen.proposal.ProposalConfig.cForm
    LaunchedEffect(Unit) { postViewModel.refresh() }
    val publishToStudent: (com.erns.alertauni.data.model.CatalogCourseEntity, com.erns.alertauni.data.model.StudentEntity, String, String, String) -> Unit =
        { c, s, pr, t, ct ->
            showAddDialog.value = false
            com.erns.alertauni.screen.proposal.ProposalConfig.cForm = null
            editingDraft.value?.let { draftVm.delete(it.id) }; editingDraft.value = null
            contactVm.addPostPrivate(
                com.erns.alertauni.data.model.PostPrivateRequest(c.id, t, ct, s.student_id, true, pr)
            )
            scope.launch { kotlinx.coroutines.delay(400); postViewModel.refresh() }
        }
    val publishToCourse: (com.erns.alertauni.data.model.CatalogCourseEntity, String, String, String) -> Unit =
        { c, pr, t, ct ->
            showAddDialog.value = false
            com.erns.alertauni.screen.proposal.ProposalConfig.cForm = null
            editingDraft.value?.let { draftVm.delete(it.id) }; editingDraft.value = null
            postViewModel.sendPost(c.id, t, ct, pr)
        }

    val onClickFloatingActionButton: () -> Unit = {
        if (catalogCourseState.value.isNotEmpty()) {
            showAddDialog.value = true
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Debes estar inscrito en al menos un curso para crear una notificación."
                )
            }
        }
    }

    onFabActionReady(onClickFloatingActionButton)

    if ((showAddDialog.value && variant == "B") || (cForm != null && variant == "C")) {
        com.erns.alertauni.screen.proposal.NewAnnouncementSheet(
            courses = catalogCourseState.value,
            loadStudents = { id -> contactVm.getStudentByCourse(com.erns.alertauni.data.model.CourseCatalogRequest(id)) },
            students = students.value,
            lockedAudience = if (variant == "C") cForm else null,
            draft = editingDraft.value,
            onSaveDraft = { draftVm.save(it) },
            onDismiss = {
                editingDraft.value = null
                showAddDialog.value = false
                com.erns.alertauni.screen.proposal.ProposalConfig.cForm = null
            },
            onPublishCourse = publishToCourse,
            onPublishStudent = publishToStudent
        )
    } else if (showAddDialog.value && variant == "A" && catalogCourseState.value.isNotEmpty()) {
        AddPostDialog(
            onDismiss = { showAddDialog.value = false },
            onClickNewPost = onClickNewPost,
            catalogCourses = catalogCourseState.value
        )
    }

    if (com.erns.alertauni.screen.proposal.ProposalConfig.p2 != null) {
        Column(modifier = modifier.fillMaxSize()) {
            // =====================================================================================
            // ESTUDIANTE 4: rememberUpdatedState
            // Al refrescar el muro, WallRefreshBanner inicia un temporizador de 4s para auto-ocultarse.
            // Pasamos una lambda 'onDismiss' que captura el estado actual del ViewModel / pantalla.
            // Si el usuario escribe en el buscador o interactúa con el muro durante esos 4 segundos,
            // PostScreen se recompone y genera una nueva instancia de esta lambda.
            // Gracias a 'rememberUpdatedState' dentro de WallRefreshBanner:
            // 1) El temporizador de 4s NO se reinicia ante cada recomposición (no se interrumpe).
            // 2) Al finalizar el tiempo, se ejecuta la versión MÁS RECIENTE de esta lambda.
            // =====================================================================================
            WallRefreshBanner(
                state = refreshState.value,
                onDismiss = {
                    android.util.Log.d(
                        "RememberUpdatedStateDemo",
                        "[rememberUpdatedState] onDismiss ejecutado -> Ocultando banner. Búsqueda activa al expirar: '${postViewModel.query.value}'"
                    )
                    postViewModel.dismissWallRefresh()
                },
                onRetry = {
                    postViewModel.refresh(showFeedback = true)
                }
            )
            Box(Modifier.weight(1f)) {
        com.erns.alertauni.screen.proposal.Problem2Board(
            variant = com.erns.alertauni.screen.proposal.ProposalConfig.p2!!,
            username = username.value,
            posts = announcementState.value,
            courses = catalogCourseState.value,
            vm = postViewModel,
            drafts = drafts.value,
            onOpenDraft = { editingDraft.value = it; showAddDialog.value = true },
            onDeleteDraft = { draftVm.delete(it.id) },
            onClickComment = onClickComment
        )
            }
        }
        return
    }
    BoardLayout(
        username.value,
        "Anuncios",
        searchQuery = searchQuery,
        notificacionesFiltradas = announcementState.value,
        onClickComment
    )

}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun BoardLayout(
    username: String,
    title: String,
    searchQuery: MutableState<String>,
    notificacionesFiltradas: List<PostEntity>,
    onClickComment: (String) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MySecondaryColor)
    ) {
        SearchBoxComponent(username, title)
        AnnounceList(notificacionesFiltradas, onClickComment)
    }

}


@Composable
fun AnnounceList(
    announcements: List<PostEntity>,
    onClickComment: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
    ) {
        items(announcements) { announce ->
            MessageCard(announce, onClickComment)
        }

    }
}


@Composable
fun MessageCard(
    announce: PostEntity,
    onClickComment: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(8.dp)
            .clickable(onClick = { onClickComment(announce.id.toString()) }),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MySurfaceColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            )
            {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = announce.courseCode,
                        color = MyMutedForegroundColor,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .background(
                                color = MyMutedColor,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Icon(
                        imageVector = if (announce.isPrivate) Icons.Filled.Lock else Icons.Outlined.Person,
                        contentDescription = "",
                        tint = if (announce.isPrivate) Color(0xFF6A1B9A) else androidx.compose.material3.LocalContentColor.current,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Text(
                        text = if (announce.isPrivate) "Privado" else "Público",
                        color = if (announce.isPrivate) Color(0xFF6A1B9A) else MyMutedForegroundColor,
                        fontWeight = if (announce.isPrivate) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 16.sp,
                    )
                    Text(
                        text = com.erns.alertauni.screen.proposal.priorityLabel(announce.priority).uppercase(),
                        color = com.erns.alertauni.screen.proposal.priorityColor(announce.priority),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .background(
                                com.erns.alertauni.screen.proposal.priorityColor(announce.priority).copy(alpha = 0.15f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = announce.date,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                )
            }

            Text(
                text = announce.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = announce.content,
                fontSize = 18.sp
            )
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MyBorderColor)
            )
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth(),
//            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            )
            {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    UserInitialCircle(announce.authorName ?: "X")
                    Text(
                        text = announce.authorName ?: "Desconocido",
                        color = MyMutedForegroundColor,
                        fontSize = 16.sp,
                        maxLines = Int.MAX_VALUE,
                        overflow = TextOverflow.Clip,
                        softWrap = true
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.outline_chat_bubble_outline_24),
                        contentDescription = "",
                    )
                    Text(
                        text = announce.countComments.toString(),
                        color = MyPrimaryColor,
                        fontSize = 16.sp,
                    )
                    Text(
                        text = "Comentarios",
                        color = MyPrimaryColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

            }

        }

    }
}
