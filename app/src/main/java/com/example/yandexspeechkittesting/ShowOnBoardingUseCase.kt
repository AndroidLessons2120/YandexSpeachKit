package com.example.yandexspeechkittesting

class ShowOnBoardingUseCase(private val sharedPrefs: SharedPrefs) {
    fun execute(): OnBoardingScreenInfo {
        val onBoardingList = sharedPrefs.onBoardingQueue
        val item = onBoardingList[0]
        sharedPrefs.onBoardingQueue = onBoardingList.drop(1)
        return item
    }
}