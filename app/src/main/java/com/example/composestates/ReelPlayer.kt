package com.example.composestates

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.database.ExoDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSourceFactory
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.SimpleExoPlayer
import androidx.media3.exoplayer.offline.ProgressiveDownloader
import androidx.media3.exoplayer.source.ConcatenatingMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.TrackSelector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File


@UnstableApi
class ReelPlayer(context: Context) {

    private var player: SimpleExoPlayer

    private var concatenatingMediaSource = ConcatenatingMediaSource()

    private var playBackStateChanged = MutableLiveData<Int>()

    private lateinit var upstreamDataSourceFactory: DefaultDataSourceFactory

    val cache by lazy {
        return@lazy cacheInstance ?: run {
            val cacheDir = File(/* parent = */ context.filesDir, /* child = */ "rooter_downloads")
            val exoCacheDir = File("${cacheDir.absolutePath}/exo")
            val evictor = LeastRecentlyUsedCacheEvictor(CACHE_SIZE)
            SimpleCache(exoCacheDir, evictor, ExoDatabaseProvider(context)).also {
                cacheInstance = it
            }
        }
    }


    private val cacheDataSourceFactory by lazy {
        CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamDataSourceFactory)
            .setCacheReadDataSourceFactory(FileDataSource.Factory())
            .setCacheWriteDataSinkFactory(
                CacheDataSink.Factory().setCache(cache)
                    .setFragmentSize(CacheDataSink.DEFAULT_FRAGMENT_SIZE)
            )
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    private val playerStateListener: Player.Listener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            playBackStateChanged.postValue(state)
        }

    }

    init {
        val defaultLoadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(2500, 5000, 2000, 2000)
            .build()
        val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(
            10000,
            5000, 5000, 0.7f
        )

        upstreamDataSourceFactory =
            DefaultDataSourceFactory(context, Util.getUserAgent(context, "Rooter"))
        val trackSelector: TrackSelector = DefaultTrackSelector(context, videoTrackSelectionFactory)
        player = SimpleExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setLoadControl(defaultLoadControl)
            .build()

        player.addListener(playerStateListener)
    }

    fun getPlayer(): SimpleExoPlayer = player

    fun appendMediaSource(url: String): Int {
        concatenatingMediaSource.addMediaSource(
            parseMediaSource(Uri.parse(url), cacheDataSourceFactory)
        )
        return concatenatingMediaSource.size - 1
    }

    fun setMediaSource() {
        if (player.mediaItemCount == 0) {
            player.setMediaSource(concatenatingMediaSource)
            player.prepare()
        }
    }

    fun seekTo(windowIndex: Int, playWhenReady: Boolean) {
        try {
            player.playWhenReady = playWhenReady
            player.seekTo(windowIndex, 0L)
            player.repeatMode = REPEAT_MODE_ONE
        } catch (e: Exception) {

        }
    }

    companion object {

        private const val CACHE_SIZE = 50 * 1024 * 1024L
        private var cacheInstance: Cache? = null
    }


    fun parseMediaSource(uri: Uri, dataSourceFactory: DataSource.Factory): MediaSource {
        val mediaItem = MediaItem.fromUri(uri)
        return ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItem)
    }


    fun parseVideoUrlsIntoMediaSource() {
        kotlin.runCatching {
            for (item in reels) {
                item.url?.let { appendMediaSource(it) }
            }
        }.onFailure {  }
        setMediaSource()
    }
}