package com.example.composestates

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.composestates.ui.theme.ComposeStatesTheme
import kotlinx.coroutines.launch

@UnstableApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeStatesTheme {
                // A surface container using the 'background' color from the theme
                ListLayout()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@UnstableApi
@Preview(showBackground = true)
@Composable
fun ListLayout() {
    val state = rememberPagerState()
    var currentMiddleItem by remember { mutableStateOf(-1) }

    val exoPlayer = rememberExoPlayerWithLifecycle()
    val scope = rememberCoroutineScope()

    val playerState = remember {
        mutableStateOf(false)
    }

    exoPlayer.getPlayer().addListener(playerListener {
        playerState.value = true
    })

    val context = LocalContext.current

    LaunchedEffect(key1 = reels) {
        scope.launch {
            exoPlayer.parseVideoUrlsIntoMediaSource()
        }
    }


    VerticalPager(pageCount = reels.size, state = state) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            val doSomething by remember {
                derivedStateOf {
                    state.currentPageOffsetFraction == 0f && it == state.currentPage
                }
            }


            LaunchedEffect(key1 = doSomething) {
                if (it == state.currentPage) {
                    if (currentMiddleItem == it)
                        return@LaunchedEffect
                    currentMiddleItem = it

                    exoPlayer.seekTo(it, true)
                    exoPlayer.getPlayer().playWhenReady = true
                }
            }
            Log.i("TAG", "ListLayout: $it")

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (it == currentMiddleItem && playerState.value)
                    AndroidView(modifier = Modifier
                        .fillMaxSize(), factory = {
                        PlayerView(context).apply {
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            player = exoPlayer.getPlayer()
                            useController = false
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    }
                    )
                else
                    AsyncImage(
                        model = reels[it].thumbnail,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
            }
        }
    }
}

fun playerListener(stateReadyCallback: () -> Unit) = object : Player.Listener {
    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)
        if (playbackState == Player.STATE_READY)
            stateReadyCallback()
    }
}


@Composable
@UnstableApi
fun rememberExoPlayerWithLifecycle(
): ReelPlayer {

    val context = LocalContext.current
    val player = remember {
        ReelPlayer(context)
    }
    var appInBackground by remember {
        mutableStateOf(false)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(key1 = lifecycleOwner, appInBackground) {
        val lifecycleObserver = getExoPlayerLifecycleObserver(player.getPlayer(), appInBackground) {
            appInBackground = it
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }
    return player
}

fun getExoPlayerLifecycleObserver(
    exoPlayer: ExoPlayer,
    wasAppInBackground: Boolean,
    setWasAppInBackground: (Boolean) -> Unit
): LifecycleEventObserver =
    LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                if (wasAppInBackground)
                    exoPlayer.playWhenReady = true
                setWasAppInBackground(false)
            }

            Lifecycle.Event.ON_PAUSE -> {
                exoPlayer.playWhenReady = false
                setWasAppInBackground(true)
            }

            Lifecycle.Event.ON_STOP -> {
                exoPlayer.playWhenReady = false
                setWasAppInBackground(true)
            }

            Lifecycle.Event.ON_DESTROY -> {
                exoPlayer.release()
            }

            else -> {}
        }
    }

