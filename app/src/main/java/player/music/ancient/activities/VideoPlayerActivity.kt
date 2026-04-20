package player.music.ancient.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import player.music.ancient.R
import player.music.ancient.activities.base.AbsThemeActivity
import player.music.ancient.databinding.ActivityVideoPlayerBinding
import player.music.ancient.util.PreferenceUtil

class VideoPlayerActivity : AbsThemeActivity() {
    private lateinit var binding: ActivityVideoPlayerBinding
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val mediaUri = intent.getStringExtra(EXTRA_URI)?.let(Uri::parse) ?: return
        val referer = intent.getStringExtra(EXTRA_REFERER)
        val origin = intent.getStringExtra(EXTRA_ORIGIN)
        val userAgent = intent.getStringExtra(EXTRA_USER_AGENT)?.takeIf { it.isNotBlank() }
        val isStreaming = mediaUri.scheme.equals("http", true) || mediaUri.scheme.equals("https", true)

        binding.toolbar.subtitle = when {
            isStreaming && PreferenceUtil.batterySaverMode -> getString(R.string.video_player_low_power_mode)
            isStreaming -> getString(R.string.video_player_streaming)
            else -> getString(R.string.video_player_local_video)
        }

        val trackSelector = DefaultTrackSelector(this).apply {
            if (isStreaming && PreferenceUtil.batterySaverMode) {
                parameters = buildUponParameters()
                    .setMaxVideoSizeSd()
                    .setForceLowestBitrate(true)
                    .build()
            }
        }

        val requestHeaders = buildMap<String, String> {
            referer?.takeIf { it.isNotBlank() }?.let { put("Referer", it) }
            origin?.takeIf { it.isNotBlank() }?.let { put("Origin", it) }
        }
        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent(userAgent ?: DEFAULT_USER_AGENT)
            .setDefaultRequestProperties(requestHeaders)
        val dataSourceFactory = DefaultDataSource.Factory(this, httpFactory)

        player = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            .also { exoPlayer ->
            binding.playerView.player = exoPlayer
            exoPlayer.setMediaItem(MediaItem.fromUri(mediaUri))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    override fun onStop() {
        player?.release()
        player = null
        binding.playerView.player = null
        super.onStop()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_URI = "extra_uri"
        private const val EXTRA_REFERER = "extra_referer"
        private const val EXTRA_ORIGIN = "extra_origin"
        private const val EXTRA_USER_AGENT = "extra_user_agent"
        private const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

        fun intent(
            context: Context,
            title: String,
            uri: String,
            referer: String? = null,
            origin: String? = null,
            userAgent: String? = null,
        ): Intent {
            return Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_URI, uri)
                referer?.takeIf { it.isNotBlank() }?.let { putExtra(EXTRA_REFERER, it) }
                origin?.takeIf { it.isNotBlank() }?.let { putExtra(EXTRA_ORIGIN, it) }
                userAgent?.takeIf { it.isNotBlank() }?.let { putExtra(EXTRA_USER_AGENT, it) }
            }
        }
    }
}
