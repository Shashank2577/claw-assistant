/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.phoneclaw.ai.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream

/**
 * Cleanup the error message from MediaPipe task.
 */
fun cleanUpMediapipeTaskErrorMessage(errorMessage: String): String {
  var message = errorMessage
  if (errorMessage.contains("NOT_FOUND:")) {
    message = errorMessage.substringAfter("NOT_FOUND:").trim()
  }
  if (message.contains("The model file path is invalid")) {
    message = "The model file path is invalid. Please make sure the model is downloaded."
  }
  return message
}

fun getOrientation(inputStream: InputStream): Int {
  val exifInterface = ExifInterface(inputStream)
  return exifInterface.getAttributeInt(
    ExifInterface.TAG_ORIENTATION,
    ExifInterface.ORIENTATION_NORMAL
  )
}

/** Decodes bitmap from uri with max width and height. */
fun decodeBitmap(context: Context, uri: Uri, maxWidth: Int, maxHeight: Int): Bitmap? {
  var inputStream = context.contentResolver.openInputStream(uri) ?: return null
  val options = BitmapFactory.Options()
  options.inJustDecodeBounds = true
  BitmapFactory.decodeStream(inputStream, null, options)
  inputStream.close()

  var inSampleSize = 1
  if (options.outHeight > maxHeight || options.outWidth > maxWidth) {
    val halfHeight = options.outHeight / 2
    val halfWidth = options.outWidth / 2
    while (halfHeight / inSampleSize >= maxHeight && halfWidth / inSampleSize >= maxWidth) {
      inSampleSize *= 2
    }
  }

  inputStream = context.contentResolver.openInputStream(uri) ?: return null
  val orientation = getOrientation(inputStream)
  inputStream.close()

  inputStream = context.contentResolver.openInputStream(uri) ?: return null
  val decodeOptions = BitmapFactory.Options()
  decodeOptions.inSampleSize = inSampleSize
  val bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
  inputStream.close()

  return bitmap?.let { rotateBitmap(it, orientation) }
}

private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
  val matrix = Matrix()
  when (orientation) {
    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
    else -> return bitmap
  }
  return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
