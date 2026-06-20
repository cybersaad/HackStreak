package com.cybersaad.hackstreak

import android.app.Application
import com.cybersaad.hackstreak.data.AppContainer

class HackStreakApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
