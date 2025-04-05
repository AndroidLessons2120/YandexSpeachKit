package com.example.yandexspeechkittesting

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.example.yandexspeechkittesting.ui.theme.YandexSpeechKitTestingTheme
import java.io.IOException

class MainActivity : ComponentActivity() {
    private var output: String? = null
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording by mutableStateOf(false)
    private val viewModel: YandexSpeechViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {

            YandexSpeechKitTestingTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column {
                            Text(viewModel.answerState.value)
                            Button(
                                onClick = {
                                    if (isRecording) {
                                        stopRecording()
                                    } else {
                                        // Проверяем разрешения
                                        if (ContextCompat.checkSelfPermission(
                                                this@MainActivity,
                                                Manifest.permission.RECORD_AUDIO
                                            ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                                                this@MainActivity,
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                                            ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                                                this@MainActivity,
                                                Manifest.permission.READ_EXTERNAL_STORAGE
                                            ) != PackageManager.PERMISSION_GRANTED
                                        ) {
                                            // Если их нет, то запрашиваем
                                            Log.d("MainActivity", "Permissions not granted")
                                            val permissions = arrayOf(
                                                Manifest.permission.RECORD_AUDIO,
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                Manifest.permission.READ_EXTERNAL_STORAGE
                                            )
                                            ActivityCompat.requestPermissions(
                                                this@MainActivity,
                                                permissions,
                                                0
                                            )
                                            Log.d("MainActivity", "Permissions request finished")

                                        } else {
                                            Log.d(
                                                "MainActivity",
                                                "Permissions granted; start recording"
                                            )

                                            startRecording()
                                        }
                                    }
                                }) {
                                Text(text = if (isRecording) "stopRecording" else "startRecording")
                            }
                            Text(text = if (isRecording) "Recording" else "")
                        }
                    }
                }
            }
        }
    }

    /**
     * Запускаем запись
     */
    private fun startRecording() {
        // Указываем куда сохранить .mp3 файл (пока это ещё не ogg)
        output =
            Environment.getExternalStorageDirectory().absolutePath + "/recording_${System.currentTimeMillis()}.mp3"

        /**
        Создаем mediaRecorder
        В зависимости от версии sdk в него нужно или не нужно передавать контекст,
        на чемпионате тебе не обязательно помнить всю эту конструкцию, можно просто создать MediaRecorder(this),
        передав ему контекст, IDE ругнется, что так не будет работать на старых версиях
        и в контекстном меню предложит сгенерить проверку версии,
        ну а при старой версии просто создать его без указания контекста
         */
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            MediaRecorder()
        }

        // Конфигурируем рекордер
        mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder?.setOutputFile(output)

        // пробуем запустить запись
        try {
            mediaRecorder?.prepare()
            mediaRecorder?.start()
            isRecording = true
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun stopRecording() {
        // Проверяем идет ли запись
        if (isRecording) {
            // если да, то завершаем её
            mediaRecorder?.stop()
            mediaRecorder?.release()
            isRecording = false

            // И конвертируем в нужный формат
            convertMp3ToOgg(output!!, output!!.replace("mp3", "ogg")){
                // Здесь отправляем файл на сервер
                viewModel.sendFileToDescribe(it)
            }
        }
    }
}


/**
 * Эта функция конвертнет наш .mp3 файл в .ogg
 * @param inputPath путь до mp3 файла
 * @param outputPath путь куда будет сохранен .ogg
 */
fun convertMp3ToOgg(inputPath: String, outputPath: String, onSuccess:  (outFileName: String) -> Unit) {
    /**
     * Эту строку можно взять перейдя в реализацию [FFmpegKit.executeAsync] на самом верху
     * в оригинале она выглядит так:
     * FFmpegSession asyncSession = FFmpegKit. executeAsync("-i file1.mp4 -c:v libxvid file1.avi", completeCallback);
     * тебе нужна из нее только сама строка, а также заменить название файлов на пути
     */
    val cmd = "-i $inputPath -c:v libxvid $outputPath"

    // Используем библиотеку FFmpegKit и запускаем у нее команду написанную выше
    FFmpegKit.executeAsync(cmd) { session ->
        val returnCode = session.returnCode // Получаем код результата конвертации

        /*
        И проверяем все ли прошло хорошо
        Здесь в идеале, надо что-то отобразить для пользователя в случае ошибки,
        а в случае успешной конвертации отправить файл на сервер
         */
        if (returnCode.isValueSuccess) {
            Log.d("Converter", "Converted is successfully!")
            onSuccess.invoke(outputPath) // Вызываем заранее переданный callback
        } else {
            Log.d("Converter", "Convert error :(")
        }
    }
}
