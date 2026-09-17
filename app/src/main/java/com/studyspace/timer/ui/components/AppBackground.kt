package com.studyspace.timer.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.rememberAsyncImagePainter
import com.studyspace.timer.ui.theme.AppPalette
import com.studyspace.timer.ui.theme.AppPalettes
import com.studyspace.timer.ui.theme.GalaxyDeepSpace
import com.studyspace.timer.wallpaper.WallpaperSelection
import java.io.File

/**
 * Renders the app-wide background behind the whole navigation graph, so every
 * screen shares one consistent visual identity instead of each screen owning
 * its own backdrop. Three cases:
 *  - [WallpaperSelection.Default]: a gradient built from the active [palette]'s
 *    preview colors (so picking a palette re-colors the background too, not
 *    just buttons/cards) — falls back to the base galaxy gradient when no
 *    palette is supplied.
 *  - [WallpaperSelection.BuiltIn]: one of the bundled wallpaper images.
 *  - [WallpaperSelection.Custom]: the user's own photo, imported via
 *    "Personalize" on the Themes screen.
 *
 * A dark scrim sits above any image so text and glass cards stay readable
 * regardless of how bright the chosen photo is.
 */
@Composable
fun AppBackground(
    selection: WallpaperSelection,
    modifier: Modifier = Modifier,
    palette: AppPalette = AppPalettes.galaxy
) {
    when (selection) {
        is WallpaperSelection.Default -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = palette.previewColors.take(2).ifEmpty {
                                listOf(GalaxyDeepSpace, GalaxyDeepSpace)
                            }
                        )
                    )
            )
        }

        is WallpaperSelection.BuiltIn -> {
            Box(modifier = modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = selection.wallpaper.drawableRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                ScrimOverlay(color = palette.previewColors.firstOrNull() ?: GalaxyDeepSpace)
            }
        }

        is WallpaperSelection.Custom -> {
            Box(modifier = modifier.fillMaxSize()) {
                Image(
                    painter = rememberAsyncImagePainter(model = File(selection.filePath)),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                ScrimOverlay(color = palette.previewColors.firstOrNull() ?: GalaxyDeepSpace)
            }
        }
    }
}

/** Darkens a wallpaper image just enough that galaxy-white/lavender text stays legible. */
@Composable
private fun ScrimOverlay(color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color.copy(alpha = 0.55f))
    )
}
