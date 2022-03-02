package com.main.bluetoothserialcontroller

import android.bluetooth.BluetoothDevice
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect


class MainViewModel : ViewModel() {

    private val _textList = MutableLiveData<SpannableString>()
    val textList : LiveData<SpannableString> = _textList

    private var _textSize : Int =0
    var buffFlag : Boolean = false
        private set
    var bufferSize : Int =1024*10
        private set

    fun setComments(newText: ByteArray, color: Int = Color.YELLOW){
        if(_textSize >= bufferSize && !buffFlag){
            buffFlag  = true
        } else{
            val string = String(newText)
            val spannable = SpannableString(string)
            spannable.setSpan(ForegroundColorSpan(color), 0, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            _textSize += newText.size
            _textList.value = spannable
        }
    }

    fun setTextSize(newSize:Int){
        _textSize=newSize
    }

    fun setTextSizeZero(){
        setTextSize(0)
        val spannable = SpannableString("")
        spannable.setSpan(ForegroundColorSpan(Color.BLACK), 0, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        _textList.value=spannable
    }


    fun buffFlagFalse(){ buffFlag = false }

    override fun onCleared() {
        bluetooth.close()
        super.onCleared()
    }

    //bluetooth////////////////////////////////////////////////////////////

    //bluetooth//////////////////////////////////
    private var _connected = MutableLiveData(false)
    val connected : LiveData<Boolean> = _connected

    private val bluetooth = Bluetooth()

    fun bluetoothAdapterIsNull():Boolean{
        return bluetooth.bluetoothAdapterIsNull()
    }

    fun bluetoothAdapterIsEnabled(): Boolean {
        return bluetooth.bluetoothAdapterIsEnabled()
    }

    fun bluetoothSocketIsNull(): Boolean {
        return bluetooth.bluetoothSocketIsNull()
    }

    fun bluetoothPairedDevicesUpdate(){
        bluetooth.bluetoothPairedDevicesUpdate()
    }

    fun getBluetoothDeviceNameList(): List<String> {
        return bluetooth.getBluetoothDeviceNameList()
    }

    fun getBluetoothDevice(deviceName: String): BluetoothDevice? {
        return bluetooth.getBluetoothDevice(deviceName)
    }

    fun bluetoothClose(){
        bluetooth.close()
    }

    fun connect(deviceName: String){
        viewModelScope.launch {
            setComments("\n\nConnect to the $deviceName ...".toByteArray())
            val device: BluetoothDevice? = bluetooth.getBluetoothDevice(deviceName)
            if ((device != null) && (bluetooth.connectClient(device))) {
                connected()
                bluetooth.read().collect {
                    setComments(it, Color.LTGRAY)
                }
            }

            disconnected()

        }
    }

    private fun connected(){
        setComments("\nConnected \n".toByteArray())
        _connected.value=true
    }

    fun disconnected(){
        bluetooth.close()
        if(_connected.value == true){
            _connected.value=false
            setComments("\nDisconnected".toByteArray())
        }
        else {
            setComments("\nCouldn't connect".toByteArray())
        }
    }

    fun writeString(newString: String){
        if(connected.value!=true)return
        viewModelScope.launch {
            bluetooth.writeString(newString+"\n")
            setComments(("\n"+newString).toByteArray(), Color.GREEN)
        }
    }

    fun write(byteArray: ByteArray){
        viewModelScope.launch {
            bluetooth.write(byteArray)
        }
    }

}