package com.example.notifications

data class NotificationTip(
    val title: String,
    val message: String,
    val type: String = "TIP"
)

object NotificationCatalog {
    val dailyTips = listOf(
        NotificationTip(
            title = "📶 Instant Wi-Fi Sharing",
            message = "Generate a Wi-Fi QR code in the Create tab so guests can connect with a single scan without typing complex passwords.",
            type = "TIP"
        ),
        NotificationTip(
            title = "🛡️ QR Code Security Check",
            message = "Always review unfamiliar web links in the scan preview before opening them in your browser to stay safe online.",
            type = "SECURITY"
        ),
        NotificationTip(
            title = "📋 Fast Clipboard Copy",
            message = "Tap any scan result in your History to quickly copy URLs, serial numbers, or texts to your clipboard.",
            type = "TIP"
        ),
        NotificationTip(
            title = "🎨 Personalized QR Designs",
            message = "Customize your QR code foreground and background colors in the Create screen for high-contrast, stylish codes.",
            type = "FEATURE"
        ),
        NotificationTip(
            title = "🔍 Distance & Zoom Scanning",
            message = "Need to scan a code from far away? Pinch on the camera screen or adjust the zoom slider for crisp detection.",
            type = "TIP"
        ),
        NotificationTip(
            title = "👥 Save Contacts with vCard",
            message = "Scan contact QR codes to immediately add names, phone numbers, and emails to your contacts with one tap.",
            type = "TIP"
        ),
        NotificationTip(
            title = "📦 History Filters & Search",
            message = "Keep your scans organized. Use the search bar in the History tab to quickly locate past scans and generated codes.",
            type = "FEATURE"
        ),
        NotificationTip(
            title = "🌙 Battery-Saving Dark Theme",
            message = "Switch to Dark Mode in Settings for a sleek, eye-friendly interface that conserves battery on AMOLED screens.",
            type = "TIP"
        ),
        NotificationTip(
            title = "⚡ Batch Scanning Efficiency",
            message = "Scanning inventory or event tickets? Enable Batch Scan mode in the scanner toolbar to scan multiple codes in seconds.",
            type = "FEATURE"
        ),
        NotificationTip(
            title = "🔗 Quick URL Redirection",
            message = "Enable 'Auto-Open URLs' in Settings if you want trusted web links to launch directly in your default browser.",
            type = "TIP"
        ),
        NotificationTip(
            title = "💡 Low-Light Torch",
            message = "Don't let dark rooms stop you. Tap the flashlight icon in the camera preview to illuminate codes instantly.",
            type = "TIP"
        ),
        NotificationTip(
            title = "📤 Easy QR Sharing",
            message = "You can share generated QR codes as high-resolution images directly to WhatsApp, Telegram, or email.",
            type = "FEATURE"
        )
    )

    fun getTipForIndex(index: Int): NotificationTip {
        val safeIndex = kotlin.math.abs(index) % dailyTips.size
        return dailyTips[safeIndex]
    }
}
