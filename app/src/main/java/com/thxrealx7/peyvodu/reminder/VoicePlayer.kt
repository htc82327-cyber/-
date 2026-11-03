package com.thxrealx7.peyvodu.reminder

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import com.thxrealx7.peyvodu.R
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object VoicePlayer {
    private val reminderClips = intArrayOf(
        R.raw.calm_01, R.raw.calm_02, R.raw.calm_03, R.raw.calm_04, R.raw.calm_05
    )

    fun reminderResource(): Int = reminderClips.random()

    fun feedbackResource(newProgress: Float): Int = when {
        newProgress >= 1f -> R.raw.calm_09
        newProgress >= .8f -> R.raw.calm_08
        else -> if ((0..1).random() == 0) R.raw.calm_06 else R.raw.calm_07
    }

    suspend fun play(context: Context, resId: Int, voice: String) {
        suspendCancellableCoroutine { cont ->
            val player = MediaPlayer.create(context, resId)
            if (player == null) {
                cont.resume(Unit)
                return@suspendCancellableCoroutine
            }

            try {
                if (voice == "active") {
                    player.playbackParams = PlaybackParams().setSpeed(1.08f).setPitch(1.03f)
                }
            } catch (_: Throwable) {
            }

            player.setOnCompletionListener {
                it.release()
                if (cont.isActive) cont.resume(Unit)
            }
            player.setOnErrorListener { mp, _, _ ->
                mp.release()
                if (cont.isActive) cont.resume(Unit)
                true
            }
            cont.invokeOnCancellation { runCatching { player.release() } }
            player.start()
        }
    }
}
