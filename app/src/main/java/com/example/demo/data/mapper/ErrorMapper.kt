package com.example.demo.data.mapper

import com.example.demo.domain.model.DataError
import retrofit2.HttpException
import java.io.IOException

fun Throwable.toDataError(): DataError = when (this) {
    is DataError -> this
    is IOException -> DataError.Network(this)
    is HttpException -> when (code()) {
        404 -> DataError.NotFound(this)
        403, 429 -> DataError.RateLimited(this)
        else -> DataError.Server(code(), this)
    }
    else -> DataError.Unknown(this)
}
