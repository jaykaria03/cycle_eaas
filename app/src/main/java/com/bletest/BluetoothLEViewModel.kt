package com.bletest

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.UUID

@SuppressLint("MissingPermission")
// BluetoothViewModel.kt
class BluetoothLEViewModel(application: Application): AndroidViewModel(application) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager =
            application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val _device1Data = MutableLiveData<String>()
    val device1Data: LiveData<String>
        get() = _device1Data

    private val _logData = MutableLiveData<String>()
    val logData: LiveData<String>
        get() = _logData

    private val _device2Data = MutableLiveData<String>()
    val device2Data: LiveData<String>
        get() = _device2Data

    // Add LiveData for loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    private val device1Name = "Advity RPM-1"
    private val device2Name = "Advity RPM-2"

    private val device1ServiceUUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
    private val device2ServiceUUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")

    private val device1CharacteristicUUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
    private val device2CharacteristicUUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")

    private var bluetoothGattDevice1: BluetoothGatt? = null
    private var bluetoothGattDevice2: BluetoothGatt? = null

    private val handler = Handler(Looper.getMainLooper())

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val deviceName = result.device?.name
            if (deviceName != null) {

                //_logData.postValue("device found")
                    //connectToDevice(result.device,device1ServiceUUID, device1CharacteristicUUID)
                when (deviceName) {
                    device1Name -> connectToDevice(result.device,device1ServiceUUID, device1CharacteristicUUID)
                    device2Name -> connectToDevice(result.device,device2ServiceUUID, device2CharacteristicUUID)
                }
            }else{
                //_logData.postValue("device not found")
            }
        }
    }

    init {
        // Check if Bluetooth is enabled
        if (bluetoothAdapter?.isEnabled == false) {
            // You might want to prompt the user to enable Bluetooth here
            // or handle this case based on your application's requirements.
        }
    }

    fun startScanning() {
        _isLoading.value = true
        val bluetoothScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
        bluetoothScanner?.startScan(scanCallback)
        _logData.postValue("device for scanning")

        // Stop scanning after a specified time (e.g., 10 seconds)
        handler.postDelayed({
            bluetoothScanner?.stopScan(scanCallback)
        }, 10000)
    }

    private fun connectToDevice(
        device: BluetoothDevice,
        serviceUUID: UUID,
        characteristicUUID: UUID
    ) {
        val bluetoothGattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int
            ) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices()
                    _logData.postValue("device connected")
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    // Handle disconnection
                    _isLoading.postValue(false)
                    _logData.postValue("device disconnected")
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                _isLoading.postValue(false)
                _logData.postValue("requesting data")
                val characteristic =
                    gatt.getService(serviceUUID)?.getCharacteristic(characteristicUUID)
                gatt.setCharacteristicNotification(characteristic, true)

                // Configure the descriptor for notifications
                val descriptorUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                val descriptor = characteristic?.getDescriptor(descriptorUUID)
                descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
                _logData.postValue("descriptor set")
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                _logData.postValue("reading data")
                val data = characteristic.value
                val finalValue = byteArrayToInt(data)
                 //_device1Data.postValue(data)
                when (device.name) {
                    device1Name -> _device1Data.postValue(finalValue.toString())
                    device2Name -> _device2Data.postValue(finalValue.toString())
                }
                _isLoading.postValue(false)
            }
        }

        val bluetoothGatt = device.connectGatt(getApplication(), false, bluetoothGattCallback,2)
         //bluetoothGattDevice1 = bluetoothGatt
        when (device.name) {
            device1Name -> bluetoothGattDevice1 = bluetoothGatt
            device2Name -> bluetoothGattDevice2 = bluetoothGatt
        }
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothGattDevice1?.close()
        bluetoothGattDevice2?.close()
    }

    fun byteArrayToInt(byteArray: ByteArray): Int {
        return try {
            var result = 0
            for (i in byteArray.indices) {
                result = result or (byteArray[i].toInt() and 0xFF shl 8 * i)
            }
            result
        } catch (e: Exception) {
            0
        }
    }
}
