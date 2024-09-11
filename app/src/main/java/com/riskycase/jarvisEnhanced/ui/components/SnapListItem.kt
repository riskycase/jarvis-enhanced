package com.riskycase.jarvisEnhanced.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SnapListItemComponent(sender: String, time: String) {
    Column(
        modifier = Modifier
            .padding(16.dp)
    ) {
        Text(sender, fontSize = 24.sp)
        Text(time)
    }
}