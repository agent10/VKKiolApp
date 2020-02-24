package kiol.vkapp.taskc

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

operator fun CompositeDisposable.plusAssign(d: Disposable) {
    this.add(d)
}

fun Fragment.getAppContext() = requireContext().applicationContext

class MainActivity : AppCompatActivity() {

    private lateinit var simpleRouter: SimpleRouter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        window?.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        simpleRouter = SimpleRouter(supportFragmentManager)

        simpleRouter.routeToCamera()
    }


    fun getRouter() = simpleRouter
}
