package org.skepsun.kototoro.reader.novel.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.skepsun.kototoro.R
import org.skepsun.kototoro.reader.novel.NovelPageTurnAnimation
import org.skepsun.kototoro.reader.novel.NovelReaderSettings
import org.skepsun.kototoro.reader.novel.NovelReaderThemePreset
import org.skepsun.kototoro.reader.novel.NovelTranslationDisplayMode
import org.skepsun.kototoro.reader.novel.ReadingMode
import org.skepsun.kototoro.reader.novel.novelReaderPalette
import org.skepsun.kototoro.core.prefs.ReaderAnimation
import org.skepsun.kototoro.reader.ui.compose.ReaderAnimationIcon
import org.skepsun.kototoro.reader.ui.compose.ReaderAnchoredBottomSheet
import org.skepsun.kototoro.reader.ui.compose.design.ReaderOptionDivider
import org.skepsun.kototoro.reader.ui.compose.design.ReaderOptionGroup
import org.skepsun.kototoro.reader.ui.compose.design.ReaderOptionSwitchRow
import org.skepsun.kototoro.reader.ui.compose.design.ReaderOptionValueRow
import org.skepsun.kototoro.reader.ui.compose.design.ReaderSegmentedChoice
import kotlinx.coroutines.launch

private const val DEFAULT_READER_BRIGHTNESS = 0.8f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComposeNovelReaderOptionsSheet(
    settings: NovelReaderSettings,
    onDismiss: () -> Unit,
    onSettingsChanged: (NovelReaderSettings) -> Unit,
    onToggleTranslation: () -> Unit,
    replaceRulesEnabled: Boolean = true,
    onToggleReplaceRules: () -> Unit = {},
    onShowReplaceRules: () -> Unit = {},
    onShowMarkings: () -> Unit = {},
    onBookmark: () -> Unit = {},
    onTts: () -> Unit,
    onClearTranslationCache: () -> Unit,
) {
    var sliderEditor by remember { mutableStateOf<SliderEditor?>(null) }
    fun update(transform: NovelReaderSettings.() -> NovelReaderSettings) {
        onSettingsChanged(settings.transform().normalized())
    }
    val pages = listOf(
        NovelOptionsPage(R.drawable.ic_book_page, R.string.novel_reader_tab_reading),
        NovelOptionsPage(R.drawable.ic_lightbulb, R.string.novel_reader_tab_brightness),
        NovelOptionsPage(R.drawable.ic_format_size, R.string.novel_reader_tab_typography),
        NovelOptionsPage(R.drawable.ic_translate, R.string.novel_reader_tab_translation),
        NovelOptionsPage(R.drawable.ic_more_vert, R.string.novel_reader_tab_tools),
    )
    val pagerState = rememberPagerState(pageCount = pages::size)
    val scope = rememberCoroutineScope()
    val palette = novelReaderPalette(settings.themePreset, isSystemInDarkTheme())
    val sheetColor = Color(palette.backgroundColor)
    val sheetContentColor = Color(palette.textColor)
    ReaderAnchoredBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = sheetColor,
        contentColor = sheetContentColor,
        scrimColor = Color.Black.copy(alpha = if (palette.isDark) 0.58f else 0.42f),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Color(palette.secondaryTextColor))
        },
    ) { sheetDragModifier ->
        val readerColors = MaterialTheme.colorScheme.copy(
            primary = Color(palette.chromeTextColor),
            onPrimary = sheetColor,
            primaryContainer = Color(palette.placeholderColor),
            onPrimaryContainer = Color(palette.textColor),
            secondaryContainer = Color(palette.highlightColor),
            onSecondaryContainer = Color(palette.textColor),
            background = sheetColor,
            surface = sheetColor,
            surfaceVariant = Color(palette.placeholderColor),
            onSurface = sheetContentColor,
            onSurfaceVariant = Color(palette.secondaryTextColor),
        )
        MaterialTheme(colorScheme = readerColors) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .navigationBarsPadding(),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(sheetDragModifier)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                ) {
                    pages.forEachIndexed { index, page ->
                        NovelOptionsTab(
                            page = page,
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.scrollToPage(index) } },
                            modifier = Modifier.widthIn(min = 68.dp),
                        )
                    }
                }
                HorizontalPager(
                    state = pagerState,
                    overscrollEffect = null,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                ) { page ->
                    when (page) {
                        0 -> NovelReaderReadingPage(settings = settings, update = ::update)
                        1 -> NovelReaderBrightnessPage(settings = settings, update = ::update)
                        2 -> NovelReaderTypographyPage(
                            settings = settings,
                            update = ::update,
                            onEditSlider = { sliderEditor = it },
                        )
                        3 -> NovelReaderTranslationPage(
                            settings = settings,
                            update = ::update,
                            onToggleTranslation = onToggleTranslation,
                            onClearTranslationCache = onClearTranslationCache,
                        )
                        else -> NovelReaderToolsPage(
                            onShowMarkings = { onShowMarkings(); onDismiss() },
                            onBookmark = { onBookmark(); onDismiss() },
                            onTts = { onTts(); onDismiss() },
                            replaceRulesEnabled = replaceRulesEnabled,
                            onToggleReplaceRules = onToggleReplaceRules,
                            onShowReplaceRules = { onShowReplaceRules(); onDismiss() },
                            onReset = { onSettingsChanged(NovelReaderSettings()) },
                        )
                    }
                }
            }
        }
        SliderEditorDialog(sliderEditor) { sliderEditor = null }
    }
}

@Immutable
private data class NovelOptionsPage(val icon: Int, val label: Int)

@Composable
private fun NovelOptionsTab(
    page: NovelOptionsPage,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f) else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Icon(
                painter = painterResource(page.icon),
                contentDescription = stringResource(page.label),
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(page.label),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                modifier = Modifier.semantics { role = Role.Tab },
            )
        }
    }
}

@Composable
private fun NovelReaderReadingPage(
    settings: NovelReaderSettings,
    update: (NovelReaderSettings.() -> NovelReaderSettings) -> Unit,
) = NovelOptionsPageList {
    item {
        Text(
            text = stringResource(R.string.novel_reader_tab_reading),
            style = MaterialTheme.typography.titleMedium,
        )
    }
    novelReadingOptionsContent(settings, update)
}

@Composable
private fun NovelReaderBrightnessPage(
    settings: NovelReaderSettings,
    update: (NovelReaderSettings.() -> NovelReaderSettings) -> Unit,
) = NovelOptionsPageList {
    item {
        Text(
            text = stringResource(R.string.novel_reader_brightness_section),
            style = MaterialTheme.typography.titleMedium,
        )
    }
    item {
        ReaderOptionGroup {
            NovelReaderSliderRow(
                label = stringResource(R.string.brightness),
                valueLabel = settings.screenBrightness?.let { "${(it * 100f).roundToInt()}%" }
                    ?: stringResource(R.string.follow_system),
                value = settings.screenBrightness ?: DEFAULT_READER_BRIGHTNESS,
                valueRange = NovelReaderSettings.SCREEN_BRIGHTNESS_RANGE,
                enabled = settings.screenBrightness != null,
                onValueChange = { value -> update { copy(screenBrightness = value) } },
                icon = R.drawable.ic_lightbulb,
            )
            ReaderOptionDivider()
            ReaderOptionSwitchRow(
                label = stringResource(R.string.novel_reader_follow_system_brightness),
                checked = settings.screenBrightness == null,
                onCheckedChange = { followSystem ->
                    update {
                        copy(screenBrightness = if (followSystem) {
                            null
                        } else {
                            screenBrightness ?: DEFAULT_READER_BRIGHTNESS
                        })
                    }
                },
            )
        }
    }
    item {
        Text(
            text = stringResource(R.string.novel_reader_palette_section),
            style = MaterialTheme.typography.titleMedium,
        )
    }
    item {
        ReaderSegmentedChoice(
            title = null,
            options = NovelReaderThemePreset.entries.map { stringResource(it.label) },
            selectedIndex = NovelReaderThemePreset.entries.indexOf(settings.themePreset),
            onSelected = { index -> update { copy(themePreset = NovelReaderThemePreset.entries[index]) } },
            icon = { NovelThemeSwatch(NovelReaderThemePreset.entries[it]) },
            stackedTitle = true,
            verticalOptions = true,
        )
    }
}

@Composable
private fun NovelReaderTypographyPage(
    settings: NovelReaderSettings,
    update: (NovelReaderSettings.() -> NovelReaderSettings) -> Unit,
    onEditSlider: (SliderEditor) -> Unit,
) = NovelOptionsPageList {
    item {
        Text(
            text = stringResource(R.string.novel_reader_typography_section),
            style = MaterialTheme.typography.titleMedium,
        )
    }
    item {
        ReaderOptionGroup {
            NovelReaderFontOptionRow(
                selected = settings.font,
                onSelected = { update { copy(font = it) } },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            )
            ReaderOptionDivider()
            NovelTypographyRows(settings, update, onEditSlider)
            ReaderOptionDivider()
            ReaderOptionSwitchRow(
                label = stringResource(R.string.novel_first_line_indent),
                checked = settings.enableParagraphIndent,
                onCheckedChange = { update { copy(enableParagraphIndent = it) } },
            )
        }
    }
}

@Composable
private fun NovelReaderTranslationPage(
    settings: NovelReaderSettings,
    update: (NovelReaderSettings.() -> NovelReaderSettings) -> Unit,
    onToggleTranslation: () -> Unit,
    onClearTranslationCache: () -> Unit,
) = NovelOptionsPageList {
    item {
        Text(
            text = stringResource(R.string.novel_reader_translation_section),
            style = MaterialTheme.typography.titleMedium,
        )
    }
    item {
        ReaderOptionGroup {
            ReaderOptionSwitchRow(
                label = stringResource(R.string.novel_reader_translation_enabled),
                checked = settings.isTranslationEnabled,
                onCheckedChange = { onToggleTranslation() },
            )
            ReaderOptionDivider()
            ReaderSegmentedChoice(
                title = stringResource(R.string.novel_translation_display_mode),
                options = listOf(
                    stringResource(R.string.novel_translation_only),
                    stringResource(R.string.novel_translation_bilingual),
                ),
                selectedIndex = if (settings.translationDisplayMode == NovelTranslationDisplayMode.TRANSLATION_ONLY) {
                    0
                } else {
                    1
                },
                onSelected = { index ->
                    update {
                        copy(
                            translationDisplayMode = if (index == 0) {
                                NovelTranslationDisplayMode.TRANSLATION_ONLY
                            } else {
                                NovelTranslationDisplayMode.BILINGUAL
                            },
                        )
                    }
                },
                stackedTitle = true,
                // Keep both display cards clear of the parent MD3 group's rounded clip.
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
    }
    item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
                onClick = onToggleTranslation,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(painterResource(R.drawable.ic_translate), contentDescription = null)
                Text(stringResource(R.string.novel_reader_translation_start), modifier = Modifier.padding(start = 8.dp))
            }
            FilledTonalButton(
                onClick = onClearTranslationCache,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = null)
                Text(stringResource(R.string.clear_translation_cache), modifier = Modifier.padding(start = 8.dp))
            }
            Text(
                text = stringResource(R.string.novel_reader_translation_cache_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun NovelReaderToolsPage(
    onShowMarkings: () -> Unit,
    onBookmark: () -> Unit,
    onTts: () -> Unit,
    replaceRulesEnabled: Boolean,
    onToggleReplaceRules: () -> Unit,
    onShowReplaceRules: () -> Unit,
    onReset: () -> Unit,
) = NovelOptionsPageList {
    item {
        Text(
            text = stringResource(R.string.novel_reader_tab_tools),
            style = MaterialTheme.typography.titleMedium,
        )
    }
    item {
        ReaderOptionGroup {
            NovelToolActionRow(
                icon = R.drawable.ic_voice_input,
                title = stringResource(R.string.tts_settings_title),
                supporting = stringResource(R.string.novel_reader_tts_summary),
                onClick = onTts,
            )
            ReaderOptionDivider()
            NovelToolActionRow(
                icon = R.drawable.ic_replace,
                title = stringResource(R.string.replace_rule_effective_title),
                supporting = stringResource(R.string.novel_reader_replace_summary),
                onClick = onShowReplaceRules,
            )
            ReaderOptionDivider()
            ReaderOptionSwitchRow(
                label = stringResource(R.string.replace_rule_book_toggle),
                checked = replaceRulesEnabled,
                onCheckedChange = { onToggleReplaceRules() },
            )
            ReaderOptionDivider()
            NovelToolActionRow(
                icon = R.drawable.ic_bookmark,
                title = stringResource(R.string.novel_reader_bookmarks_notes),
                supporting = stringResource(R.string.novel_reader_bookmarks_notes_summary),
                onClick = onShowMarkings,
            )
            ReaderOptionDivider()
            NovelToolActionRow(
                icon = R.drawable.ic_bookmark_added,
                title = stringResource(R.string.novel_reader_bookmark_current),
                supporting = stringResource(R.string.bookmark_add),
                onClick = onBookmark,
            )
        }
    }
    item {
        ReaderOptionGroup {
            NovelToolActionRow(
                icon = R.drawable.ic_backup_restore,
                title = stringResource(R.string.novel_reset),
                supporting = stringResource(R.string.novel_reader_reset_confirm),
                onClick = onReset,
            )
        }
    }
}

@Composable
private fun NovelReaderSliderRow(
    label: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    icon: Int? = null,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            icon?.let {
                Icon(
                    painter = painterResource(it),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp).size(20.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = value.coerceIn(valueRange),
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun NovelToolActionRow(
    icon: Int,
    title: String,
    supporting: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f),
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_arrow_forward),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.novelReadingOptionsContent(
    settings: NovelReaderSettings,
    update: (NovelReaderSettings.() -> NovelReaderSettings) -> Unit,
) {
    item {
        ReaderSegmentedChoice(
            title = stringResource(R.string.novel_reading_mode),
            options = listOf(stringResource(R.string.novel_mode_paged), stringResource(R.string.novel_mode_scroll)),
            selectedIndex = if (settings.readingMode == ReadingMode.PAGED) 0 else 1,
            onSelected = { update { copy(readingMode = if (it == 0) ReadingMode.PAGED else ReadingMode.SCROLL) } },
            icon = { NovelReadingModeIcon(it) },
            stackedTitle = true,
            verticalOptions = true,
        )
    }
    if (settings.readingMode == ReadingMode.PAGED) {
        item {
            ReaderSegmentedChoice(
                title = stringResource(R.string.novel_page_turn_animation),
                options = NovelPageTurnAnimation.entries.map { stringResource(it.label) },
                selectedIndex = NovelPageTurnAnimation.entries.indexOf(settings.pageTurnAnimation),
                onSelected = { update { copy(pageTurnAnimation = NovelPageTurnAnimation.entries[it]) } },
                icon = { NovelPageAnimationIcon(NovelPageTurnAnimation.entries[it]) },
                stackedTitle = true,
                verticalOptions = true,
            )
        }
    }
    item {
        ReaderOptionGroup {
            NovelSwitchRows(settings, update, includeParagraphIndent = false)
        }
    }
}

@Composable
private fun NovelOptionsPageList(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        modifier = Modifier.fillMaxSize(),
        content = content,
    )
}

@Composable
internal fun ComposeNovelReaderOptionsPanel(
    settings: NovelReaderSettings,
    onSettingsChanged: (NovelReaderSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sliderEditor by remember { mutableStateOf<SliderEditor?>(null) }
    fun update(transform: NovelReaderSettings.() -> NovelReaderSettings) {
        onSettingsChanged(settings.transform().normalized())
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        item {
            ReaderSegmentedChoice(
                title = stringResource(R.string.novel_reading_mode),
                options = listOf(stringResource(R.string.novel_mode_paged), stringResource(R.string.novel_mode_scroll)),
                selectedIndex = if (settings.readingMode == ReadingMode.PAGED) 0 else 1,
                onSelected = { update { copy(readingMode = if (it == 0) ReadingMode.PAGED else ReadingMode.SCROLL) } },
                iconOnly = true,
                icon = { NovelReadingModeIcon(it) },
            )
        }
        item {
            ReaderSegmentedChoice(
                title = stringResource(R.string.novel_theme_preset),
                options = NovelReaderThemePreset.entries.map { stringResource(it.label) },
                selectedIndex = NovelReaderThemePreset.entries.indexOf(settings.themePreset),
                onSelected = { update { copy(themePreset = NovelReaderThemePreset.entries[it]) } },
                iconOnly = true,
                icon = { NovelThemeSwatch(NovelReaderThemePreset.entries[it]) },
            )
        }
        item {
            ReaderOptionGroup {
                NovelTypographyRows(settings, ::update) { sliderEditor = it }
            }
        }
        item {
            ReaderOptionGroup {
                NovelSwitchRows(settings, ::update, includeTransparentStatusBar = false)
            }
        }
        if (settings.readingMode == ReadingMode.PAGED) {
            item {
                ReaderSegmentedChoice(
                    title = stringResource(R.string.novel_page_turn_animation),
                    options = NovelPageTurnAnimation.entries.map { stringResource(it.label) },
                    selectedIndex = NovelPageTurnAnimation.entries.indexOf(settings.pageTurnAnimation),
                    onSelected = { update { copy(pageTurnAnimation = NovelPageTurnAnimation.entries[it]) } },
                    iconOnly = true,
                    icon = { NovelPageAnimationIcon(NovelPageTurnAnimation.entries[it]) },
                )
            }
        }
        item {
            Action(R.drawable.ic_backup_restore, R.string.novel_reset, onClick = {
                onSettingsChanged(NovelReaderSettings())
            })
        }
    }
    SliderEditorDialog(sliderEditor) { sliderEditor = null }
}

@Composable
private fun NovelSwitchRows(
    settings: NovelReaderSettings,
    update: (NovelReaderSettings.() -> NovelReaderSettings) -> Unit,
    includeTransparentStatusBar: Boolean = true,
    includeParagraphIndent: Boolean = true,
) {
    ReaderOptionSwitchRow(
        label = stringResource(R.string.novel_dual_page_mode),
        checked = settings.enableDualPage,
        onCheckedChange = { update { copy(enableDualPage = it) } },
    )
    ReaderOptionDivider()
    ReaderOptionSwitchRow(
        label = stringResource(R.string.novel_fullscreen_mode),
        checked = settings.enableFullscreen,
        onCheckedChange = { update { copy(enableFullscreen = it) } },
    )
    ReaderOptionDivider()
    ReaderOptionSwitchRow(
        label = stringResource(R.string.novel_show_reading_status),
        checked = settings.showReadingStatus,
        onCheckedChange = { update { copy(showReadingStatus = it) } },
    )
    ReaderOptionDivider()
    ReaderOptionSwitchRow(
        label = stringResource(R.string.reader_chapter_title_at_bottom),
        checked = settings.chapterTitleAtBottom,
        onCheckedChange = { update { copy(chapterTitleAtBottom = it) } },
    )
    if (includeTransparentStatusBar) {
        ReaderOptionDivider()
        ReaderOptionSwitchRow(
            label = stringResource(R.string.novel_transparent_status_bar),
            checked = settings.isReadingStatusTransparent,
            onCheckedChange = { update { copy(isReadingStatusTransparent = it) } },
        )
    }
    if (includeParagraphIndent) {
        ReaderOptionDivider()
        ReaderOptionSwitchRow(
            label = stringResource(R.string.novel_first_line_indent),
            checked = settings.enableParagraphIndent,
            onCheckedChange = { update { copy(enableParagraphIndent = it) } },
        )
    }
}

@Composable
private fun NovelTypographyRows(
    settings: NovelReaderSettings,
    update: (NovelReaderSettings.() -> NovelReaderSettings) -> Unit,
    onEditSlider: (SliderEditor) -> Unit,
) {
    ReaderOptionValueRow(
        label = stringResource(R.string.novel_font_size),
        value = "%.1fsp".format(settings.fontSizeSp),
        onClick = {
            onEditSlider(
                SliderEditor(R.string.novel_font_size, settings.fontSizeSp, NovelReaderSettings.FONT_SIZE_RANGE) {
                    update { copy(fontSizeSp = it) }
                },
            )
        },
    )
    ReaderOptionDivider()
    ReaderOptionValueRow(
        label = stringResource(R.string.novel_line_spacing),
        value = "%.1f".format(settings.lineSpacing),
        onClick = {
            onEditSlider(
                SliderEditor(R.string.novel_line_spacing, settings.lineSpacing, NovelReaderSettings.LINE_SPACING_RANGE) {
                    update { copy(lineSpacing = it) }
                },
            )
        },
    )
    ReaderOptionDivider()
    ReaderOptionValueRow(
        label = stringResource(R.string.novel_paragraph_spacing),
        value = stringResource(R.string.novel_paragraph_spacing_value, settings.paragraphSpacingLines),
        onClick = {
            onEditSlider(
                SliderEditor(
                    R.string.novel_paragraph_spacing,
                    settings.paragraphSpacing,
                    NovelReaderSettings.PARAGRAPH_SPACING_RANGE,
                    steps = 2,
                ) { update { copy(paragraphSpacing = it) } },
            )
        },
    )
    ReaderOptionDivider()
    ReaderOptionValueRow(
        label = stringResource(R.string.novel_margin_horizontal),
        value = "${settings.marginHorizontal}dp",
        onClick = {
            onEditSlider(
                SliderEditor(
                    R.string.novel_margin_horizontal,
                    settings.marginHorizontal.toFloat(),
                    NovelReaderSettings.MARGIN_RANGE.asFloatRange(),
                ) { update { copy(marginHorizontal = it.toInt()) } },
            )
        },
    )
    ReaderOptionDivider()
    ReaderOptionValueRow(
        label = stringResource(R.string.novel_margin_vertical),
        value = "${settings.marginVertical}dp",
        onClick = {
            onEditSlider(
                SliderEditor(
                    R.string.novel_margin_vertical,
                    settings.marginVertical.toFloat(),
                    NovelReaderSettings.MARGIN_RANGE.asFloatRange(),
                ) { update { copy(marginVertical = it.toInt()) } },
            )
        },
    )
}

@Composable
private fun NovelReadingModeIcon(index: Int) {
    Icon(
        painter = painterResource(if (index == 0) R.drawable.ic_book_page else R.drawable.ic_gesture_vertical),
        contentDescription = null,
        modifier = Modifier.size(20.dp),
    )
}

@Composable
private fun NovelPageAnimationIcon(animation: NovelPageTurnAnimation) {
    ReaderAnimationIcon(
        if (animation == NovelPageTurnAnimation.SLIDE) ReaderAnimation.DEFAULT else ReaderAnimation.SIMULATION,
    )
}

@Composable
private fun NovelThemeSwatch(preset: NovelReaderThemePreset) {
    val palette = novelReaderPalette(preset, isSystemInDarkTheme())
    Box(
        modifier = Modifier
            .size(20.dp)
            .background(Color(palette.backgroundColor), RoundedCornerShape(5.dp))
            .border(1.dp, Color(palette.secondaryTextColor).copy(alpha = 0.7f), RoundedCornerShape(5.dp)),
    )
}

@Composable
private fun Action(
    icon: Int,
    label: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(onClick = onClick, modifier = modifier) {
        Icon(painterResource(icon), contentDescription = null)
        Text(stringResource(label), modifier = Modifier.padding(start = 8.dp))
    }
}

private fun IntRange.asFloatRange() = first.toFloat()..last.toFloat()
private data class SliderEditor(
    val title: Int,
    val value: Float,
    val range: ClosedFloatingPointRange<Float>,
    val steps: Int = 0,
    val onChange: (Float) -> Unit,
)

@Composable private fun SliderEditorDialog(editor: SliderEditor?, onDismiss: () -> Unit) {
    if (editor == null) return
    var value by remember(editor) { mutableStateOf(editor.value) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(editor.title)) },
        text = {
            Slider(
                value = value,
                onValueChange = { value = it; editor.onChange(it) },
                valueRange = editor.range,
                steps = editor.steps,
            )
        },
        confirmButton = { FilledTonalButton(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) } },
    )
}
private val NovelReaderThemePreset.label: Int get() = when (this) {
    NovelReaderThemePreset.PAPER -> R.string.novel_theme_paper
    NovelReaderThemePreset.SEPIA -> R.string.novel_theme_sepia
    NovelReaderThemePreset.MOSS -> R.string.novel_theme_moss
    NovelReaderThemePreset.SLATE -> R.string.novel_theme_slate
}
private val NovelPageTurnAnimation.label: Int get() = when (this) {
    NovelPageTurnAnimation.SLIDE -> R.string.novel_page_turn_slide
    NovelPageTurnAnimation.SIMULATION -> R.string.novel_page_turn_simulation
}
