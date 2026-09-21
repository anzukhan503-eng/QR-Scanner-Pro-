package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.scanner.QrContentType
import com.example.scanner.QrTypeDetector
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("QR Scanner Pro", appName)
  }

  @Test
  fun `qr type detector detects url correctly`() {
    val result = QrTypeDetector.parse("https://google.com")
    assertEquals(QrContentType.URL, result.type)
  }

  @Test
  fun `qr type detector detects wifi correctly`() {
    val result = QrTypeDetector.parse("WIFI:S:MyWifiNetwork;T:WPA;P:secret123;;")
    assertEquals(QrContentType.WIFI, result.type)
    assertEquals("MyWifiNetwork", result.wifiSsid)
    assertEquals("secret123", result.wifiPassword)
  }
}

