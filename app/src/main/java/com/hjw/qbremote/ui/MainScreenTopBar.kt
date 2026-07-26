package com.hjw.qbremote.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hjw.qbremote.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainScreenTopBar(
    currentPage: AppPage,
    connected: Boolean,
    isManualRefreshing: Boolean,
    sortOption: TorrentListSortOption,
    sortDescending: Boolean,
    showSearchBar: Boolean,
    showSortMenu: Boolean,
    onSortMenuVisibilityChange: (Boolean) -> Unit,
    onOpenDrawer: () -> Unit,
    onBack: () -> Unit,
    onScrollToTop: () -> Unit,
    onOpenServerSheet: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCardManager: () -> Unit,
    onRefreshOrConnect: () -> Unit,
    onSortOptionSelected: (TorrentListSortOption) -> Unit,
    onSortDirectionSelected: (Boolean) -> Unit,
    onToggleSearchBar: () -> Unit,
    onOpenAddTorrent: () -> Unit,
) {
    val openDrawerDescription = stringResource(R.string.menu_open_drawer)
    val backDescription = stringResource(R.string.back)
    val manageServersDescription = stringResource(R.string.menu_manage_servers)
    val sortDescription = stringResource(R.string.menu_sort)
    val searchDescription = stringResource(R.string.menu_search)
    val addTorrentDescription = stringResource(R.string.menu_add_torrent)
    val compactTorrentTopBar = currentPage == AppPage.TORRENT_LIST
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
        navigationIcon = {
            TextButton(
                modifier = Modifier.semantics {
                    contentDescription = if (currentPage == AppPage.DASHBOARD) {
                        openDrawerDescription
                    } else {
                        backDescription
                    }
                },
                onClick = {
                    if (currentPage == AppPage.DASHBOARD) {
                        onOpenDrawer()
                    } else {
                        onBack()
                    }
                },
                contentPadding = if (compactTorrentTopBar) {
                    PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                } else {
                    ButtonDefaults.TextButtonContentPadding
                },
            ) {
                Text(
                    text = if (currentPage == AppPage.DASHBOARD) "≡" else "←",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        },
        title = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (compactTorrentTopBar) {
                            Modifier.offset(x = (-4).dp)
                        } else {
                            Modifier
                        }
                    )
                    .pointerInput(currentPage) {
                        detectTapGestures(
                            onDoubleTap = {
                                onScrollToTop()
                            },
                        )
                    },
            ) {
                TopBrandTitle(
                    modifier = Modifier.fillMaxWidth(),
                    compact = compactTorrentTopBar,
                )
            }
        },
        actions = {
            when (currentPage) {
                AppPage.DASHBOARD -> {
                    Row(
                        modifier = Modifier.padding(end = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            modifier = Modifier.semantics {
                                contentDescription = manageServersDescription
                            },
                            onClick = onOpenServerSheet,
                        ) {
                            Text(
                                text = stringResource(R.string.menu_servers),
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        TextButton(onClick = onOpenSettings) {
                            Text(
                                text = stringResource(R.string.menu_settings),
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }
                }

                AppPage.SERVER_DASHBOARD -> {
                    TextButton(
                        onClick = onOpenCardManager,
                    ) {
                        Text(
                            text = stringResource(R.string.dashboard_manage_cards_action),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }

                AppPage.SETTINGS -> {
                    TextButton(
                        onClick = onRefreshOrConnect,
                    ) {
                        Text(
                            text = if (connected) {
                                if (isManualRefreshing) {
                                    stringResource(R.string.refreshing)
                                } else {
                                    stringResource(R.string.refresh)
                                }
                            } else {
                                stringResource(R.string.connect)
                            },
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }

                AppPage.TORRENT_DETAIL -> {
                    TextButton(
                        onClick = onRefreshOrConnect,
                    ) {
                        Text(
                            text = stringResource(R.string.refresh),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }

                AppPage.TORRENT_LIST -> {
                    Row(
                        modifier = Modifier
                            .padding(end = 2.dp)
                            .offset(x = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box {
                            TextButton(
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 1.dp, minHeight = 36.dp)
                                    .semantics {
                                        contentDescription = sortDescription
                                    },
                                onClick = { onSortMenuVisibilityChange(true) },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.menu_sort),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { onSortMenuVisibilityChange(false) },
                            ) {
                                TorrentListSortOption.entries.forEach { option ->
                                    val isSelected = option == sortOption
                                    DropdownMenuItem(
                                        text = {
                                            val prefix = if (isSelected) "✓ " else ""
                                            Text("$prefix${torrentListSortLabel(option)}")
                                        },
                                        onClick = {
                                            onSortOptionSelected(option)
                                        },
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                        text = {
                                            val prefix = if (sortDescending) "✓ " else ""
                                            Text("${prefix}${stringResource(R.string.sort_descending)}")
                                        },
                                    onClick = {
                                        onSortDirectionSelected(true)
                                    },
                                )
                                DropdownMenuItem(
                                        text = {
                                            val prefix = if (!sortDescending) "✓ " else ""
                                            Text("${prefix}${stringResource(R.string.sort_ascending)}")
                                        },
                                    onClick = {
                                        onSortDirectionSelected(false)
                                    },
                                )
                            }
                        }
                        TextButton(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 1.dp, minHeight = 36.dp)
                                .semantics {
                                    contentDescription = searchDescription
                                },
                            onClick = onToggleSearchBar,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        ) {
                            Text(
                                text = if (showSearchBar) {
                                    stringResource(R.string.menu_collapse)
                                } else {
                                    stringResource(R.string.menu_search)
                                },
                                color = MaterialTheme.colorScheme.onBackground,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                        TextButton(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 1.dp, minHeight = 36.dp)
                                .semantics {
                                    contentDescription = addTorrentDescription
                                },
                            onClick = onOpenAddTorrent,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        ) {
                            Text(
                                text = "+",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 22.sp,
                            )
                        }
                    }
                }

            }
        },
    )
}

@Composable
private fun torrentListSortLabel(option: TorrentListSortOption): String {
    return when (option) {
        TorrentListSortOption.ADDED_TIME -> stringResource(R.string.sort_added_time)
        TorrentListSortOption.UPLOAD_SPEED -> stringResource(R.string.sort_upload_speed)
        TorrentListSortOption.DOWNLOAD_SPEED -> stringResource(R.string.sort_download_speed)
        TorrentListSortOption.SHARE_RATIO -> stringResource(R.string.sort_share_ratio)
        TorrentListSortOption.TOTAL_UPLOADED -> stringResource(R.string.sort_total_uploaded)
        TorrentListSortOption.TOTAL_DOWNLOADED -> stringResource(R.string.sort_total_downloaded)
        TorrentListSortOption.TORRENT_SIZE -> stringResource(R.string.sort_torrent_size)
        TorrentListSortOption.ACTIVITY_TIME -> stringResource(R.string.sort_activity_time)
        TorrentListSortOption.SEEDERS -> stringResource(R.string.sort_seeders)
        TorrentListSortOption.LEECHERS -> stringResource(R.string.sort_leechers)
        TorrentListSortOption.CROSS_SEED_COUNT -> stringResource(R.string.sort_cross_seed_count)
    }
}
