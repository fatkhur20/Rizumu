package com.kyrix.ftxmusic.playback

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.kyrix.ftxmusic.FtxApp
import com.kyrix.ftxmusic.R

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private val playback: PlaybackManager
        get() = (application as FtxApp).playback

    override fun onCreate() {
        super.onCreate()
        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(CHANNEL_ID)
            .setChannelName(R.string.notification_channel_name)
            .setNotificationId(NOTIFICATION_ID)
            .build()
        notificationProvider.setSmallIcon(R.drawable.ic_stat_rizumu)
        setMediaNotificationProvider(notificationProvider)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession =
        playback.mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = playback.player
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    private companion object {
        const val CHANNEL_ID = "rizumu_playback"
        const val NOTIFICATION_ID = 1201
    }
}
