package kiol.vkapp.taskb

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        supportFragmentManager.beginTransaction()
            .replace(R.id.contentViewer, CameraFragment.newInstance()).commit()

        val app = applicationContext as TheApp
        val qrBarRecognizer = app.qrBarRecognizer
        val d = qrBarRecognizer.subscribe().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({
            Toast.makeText(this, it.text, Toast.LENGTH_SHORT).show()
        }, {
            Timber.e("Recognize error")
        })
    }
}
