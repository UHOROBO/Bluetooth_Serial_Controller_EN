package com.main.bluetoothserialcontroller

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

const val TAG = "Bluetooth"
const val SERVER_NAME = "ServerName"

class Bluetooth {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val _bluetoothDevices = MutableLiveData<List<BluetoothDevice>>()
    private val bluetoothDevices: LiveData<List<BluetoothDevice>> = _bluetoothDevices

    private var mmSocket: BluetoothSocket? = null
    private var mmServerSocket: BluetoothServerSocket? = null

    private var mmInStream: InputStream? = null
    private var mmOutStream: OutputStream? = null
    private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream (1KB)

    //private val uuid : UUID = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    //bluetoothのアダプタがあるか確認する
    //そのデバイスでbluetoothが使えるか確認する
    //デバイスで使えない時はAdapterがないのでNULLになる
    fun bluetoothAdapterIsNull(): Boolean {//nullだとtrue
        return (bluetoothAdapter == null)
    }

    //bluetoothが利用可能であるか確認する
    //Enableの時使える
    fun bluetoothAdapterIsEnabled(): Boolean {//Enableだとtrue
        if (bluetoothAdapterIsNull()) return false //アダプタがnullの時返す
        return (bluetoothAdapter?.isEnabled == true)
    }

    fun bluetoothSocketIsNull(): Boolean {//nullだとtrue
        return (mmSocket == null)
    }

    //ペアのデバイスを更新する
    fun bluetoothPairedDevicesUpdate() {
        if (bluetoothAdapterIsNull()) return//アダプタがnullの時返す
        val bondedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        _bluetoothDevices.value = bondedDevices?.toList()
    }

    //ペアデバイスの名前のリストを返す
    fun getBluetoothDeviceNameList(): List<String> {
        val bluetoothDevicesName = mutableListOf<String>()
        bluetoothDevices.value?.forEach { device ->
            bluetoothDevicesName.add(device.name)
        }
        return bluetoothDevicesName
    }

    //デバイスの名前からデバイス本体を探して返す
    //あったらBluetoothDeviceを返して、なかったらnullを返す
    fun getBluetoothDevice(deviceName: String): BluetoothDevice? {
        bluetoothDevices.value?.forEach { device ->
            if (deviceName == device.name) return device
        }
        return null
    }

    //クライアント接続を行う。接続できると使えるsocketを入手する
    //接続したらtrueを返す
    //止めるときはclose()を呼ぶ
    suspend fun connectClient(device: BluetoothDevice, defaultDispatcher: CoroutineDispatcher = Dispatchers.IO): Boolean {
        return withContext(defaultDispatcher) {
            mmSocket = device.createInsecureRfcommSocketToServiceRecord(device.uuids[0].uuid)
            bluetoothAdapter?.cancelDiscovery()
            try {
                mmSocket?.connect()
            } catch (e: IOException) {
                return@withContext false
            }
            mmInStream = mmSocket!!.inputStream
            mmOutStream = mmSocket!!.outputStream
            return@withContext true
        }
    }

    //サーバー接続を行う。接続できると使えるsocketを入手する
    //accept()でsocketを返すまでブロックする
    //socketを返すとtrueを返すが、そもそも接続するまで終わらないので意味ない
    //止めるときはclose()を呼ぶ
    suspend fun connectServer(defaultDispatcher: CoroutineDispatcher = Dispatchers.IO): Boolean {
        return withContext(defaultDispatcher) {
            mmServerSocket = bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord(
                SERVER_NAME,
                UUID.randomUUID())
            try {
                mmSocket = mmServerSocket?.accept()
            } catch (e: IOException) {
                //Log.e(TAG, "Socket's accept() method failed", e)
                mmSocket = null
                return@withContext false
            }
            mmSocket?.also {
                mmInStream = it.inputStream
                mmOutStream = it.outputStream
                mmServerSocket?.close()
            }
            return@withContext true
        }
    }

    //connectの処理を止めたいときに呼ぶ
    //socketを止める処理をしたいときにも使える
    fun close() {
        try {
            mmServerSocket?.close()
            mmSocket?.close()
            mmInStream = null
            mmOutStream = null
        } catch (e: IOException) {
            //Log.e(TAG, "Could not close the connect socket", e)
        }
    }

    //これ動くか？
    suspend fun read(defaultDispatcher: CoroutineDispatcher = Dispatchers.IO): Flow<ByteArray> {
        //if(bluetoothSocketIsNull())return flow { emit(arrayOf()) }
        return flow{
            //if (bluetoothAdapterIsNull()) return
            var numBytes: Int // bytes returned from read()
            while (true) {
                try {
                    numBytes = mmInStream!!.read(mmBuffer)
                    emit(mmBuffer.copyOf(numBytes))
                } catch (e: IOException) {
                    //Log.d(TAG, "Error occurred when reading data", e)
                    break
                }
            }
        }.flowOn(defaultDispatcher)
    }

    //書く
    suspend fun writeString(string: String, defaultDispatcher: CoroutineDispatcher = Dispatchers.IO): Boolean {
        return withContext(defaultDispatcher){
            try {
                mmOutStream?.write(string.toByteArray())
                return@withContext true
            } catch (e: IOException) {
                //Log.e(TAG, "Error occurred when sending data", e)
                return@withContext false
            }
        }
    }

    suspend fun write(byteArray: ByteArray, defaultDispatcher: CoroutineDispatcher = Dispatchers.IO): Boolean {
        return withContext(defaultDispatcher){
            try {
                mmOutStream?.write(byteArray)
                return@withContext true
            } catch (e: IOException) {
                //Log.e(TAG, "Error occurred when sending data", e)
                return@withContext false
            }
        }
    }


}


