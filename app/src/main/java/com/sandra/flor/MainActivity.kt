package com.sandra.flor

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.*
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class MainActivity : Activity() {
    private lateinit var view: FlowerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = FlowerView(this)
        setContentView(view)
    }
    override fun onDestroy() {
        view.release()
        super.onDestroy()
    }
}

class FlowerView(context: Context) : View(context) {
    private enum class Screen { COVER, FLOWER }
    private var screen = Screen.COVER
    private val d = resources.displayMetrics.density
    private fun dp(v: Float) = v * d

    private val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(95, 60, 18)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    }
    private val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(125, 90, 42)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
    }
    private val button = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(211, 157, 24) }
    private val white = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val petal = Paint(Paint.ANTI_ALIAS_FLAG)
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.4f)
        color = Color.rgb(183, 126, 0)
    }
    private val petalText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(92, 61, 0)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val photoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    private val photoBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(3f)
        color = Color.rgb(224, 168, 28)
    }

    private val opened = BooleanArray(12)
    private val coverButton = RectF()
    private val photo = BitmapFactory.decodeResource(resources, R.drawable.pareja)
    private val melody = RomanticMelody()

    private var cx = 0f
    private var cy = 0f
    private var centerR = 0f
    private var petalDistance = 0f

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        drawBackground(c)
        if (screen == Screen.COVER) drawCover(c) else drawFlower(c)
    }

    private fun drawBackground(c: Canvas) {
        val p = Paint()
        p.shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            intArrayOf(
                Color.rgb(255, 251, 232),
                Color.rgb(255, 240, 176),
                Color.rgb(255, 249, 226)
            ),
            null,
            Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), p)
    }

    private fun drawCover(c: Canvas) {
        val x = width / 2f
        title.textSize = min(dp(34f), width * .085f)
        sub.textSize = min(dp(17f), width * .043f)
        c.drawText("Para Sandra Moreira", x, dp(58f), title)
        c.drawText("Una flor amarilla para alguien que llegó bonito", x, dp(88f), sub)

        val w = min(width * .86f, dp(360f))
        val h = min(height * .50f, dp(430f))
        val r = RectF(x - w / 2, dp(115f), x + w / 2, dp(115f) + h)
        drawPhoto(c, r)

        sub.textSize = dp(15f)
        c.drawText("Te conozco hace poco, pero tienes mucho de lo que buscaba.", x, r.bottom + dp(34f), sub)

        val bw = min(width * .76f, dp(310f))
        coverButton.set(x - bw / 2, r.bottom + dp(55f), x + bw / 2, r.bottom + dp(113f))
        c.drawRoundRect(coverButton, dp(29f), dp(29f), button)
        white.textSize = dp(17f)
        c.drawText("Abrir nuestra flor", x, coverButton.centerY() + dp(6f), white)

        sub.textSize = dp(13f)
        c.drawText("12 pétalos · 12 poemas · una melodía para ti", x, height - dp(20f), sub)
    }

    private fun drawPhoto(c: Canvas, dst: RectF) {
        val path = Path().apply { addRoundRect(dst, dp(24f), dp(24f), Path.Direction.CW) }
        c.save()
        c.clipPath(path)
        val srcRatio = photo.width.toFloat() / photo.height
        val dstRatio = dst.width() / dst.height()
        val src = if (srcRatio > dstRatio) {
            val nw = (photo.height * dstRatio).toInt()
            val left = (photo.width - nw) / 2
            Rect(left, 0, left + nw, photo.height)
        } else {
            val nh = (photo.width / dstRatio).toInt()
            val top = (photo.height - nh) / 2
            Rect(0, top, photo.width, top + nh)
        }
        c.drawBitmap(photo, src, dst, photoPaint)
        c.restore()
        c.drawRoundRect(dst, dp(24f), dp(24f), photoBorder)
    }

    private fun drawFlower(c: Canvas) {
        val x = width / 2f
        title.textSize = min(dp(30f), width * .075f)
        sub.textSize = dp(14f)
        c.drawText("Sandra Moreira", x, dp(48f), title)
        c.drawText("Toca cada pétalo cerrado y descubre su poema", x, dp(74f), sub)

        val thumb = RectF(x - dp(78f), dp(92f), x + dp(78f), dp(210f))
        drawPhoto(c, thumb)

        cx = x
        cy = (thumb.bottom + height - dp(70f)) / 2f + dp(28f)
        centerR = dp(45f)
        petalDistance = min(width * .28f, dp(145f))
        val pwClosed = dp(42f)
        val pwOpen = dp(62f)
        val ph = dp(108f)

        for (i in 0 until 12) {
            val angle = i * 30f
            c.save()
            c.translate(cx, cy)
            c.rotate(angle)

            val pw = if (opened[i]) pwOpen else pwClosed
            val out = if (opened[i]) dp(12f) else 0f
            val rect = RectF(-pw / 2, -(petalDistance + out + ph), pw / 2, -(petalDistance + out))

            petal.shader = LinearGradient(
                0f, rect.top, 0f, rect.bottom,
                if (opened[i]) Color.rgb(255, 239, 128) else Color.rgb(247, 191, 31),
                if (opened[i]) Color.rgb(255, 250, 207) else Color.rgb(255, 225, 94),
                Shader.TileMode.CLAMP
            )
            c.drawOval(rect, petal)
            petal.shader = null
            c.drawOval(rect, border)

            petalText.textSize = dp(14f)
            c.save()
            c.rotate(-angle, 0f, rect.centerY())
            c.drawText(if (opened[i]) "♥" else "${i + 1}", 0f, rect.centerY() + dp(5f), petalText)
            c.restore()
            c.restore()
        }

        centerPaint.shader = RadialGradient(
            cx - centerR * .25f, cy - centerR * .25f, centerR * 1.3f,
            if (melody.isPlaying)
                intArrayOf(Color.rgb(235, 182, 67), Color.rgb(136, 83, 17))
            else
                intArrayOf(Color.rgb(210, 155, 44), Color.rgb(103, 63, 16)),
            null, Shader.TileMode.CLAMP
        )
        c.drawCircle(cx, cy, centerR, centerPaint)
        centerPaint.shader = null
        white.textSize = dp(26f)
        c.drawText(if (melody.isPlaying) "II" else "♫", cx, cy + dp(9f), white)

        sub.textSize = dp(13f)
        c.drawText("Centro: música · pétalos abiertos: ♥", x, height - dp(20f), sub)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action != MotionEvent.ACTION_UP) return true

        if (screen == Screen.COVER) {
            if (coverButton.contains(e.x, e.y)) {
                screen = Screen.FLOWER
                invalidate()
            }
            return true
        }

        val dx = e.x - cx
        val dy = e.y - cy
        val dist = hypot(dx, dy)

        if (dist <= centerR * 1.25f) {
            if (melody.isPlaying) melody.pause() else melody.play()
            invalidate()
            return true
        }

        if (dist > centerR && dist < petalDistance + dp(125f)) {
            var deg = Math.toDegrees(atan2(dx.toDouble(), -dy.toDouble())).toFloat()
            if (deg < 0) deg += 360f
            val idx = (((deg + 15f) % 360f) / 30f).toInt().coerceIn(0, 11)
            opened[idx] = true
            invalidate()
            val poem = poems[idx]
            AlertDialog.Builder(context)
                .setTitle("Pétalo ${idx + 1} · ${poem.title}")
                .setMessage("${poem.text}\n\nAutor: ${poem.author}")
                .setPositiveButton("Cerrar", null)
                .show()
        }
        return true
    }

    fun release() {
        melody.release()
        photo.recycle()
    }
}

class RomanticMelody {
    private val sampleRate = 22050
    private var track: AudioTrack? = null
    var isPlaying = false
        private set

    private val notes = doubleArrayOf(
        261.63, 329.63, 392.00, 523.25,
        493.88, 392.00, 329.63, 293.66,
        329.63, 392.00, 440.00, 392.00,
        329.63, 293.66, 261.63, 329.63
    )

    private fun build(): ShortArray {
        val noteSeconds = .42
        val pauseSeconds = .06
        val noteSamples = (sampleRate * noteSeconds).toInt()
        val pauseSamples = (sampleRate * pauseSeconds).toInt()
        val all = ShortArray((noteSamples + pauseSamples) * notes.size)
        var pos = 0
        for (freq in notes) {
            for (i in 0 until noteSamples) {
                val fade = min(1.0, min(i / 900.0, (noteSamples - i) / 900.0))
                val v = sin(2.0 * Math.PI * freq * i / sampleRate) * .20 * fade
                all[pos++] = (v * Short.MAX_VALUE).toInt().toShort()
            }
            repeat(pauseSamples) { all[pos++] = 0 }
        }
        return all
    }

    fun play() {
        if (track == null) {
            val data = build()
            val bytes = data.size * 2
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(bytes)
                .build().apply {
                    write(data, 0, data.size)
                    setLoopPoints(0, data.size, -1)
                }
        }
        track?.play()
        isPlaying = true
    }

    fun pause() {
        track?.pause()
        isPlaying = false
    }

    fun release() {
        track?.release()
        track = null
        isPlaying = false
    }
}
