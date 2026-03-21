package com.riskycase.jarvisEnhanced.util.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackgroundImageUtils @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var backgroundImage: Bitmap? = null
    private var smallBackgroundImage: Bitmap? = null
    private var loadedFromDisk = false

    private var baseColor = Color.WHITE
    private var darkBaseColor = Color.GRAY
    private var darkerBaseColor = Color.DKGRAY
    private var colorOnBaseColor = Color.BLACK

    fun loadFromDisk() {
        val filesDir = context.filesDir
        val wallpaperFile = File(filesDir, "wallpaper")
        val smallFile = File(filesDir, "wallpaper_small")
        if (smallFile.exists()) {
            BitmapFactory.decodeFile(smallFile.absolutePath)?.let { setSmallBackgroundImage(it) }
        }
        if (wallpaperFile.exists()) {
            BitmapFactory.decodeFile(wallpaperFile.absolutePath)?.let { setBackgroundImage(it) }
        }
        loadedFromDisk = true
    }

    private fun ensureLoaded() {
        if (!loadedFromDisk) loadFromDisk()
    }

    fun getBackgroundImage(): Bitmap? {
        ensureLoaded()
        return backgroundImage
    }

    fun setBackgroundImage(newImage: Bitmap?) {
        newImage?.also { bitmap ->
            val backgroundImagePalette = Palette.Builder(bitmap).generate()
            baseColor = backgroundImagePalette.getVibrantColor(Color.WHITE)
            val darkColorHSL = floatArrayOf(0f, 0f, 0f)
            ColorUtils.colorToHSL(
                backgroundImagePalette.getDarkVibrantColor(Color.DKGRAY), darkColorHSL
            )
            darkerBaseColor =
                ColorUtils.HSLToColor(floatArrayOf(darkColorHSL[0], darkColorHSL[1], 0.47f))
            darkBaseColor =
                ColorUtils.HSLToColor(floatArrayOf(darkColorHSL[0], darkColorHSL[1], 0.77f))
            colorOnBaseColor =
                if (ColorUtils.calculateLuminance(baseColor) > 0.5f) Color.BLACK else Color.WHITE
        }
        backgroundImage?.recycle()
        backgroundImage = newImage
    }

    fun getSmallBackgroundImage(): Bitmap? {
        ensureLoaded()
        return smallBackgroundImage
    }

    fun setSmallBackgroundImage(newImage: Bitmap?) {
        smallBackgroundImage?.recycle()
        smallBackgroundImage = newImage
    }

    fun getBaseColor(): Int {
        return baseColor
    }

    fun getDarkBaseColor(): Int {
        return darkBaseColor
    }

    fun getDarkerBaseColor(): Int {
        return darkerBaseColor
    }

    fun getColorOnBaseColor(): Int {
        return colorOnBaseColor
    }

}
