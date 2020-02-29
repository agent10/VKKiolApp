package kiol.vkapp.taskb

import android.app.Application
import io.reactivex.plugins.RxJavaPlugins
import kiol.vkapp.commonui.VKBaseApp
import ru.timepad.domain.qr.QRBarRecognizer
import ru.timepad.domain.qr.QrBarRecognizerImageDataParser
import timber.log.Timber

class TheApp : VKBaseApp() {
    override fun onCreate() {
        super.onCreate()
        RxJavaPlugins.setErrorHandler {
            Timber.e("UndeliverableException $it")
        }
    }
}