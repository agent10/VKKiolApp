package kiol.vkapp.commonui

import android.app.Application
import io.reactivex.plugins.RxJavaPlugins
import timber.log.Timber

open class VKBaseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        RxJavaPlugins.setErrorHandler {
            Timber.e("UndeliverableException $it")
        }
    }
}