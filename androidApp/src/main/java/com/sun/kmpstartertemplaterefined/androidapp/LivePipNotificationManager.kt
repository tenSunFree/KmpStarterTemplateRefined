package com.sun.kmpstartertemplaterefined.androidapp

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Responsible for showing/updating/dismissing the persistent notification displayed when in PiP floating mode.
 *
 * Key difference from LiveBackgroundAudioService
 * No Service is needed here: in PiP mode the Activity is still within the foreground lifecycle
 * (only shrunk/resized by the system), not the "background execution" restricted by Android 8+,
 * so the system does not force a ForegroundService to be required for survival.
 * Simply calling NotificationManagerCompat.notify()/cancel() is sufficient; the lifecycle is
 * completely controlled manually by MainActivity according to the PiP state.
 *
 * How to wire three action buttons
 * The requirement is to have three operations in parallel: "return to live," "mute," and "stop playback,"
 * so we make all three explicit addAction buttons instead of only relying on implicit interaction like
 * "click the notification body" (the "return to live" button shares the same PendingIntent as contentIntent,
 * so the behavior is exactly the same, and clicking the body is equally effective).
 *
 * - "Return to live" → uses FLAG_ACTIVITY_REORDER_TO_FRONT to bring the already-running MainActivity
 *   back to the foreground. This is the standard approach recommended by Android's official documentation.
 *   The system automatically restores the PiP window to full screen without needing any extra "leave PiP" API
 *   calls (in fact, no such public API exists).
 * - "Mute/unmute" → broadcast to [LivePipNotificationActionReceiver], which forwards it to
 *   AndroidLivePipNotificationBridge, ultimately toggling speakerEnabled on the Compose side.
 * - "Stop playback" → also via broadcast, where the Receiver simultaneously:
 *   (1) immediately cancel the notification itself (without waiting for Compose to finish processing),
 *   (2) forward to the bridge to trigger the leave-live-room logic,
 *   (3) bring MainActivity back to the foreground (to prevent the PiP window from staying on a
 *       small window that no longer has video content after the user stops playback).
 */
object LivePipNotificationManager {

    private const val REQUEST_CODE_CONTENT = 100
    private const val REQUEST_CODE_TOGGLE_MUTE = 101
    private const val REQUEST_CODE_STOP = 102

    /** Whether the notification is currently showing. Lets calls like reportMuteState() know whether to actually update the notification. */
    @Volatile
    private var isShowing = false

    @SuppressLint("MissingPermission")
    fun show(context: Context, courseTitle: String, isMuted: Boolean) {
        // ...existing code...
        if (!hasNotificationPermission(context)) {
            // No notification permission means no notification will be shown, but this does not affect
            // the live playback itself—this is a reasonable graceful degradation and should not interrupt
            // the user in any way.
            return
        }
        LiveNotificationChannel.ensureCreated(context)
        val notification = buildNotification(context, courseTitle, isMuted)
        NotificationManagerCompat.from(context)
            .notify(LiveNotificationChannel.NOTIFICATION_ID_PIP_CONTROLS, notification)
        isShowing = true
    }

    /** When showing, update the button text (for example, when mute state changes); if not showing, ignore. */
    fun updateMuteState(context: Context, courseTitle: String, isMuted: Boolean) {
        if (!isShowing) return
        show(context, courseTitle, isMuted)
    }

    fun cancel(context: Context) {
        if (!isShowing) return
        NotificationManagerCompat.from(context)
            .cancel(LiveNotificationChannel.NOTIFICATION_ID_PIP_CONTROLS)
        isShowing = false
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    /**
     * Intent to bring MainActivity back to the foreground. When the system receives a "start request for the
     * same already-running Activity with REORDER_TO_FRONT," it automatically restores it from PiP to full
     * screen—this is standard behavior documented in the official docs, not a hack.
     */
    private fun buildBringToFrontIntent(context: Context): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
    }

    private fun buildNotification(
        context: Context,
        courseTitle: String,
        isMuted: Boolean,
    ): android.app.Notification {
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_CONTENT,
            buildBringToFrontIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val toggleMuteIntent =
            Intent(context, LivePipNotificationActionReceiver::class.java).apply {
                action = LivePipNotificationActionReceiver.ACTION_TOGGLE_MUTE
            }
        val toggleMutePendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_TOGGLE_MUTE,
            toggleMuteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = Intent(context, LivePipNotificationActionReceiver::class.java).apply {
            action = LivePipNotificationActionReceiver.ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_STOP,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val muteActionLabel = if (isMuted) "取消靜音" else "靜音"
        val muteActionIcon = if (isMuted) {
            android.R.drawable.ic_lock_silent_mode_off
        } else {
            android.R.drawable.ic_lock_silent_mode
        }
        return NotificationCompat.Builder(context, LiveNotificationChannel.CHANNEL_ID_PIP_CONTROLS)
            .setSmallIcon(android.R.drawable.ic_menu_view) // Recommended to replace with androidApp/res/drawable/ic_notification.xml
            // ...existing code...
            .setContentTitle(courseTitle)
            .setContentText("直播仍在播放中")
            // ...existing code...
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            // This is a status notification, not a message notification, so there is no need to show
            // a timestamp like "a few minutes ago"—hiding it keeps the appearance cleaner.
            // ...existing code...
            .setShowWhen(false)
            // ...existing code...
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            // ...existing code...
            .setContentIntent(contentPendingIntent)
            // "Return to live" is made an explicit button (sharing the same PendingIntent as contentIntent),
            // corresponding to one of the three operations explicitly required, rather than only relying
            // on implicit interaction where the user figures out that clicking the notification body works.
            .addAction(0, "返回直播", contentPendingIntent)
            .addAction(muteActionIcon, muteActionLabel, toggleMutePendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "結束觀看",
                stopPendingIntent,
            )
            .build()
    }
}