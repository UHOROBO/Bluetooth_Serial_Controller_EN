package com.main.bluetoothserialcontroller

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.main.bluetoothserialcontroller.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {
    private val viewModel : MainViewModel by viewModels()
    private var mTimer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.textView.movementMethod = ScrollingMovementMethod.getInstance()

        viewModel.textList.observe(this, {

            if(viewModel.buffFlag){
                val newString = binding.textView.text
                binding.textView.text = newString.takeLast((newString.length*(2.0/3.0)).toInt())
                viewModel.setTextSize(binding.textView.text.toString().toByteArray().size)
                viewModel.buffFlagFalse()

            }
            binding.textView.append(it)
        })

        viewModel.connected.observe(this, {
            invalidateOptionsMenu()
            if(viewModel.connected.value==true){

                val task2 : TimerTask = object : TimerTask() {
                    override fun run() {
                        val data = ByteArray(3)
                        data[0] =
                            ((binding.button4.isPressed.toInt() shl 3) + (binding.button3.isPressed.toInt() shl 2) + (binding.button2.isPressed.toInt() shl 1) + (binding.button1.isPressed.toInt())).toByte()
                        data[1] = ((binding.joystick.xPercent * 127.5f) + 127.5).toUInt().toByte()
                        data[2] = ((binding.joystick.yPercent * 127.5f) + 127.5).toUInt().toByte()

                        viewModel.write(data)
                    }
                }
                mTimer = Timer()
                mTimer!!.schedule(task2, 0, 50)
            }
            else{
                mTimer?.cancel()
            }
        })
    }

    fun Boolean.toInt() = if (this) 1 else 0

    override fun onDestroy() {
        super.onDestroy()
        viewModel.setTextSizeZero()
        mTimer?.cancel()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)

        val menuConnect : MenuItem = menu.findItem(R.id.connect)
        if(viewModel.connected.value == true)menuConnect.setIcon(R.drawable.ic_round_bluetooth_connected_24)
        else menuConnect.setIcon(R.drawable.ic_round_bluetooth_24)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.connect){
            if(viewModel.connected.value==false)bluetoothCheck()
            else viewModel.bluetoothClose()
        }
        return super.onOptionsItemSelected(item)
    }


    //bluetooth//////////////////////////////////
    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            viewModel.bluetoothPairedDevicesUpdate()
            alertDialogArray(*viewModel.getBluetoothDeviceNameList().toTypedArray())
        }

    private fun bluetoothCheck(): Boolean {
        if (viewModel.bluetoothAdapterIsNull()) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!viewModel.bluetoothAdapterIsEnabled()) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startForResult.launch(enableBtIntent);
        }
        else{
            viewModel.bluetoothPairedDevicesUpdate()
            alertDialogArray(*viewModel.getBluetoothDeviceNameList().toTypedArray())
        }
        return true
    }

    private fun alertDialogArray(vararg values: String) {
        if(values.isEmpty()){
            Toast.makeText(this, "No paired devices", Toast.LENGTH_SHORT).show()
            return
        }
        MaterialAlertDialogBuilder(this).setTitle("Select the device to be connected")
            .setItems(values) { dialog, which ->
                viewModel.connect(values[which])
            }
            .show()
    }

}