package com.dubtel.mobileapikotlin.extensions

import android.graphics.Canvas
import android.graphics.Matrix

// from https://github.com/android/android-ktx/blob/master/src/main/java/androidx/core/graphics/Canvas.kt

/**
 * Wrap the specified [block] in calls to [Canvas.save]
 * and [Canvas.restoreToCount].
 */
inline fun Canvas.withSave(block: Canvas.() -> Unit) {
    val checkpoint = save()
    try {
        block()
    } finally {
        restoreToCount(checkpoint)
    }
}

/**
 * Wrap the specified [block] in calls to [Canvas.save]/[Canvas.translate]
 * and [Canvas.restoreToCount].
 */
inline fun Canvas.withTranslation(
        x: Float = 0.0f,
        y: Float = 0.0f,
        block: Canvas.() -> Unit
) {
    val checkpoint = save()
    translate(x, y)
    try {
        block()
    } finally {
        restoreToCount(checkpoint)
    }
}

/**
 * Wrap the specified [block] in calls to [Canvas.save]/[Canvas.rotate]
 * and [Canvas.restoreToCount].
 */
inline fun Canvas.withRotation(
        degrees: Float = 0.0f,
        pivotX: Float = 0.0f,
        pivotY: Float = 0.0f,
        block: Canvas.() -> Unit
) {
    val checkpoint = save()
    rotate(degrees, pivotX, pivotY)
    try {
        block()
    } finally {
        restoreToCount(checkpoint)
    }
}

/**
 * Wrap the specified [block] in calls to [Canvas.save]/[Canvas.scale]
 * and [Canvas.restoreToCount].
 */
inline fun Canvas.withScale(
        x: Float = 1.0f,
        y: Float = 1.0f,
        pivotX: Float = 0.0f,
        pivotY: Float = 0.0f,
        block: Canvas.() -> Unit
) {
    val checkpoint = save()
    scale(x, y, pivotX, pivotY)
    try {
        block()
    } finally {
        restoreToCount(checkpoint)
    }
}

/**
 * Wrap the specified [block] in calls to [Canvas.save]/[Canvas.skew]
 * and [Canvas.restoreToCount].
 */
inline fun Canvas.withSkew(
        x: Float = 0.0f,
        y: Float = 0.0f,
        block: Canvas.() -> Unit
) {
    val checkpoint = save()
    skew(x, y)
    try {
        block()
    } finally {
        restoreToCount(checkpoint)
    }
}

/**
 * Wrap the specified [block] in calls to [Canvas.save]/[Canvas.concat]
 * and [Canvas.restoreToCount].
 */
inline fun Canvas.withMatrix(
        matrix: Matrix = Matrix(),
        block: Canvas.() -> Unit
) {
    val checkpoint = save()
    concat(matrix)
    try {
        block()
    } finally {
        restoreToCount(checkpoint)
    }
}