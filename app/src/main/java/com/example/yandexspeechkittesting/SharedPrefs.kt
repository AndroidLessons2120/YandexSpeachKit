package com.example.yandexspeechkittesting

import android.content.Context
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.core.content.edit

class SharedPrefs(context: Context) {

    companion object {
        private const val PREFS_KEY = "PREFS KEY"
        private const val ON_BOARDING_KEY = "ON BOARDING"
    }

    private val sharedPrefs = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)

    /**
     * В качестве домашки попробуй переписать логику работы поля, чтобы все сохранялось не в set<String>, а просто в строке, по типу:
     * <image-id>/<title>/<description>|<image-id>/<title>/<description>|<image-id>/<title>/<description>
     */
    var onBoardingQueue: List<OnBoardingScreenInfo>
        get() {
            val items =
                sharedPrefs.getStringSet(ON_BOARDING_KEY, setOf()) ?: setOf<String>().toList()

            return items.map<String, Pair<Int, OnBoardingScreenInfo>> {
                Log.d("SharedPrefs", "get from shared prefs data: $it")
                /*
                формат хранения
                 "1|2817471841874711/Some text/Some big big big lorem ipsum text"
                 */
                val (index, data) = it.split("|") // Отделяем индекс
                val parsedData = data.split("/") // Разделяем на поля

                // Составляем стрктуру
                val finalData = OnBoardingScreenInfo(
                    image = parsedData[0].toInt(),
                    mainText = parsedData[1],
                    descriptionText = parsedData[2]
                )
                val result = Pair(index.toInt(), finalData)
                Log.d("SharedPrefs", "Parsed to: $result")

                return@map result
            }.sortedBy { it.first }.map { it.second } // Сортируем и убираем индексы
        }
        set(value) {
            Log.d("SharedPrefs", "get from some other: $value")
            val setOfValues = value.mapIndexed { index, onBoardingScreenInfo ->
                "$index|${onBoardingScreenInfo.image}/${onBoardingScreenInfo.mainText}/${onBoardingScreenInfo.descriptionText}" // переводим каждый элемент в текст с индексом
            }.toSet()
            Log.d("SharedPrefs", "get from some other: $setOfValues")

            sharedPrefs.edit { putStringSet(ON_BOARDING_KEY, setOfValues) }
        }
}

data class OnBoardingScreenInfo(
    @DrawableRes val image: Int,
    val mainText: String,
    val descriptionText: String
)