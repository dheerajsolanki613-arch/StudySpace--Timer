package com.studyspace.timer.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.rememberAsyncImagePainter
import com.studyspace.timer.ui.theme.AppPalette
import com.studyspace.timer.ui.theme.AppPalettes
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
 *
 * Scaling: always [ContentScale.Crop], in both portrait and landscape. An
 * earlier version switched to [ContentScale.Fit] on rotation so a tall
 * portrait wallpaper wouldn't get blown up — but that meant the background
 * visibly changed shape/framing on every rotation (a letterboxed vertical
 * strip appearing mid-screen), which read as broken rather than
 * intentional. [WallpaperImage] now always crops to fill its measured box,
 * so the wallpaper's framing/zoom looks the same regardless of
 * orientation — it just fills whatever box it's given, exactly like a
 * normal Android background/wallpaper does.
 */
@Composable
fun AppBackground(
    selection: WallpaperSelection,
    modifier: Modifier = Modifier,
    palette: AppPalette = AppPalettes.galaxy
) {
    when (selection) {
        is WallpaperSelection.Default -> {
            val fallback = MaterialTheme.colorScheme.background
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = palette.previewColors.ifEmpty { listOf(fallback, fallback) }
                        )
                    )
            )
        }

        is WallpaperSelection.BuiltIn -> {
            val scrimColor = palette.previewColors.firstOrNull() ?: MaterialTheme.colorScheme.background
            WallpaperImage(
                modifier = modifier,
                palette = palette,
                scrimColor = scrimColor
            ) { scale ->
                Image(
                    painter = painterResource(id = selection.wallpaper.drawableRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = scale
                )
            }
        }

        is WallpaperSelection.Custom -> {
            val scrimColor = palette.previewColors.firstOrNull() ?: MaterialTheme.colorScheme.background
            WallpaperImage(
                modifier = modifier,
                palette = palette,
                scrimColor = scrimColor
            ) { scale ->
                Image(
                    painter = rememberAsyncImagePainter(model = File(selection.filePath)),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = scale
                )
            }
        }
    }
}

/**
 * Shared shell for the two image-backed wallpaper cases: a themed gradient
 * base, the image itself always cropped to fill, then the readability
 * scrim on top. No longer needs [BoxWithConstraints] to pick a scale per
 * orientation — [ContentScale.Crop] is passed through unconditionally now,
 * so the wallpaper's framing stays visually identical across rotation.
 */
@Composable
private fun WallpaperImage(
    palette: AppPalette,
    scrimColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    image: @Composable (ContentScale) -> Unit
) {
    val fallback = MaterialTheme.colorScheme.background
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = palette.previewColors.ifEmpty { listOf(fallback, fallback) }
                )
            )
    ) {
        image(ContentScale.Crop)
        ScrimOverlay(color = scrimColor, isDark = palette.isDark)
    }
}

/**
 * Tints a wallpaper image just enough that the active palette's text stays
 * legible on top of it. Dark galaxy palettes darken the photo (as before);
 * Kawaii Pastel instead lightens it slightly toward cream, since espresso
 * text needs a *light* backdrop, not a dark one.
 */
@Composable
private fun ScrimOverlay(color: androidx.compose.ui.graphics.Color, isDark: Boolean) {
    val scrimAlpha = if (isDark) 0.55f else 0.35f
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color.copy(alpha = scrimAlpha))
    )
}
