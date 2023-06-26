package com.levi.tellmeajokeapp.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class JokesResponse(
    @Json(name = "jokes") val jokes: List<Joke>
)
@JsonClass(generateAdapter = true)
data class Joke(
    @Json(name = "id") val id: Int,
    @Json(name = "punchline") val punchline: String,
    @Json(name = "setup") val setup: String,
    @Json(name = "type") val type: Category
) {
    enum class Category {
        programming,
        general
    }
}