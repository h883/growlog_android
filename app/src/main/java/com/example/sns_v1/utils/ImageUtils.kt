package com.example.sns_v1.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream

/**
 * 端末のギャラリー画像を読み込み、長辺 [maxDimen] px に収まる JPEG バイト列へ変換する。
 * HEIC など Worker 側が受け付けない形式も JPEG に統一され、アップロードサイズも抑えられる。
 * 読み込めなかった場合は null。
 */
fun compressImageForUpload(
    context: Context,
    uri: Uri,
    maxDimen: Int = 1600,
    quality: Int = 85
): ByteArray? {
    val resolver = context.contentResolver

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    // inJustDecodeBounds = true のとき decodeStream は必ず null を返す。
    // その戻り値で成否を判定してはいけない（サイズは bounds 側に入る）
    val boundsStream = resolver.openInputStream(uri) ?: return null
    boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    // まず粗く間引いてデコードし、巨大な写真での OOM を避ける
    var sample = 1
    while (bounds.outWidth / sample > maxDimen * 2 || bounds.outHeight / sample > maxDimen * 2) {
        sample *= 2
    }

    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        ?: return null

    val rotation = resolver.openInputStream(uri)?.use { readExifRotation(it) } ?: 0f

    val scale = maxDimen.toFloat() / maxOf(decoded.width, decoded.height)
    val matrix = Matrix().apply {
        if (scale < 1f) postScale(scale, scale)
        if (rotation != 0f) postRotate(rotation)
    }

    val bitmap = if (matrix.isIdentity) {
        decoded
    } else {
        Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    }

    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
    if (bitmap !== decoded) bitmap.recycle()
    decoded.recycle()
    return out.toByteArray()
}

private fun readExifRotation(input: java.io.InputStream): Float = runCatching {
    when (ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
}.getOrDefault(0f)
