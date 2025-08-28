package com.riskycase.jarvisEnhanced.ui.screen

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.riskycase.jarvisEnhanced.ui.components.TopBarComponent
import com.riskycase.jarvisEnhanced.viewModel.WallpaperPreviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperPreviewScreen(
    wallpaperPreviewViewModel: WallpaperPreviewViewModel, navController: NavController, drawerState: DrawerState
) {

    val engine by remember { mutableStateOf(wallpaperPreviewViewModel.getWallpaperEngine()) }

    val aspectRatio = LocalContext.current.resources.displayMetrics.let { metrics ->
        metrics.widthPixels.toFloat() / metrics.heightPixels.toFloat()
    }

    DisposableEffect(Unit) {
        onDispose { wallpaperPreviewViewModel.cleanup() }
    }

    Scaffold (
        topBar = { TopBarComponent(navController, drawerState, "Wallpaper Preview", false) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxHeight(0.8f)
                .fillMaxWidth(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AndroidView(factory = { ctx ->
                SurfaceView(ctx).apply {

                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            engine.onSurfaceCreated(holder)
                        }

                        override fun surfaceChanged(
                            holder: SurfaceHolder,
                            format: Int,
                            width: Int,
                            height: Int
                        ) {
                            engine.onSurfaceChanged(
                                holder, format, width, height)
                        }

                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            engine.onSurfaceDestroyed(holder)
                        }
                    })
                }
            }, modifier = Modifier
                .aspectRatio(aspectRatio)
            )
        }
    }
}
