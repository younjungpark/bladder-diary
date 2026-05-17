package com.bladderdiary.app.data.drive

import com.bladderdiary.app.data.backup.BACKUP_FILE_NAME
import com.bladderdiary.app.data.backup.BackupJson
import com.bladderdiary.app.data.backup.BackupNetworkException
import com.bladderdiary.app.data.backup.BackupNotFoundException
import com.bladderdiary.app.data.backup.BackupPermissionException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

class KtorDriveBackupFileClient(private val client: HttpClient = defaultClient()) :
    DriveBackupFileClient {
    override suspend fun uploadLatestBackup(
        accessToken: String,
        backupJson: String
    ): DriveBackupFileMetadata {
        val existing = findLatestBackupFile(accessToken)
        return if (existing == null) {
            createBackupFile(accessToken, backupJson)
        } else {
            updateBackupFile(accessToken, existing.fileId, backupJson)
        }
    }

    override suspend fun downloadLatestBackup(accessToken: String): String {
        val existing = findLatestBackupFile(accessToken) ?: throw BackupNotFoundException()
        val response = client.get("$DRIVE_BASE/files/${existing.fileId}") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            url {
                parameters.append("alt", "media")
            }
        }
        handleDriveFailure(response, "Google Drive 백업 다운로드 실패")
        return response.bodyAsText()
    }

    private suspend fun findLatestBackupFile(accessToken: String): DriveBackupFileMetadata? {
        val response = client.get("$DRIVE_BASE/files") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            url {
                parameters.append("spaces", "appDataFolder")
                parameters.append(
                    "q",
                    "'appDataFolder' in parents and " +
                        "name = '$BACKUP_FILE_NAME' and trashed = false"
                )
                parameters.append("fields", "files(id,name,modifiedTime)")
                parameters.append("pageSize", "1")
                parameters.append("orderBy", "modifiedTime desc")
            }
        }
        handleDriveFailure(response, "Google Drive 백업 파일 조회 실패")
        val files = response.body<DriveFileListDto>().files
        return files.firstOrNull()?.toMetadata()
    }

    private suspend fun createBackupFile(
        accessToken: String,
        backupJson: String
    ): DriveBackupFileMetadata {
        val metadata = DriveFileCreateDto(
            name = BACKUP_FILE_NAME,
            parents = listOf("appDataFolder")
        )
        val response = client.post("$DRIVE_UPLOAD_BASE/files") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            url {
                parameters.append("uploadType", "multipart")
                parameters.append("fields", "id,name,modifiedTime")
            }
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            key = "metadata",
                            value = BackupJson.encodeToString(metadata),
                            headers = Headers.build {
                                append(
                                    HttpHeaders.ContentType,
                                    ContentType.Application.Json.toString()
                                )
                            }
                        )
                        append(
                            key = "file",
                            value = backupJson,
                            headers = Headers.build {
                                append(
                                    HttpHeaders.ContentType,
                                    ContentType.Application.Json.toString()
                                )
                            }
                        )
                    }
                )
            )
        }
        handleDriveFailure(response, "Google Drive 백업 생성 실패")
        return response.body<DriveFileDto>().toMetadata()
    }

    private suspend fun updateBackupFile(
        accessToken: String,
        fileId: String,
        backupJson: String
    ): DriveBackupFileMetadata {
        val response = client.patch("$DRIVE_UPLOAD_BASE/files/$fileId") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            url {
                parameters.append("uploadType", "media")
                parameters.append("fields", "id,name,modifiedTime")
            }
            setBody(backupJson)
        }
        handleDriveFailure(response, "Google Drive 백업 갱신 실패")
        return response.body<DriveFileDto>().toMetadata()
    }

    private suspend fun handleDriveFailure(response: HttpResponse, message: String) {
        if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created) {
            return
        }
        when (response.status) {
            HttpStatusCode.Unauthorized,
            HttpStatusCode.Forbidden -> throw BackupPermissionException(
                "$message: 권한이 없거나 권한이 만료되었습니다."
            )

            HttpStatusCode.NotFound -> throw BackupNotFoundException()

            else -> throw BackupNetworkException("$message: ${response.bodyAsText()}")
        }
    }

    private fun DriveFileDto.toMetadata(): DriveBackupFileMetadata = DriveBackupFileMetadata(
        fileId = id,
        fileName = name,
        modifiedTime = modifiedTime
    )

    @Serializable
    private data class DriveFileListDto(val files: List<DriveFileDto> = emptyList())

    @Serializable
    private data class DriveFileDto(
        val id: String,
        val name: String,
        val modifiedTime: String? = null
    )

    @Serializable
    private data class DriveFileCreateDto(val name: String, val parents: List<String>)

    private companion object {
        private const val DRIVE_BASE = "https://www.googleapis.com/drive/v3"
        private const val DRIVE_UPLOAD_BASE = "https://www.googleapis.com/upload/drive/v3"

        private fun defaultClient(): HttpClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(BackupJson)
            }
        }
    }
}
