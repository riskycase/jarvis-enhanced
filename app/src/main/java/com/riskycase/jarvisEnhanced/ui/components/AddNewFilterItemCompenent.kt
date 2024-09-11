package com.riskycase.jarvisEnhanced.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.riskycase.jarvisEnhanced.util.Destinations.EDIT_FILTER

@Composable
fun AddNewFilterItemComponent(navController: NavController) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .clickable {
                navController.navigate("$EDIT_FILTER/0")
            },
    ) {
        Text(
            "Add new",
            fontSize = 24.sp,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}