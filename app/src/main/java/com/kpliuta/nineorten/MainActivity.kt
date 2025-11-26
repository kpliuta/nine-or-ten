package com.kpliuta.nineorten

import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import com.kpliuta.nineorten.ui.theme.NineOrTenTheme
import kotlin.math.min

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NineOrTenTheme {
                DrumMachineScreen()
            }
        }
    }
}

@Composable
private fun drawableBitmap(@DrawableRes resId: Int): ImageBitmap? {
    val context = LocalContext.current
    return remember(resId) {
        val options = BitmapFactory.Options().apply {
            inScaled = false
        }
        BitmapFactory.decodeResource(context.resources, resId, options)?.asImageBitmap()
    }
}

// Coordinates from Photoshop
private val loopButtonRects = listOf(
    IntRect(70, 239, 70 + 242, 239 + 148), // Loop 1
    IntRect(323, 239, 323 + 242, 239 + 148), // Loop 2
    IntRect(576, 239, 576 + 242, 239 + 148), // Loop 3
    IntRect(829, 239, 829 + 242, 239 + 148) // Loop 4
)

private val drumPadRects = listOf(
    // Row 1
    IntRect(68, 429, 68 + 250, 429 + 250),
    IntRect(320, 429, 320 + 250, 429 + 250),
    IntRect(572, 429, 572 + 250, 429 + 250),
    IntRect(823, 429, 823 + 250, 429 + 250),
    // Row 2
    IntRect(68, 684, 68 + 250, 684 + 250),
    IntRect(320, 684, 320 + 250, 684 + 250),
    IntRect(572, 684, 572 + 250, 684 + 250),
    IntRect(823, 684, 823 + 250, 684 + 250),
    // Row 3
    IntRect(68, 939, 68 + 250, 939 + 250),
    IntRect(320, 939, 320 + 250, 939 + 250),
    IntRect(572, 939, 572 + 250, 939 + 250),
    IntRect(823, 939, 823 + 250, 939 + 250)
)

@Composable
fun DrumMachineScreen() {
    val idleBitmap = drawableBitmap(R.drawable.idle)
    val pressedBitmap = drawableBitmap(R.drawable.pressed)

    var loopStates by remember { mutableStateOf(List(4) { false }) }
    var padStates by remember { mutableStateOf(List(12) { false }) }

    val context = LocalContext.current

    val loopMediaPlayers = remember {
        loopButtonRects.mapIndexed { index, _ ->
            val soundId =
                context.resources.getIdentifier("loop${index + 1}", "raw", context.packageName)
            if (soundId != 0) MediaPlayer.create(context, soundId)?.apply {
                isLooping = true
            } else null
        }
    }

    val padMediaPlayers = remember {
        drumPadRects.mapIndexed { index, _ ->
            val soundId =
                context.resources.getIdentifier("sound${index + 1}", "raw", context.packageName)
            if (soundId != 0) MediaPlayer.create(context, soundId) else null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            loopMediaPlayers.forEach { it?.release() }
            padMediaPlayers.forEach { it?.release() }
        }
    }

    padMediaPlayers.forEachIndexed { index, mediaPlayer ->
        mediaPlayer?.setOnCompletionListener {
            padStates = padStates.toMutableList().also { it[index] = false }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .pointerInput(idleBitmap) {
                if (idleBitmap == null) return@pointerInput

                val imageWidth = idleBitmap.width.toFloat()
                val imageHeight = idleBitmap.height.toFloat()

                detectTapGestures { offset ->
                    val canvasWidth = size.width.toFloat()
                    val canvasHeight = size.height.toFloat()
                    val scaleX = canvasWidth / imageWidth
                    val scaleY = canvasHeight / imageHeight
                    val scale = min(scaleX, scaleY)
                    val scaledWidth = imageWidth * scale
                    val scaledHeight = imageHeight * scale
                    val offsetX = (canvasWidth - scaledWidth) / 2
                    val offsetY = (canvasHeight - scaledHeight) / 2

                    fun checkTap(rects: List<IntRect>, onTapped: (Int) -> Unit) {
                        rects.forEachIndexed { index, rect ->
                            val scaledRect = Rect(
                                left = offsetX + rect.left * scale,
                                top = offsetY + rect.top * scale,
                                right = offsetX + rect.right * scale,
                                bottom = offsetY + rect.bottom * scale
                            )
                            if (scaledRect.contains(offset)) {
                                onTapped(index)
                            }
                        }
                    }

                    checkTap(loopButtonRects) { index ->
                        val player = loopMediaPlayers[index] ?: return@checkTap
                        val newStates = loopStates.toMutableList()
                        newStates[index] = !newStates[index]
                        loopStates = newStates
                        if (newStates[index]) {
                            player.start()
                        } else {
                            if (player.isPlaying) {
                                player.pause()
                                player.seekTo(0)
                            }
                        }
                    }

                    checkTap(drumPadRects) { index ->
                        val player = padMediaPlayers[index] ?: return@checkTap
                        if (player.isPlaying) {
                            player.seekTo(0)
                        } else {
                            player.playbackParams = player.playbackParams.setSpeed(1.3f)
                            player.start()
                        }
                        padStates = padStates.toMutableList().also { it[index] = true }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (idleBitmap == null || pressedBitmap == null) return@Canvas

            val canvasWidth = size.width
            val canvasHeight = size.height
            val imageWidth = idleBitmap.width.toFloat()
            val imageHeight = idleBitmap.height.toFloat()

            val scaleX = canvasWidth / imageWidth
            val scaleY = canvasHeight / imageHeight
            val scale = min(scaleX, scaleY)
            val scaledWidth = (imageWidth * scale).toInt()
            val scaledHeight = (imageHeight * scale).toInt()
            val offsetX = ((canvasWidth - scaledWidth) / 2).toInt()
            val offsetY = ((canvasHeight - scaledHeight) / 2).toInt()

            drawImage(
                image = idleBitmap,
                dstOffset = IntOffset(offsetX, offsetY),
                dstSize = IntSize(scaledWidth, scaledHeight)
            )

            fun drawPressedStates(rects: List<IntRect>, states: List<Boolean>) {
                rects.forEachIndexed { index, rect ->
                    if (states[index]) {
                        drawImage(
                            image = pressedBitmap,
                            srcOffset = IntOffset(rect.left, rect.top),
                            srcSize = IntSize(rect.width, rect.height),
                            dstOffset = IntOffset(
                                offsetX + (rect.left * scale).toInt(),
                                offsetY + (rect.top * scale).toInt()
                            ),
                            dstSize = IntSize(
                                (rect.width * scale).toInt(),
                                (rect.height * scale).toInt()
                            )
                        )
                    }
                }
            }

            drawPressedStates(loopButtonRects, loopStates)
            drawPressedStates(drumPadRects, padStates)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DrumMachineScreenPreview() {
    NineOrTenTheme {
        DrumMachineScreen()
    }
}
