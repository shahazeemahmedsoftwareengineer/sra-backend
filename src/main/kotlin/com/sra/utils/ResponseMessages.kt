package com.sra.utils

object ResponseMessages {
    const val SUCCESS = "Operation was successful"
    const val CREATED = "Resource was created successfully"
    const val NOT_FOUND = "The requested resource was not found"
    const val BAD_REQUEST = "The request was malformed or invalid"
    const val UNAUTHORIZED = "Authentication is required and has failed or has not yet been provided"
    const val FORBIDDEN = "You do not have permission to access this resource"
    const val INTERNAL_ERROR = "An unexpected error occurred on the server"
}
