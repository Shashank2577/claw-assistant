package com.phoneclaw.ai.common

import com.google.gson.Gson
import java.net.URL

data class JsonResponse<T>(val jsonObj: T, val textContent: String)

fun <T> getJsonResponse(url: String, clazz: Class<T>): JsonResponse<T>? {
    return try {
        val text = URL(url).readText()
        val jsonObj = Gson().fromJson(text, clazz)
        JsonResponse(jsonObj, text)
    } catch (e: Exception) {
        null
    }
}

inline fun <reified T> getJsonResponse(url: String): JsonResponse<T>? {
    return getJsonResponse(url, T::class.java)
}
