package org.skepsun.kototoro.core

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.net.toUri
import org.skepsun.kototoro.BuildConfig
import org.skepsun.kototoro.R
import org.skepsun.kototoro.core.nav.AppRouter
import org.skepsun.kototoro.core.util.ext.copyToClipboard
import org.skepsun.kototoro.core.util.ext.getSerializableExtraCompat
import org.skepsun.kototoro.core.util.ext.printStackTraceDebug

class ErrorReporterReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val appContext = context ?: return
        val e = intent?.getSerializableExtraCompat<Throwable>(AppRouter.KEY_ERROR) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        if (notificationId != 0) {
            val notificationTag = intent.getStringExtra(EXTRA_NOTIFICATION_TAG)
            NotificationManagerCompat.from(appContext).cancel(notificationTag, notificationId)
        }
        appContext.copyToClipboard(appContext.getString(R.string.error), e.stackTraceToString())
        Toast.makeText(appContext, R.string.crash_report_copied, Toast.LENGTH_SHORT).show()
    }

    companion object {

        private const val ACTION_REPORT = "${BuildConfig.APPLICATION_ID}.action.REPORT_ERROR"
        private const val EXTRA_NOTIFICATION_ID = "notify.id"
        private const val EXTRA_NOTIFICATION_TAG = "notify.tag"

        fun getPendingIntent(context: Context, e: Throwable): PendingIntent? = getPendingIntentInternal(
            context = context,
            e = e,
            notificationId = 0,
            notificationTag = null,
        )

        fun getNotificationAction(
            context: Context,
            e: Throwable,
            notificationId: Int,
            notificationTag: String?,
        ): NotificationCompat.Action? {
            val intent = getPendingIntentInternal(
                context = context,
                e = e,
                notificationId = notificationId,
                notificationTag = notificationTag,
            ) ?: return null
            return NotificationCompat.Action(
                R.drawable.ic_alert_outline,
                context.getString(R.string.copy),
                intent,
            )
        }

        private fun getPendingIntentInternal(
            context: Context,
            e: Throwable,
            notificationId: Int,
            notificationTag: String?,
        ): PendingIntent? = runCatching {
            val intent = Intent(context, ErrorReporterReceiver::class.java)
            intent.setAction(ACTION_REPORT)
            intent.setData("err://${e.hashCode()}".toUri())
            intent.putExtra(AppRouter.KEY_ERROR, e)
            intent.putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            intent.putExtra(EXTRA_NOTIFICATION_TAG, notificationTag)
            PendingIntentCompat.getBroadcast(context, 0, intent, 0, false)
        }.onFailure { e ->
            // probably cannot write exception as serializable
            e.printStackTraceDebug()
        }.getOrNull()
    }
}
