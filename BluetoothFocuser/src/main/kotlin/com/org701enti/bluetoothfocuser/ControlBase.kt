package com.org701enti.bluetoothfocuser

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothStatusCodes
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 蓝牙控制基础,每个独立唯一设备的控制和回调等相关资源被封装到ControlBaseBluetooth
 * 将ControlBaseBluetooth当作BluetoothGattCallback,通过BluetoothDevice实例运行连接,将自动进行ControlBaseBluetooth实例的完善
 * 完善之后即可通过get设备的BluetoothControl和BluetoothUI实例,进行控制和用户界面绘制
 * @param deviceModel BluetoothDeviceModel设备模型
 */
abstract class ControlBase(deviceModel: BluetoothDeviceModel) :
    BluetoothGattCallback(), BluetoothControl.BluetoothGattDataAccessCallback {
        
    private var gattIsReadBusy = false
    private var gattIsWriteBusy = false
    var gattState: Int //蓝牙BLE-GATT实例的状态码
    var bluetoothControl: BluetoothControl? = null //蓝牙控制实例(仅支持了非用户操作的控制)


    //读取和写入Flow,在调用controlFragment.consoleShowBluetooth()切换控制设备时需要确保队列清空并关闭订阅
    private val readBleFlow = MutableSharedFlow<BleReadMessage>(extraBufferCapacity = 256, replay = 0, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private var readBlePendingCount = 0 //等待读取数量
    private val writeBleFlow = MutableSharedFlow<BleWriteMessage>(extraBufferCapacity = 256, replay = 0, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private var writeBlePendingCount = 0 //等待写入数量


    //Message数据类
    data class BleReadMessage(
        val deviceSha256: String,
        val characteristic: BluetoothGattCharacteristic,
    )

    data class BleWriteMessage(
        val deviceSha256: String,
        val data: ByteArray,
        val writeType: Int,
        val characteristic: BluetoothGattCharacteristic,
    )


    var deviceModel: BluetoothDeviceModel //设备模型
    var bluetoothUI: BluetoothUI? =
        null //蓝牙用户界面实例(支持有用户界面环境的用户控制,并在内部链接BluetoothControl到View控件或其他控制器)

    init {
        gattState = BluetoothGatt.STATE_DISCONNECTED
        this.deviceModel = deviceModel

        CoroutineScope(Dispatchers.IO).launch {
            readBleFlow.collect { message ->
                if (message.deviceSha256 != deviceModel.deviceSha256) {
                    return@collect
                }
                while (gattIsReadBusy) {
                    delay(50)
                }
                nowReadCharacteristic(message.deviceSha256, message.characteristic)
                readBlePendingCount--
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            writeBleFlow.collect { data ->
                if (data.deviceSha256 != deviceModel.deviceSha256) {
                    return@collect
                }
                while (gattIsWriteBusy) {
                    delay(50)
                }
                nowWriteCharacteristic(
                    data.deviceSha256,
                    data.data,
                    data.writeType,
                    data.characteristic
                )
                writeBlePendingCount--
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun nowReadCharacteristic(
        deviceSha256: String,
        characteristic: BluetoothGattCharacteristic
    ): Boolean {
        if (bluetoothControl?.gatt != null && gattState == BluetoothGatt.STATE_CONNECTED) {
            if (deviceSha256 == deviceModel.deviceSha256) {
                gattIsReadBusy = true
                return bluetoothControl?.gatt?.readCharacteristic(characteristic) ?: false
            }
        }
        return false
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    fun nowWriteCharacteristic(
        deviceSha256: String,
        data: ByteArray,
        writeType: Int,
        characteristic: BluetoothGattCharacteristic
    ): Boolean {
        if (bluetoothControl?.gatt != null && gattState == BluetoothGatt.STATE_CONNECTED) {
            if (deviceSha256 == deviceModel.deviceSha256) {
                gattIsWriteBusy = true

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    return bluetoothControl?.gatt?.writeCharacteristic(
                        characteristic,
                        data,
                        writeType
                    ) == BluetoothStatusCodes.SUCCESS
                } else {
                    characteristic.setValue(data)
                    characteristic.writeType = writeType
                    return bluetoothControl?.gatt?.writeCharacteristic(characteristic) ?: false
                }
            }
        }
        return false
    }

    override fun requireGattState(): Int {
        return if (bluetoothControl?.gatt != null) {
            gattState
        } else {
            StandardSync.RESULT_FAIL_UNKNOWN
        }
    }

    override fun isDeviceSha256EqualsTo(deviceSha256: String): Boolean {
        return deviceModel.deviceSha256 == deviceSha256
    }

    override fun getAllServicesBluetoothGatt(deviceSha256: String): List<BluetoothGattService>? {
        if (bluetoothControl?.gatt != null && gattState == BluetoothGatt.STATE_CONNECTED) {
            if (deviceSha256 == deviceModel.deviceSha256) {
                return bluetoothControl?.gatt?.services
            }
        }
        return null
    }

    override fun getThisServiceAllCharacteristicsBluetoothGatt(
        deviceSha256: String,
        service: BluetoothGattService
    ): List<BluetoothGattCharacteristic>? {
        if (bluetoothControl?.gatt != null && gattState == BluetoothGatt.STATE_CONNECTED) {
            if (deviceSha256 == deviceModel.deviceSha256) {
                return service.characteristics
            }
        }
        return null
    }

    override fun getServiceBluetoothGatt(
        deviceSha256: String,
        serviceUuid: UUID
    ): BluetoothGattService? {
        if (bluetoothControl?.gatt != null && gattState == BluetoothGatt.STATE_CONNECTED) {
            if (deviceSha256 == deviceModel.deviceSha256) {
                return bluetoothControl?.gatt?.getService(serviceUuid)
            }
        }
        return null
    }

    override fun getCharacteristicsBluetoothGatt(
        deviceSha256: String,
        characteristicUuid: UUID,
        service: BluetoothGattService
    ): BluetoothGattCharacteristic? {
        if (bluetoothControl?.gatt != null && gattState == BluetoothGatt.STATE_CONNECTED) {
            if (deviceSha256 == deviceModel.deviceSha256) {
                return service.getCharacteristic(characteristicUuid)
            }
        }
        return null
    }

    override fun readCharacteristic(
        deviceSha256: String,
        characteristic: BluetoothGattCharacteristic
    ): Boolean {
        readBlePendingCount++
        return readBleFlow.tryEmit(BleReadMessage(deviceSha256, characteristic))
    }

    override fun writeCharacteristic(
        deviceSha256: String,
        data: ByteArray,
        writeType: Int,
        characteristic: BluetoothGattCharacteristic
    ): Boolean {
        writeBlePendingCount++
        return writeBleFlow.tryEmit(
            BleWriteMessage(
                deviceSha256,
                data,
                writeType,
                characteristic
            )
        )
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        super.onCharacteristicRead(gatt, characteristic, status)
//        if (this.deviceName != null) {
//            Log.i(
//                logTag,
//                "onCharacteristicRead: 接收到读取请求的回复,系统使用了旧版的回调重载 - " + this.deviceName
//            )
//        } else {
//            Log.i(
//                logTag,
//                "onCharacteristicRead: 接收到读取请求的回复,系统使用了旧版的回调重载 - " + this.deviceModel.deviceSha256
//            )
//        }
        if (status == BluetoothGatt.GATT_SUCCESS) {
            bluetoothControl?.dataUpdate(
                characteristic.service.uuid,
                characteristic.uuid,
                characteristic.value
            )
        }

        gattIsReadBusy = false
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int
    ) {
        super.onCharacteristicRead(gatt, characteristic, value, status)
//        if (this.deviceName != null) {
//            Log.i(
//                logTag,
//                "onCharacteristicRead: 接收到读取请求的回复,系统使用了较新的回调重载 - " + this.deviceName
//            )
//        } else {
//            Log.i(
//                logTag,
//                "onCharacteristicRead: 接收到读取请求的回复,系统使用了较新的回调重载 - " + this.deviceModel.deviceSha256
//            )
//        }
        if (status == BluetoothGatt.GATT_SUCCESS) {
            bluetoothControl?.dataUpdate(
                characteristic.service.uuid,
                characteristic.uuid,
                value
            )
        }

        gattIsReadBusy = false
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        super.onCharacteristicWrite(gatt, characteristic, status)
        gattIsWriteBusy = false
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        super.onCharacteristicChanged(gatt, characteristic, value)
        bluetoothControl?.dataUpdate(
            characteristic.service.uuid,
            characteristic.uuid,
            value
        )
    }

}