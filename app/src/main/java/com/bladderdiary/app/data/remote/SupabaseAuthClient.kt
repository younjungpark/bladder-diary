package com.bladderdiary.app.data.remote

import com.bladderdiary.app.BuildConfig
import com.bladderdiary.app.data.remote.dto.AuthUserDto
import com.bladderdiary.app.domain.model.SocialProvider
import com.bladderdiary.app.domain.model.UserSession
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class SupabaseAuthClient {
    private val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val anonKey = BuildConfig.SUPABASE_ANON_KEY
    private val redirectUri = BuildConfig.SUPABASE_REDIRECT_URI

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                }
            )
        }
    }

    fun buildOAuthSignInUrl(provider: SocialProvider): String {
        return URLBuilder("$baseUrl/auth/v1/authorize").apply {
            parameters.append("provider", provider.providerKey)
            parameters.append("redirect_to", redirectUri)
        }.buildString()
    }

    suspend fun createSessionFromCallback(
        callbackUrl: String,
        fallbackProvider: String? = null
    ): UserSession {
        val uri = android.net.Uri.parse(callbackUrl)
        val fragmentMap = uri.fragment.toParameterMap()
        val queryMap = uri.query.toParameterMap()
        val error = fragmentMap["error"] ?: queryMap["error"]
        val errorDescription = fragmentMap["error_description"] ?: queryMap["error_description"]
        if (!error.isNullOrBlank()) {
            throw IllegalStateException(
                buildString {
                    append("OAuth 로그인 실패: ")
                    append(error)
                    if (!errorDescription.isNullOrBlank()) {
                        append(" (")
                        append(errorDescription)
                        append(")")
                    }
                }
            )
        }

        val accessToken = fragmentMap["access_token"] ?: queryMap["access_token"]
            ?: throw IllegalStateException("OAuth 액세스 토큰이 없습니다.")
        val refreshToken = fragmentMap["refresh_token"] ?: queryMap["refresh_token"].orEmpty()
        val user = fetchUser(accessToken)

        return UserSession(
            userId = user.id,
            accessToken = accessToken,
            refreshToken = refreshToken,
            email = user.email,
            provider = resolveProvider(
                preferredProvider = user.appMetadata?.provider,
                fallbackProvider = fallbackProvider
            )
        )
    }

    private suspend fun fetchUser(accessToken: String): AuthUserDto {
        return client.get("$baseUrl/auth/v1/user") {
            header("apikey", anonKey)
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }.body()
    }
}

internal fun resolveProvider(
    preferredProvider: String?,
    fallbackProvider: String?
): String? {
    val normalizedPreferred = preferredProvider?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
    val normalizedFallback = fallbackProvider?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
    return when {
        normalizedPreferred == null -> normalizedFallback
        normalizedPreferred == "email" && normalizedFallback in setOf("google", "kakao") -> normalizedFallback
        else -> normalizedPreferred
    }
}

private fun String?.toParameterMap(): Map<String, String> {
    if (this.isNullOrBlank()) return emptyMap()
    return split("&")
        .mapNotNull { part ->
            if (part.isBlank()) return@mapNotNull null
            val idx = part.indexOf('=')
            val key = if (idx >= 0) part.substring(0, idx) else part
            val value = if (idx >= 0) part.substring(idx + 1) else ""
            android.net.Uri.decode(key) to android.net.Uri.decode(value)
        }
        .toMap()
}
