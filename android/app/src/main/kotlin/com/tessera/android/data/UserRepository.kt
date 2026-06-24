package com.tessera.android.data

import android.content.Context
import com.tessera.android.data.dto.UserDto
import com.tessera.android.shared.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.http4k.client.OkHttp
import org.http4k.core.Method
import org.http4k.core.Request
import org.json.JSONArray
import org.json.JSONObject

class UserRepository(context: Context) {

    private val client = OkHttp()
    private val keycloak = KeycloakClient(context)

    suspend fun list(): List<UserDto> = withContext(Dispatchers.IO) {
        JSONArray(fetch("/api/v1/users?max=100")).mapObjects(::parseUser)
    }

    suspend fun search(query: String): List<UserDto> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return emptyList()
        return list().filter { u ->
            listOfNotNull(u.username, u.email, u.firstName, u.lastName).any { it.lowercase().contains(q) }
        }
    }

    suspend fun create(
        username: String,
        email: String,
        firstName: String,
        lastName: String,
        password: String,
        role: String,
    ) = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("username", username)
            .apply { if (email.isNotBlank()) put("email", email) }
            .put("firstName", firstName)
            .put("lastName", lastName)
            .put("password", password)
            .put("role", role)
            .toString()
        mutate(Method.POST, "/api/v1/users", body)
        Unit
    }

    suspend fun updateUser(id: String, email: String?, firstName: String, lastName: String, role: String?) =
        withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("firstName", firstName)
                .put("lastName", lastName)
                .apply {
                    if (!email.isNullOrBlank()) put("email", email)
                    if (role != null) put("role", role)
                }
                .toString()
            mutate(Method.PATCH, "/api/v1/users/$id", body)
            Unit
        }

    suspend fun setEnabled(id: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        mutate(Method.PATCH, "/api/v1/users/$id", JSONObject().put("enabled", enabled).toString())
        Unit
    }

    suspend fun forcePasswordReset(id: String) = withContext(Dispatchers.IO) {
        mutate(Method.PATCH, "/api/v1/users/$id", JSONObject().put("forcePasswordReset", true).toString())
        Unit
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        mutate(Method.DELETE, "/api/v1/users/$id", null)
        Unit
    }

    private fun parseUser(o: JSONObject) = UserDto(
        id = o.getString("id"),
        username = o.optStringOrNull("username"),
        email = o.optStringOrNull("email"),
        firstName = o.optStringOrNull("firstName"),
        lastName = o.optStringOrNull("lastName"),
        enabled = o.optBoolean("enabled", true),
        roles = if (o.has("roles") && !o.isNull("roles")) o.getJSONArray("roles").toStringList() else emptyList(),
    )

    private suspend fun fetch(path: String): String = mutate(Method.GET, path, null)

    private suspend fun mutate(method: Method, path: String, jsonBody: String?): String {
        val token = keycloak.freshAccessToken()
        var req = Request(method, "${ServerConfig.baseUrl}$path").header("Accept", "application/json")
        if (token != null) req = req.header("Authorization", "Bearer $token")
        if (jsonBody != null) req = req.header("Content-Type", "application/json").body(jsonBody)
        val resp = client(req)
        if (!resp.status.successful) throw IllegalStateException("Erro ${resp.status.code}")
        return resp.bodyString()
    }
}
