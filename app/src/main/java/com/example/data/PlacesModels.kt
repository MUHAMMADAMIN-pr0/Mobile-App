package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PlacesResponse(
    @Json(name = "results") val results: List<PlaceResult>,
    @Json(name = "next_page_token") val nextPageToken: String?,
    @Json(name = "status") val status: String,
    @Json(name = "error_message") val errorMessage: String?
)

@JsonClass(generateAdapter = true)
data class PlaceResult(
    @Json(name = "place_id") val placeId: String,
    @Json(name = "name") val name: String,
    @Json(name = "vicinity") val vicinity: String?,
    @Json(name = "geometry") val geometry: PlaceGeometry,
    @Json(name = "opening_hours") val openingHours: PlaceOpeningHours?,
    @Json(name = "rating") val rating: Double?,
    @Json(name = "user_ratings_total") val userRatingsTotal: Int?
)

@JsonClass(generateAdapter = true)
data class PlaceGeometry(
    @Json(name = "location") val location: LatLngLiteral
)

@JsonClass(generateAdapter = true)
data class LatLngLiteral(
    @Json(name = "lat") val lat: Double,
    @Json(name = "lng") val lng: Double
)

@JsonClass(generateAdapter = true)
data class PlaceOpeningHours(
    @Json(name = "open_now") val openNow: Boolean?
)

data class MosquePlace(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val openNow: Boolean?,
    val rating: Double,
    val ratingCount: Int,
    val distanceMeters: Int = 0
)
