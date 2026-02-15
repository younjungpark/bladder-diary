package com.bladderdiary.app.data.remote

import com.bladderdiary.app.BuildConfig
import com.bladderdiary.app.data.remote.dto.AuthRequest
import com.bladderdiary.app.data.remote.dto.AuthResponseDto
import com.bladderdiary.app.data.remote.dto.SoftDeleteRequestDto
import com.bladderdiary.app.data.remote.dto.VoidingEventRemoteDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class SupabaseApi {
    private val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val anonKey = BuildConfig.SUPABASE_ANON_KEY

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

    suspend fun signUp(email: String, password: String): AuthResponseDto {
        val response = client.post("$baseUrl/auth/v1/signup") {
            contentType(ContentType.Application.Json)
            header("apikey", anonKey)
            setBody(AuthRequest(email, password))
        }
        if (response.status !in listOf(HttpStatusCode.OK, HttpStatusCode.Created)) {
            throw IllegalStateException("회원가입 실패: ${response.bodyAsText()}")
        }
        return response.body()
    }

    suspend fun signIn(email: String, password: String): AuthResponseDto {
        val response = client.post("$baseUrl/auth/v1/token?grant_type=password") {
            contentType(ContentType.Application.Json)
            header("apikey", anonKey)
            setBody(AuthRequest(email, password))
        }
        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("로그인 실패: ${response.bodyAsText()}")
        }
        return response.body()
    }

    suspend fun upsertVoidingEvent(accessToken: String, event: VoidingEventRemoteDto) {
        val response = client.post("$baseUrl/rest/v1/voiding_events?on_conflict=id") {
            contentType(ContentType.Application.Json)
            header("apikey", anonKey)
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header("Prefer", "resolution=merge-duplicates,return=minimal")
            setBody(listOf(event))
        }
        if (response.status !in listOf(HttpStatusCode.OK, HttpStatusCode.Created, HttpStatusCode.NoContent)) {
            throw IllegalStateException("이벤트 업로드 실패: ${response.bodyAsText()}")
        }
    }

    suspend fun softDeleteVoidingEvent(accessToken: String, id: String, userId: String, deletedAtIso: String) {
        val response = client.patch("$baseUrl/rest/v1/voiding_events?id=eq.$id&user_id=eq.$userId") {
            contentType(ContentType.Application.Json)
            header("apikey", anonKey)
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header("Prefer", "return=minimal")
            setBody(SoftDeleteRequestDto(deletedAtIso))
        }
        if (response.status !in listOf(HttpStatusCode.OK, HttpStatusCode.NoContent)) {
            throw IllegalStateException("이벤트 삭제 동기화 실패: ${response.bodyAsText()}")
        }
    }
}
