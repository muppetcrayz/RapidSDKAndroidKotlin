package com.dubtel.mobileapikotlin.extensions

import android.content.res.TypedArray

/**
 * Executes the given [block] function on this TypedArray and then recycles it.
 *
 * @see kotlin.io.use
 */
inline fun <R> TypedArray.use(block: (TypedArray) -> R): R {
    return block(this).also {
        recycle()
    }
}