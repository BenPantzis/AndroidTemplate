package com.template.android.core.data.util

import com.template.android.core.common.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

inline fun <Local, Network> networkBoundResource(
    crossinline query: () -> Flow<Local>,
    crossinline fetch: suspend () -> Network,
    crossinline saveFetchResult: suspend (Network) -> Unit,
    crossinline shouldFetch: (Local) -> Boolean = { true },
): Flow<Result<Local>> = flow {
    emit(Result.Loading)
    val cached = query().first()
    if (shouldFetch(cached)) {
        emit(Result.Success(cached, isRefreshing = true))
        try {
            saveFetchResult(fetch())
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            emit(Result.Error(t))
            emitAll(query().map { Result.Success(it) })
            return@flow
        }
    }
    emitAll(query().map { Result.Success(it) })
}
