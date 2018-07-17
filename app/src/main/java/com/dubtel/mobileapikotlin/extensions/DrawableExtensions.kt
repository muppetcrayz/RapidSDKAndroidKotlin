package com.dubtel.mobileapikotlin.extensions

import android.graphics.drawable.Drawable

fun Drawable.setBoundsToSizeWithPadding(size: Int, padding: Int) {
    setBounds(padding, padding, padding + size, padding + size)
}