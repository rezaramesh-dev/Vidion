package app.onestack.vidion.View.Activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import app.onestack.vidion.R
import app.onestack.vidion.Utils.savePositionListPref
import app.onestack.vidion.Utils.saveSortPref
import kotlinx.coroutines.*

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {

    private var currentApiVersion = 0

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        savePositionListPref(this, 0)
        saveSortPref(this, "A-Z")
        CoroutineScope(Dispatchers.Main).launch { timer() }
    }

    override fun onResume() {
        super.onResume()
        CoroutineScope(Dispatchers.Main).launch {
            timer()
        }
    }

    private suspend fun timer() {
        delay(2000L)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

