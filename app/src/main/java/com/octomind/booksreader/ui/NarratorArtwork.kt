package com.octomind.booksreader.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.octomind.booksreader.R
import com.octomind.booksreader.domain.NarratorAvatar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

private data class NarratorArtworkSpec(
    @DrawableRes val drawableResource: Int,
    @StringRes val descriptionResource: Int,
    val bookTopFraction: Float,
)

@Composable
internal fun BookColoredNarratorArtwork(
    avatar: NarratorAvatar,
    coverImagePath: String?,
    fallbackBookColor: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val spec = narratorArtworkSpec(avatar) ?: return
    val coloredArtwork by produceState<Bitmap?>(
        initialValue = null,
        avatar,
        coverImagePath,
        fallbackBookColor,
    ) {
        value =
            withContext(Dispatchers.IO) {
                createBookColoredNarratorBitmap(
                    context = context,
                    avatar = avatar,
                    coverImagePath = coverImagePath,
                    fallbackBookColor = fallbackBookColor,
                )
            }
    }
    val description = stringResource(spec.descriptionResource)
    coloredArtwork?.let { bitmap ->
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = description,
            contentScale = ContentScale.Fit,
            modifier = modifier,
        )
    } ?: Image(
        painter = painterResource(spec.drawableResource),
        contentDescription = description,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

internal fun createBookColoredNarratorBitmap(
    context: Context,
    avatar: NarratorAvatar,
    coverImagePath: String?,
    fallbackBookColor: Int,
): Bitmap? {
    return narratorArtworkSpec(avatar)
        ?.let { spec ->
            decodeSampledResource(context, spec.drawableResource, MAX_ARTWORK_EDGE)?.let { artwork ->
                val coverColor =
                    coverImagePath
                        ?.let(::File)
                        ?.takeIf(File::isFile)
                        ?.let(::dominantCoverColor)
                        ?: fallbackBookColor
                recolorBook(artwork, coverColor, spec.bookTopFraction).also { artwork.recycle() }
            }
        }
}

private fun recolorBook(
    source: Bitmap,
    targetColor: Int,
    bookTopFraction: Float,
): Bitmap {
    val output = source.copy(Bitmap.Config.ARGB_8888, true)
    val pixels = IntArray(output.width * output.height)
    output.getPixels(pixels, 0, output.width, 0, 0, output.width, output.height)
    val targetHsv = FloatArray(3).also { Color.colorToHSV(targetColor, it) }
    val sourceHsv = FloatArray(3)
    val firstBookRow = (output.height * bookTopFraction).toInt()
    for (y in firstBookRow until output.height) {
        val rowStart = y * output.width
        for (x in 0 until output.width) {
            val index = rowStart + x
            val pixel = pixels[index]
            if (Color.alpha(pixel) >= MINIMUM_VISIBLE_ALPHA) {
                Color.colorToHSV(pixel, sourceHsv)
                if (sourceHsv[0] in BOOK_HUE_RANGE && sourceHsv[1] >= MINIMUM_BOOK_SATURATION) {
                    val shade = sourceHsv[2] / REFERENCE_BOOK_BRIGHTNESS
                    val recoloredHsv =
                        floatArrayOf(
                            targetHsv[0],
                            max(targetHsv[1], MINIMUM_TARGET_SATURATION),
                            (targetHsv[2] * shade).coerceIn(
                                MINIMUM_OUTPUT_BRIGHTNESS,
                                MAXIMUM_OUTPUT_BRIGHTNESS,
                            ),
                        )
                    pixels[index] = Color.HSVToColor(Color.alpha(pixel), recoloredHsv)
                }
            }
        }
    }
    output.setPixels(pixels, 0, output.width, 0, 0, output.width, output.height)
    return output
}

private fun dominantCoverColor(file: File): Int? {
    val bitmap = decodeSampledFile(file, MAX_COVER_EDGE) ?: return null
    return try {
        val buckets = IntArray(COLOR_BUCKET_COUNT)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        pixels.forEach { pixel ->
            if (Color.alpha(pixel) >= MINIMUM_VISIBLE_ALPHA) {
                val red = Color.red(pixel) shr COLOR_BUCKET_SHIFT
                val green = Color.green(pixel) shr COLOR_BUCKET_SHIFT
                val blue = Color.blue(pixel) shr COLOR_BUCKET_SHIFT
                buckets[(red shl RED_BUCKET_OFFSET) or (green shl GREEN_BUCKET_OFFSET) or blue] += 1
            }
        }
        buckets.indices
            .maxByOrNull { index -> colorScore(index, buckets[index]) }
            ?.takeIf { buckets[it] > 0 }
            ?.let(::bucketCenterColor)
    } finally {
        bitmap.recycle()
    }
}

private fun colorScore(bucket: Int, population: Int): Double {
    if (population == 0) return 0.0
    val color = bucketCenterColor(bucket)
    val hsv = FloatArray(3).also { Color.colorToHSV(color, it) }
    val usefulBrightness = if (hsv[2] in USEFUL_BRIGHTNESS_RANGE) {
        FULL_SCORE_WEIGHT
    } else {
        MUTED_SCORE_WEIGHT
    }
    return population * (BASE_SATURATION_SCORE + hsv[1] * SATURATION_SCORE_WEIGHT) * usefulBrightness
}

private fun bucketCenterColor(bucket: Int): Int {
    val red =
        (((bucket shr RED_BUCKET_OFFSET) and COLOR_BUCKET_MASK) shl COLOR_BUCKET_SHIFT) +
            COLOR_BUCKET_CENTER
    val green =
        (((bucket shr GREEN_BUCKET_OFFSET) and COLOR_BUCKET_MASK) shl COLOR_BUCKET_SHIFT) +
            COLOR_BUCKET_CENTER
    val blue = ((bucket and COLOR_BUCKET_MASK) shl COLOR_BUCKET_SHIFT) + COLOR_BUCKET_CENTER
    return Color.rgb(red, green, blue)
}

private fun decodeSampledResource(
    context: Context,
    resource: Int,
    maximumEdge: Int,
): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeResource(context.resources, resource, bounds)
    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maximumEdge)
    }
    return BitmapFactory.decodeResource(context.resources, resource, options)
}

private fun decodeSampledFile(
    file: File,
    maximumEdge: Int,
): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maximumEdge)
    }
    return BitmapFactory.decodeFile(file.absolutePath, options)
}

private fun sampleSize(
    width: Int,
    height: Int,
    maximumEdge: Int,
): Int {
    var sample = 1
    while (width / sample > maximumEdge || height / sample > maximumEdge) {
        sample *= SAMPLE_MULTIPLIER
    }
    return sample
}

private fun narratorArtworkSpec(avatar: NarratorAvatar): NarratorArtworkSpec? =
    when (avatar) {
        NarratorAvatar.OCTI ->
            NarratorArtworkSpec(
                R.drawable.octi_reader,
                R.string.focus_mascot_description,
                OCTI_BOOK_TOP_FRACTION,
            )
        NarratorAvatar.LOVECRAFT_ILLUSTRATION ->
            NarratorArtworkSpec(
                R.drawable.lovecraft_narrator_illustration,
                R.string.lovecraft_illustration_description,
                HUMAN_BOOK_TOP_FRACTION,
            )
        NarratorAvatar.SCHOPENHAUER_ILLUSTRATION ->
            NarratorArtworkSpec(
                R.drawable.schopenhauer_narrator_illustration,
                R.string.schopenhauer_illustration_description,
                HUMAN_BOOK_TOP_FRACTION,
            )
        NarratorAvatar.NIETZSCHE_ILLUSTRATION ->
            NarratorArtworkSpec(
                R.drawable.nietzsche_narrator_illustration,
                R.string.nietzsche_illustration_description,
                HUMAN_BOOK_TOP_FRACTION,
            )
        NarratorAvatar.CAMUS_ILLUSTRATION ->
            NarratorArtworkSpec(
                R.drawable.camus_narrator_illustration,
                R.string.camus_illustration_description,
                HUMAN_BOOK_TOP_FRACTION,
            )
        NarratorAvatar.STRANGER_ILLUSTRATION ->
            NarratorArtworkSpec(
                R.drawable.stranger_narrator_illustration,
                R.string.stranger_illustration_description,
                HUMAN_BOOK_TOP_FRACTION,
            )
        NarratorAvatar.LILA_ILLUSTRATION ->
            NarratorArtworkSpec(
                R.drawable.lila_narrator_illustration,
                R.string.lila_illustration_description,
                CAT_BOOK_TOP_FRACTION,
            )
        NarratorAvatar.ACHU_ILLUSTRATION ->
            NarratorArtworkSpec(
                R.drawable.achu_narrator_illustration,
                R.string.achu_illustration_description,
                CAT_BOOK_TOP_FRACTION,
            )
        NarratorAvatar.FRANK_N_FURTER_ILLUSTRATION ->
            NarratorArtworkSpec(
                R.drawable.frank_n_furter_narrator_illustration,
                R.string.frank_n_furter_illustration_description,
                FRANK_BOOK_TOP_FRACTION,
            )
        NarratorAvatar.CUSTOM_IMAGE -> null
    }

private val BOOK_HUE_RANGE = 174f..211f
private val USEFUL_BRIGHTNESS_RANGE = 0.08f..0.92f
private const val MAX_ARTWORK_EDGE = 768
private const val MAX_COVER_EDGE = 128
private const val MINIMUM_VISIBLE_ALPHA = 180
private const val MINIMUM_BOOK_SATURATION = 0.28f
private const val MINIMUM_TARGET_SATURATION = 0.18f
private const val REFERENCE_BOOK_BRIGHTNESS = 0.62f
private const val MINIMUM_OUTPUT_BRIGHTNESS = 0.08f
private const val MAXIMUM_OUTPUT_BRIGHTNESS = 0.98f
private const val COLOR_BUCKET_SHIFT = 4
private const val COLOR_BUCKET_MASK = 0x0F
private const val COLOR_BUCKET_CENTER = 8
private const val COLOR_BUCKET_COUNT = 4_096
private const val RED_BUCKET_OFFSET = 8
private const val GREEN_BUCKET_OFFSET = 4
private const val SAMPLE_MULTIPLIER = 2
private const val FULL_SCORE_WEIGHT = 1.0
private const val MUTED_SCORE_WEIGHT = 0.35
private const val BASE_SATURATION_SCORE = 0.45
private const val SATURATION_SCORE_WEIGHT = 1.4
private const val OCTI_BOOK_TOP_FRACTION = 0.50f
private const val HUMAN_BOOK_TOP_FRACTION = 0.56f
private const val CAT_BOOK_TOP_FRACTION = 0.54f
private const val FRANK_BOOK_TOP_FRACTION = 0.62f
