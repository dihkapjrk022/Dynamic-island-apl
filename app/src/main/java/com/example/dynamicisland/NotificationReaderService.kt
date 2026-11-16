package com.example.dynamicisland

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.content.Intent

class NotificationReaderService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val n = sbn.notification
        val extras = n.extras

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val body = (
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)
            )?.toString().orEmpty()

        val category = n.category.orEmpty()
        val type = classify(sbn.packageName, category, n)

        // Ignore most silent/system housekeeping notifications.
        if (type == "other" || (title.isBlank() && body.isBlank())) return

        val event = Intent(OverlayService.ACTION_SHOW).apply {
            setPackage(packageName)
            putExtra("title", title)
            putExtra("body", body)
            putExtra("type", type)
            putExtra("source_package", sbn.packageName)
            putExtra("notification_key", sbn.key)
        }
        sendBroadcast(event)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Do not automatically hide the island here: another notification may still be active.
    }

    private fun classify(pkg: String, category: String, n: Notification): String {
        val p = pkg.lowercase()

        if (category == Notification.CATEGORY_CALL) return "call"
        if (category == Notification.CATEGORY_TRANSPORT ||
            n.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)) return "music"
        if (category == Notification.CATEGORY_ALARM ||
            p.contains("clock") || p.contains("timer")) return "timer"
        if (category == Notification.CATEGORY_EVENT ||
            p.contains("calendar")) return "calendar"
        if (category == Notification.CATEGORY_MESSAGE ||
            category == Notification.CATEGORY_EMAIL ||
            p.contains("whatsapp") || p.contains("telegram") ||
            p.contains("messaging") || p.contains("sms")) return "chat"

        return "other"
    }
}
