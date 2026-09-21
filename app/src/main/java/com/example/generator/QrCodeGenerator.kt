package com.example.generator

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object QrCodeGenerator {

    fun generateQrBitmap(
        content: String,
        width: Int = 800,
        height: Int = 800,
        darkColor: Int = Color.BLACK,
        lightColor: Int = Color.WHITE
    ): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = hashMapOf<EncodeHintType, Any>(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
                EncodeHintType.MARGIN to 2
            )
            val bitMatrix = QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                width,
                height,
                hints
            )
            val matrixWidth = bitMatrix.width
            val matrixHeight = bitMatrix.height
            val pixels = IntArray(matrixWidth * matrixHeight)
            for (y in 0 until matrixHeight) {
                val offset = y * matrixWidth
                for (x in 0 until matrixWidth) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) darkColor else lightColor
                }
            }
            val bitmap = Bitmap.createBitmap(matrixWidth, matrixHeight, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, matrixWidth, 0, 0, matrixWidth, matrixHeight)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri? {
        return try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "qr_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Boolean {
        val filename = "QR_${System.currentTimeMillis()}.png"
        var outputStream: OutputStream? = null
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/QRScannerPro")
                }
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (imageUri != null) {
                    outputStream = resolver.openOutputStream(imageUri)
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/QRScannerPro"
                val file = File(imagesDir)
                if (!file.exists()) file.mkdirs()
                val image = File(file, filename)
                outputStream = FileOutputStream(image)
            }
            if (outputStream != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
                outputStream.close()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Helper functions to format specialized QR content strings
    fun formatWifi(ssid: String, password: String, encryption: String): String {
        return "WIFI:S:$ssid;T:$encryption;P:$password;;"
    }

    fun formatContact(name: String, phone: String, email: String, org: String = ""): String {
        val sb = StringBuilder("MECARD:")
        if (name.isNotBlank()) sb.append("N:$name;")
        if (phone.isNotBlank()) sb.append("TEL:$phone;")
        if (email.isNotBlank()) sb.append("EMAIL:$email;")
        if (org.isNotBlank()) sb.append("ORG:$org;")
        sb.append(";")
        return sb.toString()
    }

    fun formatSms(phone: String, message: String): String {
        return "SMSTO:$phone:$message"
    }

    fun formatEmail(email: String, subject: String = "", body: String = ""): String {
        return if (subject.isBlank() && body.isBlank()) {
            "mailto:$email"
        } else {
            "mailto:$email?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}"
        }
    }

    fun formatPhone(phone: String): String {
        return "tel:$phone"
    }
}
