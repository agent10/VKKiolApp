package kiol.vkapp.taskb

import android.app.Application
import ru.timepad.domain.qr.QRBarRecognizer
import ru.timepad.domain.qr.QrBarRecognizerImageDataParser
import timber.log.Timber

class TheApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}