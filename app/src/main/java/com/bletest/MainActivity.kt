package com.bletest

import android.Manifest
import android.animation.Animator
import android.animation.ValueAnimator
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.orbitalsonic.waterwave.WaterWaveView


class MainActivity : AppCompatActivity() {

    private val viewModel: BluetoothLEViewModel by viewModels()

    private val loadingDialog by lazy {
        ProgressDialog(this).apply {
            setMessage("Connecting to your device...")
            setCancelable(false)
        }
    }

    private lateinit var textView: TextView
    private lateinit var ivCycle: ImageView
    private var counter = 0

    private val bluetoothPermissionRequestCode = 1

    private val MY_PERMISSIONS_REQUEST_LOCATION = 123

    lateinit var rpm1:TextView
    lateinit var rpm2:TextView
lateinit var waterWaveView: WaterWaveView
    var listOfPercentage = mutableListOf<Float>()

    var rpm1value = 0
    var rpm2value = 0

    var listenData = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textview)
        ivCycle = findViewById(R.id.iv_cycle)

        rpm1 = findViewById(R.id.editTextData)
        rpm2 = findViewById(R.id.editTextData1)

        waterWaveView = findViewById(R.id.waterWaveView)
        waterWaveView.setShape(WaterWaveView.Shape.CIRCLE)
        waterWaveView.setHideText(true)
        waterWaveView.max = 100
        waterWaveView.progress = 0
        waterWaveView.setWaveStrong(100)
        waterWaveView.setShapePadding(10F)
        waterWaveView.setAnimationSpeed(100)


        viewModel.isLoading.observe(this) { isLoading ->
            /*if (isLoading) {
                showLoader()
            } else {
                hideLoader()
            }*/
        }
        viewModel.device1Data.observe(this){data ->
            rpm1.text = "Device 1: ${data.trim().ifEmpty { "0" }}"
            if (listenData){
                val temp = if (data.trim().isEmpty()) 0 else data.trim().toInt()
                rpm1value = if (temp>0) temp else rpm1value
                animateColorAndSize()
            }

        }
        //animateColorAndSize()
        viewModel.device2Data.observe(this){data ->
            rpm2.text = "Device 2: ${data.trim().ifEmpty { "0" }}"
            if (listenData){
                val temp = if (data.trim().isEmpty()) 0 else data.trim().toInt()
                rpm2value = if (temp>0) temp else rpm2value
                animateColorAndSize()
            }
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

        val spinner: Spinner = findViewById(R.id.spinner)

        // Create an ArrayAdapter using the string array and a default spinner layout
        val values = (300..700 step 50).map { it.toString() }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, values)

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Apply the adapter to the spinner
        spinner.adapter = adapter

        // Set up a listener to handle item selections
        spinner.setOnItemSelectedListener(object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                val selectedValue = values[position]
                maxValue = selectedValue.toInt()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

        });

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

    private var maxValue = 500
    private fun animateColorAndSize() {
        ivCycle.visibility = View.GONE
        textView.visibility = View.VISIBLE
        waterWaveView.visibility = View.VISIBLE
        val percentage = ((((rpm1value/2f)+(rpm2value/2f)))/maxValue.toFloat())*100 // Ensure the value is between 0 and 1
        val redPercentage = 1.5f - (percentage/100.0)
        val bluePercentage = percentage/100.0

        // Calculate the start and end colors for the gradient
        val startColor = android.graphics.Color.rgb(
            (255).toInt(),
            (255).toInt(),
            (255).toInt()
        )

        val endColor = android.graphics.Color.rgb(
            (255).toInt(),
            (255).toInt(),
            0
        )

        // Calculate the gradient color for the entire text (bottom to top)
        val gradient = LinearGradient(
            0f, 0f, 0f, (textView.height.toFloat()*redPercentage.toFloat()),  // Start from the bottom and end at the top
            startColor, endColor, Shader.TileMode.CLAMP
        )

        waterWaveView.progress = percentage.toInt()
        /*if (percentage in 11f..100f){
            // Apply the gradient to the text
            textView.paint.shader = gradient

            // Trigger a redraw to make the gradient change visible
            textView.invalidate()
        }*/

        if (percentage>=90f){
            listOfPercentage.add(percentage)
        }else{
            listOfPercentage.clear()
        }
        var checkedLastFewValues = false
        if (listOfPercentage.size>=10){
            checkedLastFewValues = listOfPercentage.all { it>=90f }
        }

        // Start font size animation
        if ((percentage>=100f) || checkedLastFewValues){
            listenData = false
            val newSize = 450
            val sizeAnimator = ValueAnimator.ofFloat(textView.textSize, newSize.toFloat())
            sizeAnimator.addUpdateListener { animator ->
                val animatedValue = animator.animatedValue as Float
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, animatedValue)
            }
            sizeAnimator.duration = 2000 // Set the duration of the font size change animation
            sizeAnimator.start()
            sizeAnimator.addListener(object : Animator.AnimatorListener{
                override fun onAnimationStart(p0: Animator) {
                }

                override fun onAnimationEnd(p0: Animator) {
                    val imageDialog = ImageDialog(this@MainActivity, R.drawable.new_qr, ::onCloseButtonClick)
                    imageDialog.show()
                }

                override fun onAnimationCancel(p0: Animator) {

                }

                override fun onAnimationRepeat(p0: Animator) {
                }

            })
        }


        // Update the counter and repeat the animation if the counter is less than 180
       /* if (rpm1value < 300) {
            rpm1value += 10
            rpm2value += 10
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

    private fun onCloseButtonClick() {
        // Your code here

        textView.paint.shader = null
        textView.invalidate()
        textView.setTextColor(ContextCompat.getColor(this@MainActivity,R.color.white))
        textView.textSize = 170f
        ivCycle.visibility = View.VISIBLE
        textView.visibility = View.GONE
        waterWaveView.visibility = View.GONE
        listOfPercentage.clear()
        waterWaveView.progress = 0
        Handler(Looper.getMainLooper()).postDelayed({
            rpm1value = 0
            rpm2value = 0
            listenData = true
        }, 500)
    }
}