package com.android.cts.resolutionswitchadb

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.android.cts.resolutionswitchadb.R
//import org.tag.resolutionswitchadb.BuildConfig.APPLICATION_ID
import org.tag.resolutionswitchadb.WmApi
import kotlin.concurrent.fixedRateTimer
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt


class MainActivity : AppCompatActivity() {
    /* Command that user must run to grant permissions */
    private val adbCommand = "pm grant APPLICATION_ID android.permission.WRITE_SECURE_SETTINGS"

    /* Preferences */
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    /* Internal classes */
    private lateinit var wmApi: WmApi

    /* UI elements */
    private lateinit var reset: Button
    private lateinit var switchbutton: Button


    /* Default screen size and density on start */
    object DefaultScreenSpecs {
        var width: Int = 0
        var height: Int = 0
        var density: Int = 0
        var diagInches: Double = 0.0
        var diagPixels: Double = 0.0

        /* Set DefaultScreenSpecs to current settings */
        fun setup(context: Context, windowManager: WindowManager) {
            val dm = DisplayMetrics()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display!!.getRealMetrics(dm)
            } else {
                windowManager.defaultDisplay.getRealMetrics(dm)
            }

            width = dm.widthPixels
            height = dm.heightPixels
            density = dm.densityDpi

            val wi = dm.widthPixels.toDouble() / dm.xdpi.toDouble()
            val hi = dm.heightPixels.toDouble() / dm.ydpi.toDouble()

            diagInches = sqrt(wi.pow(2.0) + hi.pow(2.0))
            diagPixels = sqrt(dm.widthPixels.toDouble().pow(2) +
                    dm.heightPixels.toDouble().pow(2))
        }
    }

    /* Estimate proper DPI for device */
    private fun calculateDPI(x: Int, y: Int): Int {
        val diagPixels = sqrt(x.toDouble().pow(2) + y.toDouble().pow(2)).toInt()
        return (diagPixels / DefaultScreenSpecs.diagInches).roundToInt()
    }

    /* Apply resolution and density */
    private fun apply() {

        wmApi.setBypassBlacklist(true)
        wmApi.setDisplayDensity(90)

        /* Delay because when we change resolution, window changes */
        Handler(Looper.myLooper()!!).postDelayed({
          //  showWarningDialog()
            updateEditTexts()
        }, 500)
    }

    /* Show 5 second countdown
    private fun showWarningDialog() {
        var dismissed = false
        val confirmMessage = "If these settings look correct, press Confirm to keep them.\n\n"

        val dialog = AlertDialog.Builder(this)
                .setTitle("Confirm Settings")
                .setMessage(confirmMessage)
                .setPositiveButton("Confirm") { _: DialogInterface, _: Int ->
                    dismissed = true
                }
                .setCancelable(false)
                .create()

        dialog.show()

        object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (dismissed) {
                    this.cancel()
                } else {
                    val secondsLeft = ((millisUntilFinished / 1000) + 1)
                    dialog.setMessage(confirmMessage + "Resetting in " + secondsLeft + " seconds.")
                }
            }

            override fun onFinish() {
                if (!dismissed) {
                    dialog.dismiss()
                    reset()
                }
            }
        }.start()
    }
    */

    /* Use new resolution for text */
    private fun updateEditTexts() {
        /* Read screen specs */
        DefaultScreenSpecs.setup(this, windowManager)


        /* Store this for when we need to reset */
        if (sharedPreferences.getInt("defaultDensity", 0) == 0) {
            editor.putInt("defaultDensity", DefaultScreenSpecs.density)
            editor.apply()
        }
    }

    /* Reset resolution and density */
    private fun reset() {
        wmApi.setBypassBlacklist(true)
        wmApi.clearDisplayResolution()

        /* Fall back to default density if we can */
        val defaultDensity = sharedPreferences.getInt("defaultDensity", 0)
        if (defaultDensity != 0)
            wmApi.setDisplayDensity(defaultDensity)
        else
            wmApi.clearDisplayDensity()

        /* Restart activity because windowManager breaks after reset */
        finish()
        overridePendingTransition(0, 0)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /* Configure classes for our activity */
        wmApi = WmApi(contentResolver)

        /* Setup preferences */
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        editor = sharedPreferences.edit()

        /* Setup UI elements */
        reset = findViewById(R.id.reset)
        switchbutton = findViewById(R.id.switchbutton)


        reset.setOnClickListener {
            reset()

            reset.startAnimation(AnimationUtils.loadAnimation(this, R.anim.press))
        }

        switchbutton.setOnClickListener {
            apply()

            switchbutton.startAnimation(AnimationUtils.loadAnimation(this, R.anim.press))
        }

        /* Request or confirm if we can perform proper commands */
        checkPermissions()

        /* Show the current display config */
        updateEditTexts()
    }

    private fun hasPermissions(): Boolean {
        val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_SECURE_SETTINGS)
        return permissionCheck == PackageManager.PERMISSION_GRANTED
    }

    private fun checkPermissions() {
        if (hasPermissions())
            return

        val dialog = AlertDialog.Builder(this)
                .setTitle("Missing Permissions")
                .setPositiveButton("Check Again", null)
                .setNeutralButton("Setup ADB", null)
                .setCancelable(false)
                .create()

        dialog.setOnShowListener {
            /* We don't dismiss on Check Again unless we actually have the permission */
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                if (hasPermissions())
                    dialog.dismiss()
            }

            /* Open tutorial but do not dismiss until user presses Check Again */
            val neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            neutralButton.setOnClickListener {
                val uri = Uri.parse("https://www.xda-developers.com/install-adb-windows-macos-linux/")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
        }

        dialog.show()

        /* Check every second if the permission was granted */
        fixedRateTimer("permissionCheck", false, 0, 1000) {
            if (hasPermissions()) {
                dialog.dismiss()
                this.cancel()
            }
        }
    }
}
