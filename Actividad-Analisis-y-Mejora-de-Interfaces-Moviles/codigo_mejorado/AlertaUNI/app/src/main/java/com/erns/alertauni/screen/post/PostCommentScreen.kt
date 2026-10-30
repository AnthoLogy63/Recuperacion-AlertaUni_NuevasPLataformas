package com.erns.alertauni.screen.post

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.erns.alertauni.data.model.CommentEntity
import com.erns.alertauni.screen.common.SearchBoxComponent

@Composable
fun PostCommentScreen(
    viewModel: PostCommentViewModel = hiltViewModel(),
) {
    val comments = viewModel.comments.collectAsState()
    val username = remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.username.collect {
            username.value = it
        }
    }

    val onClickAddComment: (String) -> Unit = { comment ->
        viewModel.sendComment(comment)
    }

    CommentScreenLayout(
        username.value,
        "Comentarios",
        comments.value,
        onClickAddComment
    )

}

@Composable
fun CommentScreenLayout(
    username: String,
    title: String,
    comments: List<CommentEntity>,
    onClickAddComment: (String) -> Unit
) {
    // Con reverseLayout el índice 0 es el mensaje más reciente (abajo).
    val messages = remember(comments) { comments.reversed() }
    val listState = rememberLazyListState()

    // Al llegar un mensaje nuevo, la lista vuelve al final de la conversación.
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(0)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SearchBoxComponent(username, title)

        // En pantallas anchas (tableta, horizontal) el chat se centra con un ancho
        // máximo para que las líneas de texto sigan siendo legibles.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .weight(1f),
                    reverseLayout = true // To show latest at bottom
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(message)
                    }
                }

                MessageInputBar(onSend = onClickAddComment)
            }
        }
    }
}

/**
 * Barra de entrada de mensajes.
 * - El texto sobrevive a rotaciones (rememberSaveable).
 * - Enter en el teclado envía el mensaje (ImeAction.Send).
 * - El botón de enviar se deshabilita mientras no haya texto.
 * - En horizontal limita las líneas para no tapar la lista con el teclado abierto.
 */
@Composable
fun MessageInputBar(onSend: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val canSend = text.isNotBlank()

    val send = {
        if (canSend) {
            onSend(text.trim())
            text = ""
        }
    }

    TextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        // 1. Esquinas redondeadas estilo WhatsApp
        shape = RoundedCornerShape(28.dp),
        // 2. Eliminamos los indicadores visuales de "caja" de Material
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,   // Quita la línea al enfocar
            unfocusedIndicatorColor = Color.Transparent, // Quita la línea al desenfocar
            disabledIndicatorColor = Color.Transparent
        ),
        placeholder = {
            Text("Mensaje", color = Color.Gray)
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { send() }),
        trailingIcon = {
            IconButton(onClick = { send() }, enabled = canSend) {
                Icon(
                    Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "Enviar",
                    tint = if (canSend) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        },
        maxLines = if (isLandscape) 2 else 5
    )
}

@Composable
fun MessageBubble(message: CommentEntity) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = if (message.isMine) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        // La burbuja ocupa como máximo 80% del ancho, con tope de 480dp,
        // para que el texto siga siendo legible en horizontal y tabletas.
        val bubbleMaxWidth = (maxWidth * 0.8f).coerceAtMost(480.dp)
        Surface(
            modifier = Modifier.widthIn(max = bubbleMaxWidth),
            shape = RoundedCornerShape(16.dp),
            color = if (message.isMine) Color(0xFFDCF8C6) else Color.White,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                if (!message.isMine) {
                    Text(
                        text = message.authorName,
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = message.content,
                    textAlign = if (message.isMine) TextAlign.End else TextAlign.Start
                )
                Text(
                    text = message.publishedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.align(if (message.isMine) Alignment.End else Alignment.Start)
                )
            }
        }
    }
}
