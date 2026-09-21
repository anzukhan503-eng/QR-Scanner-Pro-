package com.example.scanner

import android.content.Context
import android.content.Intent
import android.net.Uri

enum class QrContentType {
    URL,
    PHONE,
    EMAIL,
    WIFI,
    CONTACT,
    SMS,
    TEXT
}

data class ParsedQrResult(
    val rawContent: String,
    val type: QrContentType,
    val title: String,
    val summary: String,
    val wifiSsid: String? = null,
    val wifiPassword: String? = null,
    val wifiType: String? = null,
    val phoneNumber: String? = null,
    val emailAddress: String? = null,
    val contactName: String? = null
)

object QrTypeDetector {

    fun parse(raw: String): ParsedQrResult {
        val trimmed = raw.trim()

        // 1. Wi-Fi
        if (trimmed.startsWith("WIFI:", ignoreCase = true)) {
            val ssid = extractWifiField(trimmed, "S:")
            val pass = extractWifiField(trimmed, "P:")
            val type = extractWifiField(trimmed, "T:") ?: "WPA"
            return ParsedQrResult(
                rawContent = raw,
                type = QrContentType.WIFI,
                title = "Wi-Fi Network: $ssid",
                summary = "Security: $type • Password: ${if (pass.isNullOrEmpty()) "None" else "••••••••"}",
                wifiSsid = ssid,
                wifiPassword = pass,
                wifiType = type
            )
        }

        // 2. URL
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("www.", ignoreCase = true)
        ) {
            val validUrl = if (trimmed.startsWith("www.", ignoreCase = true)) "https://$trimmed" else trimmed
            return ParsedQrResult(
                rawContent = validUrl,
                type = QrContentType.URL,
                title = "Website Link",
                summary = validUrl
            )
        }

        // 3. Contact (MECARD or VCARD)
        if (trimmed.startsWith("MECARD:", ignoreCase = true) || trimmed.startsWith("BEGIN:VCARD", ignoreCase = true)) {
            val name = if (trimmed.startsWith("MECARD:", ignoreCase = true)) {
                extractField(trimmed, "N:")
            } else {
                extractVcardField(trimmed, "FN:") ?: extractVcardField(trimmed, "N:")
            } ?: "Contact Card"
            val phone = if (trimmed.startsWith("MECARD:", ignoreCase = true)) {
                extractField(trimmed, "TEL:")
            } else {
                extractVcardField(trimmed, "TEL:")
            }
            val email = if (trimmed.startsWith("MECARD:", ignoreCase = true)) {
                extractField(trimmed, "EMAIL:")
            } else {
                extractVcardField(trimmed, "EMAIL:")
            }
            return ParsedQrResult(
                rawContent = raw,
                type = QrContentType.CONTACT,
                title = name,
                summary = listOfNotNull(phone, email).joinToString(" • ").ifBlank { "Contact Details" },
                phoneNumber = phone,
                emailAddress = email,
                contactName = name
            )
        }

        // 4. Phone Number
        if (trimmed.startsWith("tel:", ignoreCase = true)) {
            val phone = trimmed.substring(4)
            return ParsedQrResult(
                rawContent = raw,
                type = QrContentType.PHONE,
                title = "Phone Number",
                summary = phone,
                phoneNumber = phone
            )
        }

        // 5. SMS
        if (trimmed.startsWith("smsto:", ignoreCase = true) || trimmed.startsWith("sms:", ignoreCase = true)) {
            val parts = trimmed.substringAfter(":").split(":")
            val phone = parts.getOrNull(0) ?: ""
            val msg = parts.getOrNull(1) ?: ""
            return ParsedQrResult(
                rawContent = raw,
                type = QrContentType.SMS,
                title = "SMS to $phone",
                summary = if (msg.isNotBlank()) msg else "Send text message",
                phoneNumber = phone
            )
        }

        // 6. Email
        if (trimmed.startsWith("mailto:", ignoreCase = true)) {
            val mail = trimmed.substring(7).substringBefore("?")
            return ParsedQrResult(
                rawContent = raw,
                type = QrContentType.EMAIL,
                title = "Email Address",
                summary = mail,
                emailAddress = mail
            )
        }
        if (android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
            return ParsedQrResult(
                rawContent = raw,
                type = QrContentType.EMAIL,
                title = "Email Address",
                summary = trimmed,
                emailAddress = trimmed
            )
        }

        // 7. General Text
        return ParsedQrResult(
            rawContent = raw,
            type = QrContentType.TEXT,
            title = "Text Note",
            summary = trimmed.take(80)
        )
    }

    private fun extractWifiField(wifiStr: String, prefix: String): String? {
        val index = wifiStr.indexOf(prefix, ignoreCase = true)
        if (index == -1) return null
        val start = index + prefix.length
        val end = wifiStr.indexOf(";", start)
        return if (end != -1) wifiStr.substring(start, end) else wifiStr.substring(start)
    }

    private fun extractField(content: String, prefix: String): String? {
        val index = content.indexOf(prefix, ignoreCase = true)
        if (index == -1) return null
        val start = index + prefix.length
        val end = content.indexOf(";", start)
        return if (end != -1) content.substring(start, end) else content.substring(start)
    }

    private fun extractVcardField(content: String, prefix: String): String? {
        val lines = content.lines()
        for (line in lines) {
            if (line.startsWith(prefix, ignoreCase = true)) {
                return line.substring(prefix.length).trim()
            }
        }
        return null
    }

    fun launchAppropriateAction(context: Context, result: ParsedQrResult) {
        try {
            when (result.type) {
                QrContentType.URL -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.rawContent)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                QrContentType.PHONE -> {
                    val phone = result.phoneNumber ?: result.rawContent.removePrefix("tel:")
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                QrContentType.EMAIL -> {
                    val email = result.emailAddress ?: result.rawContent.removePrefix("mailto:")
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                QrContentType.SMS -> {
                    val phone = result.phoneNumber ?: ""
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                QrContentType.WIFI -> {
                    val intent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                QrContentType.CONTACT -> {
                    val intent = Intent(Intent.ACTION_INSERT_OR_EDIT).apply {
                        type = "vnd.android.cursor.item/contact"
                        putExtra(android.provider.ContactsContract.Intents.Insert.NAME, result.contactName)
                        if (!result.phoneNumber.isNullOrBlank()) {
                            putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, result.phoneNumber)
                        }
                        if (!result.emailAddress.isNullOrBlank()) {
                            putExtra(android.provider.ContactsContract.Intents.Insert.EMAIL, result.emailAddress)
                        }
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                QrContentType.TEXT -> {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, result.rawContent)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "Share text").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(shareIntent)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: share
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, result.rawContent)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }
}
