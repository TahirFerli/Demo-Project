package com.example.demo.ui.common

import androidx.annotation.StringRes
import com.example.demo.R
import com.example.demo.domain.model.DataError

/** Maps domain errors to user-facing strings. Kept in the UI layer (it knows about resources). */
@StringRes
fun Throwable.toErrorMessageRes(): Int = when (this) {
    is DataError.Network -> R.string.error_network
    is DataError.Server -> R.string.error_server
    else -> R.string.error_unknown
}
