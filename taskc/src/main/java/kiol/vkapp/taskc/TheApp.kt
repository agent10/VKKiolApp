package kiol.vkapp.taskc

import android.app.Application
import timber.log.Timber

class TheApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}