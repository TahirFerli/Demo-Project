package com.example.demo.data.mapper

import com.example.demo.domain.model.DataError
import retrofit2.HttpException
import java.io.IOException

fun Throwable.toDataError(): DataError = when (this) {
    is DataError -> this
    is IOException -> DataError.Network(this)
    is HttpException -> DataError.Server(code(), this)
    else -> DataError.Unknown(this)
}
