package com.erns.alertauni.screen.proposal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erns.alertauni.data.model.CatalogCourseEntity
import com.erns.alertauni.data.model.StudentEntity
import com.erns.alertauni.ui.theme.MyPrimaryColor

enum class Audience { COURSE, STUDENT }

val PRIORITIES = listOf("high" to "Alta", "medium" to "Media", "low" to "Baja")

fun priorityColor(p: String): Color = when (p) {
    "high" -> Color(0xFFD32F2F)
    "low" -> Color(0xFF2E7D32)
    else -> Color(0xFFF59E0B)
}

fun priorityLabel(p: String): String = PRIORITIES.firstOrNull { it.first == p }?.second ?: "Media"

/** Chips de prioridad (seleccion unica, color + texto). Reutilizado por A, B y C. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriorityChips(selected: String, onSelect: (String) -> Unit) {
    Column {
        Text("Prioridad", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF64748B))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            PRIORITIES.forEach { (key, label) ->
                FilterChip(
                    selected = selected == key,
                    onClick = { onSelect(key) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = priorityColor(key).copy(alpha = 0.18f),
                        selectedLabelColor = priorityColor(key)
                    )
                )
            }
        }
    }
}

/**
 * Alternativa B (elegida): formulario unico con selector de audiencia.
 * La alternativa C reutiliza el mismo formulario con [lockedAudience] fijo (un formulario por accion).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewAnnouncementSheet(
    courses: List<CatalogCourseEntity>,
    loadStudents: (String) -> Unit,
    students: List<StudentEntity>,
    lockedAudience: Audience? = null,
    initialCourseId: String? = null,
    initialStudent: StudentEntity? = null,
    draft: com.erns.alertauni.data.model.DraftEntity? = null,
    onSaveDraft: (com.erns.alertauni.data.model.DraftEntity) -> Unit = {},
    onDismiss: () -> Unit,
    onPublishCourse: (CatalogCourseEntity, String, String, String) -> Unit,
    onPublishStudent: (CatalogCourseEntity, StudentEntity, String, String, String) -> Unit
) {
    val draftStudent = draft?.takeIf { it.audience == "STUDENT" && it.studentId != null }?.let {
        StudentEntity(it.courseId, it.studentId!!, "", it.studentFirstname.orEmpty(), it.studentSurname.orEmpty(), "")
    }
    val startStudent = draftStudent ?: initialStudent
    var course by remember { mutableStateOf(courses.firstOrNull { it.id == (draft?.courseId ?: initialCourseId) }) }
    var audience by remember {
        mutableStateOf(lockedAudience ?: if (draft?.audience == "STUDENT" || startStudent != null) Audience.STUDENT else Audience.COURSE)
    }
    var student by remember { mutableStateOf(startStudent) }
    var priority by remember { mutableStateOf(draft?.priority ?: "medium") }
    var title by remember { mutableStateOf(draft?.title ?: "") }
    var content by remember { mutableStateOf(draft?.content ?: "") }
    var query by remember { mutableStateOf(startStudent?.let { "${it.firstname} ${it.surname}" } ?: "") }
    var courseMenu by remember { mutableStateOf(false) }

    val valid = course != null && title.isNotBlank() && content.isNotBlank() &&
            (audience == Audience.COURSE || student != null)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                when (lockedAudience) {
                    Audience.COURSE -> "Anuncio al curso"
                    Audience.STUDENT -> "Mensaje a un estudiante"
                    null -> "Nuevo anuncio"
                },
                fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MyPrimaryColor
            )

            // 1. Curso (al cambiarlo se reinicia el estudiante)
            Box {
                OutlinedTextField(
                    value = course?.let { "${it.courseCode} · ${it.courseName}" } ?: "",
                    onValueChange = {}, readOnly = true, label = { Text("Curso") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Box(Modifier.matchParentSize().clickable { courseMenu = true })
                DropdownMenu(expanded = courseMenu, onDismissRequest = { courseMenu = false }) {
                    courses.forEach {
                        DropdownMenuItem(
                            text = { Text("${it.courseCode} · ${it.courseName}") },
                            onClick = {
                                course = it; student = null; query = ""; courseMenu = false
                                loadStudents(it.id)
                            }
                        )
                    }
                }
            }

            // 2. Audiencia (solo en B; en C el formulario ya viene fijo)
            if (lockedAudience == null) {
                Text("¿Para quién?", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF64748B))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = audience == Audience.COURSE,
                        onClick = { audience = Audience.COURSE; student = null },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("Todo el curso") }
                    SegmentedButton(
                        selected = audience == Audience.STUDENT,
                        onClick = { audience = Audience.STUDENT },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("Un estudiante") }
                }
            }

            // 3. Campo condicional: estudiante con busqueda, filtrado por curso
            AnimatedVisibility(visible = audience == Audience.STUDENT && course != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it; student = null },
                        label = { Text("Estudiante") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (student == null) {
                        students.filter {
                            query.isBlank() || "${it.firstname} ${it.surname}".contains(query, ignoreCase = true)
                        }.forEach { s ->
                            Text(
                                "${s.firstname} ${s.surname}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFE0E7FF), RoundedCornerShape(6.dp))
                                    .clickable { student = s; query = "${s.firstname} ${s.surname}" }
                                    .padding(10.dp)
                            )
                        }
                    }
                }
            }

            // 4. Titulo y contenido
            OutlinedTextField(title, { title = it }, label = { Text("Título") }, singleLine = true,
                modifier = Modifier.fillMaxWidth())
            OutlinedTextField(content, { content = it.take(280) }, label = { Text("Contenido") },
                minLines = 3, maxLines = 3, supportingText = { Text("${content.length}/280") },
                modifier = Modifier.fillMaxWidth())

            // 5. Prioridad
            PriorityChips(priority) { priority = it }

            // 6. Resumen de destinatario (ultima verificacion antes de enviar)
            course?.let { c ->
                val to = if (audience == Audience.COURSE) "a los estudiantes de ${c.courseCode}"
                else student?.let { "solo a: ${it.firstname} ${it.surname}" } ?: "— elija un estudiante —"
                Surface(color = Color(0xFFE0E7FF), shape = RoundedCornerShape(8.dp)) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (audience == Audience.STUDENT) {
                            Icon(Icons.Default.Lock, null, tint = MyPrimaryColor, modifier = Modifier.padding(end = 6.dp))
                        }
                        Text("Se enviará $to", color = MyPrimaryColor, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            val canDraft = course != null && (title.isNotBlank() || content.isNotBlank())
            OutlinedButton(
                enabled = canDraft,
                onClick = {
                    onSaveDraft(
                        com.erns.alertauni.data.model.DraftEntity(
                            id = draft?.id ?: 0, ownerKey = "", courseId = course!!.id,
                            audience = if (audience == Audience.STUDENT) "STUDENT" else "COURSE",
                            studentId = student?.student_id, studentFirstname = student?.firstname,
                            studentSurname = student?.surname, priority = priority, title = title, content = content
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Guardar como borrador") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 24.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                Button(
                    enabled = valid,
                    onClick = {
                        val c = course!!
                        if (audience == Audience.COURSE) onPublishCourse(c, priority, title, content)
                        else onPublishStudent(c, student!!, priority, title, content)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Publicar") }
            }
        }
    }
}
