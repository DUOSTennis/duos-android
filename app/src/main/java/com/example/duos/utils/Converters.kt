package com.example.duos.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.TypeConverter
import java.io.ByteArrayOutputStream
import android.R
import androidx.compose.ui.graphics.PointMode


class Converters {
    // Bitmap -> ByteArray 변환
    @TypeConverter
    fun toByteArray(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }

    // ByteArray -> Bitmap 변환
    @TypeConverter
    fun toBitmap(bytes: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }


    // 이게 맞나 싶네




}