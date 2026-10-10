package com.novacorp.inmonode_app.core.network

import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException

/** Cancellation must propagate; otherwise a cancelled sync could be reported as successful. */
internal suspend fun <T> operationResult(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (exception: CancellationException) {
    throw exception
} catch (exception: ApiOperationException) {
    Result.failure(exception)
} catch (exception: IOException) {
    Result.failure(IOException("No se pudo conectar. Tus registros siguen guardados en este dispositivo.", exception))
} catch (exception: Exception) {
    Result.failure(exception)
}

internal fun <T> Response<T>.requireBody(): T {
    if (!isSuccessful) throw ApiOperationException(code(), errorResource()?.message ?: "El servidor no pudo completar la operación (${'$'}{code()}).")
    return body() ?: throw IOException("El servidor devolvió una respuesta vacía.")
}

class ApiOperationException(val statusCode: Int, message: String) : IOException(message)
