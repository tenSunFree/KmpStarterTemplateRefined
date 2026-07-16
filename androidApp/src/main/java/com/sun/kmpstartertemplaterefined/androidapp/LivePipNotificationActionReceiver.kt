package com.sun.kmpstartertemplaterefined.androidapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sun.kmpstartertemplaterefined.feature_live_presentation.pip.AndroidLivePipNotificationBridge
import com.sun.kmpstartertemplaterefined.feature_live_presentation.rtc.RtcEngineHolder

/**
 * Receives broadcasts triggered by the "mute/unmute" and "stop playback" buttons on the PiP notification.
 *
 * Must be registered in AndroidManifest.xml (exported=false because it is only used by its own
 * PendingIntent and should not be triggered by other apps).
 *
 * The design intentionally uses "broadcast" rather than "directly calling a method on an object"
 * because when the user clicks on a notification's PendingIntent, the system will deliver the
 * corresponding Intent directly to this Receiver—even if the app currently has no Activity running
 * (for example, the user has swiped away the app, leaving only this notification). This is exactly
 * the purpose of PendingIntent.getBroadcast: to decouple from the Activity lifecycle and have the
 * system guarantee delivery.
 */
class LivePipNotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TOGGLE_MUTE =
            "com.sun.kmpstartertemplaterefined.action.PIP_TOGGLE_MUTE"
        const val ACTION_STOP =
            "com.sun.kmpstartertemplaterefined.action.PIP_STOP"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TOGGLE_MUTE -> handleToggleMute(context)
            ACTION_STOP -> handleStop(context)
        }
    }

    /**
     * Optimistic update: immediately flip the mute state recorded in the bridge and immediately refresh
     * the notification button text, so the user sees the button change without waiting for Compose to recompose.
     * The actual mute/unmute (calling Agora's muteAllRemoteAudioStreams) is handled by the
     * onToggleMuteRequested callback on the Compose side.
     */
    private fun handleToggleMute(context: Context) {
        val newMuted = !AndroidLivePipNotificationBridge.isMuted
        AndroidLivePipNotificationBridge.isMuted = newMuted
        // courseTitle is not saved here (Receiver is stateless), so using "Live Course" as the default
        // is sufficient—the key part of the notification text is the button state, and the title does
        // not change at this update moment.
        LivePipNotificationManager.updateMuteState(
            context = context,
            courseTitle = "直播課程",
            isMuted = newMuted,
        )
        AndroidLivePipNotificationBridge.notifyToggleMuteRequested()
    }

    private fun handleStop(context: Context) {
        LivePipNotificationManager.cancel(context)
        // To maintain consistency with the system PiP X: do not wait for bring-to-front before disposing of the compose,
        // Release it directly here to ensure that the sound stops immediately when "End Viewing" is pressed.
        RtcEngineHolder.releaseCurrentSession(reason = "PiP notification stop action")
        AndroidLivePipNotificationBridge.notifyStopRequested()
        val bringToFrontIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        context.startActivity(bringToFrontIntent)
    }
}
