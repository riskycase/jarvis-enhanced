package com.riskycase.jarvisEnhanced.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.riskycase.jarvisEnhanced.models.Filter
import com.riskycase.jarvisEnhanced.util.Destinations.EDIT_FILTER

@Composable
fun FilterListItemComponent(filter: Filter, navController: NavController) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .clickable {
                navController.navigate("$EDIT_FILTER/${filter.id}")
            }
            .width(IntrinsicSize.Max),
    ) {
        Text("Title: ${filter.title}", fontSize = 24.sp)
        Text("Text: ${filter.text}")
    }
}