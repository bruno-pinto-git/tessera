package com.tessera.android.data

import org.json.JSONArray
import org.json.JSONObject

internal fun JSONObject.optStringOrNull(key: String): String? =
    if (!has(key) || isNull(key)) null else getString(key)

internal fun JSONObject.optLongOrNull(key: String): Long? =
    if (!has(key) || isNull(key)) null else getLong(key)

internal fun JSONObject.optIntOrNull(key: String): Int? =
    if (!has(key) || isNull(key)) null else getInt(key)

internal fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
    (0 until length()).map { transform(getJSONObject(it)) }

internal fun JSONArray.toStringList(): List<String> =
    (0 until length()).map { getString(it) }
