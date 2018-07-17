package com.dubtel.mobileapikotlin.extensions

import android.text.Spannable
import android.text.Spanned.SPAN_INCLUSIVE_EXCLUSIVE

/**
 * Add [span] to the [range] of the text.
 *
 * ```
 * val s = "Hello, World!".toSpannable()
 * s[0..5] = UnderlineSpan()
 * ```
 *
 * Note: The range end value is exclusive.
 *
 * @see Spannable.setSpan
 */
inline operator fun Spannable.set(range: IntRange, span: Any) {
    // This looks weird, but endInclusive is just the exact upper value.
    setSpan(span, range.start, range.endInclusive, SPAN_INCLUSIVE_EXCLUSIVE)
}