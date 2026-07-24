package com.sun.kmpstartertemplaterefined.feature_live_data.remote

import com.sun.kmpstartertemplaterefined.feature_live_data.remote.dto.*
import com.sun.kmpstartertemplaterefined.feature_live_domain.exceptions.JoinLiveException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

class LiveRemoteDataSourceImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : LiveRemoteDataSource {

    override suspend fun getLiveCourses(status: String): List<LiveCourseDto> {
        val response = httpClient
            .get("$baseUrl/live-courses") {
                parameter("status", status)
            }
            .body<LiveCoursesResponseDto>()
        return response.data.data
    }

    override suspend fun joinLive(liveId: String, clientType: String): JoinLiveDataDto {
        try {
            val response = httpClient
                .post("$baseUrl/lives/$liveId/join") {
                    contentType(ContentType.Application.Json)
                    setBody(JoinLiveRequestDto(clientType = clientType))
                }
                .body<JoinLiveResponseDto>()
            return response.data
        } catch (e: ResponseException) {
            throw when (e.response.status) {
                HttpStatusCode.Unauthorized -> JoinLiveException.Unauthorized
                HttpStatusCode.Forbidden -> JoinLiveException.LiveNotStarted
                HttpStatusCode.NotFound -> JoinLiveException.LiveNotFound
                HttpStatusCode.BadRequest -> JoinLiveException.MalformedRequest
                else -> JoinLiveException.Unknown(e.response.status.value, e.message)
            }
        }
    }
}