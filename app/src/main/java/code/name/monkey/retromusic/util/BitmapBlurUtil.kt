/*
 * Copyright (c) 2019 Hemanth Savarala.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by
 *  the Free Software Foundation either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
package code.name.monkey.retromusic.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import androidx.annotation.ColorInt

/**
 * محرك تمويه مستقل بالكامل مخصص لشاشة تفاصيل الفنان بس، مش بيلمس ولا بيتشارك كود مع أي
 * حتة تانية في التطبيق. بيعمل Progressive Blur حقيقي (واضح فوق، وبيدخل في تمويه تدريجي
 * كل ما نزلنا) زي أبل ميوزك بالظبط، من غير الاعتماد على RenderScript (متوقف من جوجل)
 * أو RenderEffect (متاح بس من API 31)، عشان يشتغل على أي نسخة أندرويد.
 *
 * الفكرة: بدل ما نعمل تمويه واحد بدرجة ثابتة على الصورة كلها (اللي بيدي خط فاصل واضح
 * بين "واضح" و"مموّه")، بنجهّز 3 نسخ من الصورة بتموّهات متزايدة (خفيف/متوسط/تقيل)،
 * وبنركّبهم فوق بعض بأقنعة Gradient شفافية بتخليهم "يدخلوا" في بعض بنعومة، فمفيش قفزة
 * واحدة حادة، لكن انتقال متدرّج حقيقي.
 */
object BitmapBlurUtil {

    /**
     * بيرجّع نسخة من [source] فيها تمويه تدريجي: واضحة تمامًا لحد [blurStartFraction] من
     * ارتفاع الصورة، وبعدها بتدخل في تمويه بيزيد تدريجيًا لحد ما يوصل لأقصى تمويه عند
     * [blurEndFraction] (ولحد آخر الصورة).
     */
    fun applyProgressiveBlur(
        source: Bitmap,
        blurStartFraction: Float,
        blurEndFraction: Float
    ): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= 0 || height <= 0) return source

        // نصف قطر التمويه الأقصى بيتحسب نسبيًا لعرض الصورة، عشان يبان بنفس القوة
        // بصرف النظر عن دقة الصورة اللي جاية من Glide (ممكن تختلف من صورة لصورة)
        val maxRadius = (width * 0.055f).toInt().coerceIn(14, 55)
        val lightRadius = (maxRadius * 0.28f).toInt().coerceAtLeast(1)
        val mediumRadius = (maxRadius * 0.6f).toInt().coerceAtLeast(2)
        val heavyRadius = maxRadius

        val lightBlur = boxBlur(source, lightRadius)
        val mediumBlur = boxBlur(source, mediumRadius)
        val heavyBlur = boxBlur(source, heavyRadius)

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // القاعدة: الصورة الأصلية الواضحة تمامًا
        canvas.drawBitmap(source, 0f, 0f, null)

        val band = (blurEndFraction - blurStartFraction).coerceAtLeast(0.01f)

        // 3 طبقات بتدخل فوق بعض تدريجيًا، كل واحدة بتبدأ تظهر قبل ما اللي قبلها تخلص
        // (Overlap) عشان النعومة تبقى حقيقية من غير أي حدود بين درجة وتانية
        drawMaskedBlurLayer(
            canvas, lightBlur, width, height,
            fadeStart = blurStartFraction,
            fadeEnd = blurStartFraction + band * 0.35f
        )
        drawMaskedBlurLayer(
            canvas, mediumBlur, width, height,
            fadeStart = blurStartFraction + band * 0.20f,
            fadeEnd = blurStartFraction + band * 0.65f
        )
        drawMaskedBlurLayer(
            canvas, heavyBlur, width, height,
            fadeStart = blurStartFraction + band * 0.50f,
            fadeEnd = blurEndFraction
        )

        return result
    }

    /**
     * بيرسم طبقة مموّهة فوق الـ Canvas الحالي، بس بيخليها تظهر تدريجيًا (Alpha من 0 لـ 255)
     * بين [fadeStart] و [fadeEnd] من ارتفاع الصورة، باستخدام قناع Gradient شفافية
     * (PorterDuff.Mode.DST_IN) بدل ما تظهر فجأة بخط واضح.
     */
    private fun drawMaskedBlurLayer(
        canvas: Canvas,
        blurredLayer: Bitmap,
        width: Int,
        height: Int,
        fadeStart: Float,
        fadeEnd: Float
    ) {
        val saveCount = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        canvas.drawBitmap(blurredLayer, 0f, 0f, null)

        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            shader = LinearGradient(
                0f, fadeStart * height,
                0f, fadeEnd * height,
                Color.TRANSPARENT, Color.BLACK,
                Shader.TileMode.CLAMP // فوق fadeStart: شفاف بالكامل. تحت fadeEnd: ظاهر بالكامل
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), maskPaint)
        canvas.restoreToCount(saveCount)
    }

    /**
     * متوسط لون منطقة أفقية (Band) من الصورة بين [startFraction] و [endFraction] من
     * ارتفاعها. لازم تتنادى على الصورة *بعد* التمويه (نتيجة [applyProgressiveBlur]) مش قبله،
     * عشان اللون يبقى فعلاً نفس اللي هيبان على الشاشة وميبقاش فيه خط فاصل.
     */
    @ColorInt
    fun averageBandColor(bitmap: Bitmap, startFraction: Float, endFraction: Float): Int {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return Color.GRAY

        val startY = (height * startFraction).toInt().coerceIn(0, height - 1)
        val endY = (height * endFraction).toInt().coerceIn(startY + 1, height)

        // بنعيّن (Sample) بدل ما نمشي على كل بكسل، عشان العملية تبقى سريعة حتى على
        // صور بدقة عالية
        val stepX = (width / 60).coerceAtLeast(1)
        val stepY = ((endY - startY) / 40).coerceAtLeast(1)

        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        var count = 0L

        var y = startY
        while (y < endY) {
            var x = 0
            while (x < width) {
                val px = bitmap.getPixel(x, y)
                rSum += Color.red(px)
                gSum += Color.green(px)
                bSum += Color.blue(px)
                count++
                x += stepX
            }
            y += stepY
        }

        return if (count == 0L) Color.GRAY
        else Color.rgb((rSum / count).toInt(), (gSum / count).toInt(), (bSum / count).toInt())
    }

    // ---------------------------------------------------------------------
    // Box Blur نقي (من غير RenderScript/RenderEffect) — Sliding Window بزمن O(1) لكل بكسل
    // بصرف النظر عن نصف قطر التمويه. بيتنفّذ 3 مرات متتالية (أفقي + رأسي) عشان يقرّب
    // شكل Gaussian Blur الحقيقي من غير التعقيد الحسابي بتاعه.
    // ---------------------------------------------------------------------

    private fun boxBlur(source: Bitmap, radius: Int): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        if (radius > 0) {
            repeat(3) {
                boxBlurHorizontal(pixels, width, height, radius)
                boxBlurVertical(pixels, width, height, radius)
            }
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    private fun boxBlurHorizontal(pixels: IntArray, width: Int, height: Int, radius: Int) {
        val temp = IntArray(width)
        val windowSize = radius * 2 + 1

        for (y in 0 until height) {
            val rowStart = y * width
            var aSum = 0
            var rSum = 0
            var gSum = 0
            var bSum = 0

            for (i in -radius..radius) {
                val x = i.coerceIn(0, width - 1)
                val px = pixels[rowStart + x]
                aSum += (px ushr 24) and 0xFF
                rSum += (px ushr 16) and 0xFF
                gSum += (px ushr 8) and 0xFF
                bSum += px and 0xFF
            }

            for (x in 0 until width) {
                temp[x] = ((aSum / windowSize) shl 24) or
                        ((rSum / windowSize) shl 16) or
                        ((gSum / windowSize) shl 8) or
                        (bSum / windowSize)

                val addX = (x + radius + 1).coerceIn(0, width - 1)
                val removeX = (x - radius).coerceIn(0, width - 1)
                val addPx = pixels[rowStart + addX]
                val removePx = pixels[rowStart + removeX]
                aSum += ((addPx ushr 24) and 0xFF) - ((removePx ushr 24) and 0xFF)
                rSum += ((addPx ushr 16) and 0xFF) - ((removePx ushr 16) and 0xFF)
                gSum += ((addPx ushr 8) and 0xFF) - ((removePx ushr 8) and 0xFF)
                bSum += (addPx and 0xFF) - (removePx and 0xFF)
            }

            System.arraycopy(temp, 0, pixels, rowStart, width)
        }
    }

    private fun boxBlurVertical(pixels: IntArray, width: Int, height: Int, radius: Int) {
        val temp = IntArray(height)
        val windowSize = radius * 2 + 1

        for (x in 0 until width) {
            var aSum = 0
            var rSum = 0
            var gSum = 0
            var bSum = 0

            for (i in -radius..radius) {
                val y = i.coerceIn(0, height - 1)
                val px = pixels[y * width + x]
                aSum += (px ushr 24) and 0xFF
                rSum += (px ushr 16) and 0xFF
                gSum += (px ushr 8) and 0xFF
                bSum += px and 0xFF
            }

            for (y in 0 until height) {
                temp[y] = ((aSum / windowSize) shl 24) or
                        ((rSum / windowSize) shl 16) or
                        ((gSum / windowSize) shl 8) or
                        (bSum / windowSize)

                val addY = (y + radius + 1).coerceIn(0, height - 1)
                val removeY = (y - radius).coerceIn(0, height - 1)
                val addPx = pixels[addY * width + x]
                val removePx = pixels[removeY * width + x]
                aSum += ((addPx ushr 24) and 0xFF) - ((removePx ushr 24) and 0xFF)
                rSum += ((addPx ushr 16) and 0xFF) - ((removePx ushr 16) and 0xFF)
                gSum += ((addPx ushr 8) and 0xFF) - ((removePx ushr 8) and 0xFF)
                bSum += (addPx and 0xFF) - (removePx and 0xFF)
            }

            for (y in 0 until height) {
                pixels[y * width + x] = temp[y]
            }
        }
    }
}
