package com.erns.alertauni.screen.common

import androidx.compose.foundation.background
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erns.alertauni.R
import com.erns.alertauni.ui.theme.MyPrimaryColor
import com.erns.alertauni.ui.theme.MyPrimaryForegroundColor
import com.erns.alertauni.ui.theme.MySecondaryColor

@Composable
fun SearchBoxComponent(
    userName:String,
    title:String
) {
    val showSearch = remember { mutableStateOf(false) }
    val query = remember { mutableStateOf("") }
    // En horizontal el alto disponible es poco: se compacta el encabezado.
    val compact = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (compact) 64.dp else 100.dp)
            .background(MyPrimaryColor)
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
            .padding(10.dp)
    ) {
        if (showSearch.value) {
            TextField(
                value = query.value,
                onValueChange = { query.value = it },
                placeholder = { Text("Buscar...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterStart),
                trailingIcon = {
                    IconButton(onClick = { showSearch.value = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        } else {
            // Normal layout: left + right
            Column(modifier = Modifier.align(Alignment.CenterStart)) {
                Text(
                    text = userName,
                    color = MySecondaryColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = title,
                    color = MyPrimaryForegroundColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 20.sp else 26.sp
                )
            }
            IconButton(
                onClick = { showSearch.value = true },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.outline_search_24),
                    contentDescription = "Search",
                    modifier = Modifier.size(32.dp),
                    tint = Color.Unspecified
                )
            }
        }
    }
}
