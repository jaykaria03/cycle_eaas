package com.bletest

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.text.toSpannable
import androidx.core.text.toSpanned
import java.text.SimpleDateFormat
import java.util.Collections.min
import java.util.Date
import java.util.Locale
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    private val viewModel: BluetoothLEViewModel by viewModels()

    private val loadingDialog by lazy {
        ProgressDialog(this).apply {
            setMessage("Connecting to your device...")
            setCancelable(false)
        }
    }

    private lateinit var textView: TextView
    private var counter = 0

    private val bluetoothPermissionRequestCode = 1

    private val MY_PERMISSIONS_REQUEST_LOCATION = 123

    lateinit var rpm1:TextView
    lateinit var rpm2:TextView

    var rpm1value = 0
    var rpm2value = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textview)

        rpm1 = findViewById(R.id.editTextData)
        rpm2 = findViewById(R.id.editTextData1)

        viewModel.isLoading.observe(this) { isLoading ->
            /*if (isLoading) {
                showLoader()
            } else {
                hideLoader()
            }*/
        }
        viewModel.device1Data.observe(this){data ->
            //rpm1.text = "Device 1: ${data.trim().ifEmpty { "0" }}"
            val temp = if (data.trim().isEmpty()) 0 else data.trim().toInt()
            rpm1value = if (temp>0) temp else rpm1value
            animateColorAndSize()
        }

        viewModel.device2Data.observe(this){data ->
            //rpm2.text = "Device 2: ${data.trim().ifEmpty { "0" }}"
            val temp = if (data.trim().isEmpty()) 0 else data.trim().toInt()
            rpm2value = if (temp>0) temp else rpm2value
            animateColorAndSize()
        }

        // Observe LiveData in the ViewModel and update UI as needed
        /*viewModel.receivedData.observe(this) { data ->
            if (data.deviceIdentifier.contains("RPM-1")){
                try {
                    rpm1.text = "Device 1: ${data.data.trim().ifEmpty { "0" }}"
                    val temp = if (data.data.trim().isEmpty()) 0 else data.data.trim().toInt()
                    rpm1value = if (temp>0) temp else rpm1value
                } catch (e: Exception) {
                    rpm1value = 0
                }
            }else{
                try {
                    rpm2.text = "Device 2: ${data.data.trim().ifEmpty { "0" }}"
                    val temp = if (data.data.trim().isEmpty()) 0 else data.data.trim().toInt()
                    rpm2value = if (temp>0) temp else rpm2value
                } catch (e: Exception) {
                    rpm2value = 0
                }
            }

            animateColorAndSize()

        }*/


        /*viewModel.logData.observe(this) { data ->
            findViewById<TextView>(R.id.logs).text = findViewById<TextView>(R.id.logs).text.toString()+"\n $data"
        }*/

        // Check if location permissions are granted
        if (checkLocationPermissions()) {
            // Location permissions are already granted, call your function
            // Check and request Bluetooth permissions
            checkBluetoothPermissions()
        } else {
            // Location permissions are not granted, request them
            requestLocationPermissions()
        }

        // Start the animation when the activity is created
        //animateColorAndSize()
    }

    private fun checkLocationPermissions(): Boolean {
        val fineLocationPermission =
            ContextCompat.checkSelfPermission(getApplication(), android.Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocationPermission =
            ContextCompat.checkSelfPermission(getApplication(), android.Manifest.permission.ACCESS_COARSE_LOCATION)

        return fineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                coarseLocationPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermissions() {
        // Request location permissions
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            MY_PERMISSIONS_REQUEST_LOCATION
        )
    }

    private fun checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissionsToRequest = mutableListOf<String>()

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH)
            }

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_ADMIN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADMIN)
            }

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }

            if (permissionsToRequest.isNotEmpty()) {
                requestPermissions(
                    permissionsToRequest.toTypedArray(),
                    bluetoothPermissionRequestCode
                )
            } else {
                // All permissions already granted, start Bluetooth scanning
                viewModel.startScanning()
            }
        } else {
            // Start Bluetooth scanning for versions lower than Android 6.0 (no runtime permission)
            viewModel.startScanning()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == bluetoothPermissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissions granted, start BLE scanning
                viewModel.startScanning()
            } else {
                // Permissions denied, handle accordingly (e.g., show a message or close the app)
                Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkBluetoothPermissions()
            } else {
                requestLocationPermissions()
            }
        }
    }

    private fun showLoader() {
        loadingDialog.show()
    }

    private fun hideLoader() {
        loadingDialog.dismiss()
    }

    private val maxValue = 150
    private fun animateColorAndSize() {
        val percentage = ((((rpm1value/2)+(rpm2value/2)))/maxValue)*100 // Ensure the value is between 0 and 1
        val redPercentage = 1.0f - percentage
        val bluePercentage = percentage

        // Calculate the start and end colors for the gradient
        val startColor = android.graphics.Color.rgb(
            (255 * redPercentage).toInt(),
            (255 * redPercentage).toInt(),
            (255 * redPercentage).toInt()
        )

        val endColor = android.graphics.Color.rgb(
            (255 * bluePercentage).toInt(),
            (255 * bluePercentage).toInt(),
            0
        )

        // Calculate the gradient color for the entire text (bottom to top)
        val gradient = LinearGradient(
            0f, 0f, 0f, textView.height.toFloat(),  // Start from the bottom and end at the top
            startColor, endColor, Shader.TileMode.CLAMP
        )

        if (percentage<=maxValue){
            // Apply the gradient to the text
            textView.paint.shader = gradient

            // Trigger a redraw to make the gradient change visible
            textView.invalidate()
        }


        // Start font size animation
        if (percentage>=maxValue){
            val newSize = 90
            val sizeAnimator = ValueAnimator.ofFloat(textView.textSize, newSize.toFloat())
            sizeAnimator.addUpdateListener { animator ->
                val animatedValue = animator.animatedValue as Float
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, animatedValue)
            }
            sizeAnimator.duration = 500 // Set the duration of the font size change animation
            sizeAnimator.start()
        }


        // Update the counter and repeat the animation if the counter is less than 180
        /*if (counter < 180) {
            counter += 20
            textView.postDelayed({ animateColorAndSize() }, 500) // Delay for the next animation
        }*/
    }


    private fun blendColors(color1: Int, color2: Int, ratio: Float): Int {
        val inverseRatio = 1f - ratio
        val r = (Color.red(color1) * ratio + Color.red(color2) * inverseRatio).toInt()
        val g = (Color.green(color1) * ratio + Color.green(color2) * inverseRatio).toInt()
        val b = (Color.blue(color1) * ratio + Color.blue(color2) * inverseRatio).toInt()
        return Color.rgb(r, g, b)
    }
}