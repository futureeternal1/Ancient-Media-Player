package player.music.ancient.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.Nullable
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import java.util.Collections
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ColorUtil {
    @JvmStatic
    @Nullable
    fun generatePalette(bitmap: Bitmap?): Palette? {
        if (bitmap == null) return null
        return Palette.from(bitmap).generate()
    }

    @JvmStatic
    @ColorInt
    fun getColor(@Nullable palette: Palette?, fallback: Int): Int {
        if (palette != null) {
            if (palette.vibrantSwatch != null) {
                return palette.vibrantSwatch!!.rgb
            } else if (palette.mutedSwatch != null) {
                return palette.mutedSwatch!!.rgb
            } else if (palette.darkVibrantSwatch != null) {
                return palette.darkVibrantSwatch!!.rgb
            } else if (palette.darkMutedSwatch != null) {
                return palette.darkMutedSwatch!!.rgb
            } else if (palette.lightVibrantSwatch != null) {
                return palette.lightVibrantSwatch!!.rgb
            } else if (palette.lightMutedSwatch != null) {
                return palette.lightMutedSwatch!!.rgb
            } else if (!palette.swatches.isEmpty()) {
                return Collections.max(palette.swatches, SwatchComparator.instance).rgb
            }
        }
        return fallback
    }

    private object SwatchComparator : Comparator<Palette.Swatch> {
        val instance = this
        override fun compare(lhs: Palette.Swatch, rhs: Palette.Swatch): Int {
            return lhs.population - rhs.population
        }
    }

    @JvmStatic
    fun desaturateColor(color: Int, ratio: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)

        hsv[1] = hsv[1] / 1 * ratio + 0.2f * (1.0f - ratio)

        return Color.HSVToColor(hsv)
    }

    @JvmStatic
    fun stripAlpha(@ColorInt color: Int): Int {
        return -0x1000000 or color
    }

    @JvmStatic
    @ColorInt
    fun shiftColor(@ColorInt color: Int, @FloatRange(from = 0.0, to = 2.0) by: Float): Int {
        if (by == 1f) return color
        val alpha = Color.alpha(color)
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] *= by // value component
        return (alpha shl 24) + (0x00ffffff and Color.HSVToColor(hsv))
    }

    @JvmStatic
    @ColorInt
    fun darkenColor(@ColorInt color: Int): Int {
        return shiftColor(color, 0.9f)
    }

    @JvmStatic
    @ColorInt
    fun darkenColorTheme(@ColorInt color: Int): Int {
        return shiftColor(color, 0.8f)
    }

    @JvmStatic
    @ColorInt
    fun lightenColor(@ColorInt color: Int): Int {
        return shiftColor(color, 1.1f)
    }

    @JvmStatic
    @ColorInt
    fun lightenColor(
        @ColorInt color: Int,
        value: Float
    ): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        hsl[2] += value
        hsl[2] = hsl[2].coerceIn(0f, 1f)
        return ColorUtils.HSLToColor(hsl)
    }

    @JvmStatic
    @ColorInt
    fun darkenColor(
        @ColorInt color: Int,
        value: Float
    ): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        hsl[2] -= value
        hsl[2] = hsl[2].coerceIn(0f, 1f)
        return ColorUtils.HSLToColor(hsl)
    }

    @JvmStatic
    @ColorInt
    fun getReadableColorLight(@ColorInt color: Int, @ColorInt bgColor: Int): Int {
        var foregroundColor = color
        while (ColorUtils.calculateContrast(foregroundColor, bgColor) <= 3.0
        ) {
            foregroundColor = darkenColor(foregroundColor, 0.1F)
        }
        return foregroundColor
    }

    @JvmStatic
    @ColorInt
    fun getReadableColorDark(@ColorInt color: Int, @ColorInt bgColor: Int): Int {
        var foregroundColor = color
        while (ColorUtils.calculateContrast(foregroundColor, bgColor) <= 3.0
        ) {
            foregroundColor = lightenColor(foregroundColor, 0.1F)
        }
        return foregroundColor
    }

    @JvmStatic
    fun isColorLight(@ColorInt color: Int): Boolean {
        val darkness =
            1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness < 0.4
    }

    @JvmStatic
    @ColorInt
    fun invertColor(@ColorInt color: Int): Int {
        val r = 255 - Color.red(color)
        val g = 255 - Color.green(color)
        val b = 255 - Color.blue(color)
        return Color.argb(Color.alpha(color), r, g, b)
    }

    @JvmStatic
    @ColorInt
    fun adjustAlpha(@ColorInt color: Int, @FloatRange(from = 0.0, to = 1.0) factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).roundToInt()
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(alpha, red, green, blue)
    }

    @JvmStatic
    @ColorInt
    fun withAlpha(@ColorInt baseColor: Int, @FloatRange(from = 0.0, to = 1.0) alpha: Float): Int {
        val a = min(255, max(0, (alpha * 255).toInt())) shl 24
        val rgb = 0x00ffffff and baseColor
        return a + rgb
    }

    /**
     * Taken from CollapsingToolbarLayout's CollapsingTextHelper class.
     */
    @JvmStatic
    fun blendColors(color1: Int, color2: Int, @FloatRange(from = 0.0, to = 1.0) ratio: Float): Int {
        val inverseRatio = 1f - ratio
        val a = Color.alpha(color1) * inverseRatio + Color.alpha(color2) * ratio
        val r = Color.red(color1) * inverseRatio + Color.red(color2) * ratio
        val g = Color.green(color1) * inverseRatio + Color.green(color2) * ratio
        val b = Color.blue(color1) * inverseRatio + Color.blue(color2) * ratio
        return Color.argb(a.toInt(), r.toInt(), g.toInt(), b.toInt())
    }

    private fun getColorDarkness(@ColorInt color: Int): Double {
        return if (color == Color.BLACK)
            1.0
        else if (color == Color.WHITE || color == Color.TRANSPARENT)
            0.0
        else
            1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
    }

    @JvmStatic
    @ColorInt
    fun getInverseColor(@ColorInt color: Int): Int {
        return 0xFFFFFF - color or -0x1
    }

    @JvmStatic
    fun isColorSaturated(@ColorInt color: Int): Boolean {
        val max = max(
            0.299 * Color.red(color),
            max(0.587 * Color.green(color), 0.114 * Color.blue(color))
        )
        val min = min(
            0.299 * Color.red(color),
            min(0.587 * Color.green(color), 0.114 * Color.blue(color))
        )
        val diff = abs(max - min)
        return diff > 20
    }

    @JvmStatic
    @ColorInt
    fun getMixedColor(@ColorInt color1: Int, @ColorInt color2: Int): Int {
        return Color.rgb(
            (Color.red(color1) + Color.red(color2)) / 2,
            (Color.green(color1) + Color.green(color2)) / 2,
            (Color.blue(color1) + Color.blue(color2)) / 2
        )
    }

    private fun getDifference(@ColorInt color1: Int, @ColorInt color2: Int): Double {
        var diff = abs(0.299 * (Color.red(color1) - Color.red(color2)))
        diff += abs(0.587 * (Color.green(color1) - Color.green(color2)))
        diff += abs(0.114 * (Color.blue(color1) - Color.blue(color2)))
        return diff
    }

    @JvmStatic
    @ColorInt
    fun getReadableText(@ColorInt textColor: Int, @ColorInt backgroundColor: Int): Int {
        return getReadableText(textColor, backgroundColor, 100)
    }

    @JvmStatic
    @ColorInt
    fun getReadableText(
        @ColorInt textColor: Int,
        @ColorInt backgroundColor: Int,
        difference: Int
    ): Int {
        var textColorFinal = textColor
        val isLight = isColorLight(backgroundColor)
        var i = 0
        while (getDifference(textColorFinal, backgroundColor) < difference && i < 100) {
            textColorFinal =
                getMixedColor(textColorFinal, if (isLight) Color.BLACK else Color.WHITE)
            i++
        }

        return textColorFinal
    }

    @JvmStatic
    @ColorInt
    fun getContrastColor(@ColorInt color: Int): Int {
        // Counting the perceptive luminance - human eye favors green color...
        val a =
            1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return if (a < 0.5) Color.BLACK else Color.WHITE
    }
}
