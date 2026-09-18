package com.studyspace.timer.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
 * Scaling: every bundled/custom wallpaper is a tall portrait photo (~9:16).
 * [ContentScale.Crop] alone handles that fine on a portrait phone screen,
 * whose aspect ratio is close to the image's — but on a landscape/wide
 * screen, "cover the whole width" forces the same narrow image to blow up
 * far beyond its own resolution and then shows only a thin vertical sliver
 * of it, which is the "excessive zoom and cropping" bug. [WallpaperImage]
 * measures its own box and only crops when the box is *taller* than it is
 * wide (i.e. roughly matches the photo's own portrait shape); once the box
 * is wider than it is tall it switches to [ContentScale.Fit] instead, which
 * scales the photo down to fit without blowing it up or slicing off most of
 * it. The palette gradient is painted underneath in every case, so any
 * space [ContentScale.Fit] leaves on the sides reads as an intentional
 * themed frame rather than a blank gap.
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
 * base (so [ContentScale.Fit] never leaves a blank gap), the image itself
 * with orientation-appropriate scaling, then the readability scrim on top.
 * `image` receives the [ContentScale] to draw itself with rather than
 * picking one internally, since only this composable has the measured
 * `BoxWithConstraints` size to decide portrait-crop vs. landscape-fit.
 */
@Composable
private fun WallpaperImage(
    palette: AppPalette,
    scrimColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    image: @Composable (ContentScale) -> Unit
) {
    val fallback = MaterialTheme.colorScheme.background
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight
        val scale = if (isLandscape) ContentScale.Fit else ContentScale.Crop

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = palette.previewColors.ifEmpty { listOf(fallback, fallback) }
                    )
                )
        ) {
            image(scale)
            // Landscape keeps the image at its native aspect via Fit rather
            // than blowing it up (see the class doc), which means the
            // busiest part of the photo sits fully within the visible
            // frame instead of being cropped away — exactly where the
            // Analytics/Session cards' translucent [GlassCard] fill used to
            // sit right on top of it with too little contrast underneath
            // ("wallpaper overlaps the UI" / "poor contrast" reports). The
            // scrim goes stronger in that orientation specifically, rather
            // than raising it everywhere and flattening the portrait look.
            ScrimOverlay(color = scrimColor, isDark = palette.isDark, boosted = isLandscape)
        }
    }
}

/**
 * Tints a wallpaper image just enough that the active palette's text stays
 * legible on top of it. Dark galaxy palettes darken the photo (as before);
 * Kawaii Pastel instead lightens it slightly toward cream, since espresso
 * text needs a *light* backdrop, not a dark one.
 *
 * [boosted] raises both alphas further for the landscape/[ContentScale.Fit]
 * case, where the full, un-cropped photo is visible behind foreground
 * content rather than just a cropped slice — see the call site's doc.
 */
@Composable
private fun ScrimOverlay(
    color: androidx.compose.ui.graphics.Color,
    isDark: Boolean,
    boosted: Boolean = false
) {
    val baseAlpha = if (isDark) 0.55f else 0.35f
    val scrimAlpha = if (boosted) (baseAlpha + 0.18f).coerceAtMost(0.9f) else baseAlpha
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color.copy(alpha = scrimAlpha))
    )
}
