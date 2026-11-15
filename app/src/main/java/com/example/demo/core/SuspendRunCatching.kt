package com.example.demo.core

import kotlin.coroutines.cancellation.CancellationException

/**
 * Like [runCatching] but safe for coroutines: the stdlib version also catches
 * [CancellationException], which silently breaks structured concurrency
 * (a cancelled coroutine would keep running as if it "failed").
 */
inline fun <T> suspendRunCatching(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
