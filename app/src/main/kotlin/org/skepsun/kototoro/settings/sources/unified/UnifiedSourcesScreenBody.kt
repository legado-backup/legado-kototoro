package org.skepsun.kototoro.settings.sources.unified


import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Badge
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.foundation.shape.RoundedCornerShape
import org.skepsun.kototoro.R
import org.skepsun.kototoro.core.model.titleResId
import org.skepsun.kototoro.core.ui.compose.KototoroPullToRefreshBox
import org.skepsun.kototoro.settings.compose.settingsContentTopInset
import org.skepsun.kototoro.parsers.model.ContentType
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedSourcesScreen(
    state: UnifiedSourcesUiState,
    isLoading: Boolean,
    updateAllInProgress: Boolean,
    searchActive: Boolean,
    onLanguageFilterClick: () -> Unit,
    onMoreFiltersClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSearchClose: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onKindClick: (UnifiedSourceKind?) -> Unit,
    onContentTypeClick: (ContentType?) -> Unit,
    onRepositoryFilterClick: (String?) -> Unit,
    onPackageStatusClick: (UnifiedPackageStatusFilter) -> Unit = {},
    onSourceEnabledChange: (String, Boolean) -> Unit,
    onEnableAllSources: () -> Unit,
    onDisableAllSources: () -> Unit,
    selectedSourceIds: Set<String>,
    onSourceSelectionChange: (Set<String>) -> Unit,
    onSelectAllVisibleSources: () -> Unit,
    onClearSourceSelection: () -> Unit,
    onEnableSelectedSources: () -> Unit,
    onDisableSelectedSources: () -> Unit,
    onTestSelectedSources: () -> Unit,
    onDeleteSelectedSources: () -> Unit,
    onToggleSelectedSourcesNsfw: () -> Unit,
    onSourcePinnedChange: (String, Boolean) -> Unit,
    onBrowseSource: (UnifiedSourceItem) -> Unit,
    onOpenSourceSettings: (UnifiedSourceItem) -> Unit,
    onAddRepository: (UnifiedSourceRepositoryItem?) -> Unit,
    onRefreshRepository: (UnifiedSourceRepositoryItem) -> Unit,
    onDeleteRepository: (UnifiedSourceRepositoryItem) -> Unit,
    onUpdateAllPackages: () -> Unit,
    onPackagePrimaryAction: (String) -> Unit,
    onPackageSystemInstall: (String) -> Unit,
    onPackageUninstall: (String) -> Unit,
    onPackageCancelInstall: (String) -> Unit,
    onImportLocalJar: () -> Unit,
    onAddRecommendedRepository: (UnifiedRecommendedRepository) -> Unit,
    onPullRefresh: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val readyState = state as? UnifiedSourcesUiState.Ready
    val pagerState = rememberPagerState(pageCount = { UNIFIED_SOURCES_TAB_COUNT })
    val coroutineScope = rememberCoroutineScope()
    val sourceListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val repositoryListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val packageListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val selectedTab = pagerState.currentPage.coerceIn(0, UNIFIED_SOURCES_TAB_COUNT - 1)
    val onTabClick: (Int, LazyListState) -> Unit = { tab, listState ->
        if (tab != UNIFIED_SOURCES_TAB_SOURCES) {
            onClearSourceSelection()
        }
        coroutineScope.launch {
            if (selectedTab == tab) {
                listState.animateScrollToItem(0)
            } else {
                pagerState.animateScrollToPage(tab)
            }
        }
    }
    val activeSelectedSourceIds = remember(readyState?.sources, selectedSourceIds) {
        val visibleSourceIds = readyState?.sources.orEmpty().mapTo(LinkedHashSet()) { it.id }
        selectedSourceIds intersect visibleSourceIds
    }
    val selectedNsfwCount = remember(readyState?.sources, activeSelectedSourceIds) {
        readyState?.sources.orEmpty().count { it.id in activeSelectedSourceIds && it.isNsfw }
    }
    val packagesById = remember(readyState?.allPackages) {
        readyState?.allPackages.orEmpty().associateBy { it.id }
    }
    val repositoryOptions = remember(
        selectedTab,
        readyState?.allRepositories,
        readyState?.allPackages,
        readyState?.allSources,
        readyState?.filters?.repositoryId,
    ) {
        val allRepositories = readyState?.allRepositories.orEmpty()
        val referencedRepositoryIds = when (selectedTab) {
            UNIFIED_SOURCES_TAB_SOURCES -> readyState?.allSources
                .orEmpty()
                .mapNotNullTo(LinkedHashSet()) { it.effectiveRepositoryId(packagesById) }
            UNIFIED_SOURCES_TAB_PACKAGES -> readyState?.allPackages
                .orEmpty()
                .mapNotNullTo(LinkedHashSet()) { it.repositoryId }
            else -> emptySet()
        }
        allRepositories
            .filter { it.id in referencedRepositoryIds || it.id == readyState?.filters?.repositoryId }
            .sortedWith(compareBy({ it.name.lowercase() }, { it.id }))
    }
    LaunchedEffect(activeSelectedSourceIds) {
        if (selectedSourceIds != activeSelectedSourceIds) {
            onSourceSelectionChange(activeSelectedSourceIds)
        }
    }
    LaunchedEffect(selectedTab) {
        if (selectedTab != UNIFIED_SOURCES_TAB_SOURCES && selectedSourceIds.isNotEmpty()) {
            onClearSourceSelection()
        }
    }
    BackHandler(
        enabled = selectedTab == UNIFIED_SOURCES_TAB_SOURCES && selectedSourceIds.isNotEmpty(),
    ) {
        onClearSourceSelection()
    }
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                Spacer(modifier = Modifier.height(settingsContentTopInset()))
                if (isLoading || state == UnifiedSourcesUiState.Loading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                if (readyState != null) {
                    SecondaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        indicator = {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorLayout { measurable, constraints, tabPositions ->
                                    if (tabPositions.isEmpty()) {
                                        val placeable = measurable.measure(constraints.copy(minWidth = 0))
                                        return@tabIndicatorLayout layout(constraints.maxWidth, placeable.height) {}
                                    }
                                    val currentPage = pagerState.currentPage.coerceIn(tabPositions.indices)
                                    val offset = pagerState.currentPageOffsetFraction
                                    val targetPage = (currentPage + sign(offset).toInt()).coerceIn(tabPositions.indices)
                                    val fraction = abs(offset)
                                    val left = lerp(tabPositions[currentPage].left, tabPositions[targetPage].left, fraction)
                                    val right = lerp(tabPositions[currentPage].right, tabPositions[targetPage].right, fraction)
                                    val width = (right - left).roundToPx()
                                    val placeable = measurable.measure(
                                        constraints.copy(minWidth = width, maxWidth = width),
                                    )
                                    layout(constraints.maxWidth, placeable.height) {
                                        placeable.placeRelative(left.roundToPx(), 0)
                                    }
                                },
                            )
                        },
                    ) {
                        Tab(
                            selected = selectedTab == UNIFIED_SOURCES_TAB_SOURCES,
                            onClick = { onTabClick(UNIFIED_SOURCES_TAB_SOURCES, sourceListState) },
                            text = { Text(stringResource(R.string.sources_tab_title, readyState.sources.size)) },
                        )
                        Tab(
                            selected = selectedTab == UNIFIED_SOURCES_TAB_REPOSITORIES,
                            onClick = { onTabClick(UNIFIED_SOURCES_TAB_REPOSITORIES, repositoryListState) },
                            text = { Text(stringResource(R.string.repositories_tab_title, readyState.repositories.size)) },
                        )
                        Tab(
                            selected = selectedTab == UNIFIED_SOURCES_TAB_PACKAGES,
                            onClick = { onTabClick(UNIFIED_SOURCES_TAB_PACKAGES, packageListState) },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(stringResource(R.string.packages_tab_title, readyState.packages.size))
                                    if (readyState.packageUpdateCount > 0) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                        ) {
                                            Text(readyState.packageUpdateCount.toString())
                                        }
                                    }
                                }
                            },
                        )
                    }
                    if (selectedTab == UNIFIED_SOURCES_TAB_REPOSITORIES) {
                        UnifiedRepositoriesKindFilterRow(
                            state = readyState,
                            onKindClick = onKindClick,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        when (state) {
            UnifiedSourcesUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }
            is UnifiedSourcesUiState.Ready -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    AnimatedVisibility(
                        visible = selectedTab == UNIFIED_SOURCES_TAB_SOURCES && activeSelectedSourceIds.isNotEmpty(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        UnifiedSourceSelectionBar(
                            selectedCount = activeSelectedSourceIds.size,
                            allVisibleSelected = activeSelectedSourceIds.size == state.sources.size,
                            selectedNsfwCount = selectedNsfwCount,
                            selectedSfwCount = activeSelectedSourceIds.size - selectedNsfwCount,
                            onSelectAllVisibleSources = onSelectAllVisibleSources,
                            onClearSelection = onClearSourceSelection,
                            onEnableSelectedSources = onEnableSelectedSources,
                            onDisableSelectedSources = onDisableSelectedSources,
                            onToggleSelectedSourcesNsfw = onToggleSelectedSourcesNsfw,
                            onTestSelectedSources = onTestSelectedSources,
                            onDeleteSelectedSources = onDeleteSelectedSources,
                        )
                    }
                    KototoroPullToRefreshBox(
                        isRefreshing = isLoading,
                        onRefresh = { onPullRefresh(selectedTab) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                        ) { page ->
                            when (page) {
                                UNIFIED_SOURCES_TAB_SOURCES -> UnifiedSourceList(
                                    modifier = Modifier.fillMaxSize(),
                                    listState = sourceListState,
                                    sources = state.sources,
                                    repositories = repositoryOptions,
                                    selectedRepositoryId = state.filters.repositoryId,
                                    onRepositoryFilterClick = onRepositoryFilterClick,
                                    availableContentTypes = state.availableContentTypes,
                                    selectedContentTypes = state.filters.contentTypes,
                                    onContentTypeClick = onContentTypeClick,
                                    availableKinds = state.availableKinds,
                                    selectedKinds = state.filters.kinds,
                                    onKindClick = onKindClick,
                                    onBrowseSource = onBrowseSource,
                                    onOpenSourceSettings = onOpenSourceSettings,
                                    onSourceEnabledChange = onSourceEnabledChange,
                                    onEnableAllSources = onEnableAllSources,
                                    onDisableAllSources = onDisableAllSources,
                                    selectedSourceIds = activeSelectedSourceIds,
                                    onSourceSelectionChange = onSourceSelectionChange,
                                    onSourcePinnedChange = onSourcePinnedChange,
                                )
                                UNIFIED_SOURCES_TAB_REPOSITORIES -> UnifiedRepositoryList(
                                    modifier = Modifier.fillMaxSize(),
                                    listState = repositoryListState,
                                    repositories = state.repositories,
                                    onAddRepository = onAddRepository,
                                    onRefreshRepository = onRefreshRepository,
                                    onDeleteRepository = onDeleteRepository,
                                )
                                UNIFIED_SOURCES_TAB_PACKAGES -> UnifiedPackageList(
                                    modifier = Modifier.fillMaxSize(),
                                    listState = packageListState,
                                    packages = state.packages,
                                    repositories = repositoryOptions,
                                    selectedRepositoryId = state.filters.repositoryId,
                                    onRepositoryFilterClick = onRepositoryFilterClick,
                                    packageStatusFilter = state.filters.packageStatusFilter,
                                    packageUpdateCount = state.packageUpdateCount,
                                    onPackageStatusClick = onPackageStatusClick,
                                    availableKinds = state.availableKinds,
                                    selectedKinds = state.filters.kinds,
                                    onKindClick = onKindClick,
                                    recommendedPackages = state.recommendedPackages,
                                    missingSourcesWithoutMatch = state.missingSourcesWithoutMatch,
                                    suggestedRepositoriesForMissing = state.suggestedRepositoriesForMissing,
                                    updateAllInProgress = updateAllInProgress,
                                    onUpdateAllPackages = onUpdateAllPackages,
                                    onPackagePrimaryAction = onPackagePrimaryAction,
                                    onPackageSystemInstall = onPackageSystemInstall,
                                    onPackageUninstall = onPackageUninstall,
                                    onPackageCancelInstall = onPackageCancelInstall,
                                    onImportLocalJar = onImportLocalJar,
                                    onAddRecommendedRepository = onAddRecommendedRepository,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnifiedRepositoriesKindFilterRow(
    state: UnifiedSourcesUiState.Ready,
    onKindClick: (UnifiedSourceKind?) -> Unit,
) {
    LazyRow(
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 1.dp),
    ) {
        item(key = "repo_kind_all") {
            CompactFilterChip(
                selected = state.filters.kinds.isEmpty(),
                onClick = { onKindClick(null) },
                text = stringResource(R.string.all_sources),
            )
        }
        items(state.availableKinds, key = { it.name }) { kind ->
            CompactFilterChip(
                selected = kind in state.filters.kinds,
                onClick = { onKindClick(kind) },
                text = kind.displayLabel(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> UnifiedFilterDropdown(
    selected: Boolean,
    currentLabel: String,
    options: List<UnifiedFilterOption<T>>,
    isOptionSelected: (T) -> Boolean,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val style = rememberUnifiedSourcesVisualStyle()
    Box(modifier = modifier) {
        FilterChip(
            selected = selected,
            onClick = { expanded = true },
            shape = style.chipShape,
            modifier = Modifier.defaultMinSize(minHeight = unifiedActionButtonHeight),
            label = {
                Text(
                    text = currentLabel,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            trailingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_expand_more),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    onClick = {
                        onOptionSelected(option.value)
                        expanded = false
                    },
                    leadingIcon = {
                        if (isOptionSelected(option.value)) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                )
            }
        }
    }
}

internal data class UnifiedFilterOption<T>(
    val value: T,
    val label: String,
)

@Composable
internal fun UnifiedRepositoryFilterDropdown(
    repositories: List<UnifiedSourceRepositoryItem>,
    selectedRepositoryId: String?,
    onRepositorySelected: (String?) -> Unit,
) {
    val selectedRepository = repositories.firstOrNull { it.id == selectedRepositoryId }
    val options = buildList {
        add(UnifiedFilterOption<String?>(null, stringResource(R.string.all)))
        repositories.forEach { repository ->
            add(UnifiedFilterOption<String?>(repository.id, repository.name))
        }
    }
    UnifiedFilterDropdown(
        selected = selectedRepository != null,
        currentLabel = selectedRepository?.name ?: stringResource(R.string.repository_source),
        options = options,
        isOptionSelected = { it == selectedRepositoryId },
        onOptionSelected = onRepositorySelected,
    )
}

@Composable
internal fun UnifiedContentTypeFilterDropdown(
    availableContentTypes: List<ContentType>,
    selectedContentTypes: Set<ContentType>,
    onContentTypeSelected: (ContentType?) -> Unit,
) {
    val allLabel = stringResource(R.string.all_content)
    val selectedContentType = selectedContentTypes.firstOrNull()
    val options = buildList {
        add(UnifiedFilterOption<ContentType?>(null, allLabel))
        availableContentTypes.forEach { type ->
            add(UnifiedFilterOption<ContentType?>(type, stringResource(type.titleResId)))
        }
    }
    UnifiedFilterDropdown(
        selected = selectedContentType != null,
        currentLabel = selectedContentType?.let { stringResource(it.titleResId) } ?: allLabel,
        options = options,
        isOptionSelected = { it == selectedContentType },
        onOptionSelected = onContentTypeSelected,
    )
}

@Composable
internal fun UnifiedKindFilterDropdown(
    availableKinds: List<UnifiedSourceKind>,
    selectedKinds: Set<UnifiedSourceKind>,
    onKindSelected: (UnifiedSourceKind?) -> Unit,
) {
    val allLabel = stringResource(R.string.all_sources)
    val selectedKind = selectedKinds.firstOrNull()
    val options = buildList {
        add(UnifiedFilterOption<UnifiedSourceKind?>(null, allLabel))
        availableKinds.forEach { kind ->
            add(UnifiedFilterOption<UnifiedSourceKind?>(kind, kind.displayLabel()))
        }
    }
    UnifiedFilterDropdown(
        selected = selectedKind != null,
        currentLabel = selectedKind?.displayLabel() ?: allLabel,
        options = options,
        isOptionSelected = { it == selectedKind },
        onOptionSelected = onKindSelected,
    )
}

@Composable
internal fun UnifiedPackageStatusFilterDropdown(
    selectedStatus: UnifiedPackageStatusFilter,
    updateAvailableCount: Int,
    onPackageStatusSelected: (UnifiedPackageStatusFilter) -> Unit,
) {
    val updateLabel = if (updateAvailableCount > 0) {
        "${stringResource(R.string.package_filter_updates)} ($updateAvailableCount)"
    } else {
        stringResource(R.string.package_filter_updates)
    }
    val options = buildList {
        add(UnifiedFilterOption(UnifiedPackageStatusFilter.ALL, stringResource(R.string.all)))
        add(UnifiedFilterOption(UnifiedPackageStatusFilter.UPDATE_AVAILABLE, updateLabel))
        add(UnifiedFilterOption(UnifiedPackageStatusFilter.INSTALLED, stringResource(R.string.package_filter_installed)))
        add(UnifiedFilterOption(UnifiedPackageStatusFilter.NOT_INSTALLED, stringResource(R.string.package_filter_available)))
    }
    val currentLabel = when (selectedStatus) {
        UnifiedPackageStatusFilter.ALL -> stringResource(R.string.all)
        UnifiedPackageStatusFilter.UPDATE_AVAILABLE -> updateLabel
        UnifiedPackageStatusFilter.INSTALLED -> stringResource(R.string.package_filter_installed)
        UnifiedPackageStatusFilter.NOT_INSTALLED -> stringResource(R.string.package_filter_available)
    }
    UnifiedFilterDropdown(
        selected = selectedStatus != UnifiedPackageStatusFilter.ALL,
        currentLabel = currentLabel,
        options = options,
        isOptionSelected = { it == selectedStatus },
        onOptionSelected = onPackageStatusSelected,
    )
}

@Composable
internal fun FilterSection(
    title: String,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 1.dp),
            content = content,
        )
    }
}

@Composable
internal fun CompactFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
) {
    val style = rememberUnifiedSourcesVisualStyle()
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = unifiedActionButtonHeight),
        shape = style.chipShape,
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

@Composable
private fun UnifiedSourceSelectionBar(
    selectedCount: Int,
    allVisibleSelected: Boolean,
    selectedNsfwCount: Int,
    selectedSfwCount: Int,
    onSelectAllVisibleSources: () -> Unit,
    onClearSelection: () -> Unit,
    onEnableSelectedSources: () -> Unit,
    onDisableSelectedSources: () -> Unit,
    onToggleSelectedSourcesNsfw: () -> Unit,
    onTestSelectedSources: () -> Unit,
    onDeleteSelectedSources: () -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item(key = "selected_count") {
            Text(
                text = stringResource(R.string.selected_count, selectedCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item(key = "select_all") {
            CompactActionChip(
                onClick = if (allVisibleSelected) onClearSelection else onSelectAllVisibleSources,
                label = {
                    Text(stringResource(if (allVisibleSelected) R.string.deselect_all else R.string.select_all))
                },
            )
        }
        item(key = "enable_selected") {
            CompactActionChip(
                onClick = onEnableSelectedSources,
                label = { Text(stringResource(R.string.enable)) },
            )
        }
        item(key = "disable_selected") {
            CompactActionChip(
                onClick = onDisableSelectedSources,
                label = { Text(stringResource(R.string.disable)) },
            )
        }
        item(key = "toggle_nsfw_selected") {
            val mixedSelection = selectedNsfwCount > 0 && selectedSfwCount > 0
            val label = when {
                mixedSelection -> stringResource(R.string.unified_sources_set_nsfw)
                selectedNsfwCount > 0 -> stringResource(R.string.unified_sources_unmark_nsfw)
                else -> stringResource(R.string.unified_sources_mark_nsfw)
            }
            CompactActionChip(
                onClick = onToggleSelectedSourcesNsfw,
                label = { Text(label) },
            )
        }
        item(key = "test_selected") {
            CompactActionChip(
                onClick = onTestSelectedSources,
                label = { Text(stringResource(R.string.source_test_action)) },
            )
        }
        item(key = "delete_selected") {
            CompactActionChip(
                onClick = onDeleteSelectedSources,
                label = { Text(stringResource(R.string.delete)) },
            )
        }
        item(key = "clear_selection") {
            IconButton(
                onClick = onClearSelection,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(android.R.string.cancel),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
