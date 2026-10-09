package com.novacorp.inmonode_app.core.network

import com.google.gson.Gson
import retrofit2.Response

/**
 * Error body returned by every backend endpoint, e.g.
 * {"code":"INVALID_CREDENTIALS","message":"...","details":null}.
 */
data class ErrorResourceDto(
    val code: String,
    val message: String,
    val details: String?
)

private val gson = Gson()

/** Parses the error body of an unsuccessful response, or returns null if it is not an ErrorResource. */
fun Response<*>.errorResource(): ErrorResourceDto? = try {
    errorBody()?.charStream()?.use { reader -> gson.fromJson(reader, ErrorResourceDto::class.java) }
} catch (e: Exception) {
    null
}
