package com.sra.utils

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)

@Serializable
data class ErrorResponse(
    val success: Boolean = false,
    val message: String,
    val errors: List<String> = emptyList()
)

object ResponseMessages {
    const val SUCCESS = "Success"
    const val CREATED = "Created successfully"
    const val NOT_FOUND = "Not found"
    const val UNAUTHORIZED = "Unauthorized"
    const val FORBIDDEN = "Forbidden"
    const val INVALID_REQUEST = "Invalid request"
    const val SERVER_ERROR = "Internal server error"
    const val DUPLICATE_ENTRY = "Already exists"
}