package kiol.vkapp.commonui

import android.app.Application
import timber.log.Timber

open class VKBaseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}