package com.github.damontecres.wholphin.ui.detail.series

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.ChosenStreams
import com.github.damontecres.wholphin.data.ExtrasItem
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.data.model.Person
import com.github.damontecres.wholphin.preferences.UserPreferences
import com.github.damontecres.wholphin.ui.AspectRatios
import com.github.damontecres.wholphin.ui.cards.BannerCard
import com.github.damontecres.wholphin.ui.cards.ExtrasRow
import com.github.damontecres.wholphin.ui.cards.PersonRow
import com.github.damontecres.wholphin.ui.components.ErrorMessage
import com.github.damontecres.wholphin.ui.components.HeaderUtils
import com.github.damontecres.wholphin.ui.components.LoadingPage
import com.github.damontecres.wholphin.ui.components.TabDetails
import com.github.damontecres.wholphin.ui.components.TabRow
import com.github.damontecres.wholphin.ui.components.TitleOrLogo
import com.github.damontecres.wholphin.ui.formatSeasonNumber
import com.github.damontecres.wholphin.ui.ifElse
import com.github.damontecres.wholphin.ui.logTab
import com.github.damontecres.wholphin.ui.playback.isPlayKeyUp
import com.github.damontecres.wholphin.ui.rememberInt
import com.github.damontecres.wholphin.ui.tryRequestFocus
import com.github.damontecres.wholphin.ui.util.KeepVisibleBringIntoViewSpec
import com.github.damontecres.wholphin.ui.util.StringStringProvider
import com.github.damontecres.wholphin.ui.util.rememberDelayedNestedScroll
import kotlinx.coroutines.launch
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.PersonKind
import kotlin.time.Duration

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SeriesOverviewContent(
    preferences: UserPreferences,
    series: BaseItem,
    seasons: List<BaseItem?>,
    episodes: EpisodeList,
    seasonExtras: List<ExtrasItem>,
    chosenStreams: ChosenStreams?,
    peopleInEpisode: List<Person>,
    position: SeriesOverviewPosition,
    firstItemFocusRequester: FocusRequester,
    episodeRowFocusRequester: FocusRequester,
    castCrewRowFocusRequester: FocusRequester,
    guestStarRowFocusRequester: FocusRequester,
    extrasRowFocusRequester: FocusRequester,
    onChangeSeason: (Int) -> Unit,
    onFocusEpisode: (Int) -> Unit,
    onClick: (BaseItem) -> Unit,
    onLongClick: (BaseItem) -> Unit,
    playOnClick: (Duration) -> Unit,
    watchOnClick: () -> Unit,
    favoriteOnClick: () -> Unit,
    moreOnClick: () -> Unit,
    overviewOnClick: () -> Unit,
    personOnClick: (Person) -> Unit,
    canDelete: (BaseItem) -> Boolean,
    onConfirmDelete: (BaseItem) -> Unit,
    onClickExtra: (Int, ExtrasItem) -> Unit,
    onChooseVersion: (BaseItem, MediaSourceInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var selectedTabIndex by rememberSaveable(position.seasonTabIndex) { mutableIntStateOf(position.seasonTabIndex) }
    LaunchedEffect(selectedTabIndex) {
        logTab("series_overview", selectedTabIndex)
    }
    val tabRowFocusRequester = remember { FocusRequester() }

    val focusedEpisode =
        (episodes as? EpisodeList.Success)?.episodes?.getOrNull(position.episodeRowIndex)
    var pageHasFocus by remember { mutableStateOf(false) }
    var cardRowHasFocus by remember { mutableStateOf(false) }
    val dimming by animateFloatAsState(if (pageHasFocus && !cardRowHasFocus) .4f else 1f)

    val scrollState = rememberScrollState()
    val scrollConnection = rememberDelayedNestedScroll()
    var requestFocusAfterSeason by remember { mutableStateOf(false) }

    val tabFocusRequesters = remember(seasons) { List(seasons.size) { FocusRequester() } }
    val contentFocusRequesters = remember(seasons) { List(seasons.size) { FocusRequester() } }
    val tabs =
        seasons.mapIndexed { index, season ->
            TabDetails(
                title =
                    StringStringProvider(
                        season?.name
                            ?: season?.data?.indexNumber?.let { formatSeasonNumber(it) }
                            ?: "",
                    ),
                tabFocusRequester = tabFocusRequesters[index],
                contentFocusRequester = contentFocusRequesters[index],
            )
        }

    val currentOnChangeSeason by rememberUpdatedState(onChangeSeason)
    val defaultBringIntoViewSpec = LocalBringIntoViewSpec.current

    Box(
        modifier =
            modifier
                .fillMaxWidth(),
    ) {
        // The page only scrolls as far as needed to keep the focused item visible. With the TV
        // default, every card focused in the episode row also asked the page to scroll it to a
        // fixed spot, which fought the header's bringIntoView below and made the page jump up
        // and back when moving quickly. Rows inside the page keep the default behaviour.
        CompositionLocalProvider(LocalBringIntoViewSpec provides KeepVisibleBringIntoViewSpec) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(vertical = 16.dp)
                        .focusGroup()
                        .nestedScroll(scrollConnection)
                        .verticalScroll(scrollState)
                        .onFocusChanged { pageHasFocus = it.hasFocus },
            ) {
                CompositionLocalProvider(LocalBringIntoViewSpec provides defaultBringIntoViewSpec) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier =
                            Modifier
                                .focusGroup()
                                .bringIntoViewRequester(bringIntoViewRequester),
                    ) {
                        val paddingValues =
                            remember(preferences.appPreferences.interfacePreferences.showClock) {
                                if (preferences.appPreferences.interfacePreferences.showClock) {
                                    PaddingValues(start = 0.dp, end = 184.dp)
                                } else {
                                    PaddingValues(start = 0.dp, end = 16.dp)
                                }
                            }
                        TabRow(
                            selectedTabIndex = selectedTabIndex,
                            tabs = tabs,
                            onClick =
                                remember {
                                    {
                                        selectedTabIndex = it
                                        currentOnChangeSeason(it)
                                        requestFocusAfterSeason = true
                                    }
                                },
                            modifier =
                                Modifier
                                    .focusRequester(tabRowFocusRequester)
                                    .padding(paddingValues)
                                    .padding(bottom = 4.dp)
                                    .fillMaxWidth(),
                        )
                        TitleOrLogo(
                            item = series,
                            showLogo = preferences.appPreferences.interfacePreferences.showLogos,
                            modifier = Modifier.padding(start = HeaderUtils.startPadding),
                        )
                        // Episodes are paged, so moving quickly can land on one that hasn't loaded yet.
                        // Hold the header's height until it does, otherwise the details rows vanish and
                        // the episode row below jumps up and back down.
                        KeepHeightWhileLoading(
                            loading = focusedEpisode == null,
                            modifier = Modifier.fillMaxWidth(.6f),
                        ) {
                            FocusedEpisodeHeader(
                                preferences = preferences,
                                ep = focusedEpisode,
                                chosenStreams = chosenStreams,
                                overviewOnClick = overviewOnClick,
                                overviewOnFocus = {
                                    if (it.isFocused) {
                                        scope.launch {
                                            bringIntoViewRequester.bringIntoView()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        //                key(position.seasonTabIndex) {
                        when (val eps = episodes) {
                            EpisodeList.Loading -> {
                                LoadingPage()
                            }

                            is EpisodeList.Error -> {
                                ErrorMessage(eps.message, eps.exception)
                            }

                            is EpisodeList.Success -> {
                                if (requestFocusAfterSeason) {
                                    // Changing seasons, so move focus once the new episodes are loaded
                                    LaunchedEffect(Unit) {
                                        firstItemFocusRequester.tryRequestFocus()
                                        requestFocusAfterSeason = false
                                    }
                                }
                                val state = rememberLazyListState(position.episodeRowIndex)
                                var epPosition by rememberInt(position.episodeRowIndex)
                                LazyRow(
                                    state = state,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    modifier =
                                        Modifier
                                            .focusRestorer(episodeRowFocusRequester)
                                            //                                    .focusRequester(episodeRowFocusRequester)
                                            .onFocusChanged {
                                                cardRowHasFocus = it.hasFocus
                                            },
                                ) {
                                    itemsIndexed(eps.episodes) { episodeIndex, episode ->
                                        val interactionSource = remember { MutableInteractionSource() }
                                        if (interactionSource.collectIsFocusedAsState().value) {
                                            onFocusEpisode.invoke(episodeIndex)
                                        }
                                        BannerCard(
                                            name = episode?.name,
                                            item = episode,
                                            aspectRatio =
                                                episode
                                                    ?.aspectRatio
                                                    ?.coerceAtLeast(AspectRatios.FOUR_THREE)
                                                    ?: (AspectRatios.WIDE),
                                            cornerText = episode?.ui?.episodeCornerText,
                                            played = episode?.data?.userData?.played ?: false,
                                            playPercent =
                                                episode?.data?.userData?.playedPercentage
                                                    ?: 0.0,
                                            onClick = {
                                                epPosition = episodeIndex
                                                if (episode != null) onClick.invoke(episode)
                                            },
                                            onLongClick = {
                                                epPosition = episodeIndex
                                                if (episode != null) onLongClick.invoke(episode)
                                            },
                                            modifier =
                                                Modifier
                                                    .ifElse(
                                                        episodeIndex == position.episodeRowIndex,
                                                        Modifier
                                                            .focusRequester(firstItemFocusRequester),
                                                    ).ifElse(
                                                        episodeIndex == epPosition,
                                                        Modifier.focusRequester(episodeRowFocusRequester),
                                                    ).background(
                                                        if (episodeIndex != position.episodeRowIndex) {
                                                            Color.Black
                                                        } else {
                                                            Color.Transparent
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                    ).graphicsLayer {
                                                        alpha =
                                                            if (episodeIndex != position.episodeRowIndex) {
                                                                dimming
                                                            } else {
                                                                1f
                                                            }
                                                    }.onFocusChanged {
                                                        if (it.isFocused) {
                                                            scope.launch {
                                                                bringIntoViewRequester.bringIntoView()
                                                            }
                                                        }
                                                    }.onKeyEvent {
                                                        if (episode != null && isPlayKeyUp(it)) {
                                                            onClick.invoke(episode)
                                                            return@onKeyEvent true
                                                        }
                                                        return@onKeyEvent false
                                                    },
                                            interactionSource = interactionSource,
                                            cardHeight = 120.dp,
                                            useSeriesForPrimary = false,
                                        )
                                    }
                                }
                            }
                            //                    }
                        }

                        KeepHeightWhileLoading(
                            loading = focusedEpisode == null,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            focusedEpisode?.let { ep ->
                                FocusedEpisodeFooter(
                                    preferences = preferences,
                                    ep = ep,
                                    chosenStreams = chosenStreams,
                                    playOnClick = playOnClick,
                                    moreOnClick = moreOnClick,
                                    watchOnClick = {
                                        watchOnClick.invoke()
                                        episodeRowFocusRequester.tryRequestFocus()
                                    },
                                    favoriteOnClick = favoriteOnClick,
                                    buttonOnFocusChanged = {
                                        if (it.isFocused) {
                                            scope.launch {
                                                bringIntoViewRequester.bringIntoView()
                                            }
                                        }
                                    },
                                    canDelete = canDelete.invoke(ep),
                                    onConfirmDelete = { onConfirmDelete.invoke(ep) },
                                    onChooseVersion = { onChooseVersion.invoke(ep, it) },
                                    modifier =
                                        Modifier
                                            .padding(top = 4.dp)
                                            .fillMaxWidth(),
                                )
                            }
                        }
                    }

                    val (guestStars, castAndCrew) =
                        remember(peopleInEpisode) {
                            peopleInEpisode.partition { it.type == PersonKind.GUEST_STAR }
                        }

                    AnimatedVisibility(
                        visible = peopleInEpisode.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (castAndCrew.isNotEmpty()) {
                                PersonRow(
                                    title = R.string.cast_and_crew,
                                    people = castAndCrew,
                                    onClick = personOnClick,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .focusRequester(castCrewRowFocusRequester),
                                )
                            }
                            if (guestStars.isNotEmpty()) {
                                PersonRow(
                                    title = R.string.guest_stars,
                                    people = guestStars,
                                    onClick = personOnClick,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .focusRequester(guestStarRowFocusRequester),
                                )
                            }
                        }
                    }
                    AnimatedVisibility(
                        visible = seasonExtras.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        ExtrasRow(
                            extras = seasonExtras,
                            onClickItem = onClickExtra,
                            onLongClickItem = { _, _ -> },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .focusRequester(extrasRowFocusRequester),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Remembers the height of [content] while [loading] is false, and keeps at least that height
 * while [loading] is true so the surrounding layout doesn't shift.
 */
@Composable
private fun KeepHeightWhileLoading(
    loading: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    var lastHeight by remember { mutableStateOf(0.dp) }
    Box(
        modifier =
            modifier.ifElse(
                loading,
                Modifier.heightIn(min = lastHeight),
                Modifier.onSizeChanged { lastHeight = with(density) { it.height.toDp() } },
            ),
    ) {
        content()
    }
}
