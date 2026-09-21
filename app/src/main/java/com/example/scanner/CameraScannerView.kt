package com.example.scanner

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun CameraScannerView(
    modifier: Modifier = Modifier,
    isTorchOn: Boolean,
    onFlashlightAvailable: (Boolean) -> Unit,
    onQrDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var cameraInfo by remember { mutableStateOf<CameraInfo?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val isScanned = remember { AtomicBoolean(false) }

    // Toggle torch whenever isTorchOn changes
    DisposableEffect(isTorchOn, cameraControl, cameraInfo) {
        if (cameraInfo?.hasFlashUnit() == true) {
            try {
                cameraControl?.enableTorch(isTorchOn)
            } catch (e: Exception) {
                Log.w("CameraScannerView", "Torch control failed", e)
            }
        }
        onDispose {
            try {
                cameraControl?.enableTorch(false)
            } catch (_: Exception) {}
        }
    }

    // Infinite animation for scanner laser line
    val infiniteTransition = rememberInfiniteTransition(label = "LaserTransition")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserPosition"
    )

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                if (!isScanned.get()) {
                                    processImageProxy(imageProxy) { result ->
                                        if (isScanned.compareAndSet(false, true)) {
                                            provideFeedback(context)
                                            onQrDetected(result)
                                        }
                                    }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        val camera: Camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                        cameraControl = camera.cameraControl
                        cameraInfo = camera.cameraInfo
                        onFlashlightAvailable(camera.cameraInfo.hasFlashUnit())
                    } catch (e: Exception) {
                        Log.e("CameraScannerView", "Use case binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        // Viewfinder overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val boxSize = (canvasWidth * 0.72f).coerceAtMost(canvasHeight * 0.5f)
            val boxLeft = (canvasWidth - boxSize) / 2f
            val boxTop = (canvasHeight - boxSize) / 2.3f
            val boxRect = Rect(boxLeft, boxTop, boxLeft + boxSize, boxTop + boxSize)

            val overlayPath = Path().apply {
                addRect(Rect(0f, 0f, canvasWidth, canvasHeight))
            }
            val cutoutPath = Path().apply {
                addRoundRect(RoundRect(boxRect, CornerRadius(24f, 24f)))
            }

            // Dark semi-transparent scrim around viewfinder
            clipPath(cutoutPath, clipOp = ClipOp.Difference) {
                drawRect(Color(0x99000000))
            }

            // Corner brackets
            val cornerLength = 48f
            val strokeWidth = 8f
            val cornerColor = Color(0xFF10B981)

            // Top-Left
            drawLine(cornerColor, Offset(boxLeft, boxTop), Offset(boxLeft + cornerLength, boxTop), strokeWidth)
            drawLine(cornerColor, Offset(boxLeft, boxTop), Offset(boxLeft, boxTop + cornerLength), strokeWidth)

            // Top-Right
            drawLine(cornerColor, Offset(boxLeft + boxSize, boxTop), Offset(boxLeft + boxSize - cornerLength, boxTop), strokeWidth)
            drawLine(cornerColor, Offset(boxLeft + boxSize, boxTop), Offset(boxLeft + boxSize, boxTop + cornerLength), strokeWidth)

            // Bottom-Left
            drawLine(cornerColor, Offset(boxLeft, boxTop + boxSize), Offset(boxLeft + cornerLength, boxTop + boxSize), strokeWidth)
            drawLine(cornerColor, Offset(boxLeft, boxTop + boxSize), Offset(boxLeft, boxTop + boxSize - cornerLength), strokeWidth)

            // Bottom-Right
            drawLine(cornerColor, Offset(boxLeft + boxSize, boxTop + boxSize), Offset(boxLeft + boxSize - cornerLength, boxTop + boxSize), strokeWidth)
            drawLine(cornerColor, Offset(boxLeft + boxSize, boxTop + boxSize), Offset(boxLeft + boxSize, boxTop + boxSize - cornerLength), strokeWidth)

            // Animated Laser Line
            val laserY = boxTop + (boxSize * laserPosition)
            drawLine(
                color = Color(0xFF34D399),
                start = Offset(boxLeft + 16f, laserY),
                end = Offset(boxLeft + boxSize - 16f, laserY),
                strokeWidth = 4f
            )
        }
    }
}

private fun processImageProxy(imageProxy: ImageProxy, onDetected: (String) -> Unit) {
    try {
        val buffer: ByteBuffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val width = imageProxy.width
        val height = imageProxy.height

        val source = PlanarYUVLuminanceSource(
            bytes,
            width,
            height,
            0,
            0,
            width,
            height,
            false
        )

        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val reader = MultiFormatReader()
        val result = reader.decodeWithState(binaryBitmap)

        if (result != null && result.text.isNotBlank()) {
            onDetected(result.text)
        }
    } catch (_: Exception) {
        // Normal when no QR code is in frame
    } finally {
        imageProxy.close()
    }
}

private fun provideFeedback(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            @Suppress("DEPRECATION")
            vibrator?.vibrate(80)
        }
        val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        tone.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
    } catch (_: Exception) {}
}
