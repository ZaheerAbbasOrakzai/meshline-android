package com.example.mesh

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.*

class MeshService : Service() {

    companion object {
        private const val TAG = "MeshService"
        private const val CHANNEL_ID = "mesh_channel"
        private const val NOTIFICATION_ID = 101

        val SERVICE_UUID: UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        val DATA_CHAR_UUID: UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567891")
    }

    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bleAdvertiser: BluetoothLeAdvertiser? = null
    private var gattServer: BluetoothGattServer? = null
    
    private val connectedNeighbors = mutableMapOf<String, BluetoothGatt>()
    private var meshEngine: MeshEngine? = null
    private val binder = MeshBinder()

    inner class MeshBinder : Binder() {
        fun getService(): MeshService = this@MeshService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter
        bleAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    fun setEngine(engine: MeshEngine) {
        this.meshEngine = engine
    }

    @SuppressLint("MissingPermission")
    fun startMesh() {
        if (bluetoothAdapter?.isEnabled == false) {
            // In a real service, we can't show UI, so we notify the engine/activity
            meshEngine?.onBluetoothRequired()
            return
        }
        startAdvertising()
        startGattServer()
        startDiscovery()
    }

    @SuppressLint("MissingPermission")
    private fun startDiscovery() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(SERVICE_UUID)).build()
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        
        scanner?.startScan(listOf(filter), settings, scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            connectToNode(result.device.address)
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToNode(deviceAddress: String) {
        if (connectedNeighbors.containsKey(deviceAddress)) return
        
        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
        device?.connectGatt(this, false, gattClientCallback)
    }

    private val gattClientCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectedNeighbors[gatt.device.address] = gatt
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectedNeighbors.remove(gatt.device.address)
                gatt.close()
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                meshEngine?.onNeighborConnected(gatt.device.address)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun sendPacketToAllNeighbors(packet: MeshPacket) {
        connectedNeighbors.forEach { (address, _) ->
            sendPacketToNeighbor(address, packet)
        }
    }

    @SuppressLint("MissingPermission")
    fun sendPacketToNeighbor(deviceAddress: String, packet: MeshPacket) {
        val gatt = connectedNeighbors[deviceAddress] ?: return
        val service = gatt.getService(SERVICE_UUID)
        val char = service?.getCharacteristic(DATA_CHAR_UUID)
        
        if (char != null) {
            val data = PacketManager.serialize(packet)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(char, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            } else {
                @Suppress("DEPRECATION")
                char.value = data
                gatt.writeCharacteristic(char)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopMesh() {
        bleAdvertiser?.stopAdvertising(advertiseCallback)
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        connectedNeighbors.forEach { (_, gatt) ->
            gatt.disconnect()
            gatt.close()
        }
        connectedNeighbors.clear()
        gattServer?.close()
    }

    @SuppressLint("MissingPermission")
    private fun startAdvertising() {
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        bleAdvertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d(TAG, "BLE Advertising started successfully")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "BLE Advertising failed: $errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    private fun startGattServer() {
        gattServer = bluetoothManager?.openGattServer(this, gattServerCallback)
        val service = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        
        val dataChar = BluetoothGattCharacteristic(
            DATA_CHAR_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_WRITE or BluetoothGattCharacteristic.PERMISSION_READ
        )
        
        service.addCharacteristic(dataChar)
        gattServer?.addService(service)
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        @SuppressLint("MissingPermission")
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value)
            
            if (characteristic.uuid == DATA_CHAR_UUID) {
                try {
                    val packet = PacketManager.deserialize(value)
                    meshEngine?.onPacketReceived(packet, device.address)
                    if (responseNeeded) {
                        gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to deserialize packet", e)
                    if (responseNeeded) {
                        gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null)
                    }
                }
            } else if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Meshline Mesh Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Meshline Active")
            .setContentText("Mesh network is running in the background")
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .build()
    }
}
