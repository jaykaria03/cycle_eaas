package com.bletest

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@SuppressLint("MissingPermission")
class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    private val applicationContext: Context = application.applicationContext

    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Use the appropriate UUID

    private val bufferSize = 1024
    private val buffer = ByteArray(bufferSize)

    // LiveData for connection state
    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean>
        get() = _isConnected

    // Data class to hold device identifier and received data
    data class DeviceData(val deviceIdentifier: String, val data: String)

    // LiveData for received data with device identifier
    private val _receivedData = MutableLiveData<DeviceData>()
    val receivedData: LiveData<DeviceData>
        get() = _receivedData

    // LiveData for loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean>
        get() = _isLoading
    // Flag to track if the receiver is registered
    private var isReceiverRegistered = false

    // BLE device names to connect
    private val targetDeviceNames = listOf("Advity RPM-1", "Advity RPM-2")

    // List to store BluetoothDevices
    private val bluetoothDevices = mutableListOf<BluetoothDevice>()

    // BluetoothSocket list
    private val bluetoothSockets = mutableListOf<BluetoothSocket?>()

    // BluetoothPairingReceiver
    private val pairingReceiver = BluetoothPairingReceiver()

    init {
        // Initialize LiveData
        _isConnected.value = false
    }

     fun connectToDevice(context: Context) {

         /*GlobalScope.launch {
           var counter = 0

           while (counter<361) {
               // Update LiveData with the current value
               _receivedData.postValue(DeviceData("needforspeed",counter.toString()))

               // Increment the counter
               counter++

               // Delay for 1 second
               delay(100)
           }
       }

       return*/


        // Check Bluetooth availability
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            // Handle no Bluetooth support
            _isLoading.postValue(false)
            return
        }

        // Check Bluetooth enabled
        if (!bluetoothAdapter.isEnabled) {
            // Handle Bluetooth not enabled
            _isLoading.postValue(false)
            return
        }

        // Check if the receiver is already registered
        if (!isReceiverRegistered) {
            // Start device discovery if not already discovering
            if (!bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.startDiscovery()
            }

            // Register the BluetoothPairingReceiver
            val pairingFilter = IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST)
            applicationContext.registerReceiver(pairingReceiver, pairingFilter)

            // Register the BluetoothDevice.ACTION_FOUND receiver
            val foundDeviceFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            applicationContext.registerReceiver(foundDeviceReceiver, foundDeviceFilter)

            isReceiverRegistered = true
        }

        // Delay to allow Bluetooth device discovery
        GlobalScope.launch(Dispatchers.Main) {
            delay(9000) // Adjust the delay based on your needs

            bluetoothAdapter.cancelDiscovery()
            if (isReceiverRegistered) {
                applicationContext.unregisterReceiver(pairingReceiver)
                applicationContext.unregisterReceiver(foundDeviceReceiver)
                isReceiverRegistered = false
            }

            // Connect to all devices
            for (targetDeviceName in targetDeviceNames) {
                val device = findBluetoothDeviceByName(targetDeviceName)
                if (device != null) {
                    bluetoothDevices.add(device)
                    connectToDevice(device)
                }
            }
        }
    }

    private val foundDeviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action: String = intent!!.action!!
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val deviceName = device?.name
                    if (deviceName != null && targetDeviceNames.contains(deviceName)) {
                        // Connect to the device if it matches the target device names
                        bluetoothDevices.add(device)
                        connectToDevice(device)
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                // Handle disconnection event
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                    try {
                        bluetoothDevices.remove(device)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    if (device != null && targetDeviceNames.contains(device.name)) {
                        // Reconnect to the device if it matches the target device names
                        connectToDevice(device)
                    }
                }
            }
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        try {
            val socket = device.createRfcommSocketToServiceRecord(uuid)
            socket.connect()
            _isConnected.postValue(true)
            _isLoading.postValue(false)
            //bluetoothSockets.add(socket)
            startListening(socket, device.name)
        } catch (e: Exception) {
            // Handle connection error
            _isConnected.postValue(false)
            _isLoading.postValue(true)
        }
    }

    private fun findBluetoothDeviceByName(deviceName: String): BluetoothDevice? {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices = bluetoothAdapter.bondedDevices
        for (device in pairedDevices) {
            if (device.name.equals(deviceName, ignoreCase = true)) {
                return device
            }
        }
        return null
    }

    private fun startListening(socket: BluetoothSocket, deviceIdentifier: String) {
        val inputStream: InputStream? = socket.inputStream

        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    val bytesRead = inputStream?.read(buffer)
                    if (bytesRead != null && bytesRead > 0) {
                        val data = String(buffer.copyOf(bytesRead))
                        _receivedData.postValue(DeviceData(deviceIdentifier, data))
                    }
                        delay(250)
                } catch (e: Exception) {
                    // Handle reading error
                    e.printStackTrace()
                    break
                }
            }
        }
    }

    fun send(data: String) {
        for (socket in bluetoothSockets) {
            val outputStream: OutputStream? = socket?.outputStream
            try {
                outputStream?.write(data.toByteArray())
            } catch (e: IOException) {
                // Handle writing error
            }
        }
    }

    fun disconnect() {
        for (socket in bluetoothSockets) {
            try {
                socket?.close()
                _isConnected.postValue(false)
            } catch (e: IOException) {
                // Handle disconnection error
            }
        }
    }

    // BluetoothPairingReceiver
    private inner class BluetoothPairingReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action: String = intent!!.action!!

            when (action) {
                BluetoothDevice.ACTION_PAIRING_REQUEST -> {
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val variant =
                        intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, -1)

                    if (device != null) {
                        // Handle pairing request automatically
                        when (variant) {
                            BluetoothDevice.PAIRING_VARIANT_PIN -> {
                                device.setPin("000000".toByteArray())
                                abortBroadcast() // Cancel the pairing request
                            }
                            // Handle other pairing variants if needed
                        }
                    }
                }
            }
        }
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
    private val todayDate = dateFormat.format(Date())

    /*// Function to insert or update the date counter
    fun insertOrUpdateDateCounter(counter: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            MyApp.database.dateCounterDao().insertOrUpdate(
                DateCounterEntity(todayDate, counter)
            )
        }
    }

    // Function to get the current date's counter value
    fun getCurrentDateCounter(callback: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.Main) {
            val dateCounter = MyApp.database.dateCounterDao().getDateCounter(todayDate)
            val counterValue = dateCounter ?: 0
            callback(counterValue.toString().toInt())
        }
    }

    // Function to get the total counter value
    fun getTotalCounter(callback: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.Main) {
            val totalCounter = MyApp.database.dateCounterDao().getTotalCounter()
            callback(totalCounter)
        }
    }



    // Function to get data from the database and save it as CSV
    fun exportDataToCSV(callback: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Retrieve data from Room database
                val dateCounterList = MyApp.database.dateCounterDao().getAll()

                // Save data as CSV
                val csvFile = saveDataAsCSV(dateCounterList)

                // Notify the callback about the success
                launch(Dispatchers.Main) {
                    callback(true)
                }

            } catch (e: Exception) {
                // Handle exceptions and notify the callback about the failure
                e.printStackTrace()
                callback(false)
            }
        }
    }

    // Function to save data as CSV
    private fun saveDataAsCSV(dateCounterList: List<DateCounterEntity>): File {
        val csvFile = createCSVFile()
        try {
            val writer = FileWriter(csvFile)

            // Write CSV header
            writer.append("Date,Counter\n")

            // Write data rows
            for (dateCounter in dateCounterList) {
                writer.append("${dateCounter.date},${dateCounter.counter}\n")
            }

            writer.flush()
            writer.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return csvFile
    }

    // Function to create a CSV file
    private fun createCSVFile(): File {
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "TippermaticsAppCSV"
        )

        if (!directory.exists()) {
            directory.mkdirs()
        }

        return File(directory, "Tippermatics_date_counter_data.csv")
    }*/
}