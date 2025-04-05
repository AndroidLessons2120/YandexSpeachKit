package com.example.yandexspeechkittesting

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Здесь все без архитектуры и так как написано здесь делать не надо :)
 * Это просто пример как использовать ktor дя отправки нашего запроса
 */
class YandexSpeechViewModel : ViewModel() {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true // Игнорировать неизвестные поля
                prettyPrint = true
            })
        }
    }

    val answerState = mutableStateOf("")

    fun sendFileToDescribe(filePath: String) {
        viewModelScope.launch {
            val response = client.post("https://stt.api.cloud.yandex.net/speech/v1/stt:recognize") {
                url {
                    parameters.append("topic", "general")
                    parameters.append("lang", "en-US")
                    parameters.append("folderId", "<сюда короткий id>")
                }
                headers {
                    append(
                        HttpHeaders.Authorization,
                        "Bearer <сюда длинный токен>"
                    )
                }
                setBody(File(filePath).readBytes())
                header(HttpHeaders.ContentType, ContentType.parse("audio/ogg"))
            }
            println(response.bodyAsText())
            val result: ServerResponse = response.body()
            answerState.value = result.result
        }
    }
}

@Serializable
data class ServerResponse(val result: String)