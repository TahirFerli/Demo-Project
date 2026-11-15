package com.example.demo.domain.model

/**
 * Domain-level errors. The data layer translates framework exceptions
 * (IOException, HttpException, ...) into these, so upper layers never
 * depend on Retrofit/OkHttp types.
 */
sealed class DataError(cause: Throwable? = null) : Exception(cause) {
    class Network(cause: Throwable? = null) : DataError(cause)
    class NotFound(cause: Throwable? = null) : DataError(cause)
    /** The API's firewall blocks clients that send too many requests (403/429). */
    class RateLimited(cause: Throwable? = null) : DataError(cause)
    class Server(val code: Int, cause: Throwable? = null) : DataError(cause)
    class Unknown(cause: Throwable? = null) : DataError(cause)
}
