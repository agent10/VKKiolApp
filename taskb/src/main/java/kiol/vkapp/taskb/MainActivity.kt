package kiol.vkapp.taskb

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kiol.vkapp.taskb.editor.VideoEditorFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

//        supportFragmentManager.beginTransaction()
//            .replace(R.id.contentViewer, CameraFragment.newInstance()).commit()

        supportFragmentManager.beginTransaction()
            .replace(R.id.contentViewer, VideoEditorFragment()).addToBackStack(null).commit()

        //        val app = applicationContext as TheApp
        //        val qrBarRecognizer = app.qrBarRecognizer
        //        val d = qrBarRecognizer.subscribe().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({
        //            Toast.makeText(this, it.text, Toast.LENGTH_SHORT).show()
        //            Timber.d("qr points: ${it.points}")
        //        }, {
        //            Timber.e("Recognize error")
        //        })
    }
}
