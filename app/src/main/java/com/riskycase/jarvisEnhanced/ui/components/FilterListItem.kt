package com.riskycase.jarvisEnhanced.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.riskycase.jarvisEnhanced.models.Filter
import com.riskycase.jarvisEnhanced.util.Destinations.EDIT_FILTER

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilterListItemComponent(
    filter: Filter, navController: NavController, deleteFilter: () -> Unit
) {
    val openConfirmDialog = remember { mutableStateOf(false) }
    when {
        openConfirmDialog.value -> {
            AlertDialog(onDismissRequest = { openConfirmDialog.value = false }, confirmButton = {
                TextButton(onClick = {
                    deleteFilter()
                    openConfirmDialog.value = false
                }) {
                    Text("Delete")
                }
            }, title = { Text("Delete filter?") }, text = {
                Column {
                    Text("Are you sure you want to delete the following filter?")
                    Text("Title: " + filter.title)
                    Text("Text: " + filter.text)
                }
            })
        }
    }
    Column(
        modifier = Modifier
            .combinedClickable(onClick = {
                navController.navigate("$EDIT_FILTER/${filter.id}")
            }, onLongClick = {
                openConfirmDialog.value = true
            })
            .padding(16.dp)
            .fillMaxWidth(1f),
    ) {
        Text("Title: ${filter.title}", fontSize = 24.sp)
        Text("Text: ${filter.text}")
    }
    Divider()
}