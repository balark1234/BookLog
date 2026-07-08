package com.booklog.app.data.remote

import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

object ApiErrorMapper {
    fun friendlyMessage(throwable: Throwable): String = when (throwable) {
        is HttpException -> when (throwable.code()) {
            404 -> "We couldn't find this book online. No worries — fill in the details below!"
            in 500..599 -> "Book lookup is having a moment. Try again or add the book manually."
            else -> "Couldn't look up this book right now. You can still add it yourself!"
        }
        is SocketTimeoutException, is IOException ->
            "No internet connection. You can still add the book manually!"
        else -> throwable.message?.takeIf { !it.contains("HTTP", ignoreCase = true) }
            ?: "Something went wrong. You can still add the book manually!"
    }
}