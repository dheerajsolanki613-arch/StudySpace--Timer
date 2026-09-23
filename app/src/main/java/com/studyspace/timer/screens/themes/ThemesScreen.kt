package com.studyspace.timer.screens.themes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.studyspace.timer.ui.components.SectionHeader
import com.studyspace.timer.ui.theme.AppPalettes
import com.studyspace.timer.ui.theme.DeepSpaceBlue
import com.studyspace.timer.ui.theme.GalaxyCrimsonRed
import com.studyspace.timer.ui.theme.GalaxyEmeraldGreen
import com.studyspace.timer.ui.theme.GalaxyNeonCyan
import com.studyspace.timer.ui.theme.GalaxyNeonPink
import com.studyspace.timer.ui.theme.GalaxySolarOrange
import com.studyspace.timer.ui.theme.MinimalLightBlue
import com.studyspace.timer.ui.theme.contrastSafeContentColor
import com.studyspace.timer.ui.util.isLandscape
import com.studyspace.timer.wallpaper.WallpaperCatalog
import com.studyspace.timer.wallpaper.WallpaperSelection
import java.io.File

/**
 * Themes & Wallpapers screen (Stage 6).
 *
 * Both pickers below are real and persist via [ThemesViewModel] + DataStore,
 * applied app-wide:
 *  - Wallpaper (Default / bundled images / "Personalize" gallery photo) is
 *    painted behind every screen by [com.studyspace.timer.ui.components.AppBackground].
 *  - Palette swaps the Material color scheme itself (primary/secondary/
 *    surface colors used by every screen, card, and button in the app) via
 *    [com.studyspace.timer.ui.theme.StudySpaceTimerTheme]. Includes the
 *    light "Kawaii Pastel" palette alongside the six dark galaxy ones.
 * Both choices survive app restarts.
 *
 * Theme/rotation update: every fixed `Galaxy*` reference is gone — selection
 * rings, badges, and labels read `MaterialTheme.colorScheme`, the "Default"
 * wallpaper tile now previews the *currently selected* palette's gradient
 * instead of a hardcoded galaxy one, and the grids use 3 columns in
 * landscape (vs. 2 in portrait).
 */
@Composable
fun ThemesScreen(modifier: Modifier = Modifier, viewModel: ThemesViewModel = viewModel()) {
    val wallpaperSelection by viewModel.selection.collectAsState()
    val importError by viewModel.importError.collectAsState()
    val paletteId by viewModel.paletteId.collectAsState()
    val accentArgb by viewModel.accentArgb.collectAsState()
    val activePalette = AppPalettes.byId(paletteId)
    val columns = if (isLandscape()) 3 else 2

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> viewModel.onGalleryImagePicked(uri) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(columns) }) {
            Text(
                text = "Themes & Wallpapers",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
        }

        item(span = { GridItemSpan(columns) }) {
            SectionHeader(title = "Wallpaper")
        }

        item {
            WallpaperTile(
                label = "Default",
                selected = wallpaperSelection is WallpaperSelection.Default,
                onClick = { viewModel.selectDefault() }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(activePalette.previewColors))
                )
            }
        }

        item {
            PersonalizeTile(
                selected = wallpaperSelection is WallpaperSelection.Custom,
                customFilePath = (wallpaperSelection as? WallpaperSelection.Custom)?.filePath,
                onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onRemove = { viewModel.removeCustomPhoto() }
            )
        }

        items(WallpaperCatalog.builtIns) { wallpaper ->
            val isSelected = (wallpaperSelection as? WallpaperSelection.BuiltIn)?.wallpaper?.id == wallpaper.id
            WallpaperTile(
                label = wallpaper.label,
                selected = isSelected,
                onClick = { viewModel.selectBuiltIn(wallpaper) }
            ) {
                Image(
                    painter = painterResource(id = wallpaper.drawableRes),
                    contentDescription = wallpaper.label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        if (importError != null) {
            item(span = { GridItemSpan(columns) }) {
                Text(
                    text = importError ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        item(span = { GridItemSpan(columns) }) {
            SectionHeader(title = "Palette")
        }
        items(AppPalettes.all) { palette ->
            Column {
                Box(
                    modifier = Modifier
                        .aspectRatio(1.4f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { viewModel.selectPalette(palette.id) }
                        .background(Brush.linearGradient(palette.previewColors))
                        .border(
                            width = if (palette.id == paletteId) 2.dp else 1.dp,
                            color = if (palette.id == paletteId) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                Color.White.copy(alpha = 0.2f)
                            },
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.TopEnd
                ) {
                    if (palette.id == paletteId) {
                        Box(
                            modifier = Modifier
                                .padding(6.dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                Text(
                    text = palette.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        item(span = { GridItemSpan(columns) }) {
            SectionHeader(title = "Accent color")
        }
        item(span = { GridItemSpan(columns) }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Personalize just the accent, on top of whichever palette you've picked above.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AccentSwatch(
                        color = activePalette.colorScheme.primary,
                        label = "Default",
                        selected = accentArgb == null,
                        showAsDefault = true,
                        onClick = { viewModel.clearAccent() }
                    )
                    ACCENT_SWATCHES.forEach { swatch ->
                        AccentSwatch(
                            color = swatch,
                            label = null,
                            selected = accentArgb == swatch.toArgb(),
                            showAsDefault = false,
                            onClick = { viewModel.setAccent(swatch.toArgb()) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Phase 15 — Accent customization's curated swatch set. A fixed, pre-picked
 * list rather than a free HSV/wheel picker: every one of these already
 * clears WCAG contrast against every palette's background via
 * [com.studyspace.timer.ui.theme.effectiveColorScheme]'s
 * `contrastSafeContentColor` fallback, and a curated set is simpler to keep
 * legible than validating an arbitrary user-picked hue. Draws from existing
 * palette accents (so a swatch always looks "at home" in the app) plus two
 * new hues not already used by any palette (amber, violet) for more choice.
 */
private val ACCENT_SWATCHES: List<Color> = listOf(
    GalaxyNeonPink, GalaxyNeonCyan, GalaxySolarOrange, GalaxyEmeraldGreen, GalaxyCrimsonRed,
    DeepSpaceBlue, MinimalLightBlue, Color(0xFFFFC107), Color(0xFF9C6ADE)
)

@Composable
private fun AccentSwatch(color: Color, label: String?, selected: Boolean, showAsDefault: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .then(
                    if (showAsDefault) {
                        Modifier.background(Brush.sweepGradient(listOf(color, Color.Transparent, color)))
                    } else {
                        Modifier.background(color)
                    }
                )
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.onBackground else Color.White.copy(alpha = 0.3f),
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = contrastSafeContentColor(background = color, preferred = Color.White),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * One selectable wallpaper thumbnail: image/preview content + label +
 * selection ring. The ring and badge use `MaterialTheme.colorScheme`, but
 * the surrounding unselected border stays a fixed translucent white/black
 * split by luminance — the tiles themselves show arbitrary bundled photos,
 * not the active palette, so their border can't lean on `onSurface` the way
 * card text does.
 */
@Composable
private fun WallpaperTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column {
        Box(
            modifier = Modifier
                .aspectRatio(0.8f)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) scheme.secondary else Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            content()
            if (selected) {
                SelectionBadge()
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onBackground,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/**
 * "Personalize" tile: lets the user pick their own photo from the device
 * gallery via the system Photo Picker (no storage permission needed). Shows
 * the currently-imported custom photo as its own thumbnail once one exists,
 * so it doubles as both the entry point and the "currently selected" state.
 * A small close button appears once a photo has been imported, so removing
 * it doesn't require picking a replacement first.
 */
@Composable
private fun PersonalizeTile(
    selected: Boolean,
    customFilePath: String?,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column {
        Box(
            modifier = Modifier
                .aspectRatio(0.8f)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) scheme.secondary else Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            if (customFilePath != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = File(customFilePath)),
                    contentDescription = "Your personalized wallpaper",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(scheme.background.copy(alpha = 0.35f))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(scheme.surfaceVariant.copy(alpha = 0.5f))
                )
            }
            Icon(
                imageVector = Icons.Filled.AddPhotoAlternate,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
            if (selected) {
                SelectionBadge()
            }
            if (customFilePath != null) {
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .size(22.dp)
                        .align(Alignment.TopStart)
                        .clip(CircleShape)
                        .background(scheme.background.copy(alpha = 0.7f))
                        .clickable(onClick = onRemove),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remove personalized photo",
                        tint = scheme.onBackground,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        Text(
            text = "Personalize",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onBackground,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.SelectionBadge() {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .padding(6.dp)
            .size(22.dp)
            .align(Alignment.TopEnd)
            .clip(CircleShape)
            .background(scheme.secondary),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = "Selected",
            tint = scheme.onSecondary,
            modifier = Modifier.size(14.dp)
        )
    }
}
