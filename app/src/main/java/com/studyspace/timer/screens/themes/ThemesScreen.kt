package com.studyspace.timer.screens.themes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.studyspace.timer.ui.components.SectionHeader
import com.studyspace.timer.ui.theme.AppPalettes
import com.studyspace.timer.ui.theme.GalaxyDeepSpace
import com.studyspace.timer.ui.theme.GalaxyMutedLavender
import com.studyspace.timer.ui.theme.GalaxyNeonCyan
import com.studyspace.timer.ui.theme.GalaxyNeonPink
import com.studyspace.timer.ui.theme.GalaxyStarWhite
import com.studyspace.timer.ui.theme.GalaxyTwilightPurple
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
 *    [com.studyspace.timer.ui.theme.StudySpaceTimerTheme].
 * Both choices survive app restarts.
 */
@Composable
fun ThemesScreen(modifier: Modifier = Modifier, viewModel: ThemesViewModel = viewModel()) {
    val wallpaperSelection by viewModel.selection.collectAsState()
    val importError by viewModel.importError.collectAsState()
    val paletteId by viewModel.paletteId.collectAsState()

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> viewModel.onGalleryImagePicked(uri) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(2) }) {
            Text(
                text = "Themes & Wallpapers",
                style = MaterialTheme.typography.headlineMedium,
                color = GalaxyMutedLavender
            )
        }

        item(span = { GridItemSpan(2) }) {
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
                        .background(Brush.linearGradient(listOf(GalaxyDeepSpace, GalaxyTwilightPurple)))
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
            item(span = { GridItemSpan(2) }) {
                Text(
                    text = importError ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GalaxyNeonPink
                )
            }
        }

        item(span = { GridItemSpan(2) }) {
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
                            color = if (palette.id == paletteId) GalaxyNeonCyan else GalaxyStarWhite.copy(alpha = 0.2f),
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
                                .background(GalaxyNeonCyan),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Selected",
                                tint = GalaxyDeepSpace,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                Text(
                    text = palette.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GalaxyStarWhite,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

/** One selectable wallpaper thumbnail: image/preview content + label + selection ring. */
@Composable
private fun WallpaperTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Column {
        Box(
            modifier = Modifier
                .aspectRatio(0.8f)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) GalaxyNeonCyan else GalaxyStarWhite.copy(alpha = 0.2f),
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
            color = GalaxyStarWhite,
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
    Column {
        Box(
            modifier = Modifier
                .aspectRatio(0.8f)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) GalaxyNeonCyan else GalaxyStarWhite.copy(alpha = 0.2f),
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
                        .background(GalaxyDeepSpace.copy(alpha = 0.35f))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GalaxyTwilightPurple.copy(alpha = 0.5f))
                )
            }
            Icon(
                imageVector = Icons.Filled.AddPhotoAlternate,
                contentDescription = null,
                tint = GalaxyStarWhite,
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
                        .background(GalaxyDeepSpace.copy(alpha = 0.7f))
                        .clickable(onClick = onRemove),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remove personalized photo",
                        tint = GalaxyStarWhite,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        Text(
            text = "Personalize",
            style = MaterialTheme.typography.bodyMedium,
            color = GalaxyStarWhite,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.SelectionBadge() {
    Box(
        modifier = Modifier
            .padding(6.dp)
            .size(22.dp)
            .align(Alignment.TopEnd)
            .clip(CircleShape)
            .background(GalaxyNeonCyan),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = "Selected",
            tint = GalaxyDeepSpace,
            modifier = Modifier.size(14.dp)
        )
    }
}
