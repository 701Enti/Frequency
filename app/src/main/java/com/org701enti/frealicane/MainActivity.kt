//        701Enti MIT License
//
//        Copyright (c) 2024 701Enti
//
//        Permission is hereby granted, free of charge, to any person obtaining a copy
//        of this software and associated documentation files (the "Software"), to deal
//        in the Software without restriction, including without limitation the rights
//        to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//        copies of the Software, and to permit persons to whom the Software is
//        furnished to do so, subject to the following conditions:
//
//        The above copyright notice and this permission notice shall be included in all
//        copies or substantial portions of the Software.
//
//        THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//        IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//        FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//        AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//        LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//        OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//        SOFTWARE.
package com.org701enti.frealicane

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.org701enti.bluetoothfocuser.BluetoothControl
import com.org701enti.bluetoothfocuser.BluetoothControl.BluetoothGattDataAccessCallback
import com.org701enti.bluetoothfocuser.BluetoothUI
import com.org701enti.bluetoothfocuser.StandardSync
import com.org701enti.frealicane.BleFragment.BleFragmentRunWant
import com.org701enti.frealicane.BleFragment.BluetoothDeviceModel
import com.org701enti.frealicane.ControlFragment.ControlFragmentRunWant
import data.DeviceBle.DeviceBleDatabase.BleDeviceMainDatabase
import data.DeviceBle.DeviceBleEntity.BleDeviceMainEntity
import org.greenrobot.eventbus.EventBus
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

class MainActivity : AppCompatActivity() {
    var logTag: String = "MainActivity"
    private lateinit var standardSync: StandardSync

    //全局事件类型
    //基本事件类型,建议继承以适配相关通用型业务
    abstract class BaseEvent(val makerId: String, val makerState: Int) {
        val makeTimestamp: Long = System.currentTimeMillis()
    }

    //设备状态相关事件类型
    class DeviceStateEvent(makerId: String, makerState: Int) :
        BaseEvent(makerId, makerState) {
        companion object {
            //状态唯一决定,不同时对于两个或多个状态
            //连接中 -> 已连接 -> 控制面板部署中 -> 控制面板部署完成
            //正在断开连接 -> 连接已断开
            const val DEVICE_STATE_CONNECTING: Int = 100 //连接中
            const val DEVICE_STATE_CONNECTED: Int = 101 //已连接
            const val DEVICE_STATE_DISCONNECTING: Int = 102 //正在断开连接
            const val DEVICE_STATE_DISCONNECTED: Int = 103 //连接已断开
            const val DEVICE_STATE_CONTROL_PLATE_DEPLOYING: Int = 104 //控制面板部署中
            const val DEVICE_STATE_CONTROL_PLATE_DEPLOYED: Int = 105 //控制面板部署完成
        }
    }

    //蓝牙相关
    //蓝牙控制基础类的实例列表
    private val controlBaseListBluetooth: MutableList<ControlBaseBluetooth> = ArrayList()

    /**
     * 蓝牙控制基础,每个独立唯一设备的控制和回调等相关资源被封装到ControlBaseBluetooth
     * 将ControlBaseBluetooth当作BluetoothGattCallback,通过BluetoothDevice实例运行连接,将自动进行ControlBaseBluetooth实例的完善
     * 完善之后即可通过get设备的BluetoothControl和BluetoothUI实例,进行控制和用户界面绘制
     * @param deviceSha256Bluetooth 连接的蓝牙设备广播数据的SHA-256校验码,即操作gatt实例以进行蓝牙相关控制的确认凭证
     * @param deviceName            连接的蓝牙设备名称,允许为空(因为蓝牙设备的设备名本身可以没有)
     */
    inner class ControlBaseBluetooth(deviceName: String?, deviceSha256Bluetooth: String) :
        BluetoothGattCallback(), BluetoothGattDataAccessCallback {
        private var gatt: BluetoothGatt? = null //蓝牙BLE-GATT实例
        private var gattState: Int //蓝牙BLE-GATT实例的状态码
        private var deviceName: String? = null //连接的蓝牙设备名称,允许为空(因为蓝牙设备的设备名本身可以没有)
        var deviceSha256Bluetooth: String //连接的蓝牙设备广播数据的SHA-256校验码,即操作gatt实例以进行蓝牙相关控制的确认凭证
        private var bluetoothControl: BluetoothControl? = null //蓝牙控制实例(仅支持了非用户操作的控制)
        var bluetoothUI: BluetoothUI? =
            null //蓝牙用户界面实例(支持有用户界面环境的用户控制,并在内部链接BluetoothControl到View控件或其他控制器)
            private set

        init {
            gattState = BluetoothGatt.STATE_DISCONNECTED
            this.deviceName = deviceName
            this.deviceSha256Bluetooth = deviceSha256Bluetooth
        }

        //BluetoothControl.BluetoothGattDataAccessCallback实现
        override fun provideStandardSync(): StandardSync {
            return standardSync
        }

        override fun getGattState(): Int {
            return if (gatt != null) {
                gattState
            } else {
                StandardSync.RESULT_FAIL_UNKNOWN
            }
        }

        override fun isDeviceSha256EqualsTo(deviceSha256: String): Boolean {
            return deviceSha256Bluetooth == deviceSha256
        }

        override fun getAllServicesBluetoothGatt(deviceSha256: String): List<BluetoothGattService>? {
            if (gatt != null && gattState == BluetoothGatt.STATE_CONNECTED) {
                if (deviceSha256 == deviceSha256Bluetooth) {
                    return gatt?.services
                }
            }
            return null
        }

        override fun getThisServiceAllCharacteristicsBluetoothGatt(
            deviceSha256: String,
            service: BluetoothGattService
        ): List<BluetoothGattCharacteristic>? {
            if (gatt != null && gattState == BluetoothGatt.STATE_CONNECTED) {
                if (deviceSha256 == deviceSha256Bluetooth) {
                    return service.characteristics
                }
            }
            return null
        }

        override fun getServiceBluetoothGatt(
            deviceSha256: String,
            serviceUuid: UUID
        ): BluetoothGattService? {
            if (gatt != null && gattState == BluetoothGatt.STATE_CONNECTED) {
                if (deviceSha256 == deviceSha256Bluetooth) {
                    return gatt?.getService(serviceUuid)
                }
            }
            return null
        }

        override fun getCharacteristicsBluetoothGatt(
            deviceSha256: String,
            characteristicUuid: UUID,
            service: BluetoothGattService
        ): BluetoothGattCharacteristic? {
            if (gatt != null && gattState == BluetoothGatt.STATE_CONNECTED) {
                if (deviceSha256 == deviceSha256Bluetooth) {
                    return service.getCharacteristic(characteristicUuid)
                }
            }
            return null
        }

        @SuppressLint("MissingPermission")
        override fun readCharacteristic(
            deviceSha256: String,
            characteristic: BluetoothGattCharacteristic
        ): Boolean {
            if (gatt != null && gattState == BluetoothGatt.STATE_CONNECTED) {
                if (deviceSha256 == deviceSha256Bluetooth) {
                    return gatt?.readCharacteristic(characteristic) ?: false
                }
            }
            return false
        }

        @SuppressLint("MissingPermission")
        @Suppress("DEPRECATION")
        override fun writeCharacteristic(
            deviceSha256: String,
            data: ByteArray,
            writeType: Int,
            characteristic: BluetoothGattCharacteristic
        ): Boolean {
            if (gatt != null && gattState == BluetoothGatt.STATE_CONNECTED) {
                if (deviceSha256 == deviceSha256Bluetooth) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        return gatt?.writeCharacteristic(
                            characteristic,
                            data,
                            writeType
                        ) == BluetoothStatusCodes.SUCCESS
                    } else {
                        characteristic.setValue(data)
                        characteristic.writeType = writeType
                        return gatt?.writeCharacteristic(characteristic) ?:false
                    }
                }
            }
            return false
        }

        override fun onBluetoothControlInitFinished() {
            bluetoothControl?.let { bluetoothControl ->
                this.bluetoothUI = BluetoothUI(bluetoothControl, this@MainActivity)
                DEVICE_STATE_EVENT_BUS.post(
                    DeviceStateEvent(
                        deviceSha256Bluetooth,
                        DeviceStateEvent.DEVICE_STATE_CONTROL_PLATE_DEPLOYED
                    )
                )
            }


        }


        //BluetoothGattCallback实现
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            gattState = newState

            when (newState) {
                BluetoothGatt.STATE_CONNECTING -> DEVICE_STATE_EVENT_BUS.post(
                    DeviceStateEvent(
                        deviceSha256Bluetooth,
                        DeviceStateEvent.DEVICE_STATE_CONNECTING
                    )
                )

                BluetoothGatt.STATE_CONNECTED -> {
                    DEVICE_STATE_EVENT_BUS.post(
                        DeviceStateEvent(
                            deviceSha256Bluetooth,
                            DeviceStateEvent.DEVICE_STATE_CONNECTED
                        )
                    )
                    if (this.deviceName != null) {
                        Log.i(logTag, "onConnectionStateChange: 已连接到 - " + this.deviceName)
                    } else {
                        Log.i(
                            logTag,
                            "onConnectionStateChange: 已连接到 - " + this.deviceSha256Bluetooth
                        )
                    }
                    gatt.discoverServices() //如果状态为已经连接,就扫描服务
                }

                BluetoothGatt.STATE_DISCONNECTING -> DEVICE_STATE_EVENT_BUS.post(
                    DeviceStateEvent(
                        deviceSha256Bluetooth,
                        DeviceStateEvent.DEVICE_STATE_DISCONNECTING
                    )
                )

                BluetoothGatt.STATE_DISCONNECTED -> DEVICE_STATE_EVENT_BUS.post(
                    DeviceStateEvent(
                        deviceSha256Bluetooth,
                        DeviceStateEvent.DEVICE_STATE_DISCONNECTED
                    )
                )

                else -> {}
            }
            this.gatt = gatt //缓存实例引用
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            DEVICE_STATE_EVENT_BUS.post(
                DeviceStateEvent(
                    deviceSha256Bluetooth,
                    DeviceStateEvent.DEVICE_STATE_CONTROL_PLATE_DEPLOYING
                )
            )
            bluetoothControl = BluetoothControl(deviceSha256Bluetooth, this.deviceName, this)
        }

        @Deprecated("Deprecated in Java")
        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (this.deviceName != null) {
                Log.i(
                    logTag,
                    "onCharacteristicRead: 接收到读取请求的回复,系统使用了旧版的回调重载 - " + this.deviceName
                )
            } else {
                Log.i(
                    logTag,
                    "onCharacteristicRead: 接收到读取请求的回复,系统使用了旧版的回调重载 - " + this.deviceSha256Bluetooth
                )
            }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                bluetoothControl?.dataUpdate(
                    characteristic.service.uuid,
                    characteristic.uuid,
                    characteristic.value
                )
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, value, status)
            if (this.deviceName != null) {
                Log.i(
                    logTag,
                    "onCharacteristicRead: 接收到读取请求的回复,系统使用了较新的回调重载 - " + this.deviceName
                )
            } else {
                Log.i(
                    logTag,
                    "onCharacteristicRead: 接收到读取请求的回复,系统使用了较新的回调重载 - " + this.deviceSha256Bluetooth
                )
            }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                bluetoothControl?.dataUpdate(
                    characteristic.service.uuid,
                    characteristic.uuid,
                    value
                )
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
        }
    }


    //Fragment需求操作实现
    inner class BleFragmentFunctionRun : BleFragmentRunWant {
        @SuppressLint("MissingPermission")
        override fun startControl(device: BluetoothDevice?, sha256: String?) {
            if (device == null || sha256 == null) {
                Log.i(logTag, "BleFragmentRunWant - startControl: 存在参数为空,无法启动控制")
                return
            }
            //检查是否将会重复创建同一设备的连接
            for (base in controlBaseListBluetooth) {
                if (base.deviceSha256Bluetooth == sha256) {
                    Log.w(
                        logTag,
                        "BleFragmentRunWant - startControl: 阻止了用户对同一个设备重复连接的请求 : [${device.name}]  [$sha256]"
                    )
                    return
                }
            }
            //创建并保存ControlBaseBluetooth实例
            val controlBaseBluetooth = ControlBaseBluetooth(device.name, sha256)
            controlBaseListBluetooth.add(controlBaseBluetooth)
            //运行连接,自动进行ControlBaseBluetooth实例的完善
            device.connectGatt(this@MainActivity, true, controlBaseBluetooth)
        }

        override fun getControlBaseBluetooth(sha256: String?): ControlBaseBluetooth? {
            if (controlBaseListBluetooth.isNotEmpty() && sha256 != null) {
                for (controlBase in controlBaseListBluetooth) {
                    if (controlBase.deviceSha256Bluetooth == sha256) {
                        return controlBase
                    }
                }
            }
            return null
        }
    }

    inner class ControlFragmentFunctionRun(override var controlBaseListBluetooth: List<ControlBaseBluetooth>) : ControlFragmentRunWant

    //Fragment需求操作实例的分配
    val bleFragmentFunctionRun: BleFragmentFunctionRun = BleFragmentFunctionRun()
    val controlFragmentFunctionRun: ControlFragmentFunctionRun = ControlFragmentFunctionRun(controlBaseListBluetooth)

    //权限相关
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //        如果用户已经拒绝这个权限请求
        if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            //显示一个提示框,希望用户改变主意
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.hint_chinese)
            builder.setMessage(R.string.app_need_permission_chinese)
            //配置授予按钮
            builder.setPositiveButton(R.string.give_chinese) { _, _ ->
                //给用户再次的选择,用户点击"授予",会弹出系统的应用信息,里面有权限管理,但是用户这时可能又矛盾地没有允许对应权限
                //如果用户一直这样做,最终会一直在当前这个if里循环,直到正式同意权限或点击这里创建的提示框的"拒绝"
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", getString(R.string.package_name), null)
                intent.setData(uri)
                startActivity(intent)
                PermissionRequestingFlag = false //重置请求中标识,关闭独立线程的阻塞
            }
            //配置拒绝按钮
            builder.setNegativeButton(R.string.reject_chinese) { _, _ ->
                PermissionRequestingFlag = false //重置请求中标识,关闭独立线程的阻塞
            }
            val dialog = builder.create()
            dialog.show() //弹出提示框
        } else { //如果同意对应请求
            when (requestCode) {
                REQUEST_COARSE_LOCATION -> {}
                REQUEST_FINE_LOCATION -> {}
                REQUEST_BLUETOOTH_SCAN -> {}
                REQUEST_BLUETOOTH_ADVERTISE -> {}
                REQUEST_BLUETOOTH_CONNECT -> {}
                else -> {}
            }
            PermissionRequestingFlag = false //重置请求中标识,关闭独立线程的阻塞
        }
    }

    /**
     * (含阻塞,必须使用非主线程调用)权限检查,如果权限未授予或拒绝,会进行对应权限申请工作
     *
     * @param requestCode 权限申请码,参考MainActivity开头的权限申请码定义
     */
    @Throws(InterruptedException::class)
    private fun permissionApplyCheck(requestCode: Int) {
        //禁止在主线程执行,因为本方法内含阻塞,会阻塞调用线程,应该使用其他非服务线程调用
        if (Thread.currentThread().name == "main") {
            throw InterruptedException(getString(R.string.permission_apply_check_threader))
        }
        //权限申请时使用主线程请求,调用本方法的线程处理请求时的阻塞,防止连续请求
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            when (requestCode) {
                REQUEST_COARSE_LOCATION -> if (ContextCompat.checkSelfPermission(
                        this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    PermissionRequestingFlag = true
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                        requestCode
                    )
                }

                REQUEST_FINE_LOCATION -> if (ContextCompat.checkSelfPermission(
                        this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    PermissionRequestingFlag = true
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        requestCode
                    )
                }

                REQUEST_BLUETOOTH_SCAN -> if (ContextCompat.checkSelfPermission(
                        this@MainActivity, Manifest.permission.BLUETOOTH_SCAN
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    PermissionRequestingFlag = true
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ActivityCompat.requestPermissions(
                            this@MainActivity,
                            arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                            requestCode
                        )
                    }
                }

                REQUEST_BLUETOOTH_ADVERTISE -> if (ContextCompat.checkSelfPermission(
                        this@MainActivity, Manifest.permission.BLUETOOTH_ADVERTISE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    PermissionRequestingFlag = true
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ActivityCompat.requestPermissions(
                            this@MainActivity,
                            arrayOf(Manifest.permission.BLUETOOTH_ADVERTISE),
                            requestCode
                        )
                    }
                }

                REQUEST_BLUETOOTH_CONNECT -> if (ContextCompat.checkSelfPermission(
                        this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    PermissionRequestingFlag = true
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ActivityCompat.requestPermissions(
                            this@MainActivity,
                            arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                            requestCode
                        )
                    }
                }

                else -> {}
            }
        }
        while (PermissionRequestingFlag) {
            Thread.sleep(50)
        }
    }

    //UI配置调度与Fragment管理
    private lateinit var managerFragmentMain: FragmentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v: View, insets: WindowInsetsCompat ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //初始化standardSync
        standardSync =
            StandardSync(StandardSync.STANDARD_ACCORDING_FILE_IN_ASSETS, this@MainActivity)

        //配置fragment
        if (savedInstanceState == null) {
            managerFragmentMain = supportFragmentManager
            val fragmentTransaction = managerFragmentMain.beginTransaction()

            val bleFragment = BleFragment.newInstance()
            val controlFragment = ControlFragment.newInstance()
            fragmentTransaction.add(
                R.id.main_fragment_container_in_main,
                bleFragment,
                getString(R.string.tag_ble_main_transaction)
            )
            fragmentTransaction.add(
                R.id.main_fragment_container_in_main,
                controlFragment,
                getString(R.string.tag_control_main_transaction)
            )
            fragmentTransaction.hide(bleFragment)
            fragmentTransaction.hide(controlFragment)
            fragmentTransaction.commitNow()
        }

        initMainUI()
    }

    ////UI-底部导航栏
    private val handlerMainBottomNavView =
        arrayOf<Handler?>(null) //缓存ThreadMainBottomNavView线程handler

    //选择标签的监听
    inner class MainBottomNavigationListener : NavigationBarView.OnItemSelectedListener {
        override fun onNavigationItemSelected(item: MenuItem): Boolean {
            //独立线程ThreadMainBottomNavView执行

            handlerMainBottomNavView[0]?.post {
                when (item.itemId) {
                    R.id.NavigationDevice -> {}
                    R.id.NavigationBLE -> //权限检查
                        try {
                            permissionApplyCheck(REQUEST_FINE_LOCATION)
                            permissionApplyCheck(REQUEST_COARSE_LOCATION)
                            if (AndroidVersion >= Build.VERSION_CODES.S) {
                                permissionApplyCheck(REQUEST_BLUETOOTH_SCAN)
                                permissionApplyCheck(REQUEST_BLUETOOTH_ADVERTISE)
                                permissionApplyCheck(REQUEST_BLUETOOTH_CONNECT)
                            }
                        } catch (inter: InterruptedException) {
                            Thread.currentThread().interrupt()
                        }

                    R.id.NavigationControl -> {}
                    R.id.NavigationWIFI -> {}
                    R.id.NavigationMe -> {}
                    else -> {}
                }
            }

            //主线程执行
            hideFragment(getString(R.string.tag_ble_main_transaction))

            when (item.itemId) {
                R.id.NavigationDevice -> {}
                R.id.NavigationBLE -> {
                    hideFragment(getString(R.string.tag_control_main_transaction))
                    showFragment(getString(R.string.tag_ble_main_transaction))
                }

                R.id.NavigationControl -> {
                    hideFragment(getString(R.string.tag_ble_main_transaction))
                    showFragment(getString(R.string.tag_control_main_transaction))
                    val obj =
                        getFragment(getString(R.string.tag_control_main_transaction))
                    if (obj is ControlFragment) {
                        val runWant: ControlFragmentRunWant = obj.controlFragmentRunWant
                        obj.consoleShowBluetooth(runWant.controlBaseListBluetooth[0])
                    }
                }

                R.id.NavigationWIFI -> {}
                R.id.NavigationMe -> {}
                else -> {}
            }
            return true
        }
    }

    private fun initMainBottomNavigation() {
        val mainBottomNavView =
            findViewById<BottomNavigationView>(R.id.main_bottom_navigation_in_main)
        //创建一个线程处理底部导航栏业务(含Looper)
        val threadMainBottomNavView = Thread {
            Looper.prepare()
            handlerMainBottomNavView[0] = Handler(Looper.myLooper()!!)
            Looper.loop()
        }
        threadMainBottomNavView.start() //启动该线程

        mainBottomNavView.setOnItemSelectedListener(MainBottomNavigationListener())
    }

    /**
     * 隐藏掉指定的Fragment,通过TAG
     *
     * @param tag 在add时注册的TAG
    */
    fun hideFragment(tag: String?) {
        val transaction = managerFragmentMain.beginTransaction()
        val fragment = managerFragmentMain.findFragmentByTag(tag)
        if (fragment != null) {
            transaction.hide(fragment)
            transaction.commit()
        }
    }

    /**
     * 隐藏掉指定的Fragment,通过Fragment的容器ID
     *
     * @param id  布局文件中设置的Fragment的容器ID
     */
    fun hideFragment(id: Int) {
        val transaction = managerFragmentMain.beginTransaction()
        val fragment = managerFragmentMain.findFragmentById(id)
        if (fragment != null) {
            transaction.hide(fragment)
            transaction.commit()
        }
    }

    /**
     * 显示出指定的Fragment,通过TAG
     *
     * @param tag              在add时注册的TAG
     */
    fun showFragment(tag: String?) {
        val transaction = managerFragmentMain.beginTransaction()
        val fragment = managerFragmentMain.findFragmentByTag(tag)
        if (fragment != null) {
            transaction.show(fragment)
            transaction.commit()
        }
    }

    /**
     * 显示出指定的Fragment,通过Fragment的容器ID
     *
     * @param id  布局文件中设置的Fragment的容器ID
     */
    fun showFragment(id: Int) {
        val transaction = managerFragmentMain.beginTransaction()
        val fragment = managerFragmentMain.findFragmentById(id)
        if (fragment != null) {
            transaction.show(fragment)
            transaction.commit()
        }
    }

    /**
     * 获取指定的Fragment,通过TAG
     *
     * @param tag              在add时注册的TAG
     */
    fun getFragment(tag: String?): Any? {
        val fragment = managerFragmentMain.findFragmentByTag(tag)
        return fragment
    }

    /**
     * 获取指定的Fragment,通过Fragment的容器ID
     *
     * @param id  布局文件中设置的Fragment的容器ID
     */
    fun getFragment(id: Int): Any? {
        val fragment = managerFragmentMain.findFragmentById(id)
        return fragment
    }


    ////主UI
    private fun initMainUI() {
        initMainBottomNavigation()
    }

    companion object {
        ////全局事件总线单例
        val DEVICE_STATE_EVENT_BUS: EventBus = EventBus.builder().build()


        ////权限检查和提取
        val AndroidVersion: Int = Build.VERSION.SDK_INT

        ////权限申请码定义
        private const val REQUEST_COARSE_LOCATION = 100
        private const val REQUEST_FINE_LOCATION = 101
        private const val REQUEST_BLUETOOTH_SCAN = 102
        private const val REQUEST_BLUETOOTH_ADVERTISE = 103
        private const val REQUEST_BLUETOOTH_CONNECT = 104

        ////权限申请标志
        private var PermissionRequestingFlag = false

        /**
         * (含阻塞,请使用非主线程调用)插入或更新到BleDeviceMainDatabase数据库
         *
         * @param targetModel 目标蓝牙设备模型
         * @param context     上下文,可以使用Activity作为上下文
         * @return 操作设备的bleDeviceSha256, 发生异常为null
         */
        @SuppressLint("MissingPermission")
        @Throws(InterruptedException::class)
        fun addToBleDeviceMainDatabase(
            targetModel: BluetoothDeviceModel?,
            context: Context
        ): String? {
            if (targetModel == null) {
                return null
            }

            //禁止在主线程执行,因为本方法内含阻塞,会阻塞调用线程,应该使用其他非服务线程调用
            if (Thread.currentThread().name == "main") {
                throw InterruptedException(context.getString(R.string.permission_apply_check_threader))
            }

            val sha256Buf = AtomicReference<String?>()
            val isComplete = AtomicReference(java.lang.Boolean.FALSE)

            val thread = Thread(Runnable {
                //检查
                val database: BleDeviceMainDatabase? = BleDeviceMainDatabase.getDatabase(context)

                if (database == null) {
                    isComplete.set(true)
                    return@Runnable
                }

                //缓存当前时间戳
                val currentTimestamp = System.currentTimeMillis()

                //缓存名称
                val nameBuf: String? = if (targetModel.device == null) { //是伪造设备
                    context.getString(R.string.fake_device_chinese)
                } else { //不是伪造设备
                    if (targetModel.device?.name != null) {
                        targetModel.device?.name
                    } else { //未知设备
                        context.getString(R.string.unknown_device_chinese)
                    }
                }

                //获取操作实体和目标动作
                val updateTargetEntity =
                    database.bleDeviceMainDao()
                        .getByNameThenBleDeviceSha256(nameBuf, targetModel.deviceSha256)
                if (updateTargetEntity != null) {
                    //已经存在,进行更新操作
                    updateTargetEntity.bleDeviceIconId = targetModel.iconID //设置图标ID
                    updateTargetEntity.lastActiveTimestamp = currentTimestamp //设置最近活动时间戳为当前时间
                    database.bleDeviceMainDao().update(updateTargetEntity)
                    Log.i(
                        "AddToBleDeviceMainDatabase",
                        context.getString(R.string.update_device_information_chinese)
                    )

                    Log.i(
                        "AddToBleDeviceMainDatabase",
                        context.getString(R.string.update_chinese) + "bleDeviceId:" + "[" + updateTargetEntity.bleDeviceId + "]"
                    )
                    Log.i(
                        "AddToBleDeviceMainDatabase",
                        context.getString(R.string.update_chinese) + "bleDeviceName:" + updateTargetEntity.bleDeviceName
                    )
                    Log.i(
                        "AddToBleDeviceMainDatabase",
                        context.getString(R.string.update_chinese) + "bleDeviceIconId:" + updateTargetEntity.bleDeviceIconId
                    )
                    Log.i(
                        "AddToBleDeviceMainDatabase",
                        context.getString(R.string.update_chinese) + "lastActiveTimestamp:" + updateTargetEntity.lastActiveTimestamp
                    )
                    Log.i(
                        "AddToBleDeviceMainDatabase",
                        context.getString(R.string.update_chinese) + "bleDeviceSha256:" + updateTargetEntity.bleDeviceSha256
                    )

                    sha256Buf.set(updateTargetEntity.bleDeviceSha256)
                } else {
                    //新设备,进行插入操作
                    val entityNew = BleDeviceMainEntity()
                    entityNew.bleDeviceName = nameBuf!! //设置设备名称
                    entityNew.bleDeviceIconId = targetModel.iconID //设置图标ID
                    entityNew.lastActiveTimestamp = currentTimestamp //设置最近活动时间戳为当前时间
                    entityNew.bleDeviceSha256 = targetModel.deviceSha256 //设置设备的SHA-256唯一性与安全校验码
                    database.bleDeviceMainDao().insert(entityNew)
                    Log.i(
                        "AddToBleDeviceMainDatabase",
                        context.getString(R.string.insert_new_device_information_chinese)
                    )

                    //检查是否成功
                    val nowEntity =
                        database.bleDeviceMainDao().getByNameThenBleDeviceSha256(
                            entityNew.bleDeviceName,
                            entityNew.bleDeviceSha256
                        )
                    if (nowEntity != null) {
                        Log.i(
                            "AddToBleDeviceMainDatabase",
                            context.getString(R.string.complete_insert_data_following_chinese)
                        )
                        Log.i(
                            "AddToBleDeviceMainDatabase",
                            context.getString(R.string.insert_chinese) + "bleDeviceId:" + "[" + nowEntity.bleDeviceId + "]"
                        )
                        Log.i(
                            "AddToBleDeviceMainDatabase",
                            context.getString(R.string.insert_chinese) + "bleDeviceName:" + nowEntity.bleDeviceName
                        )
                        Log.i(
                            "AddToBleDeviceMainDatabase",
                            context.getString(R.string.insert_chinese) + "bleDeviceIconId:" + nowEntity.bleDeviceIconId
                        )
                        Log.i(
                            "AddToBleDeviceMainDatabase",
                            context.getString(R.string.insert_chinese) + "lastActiveTimestamp:" + nowEntity.lastActiveTimestamp
                        )
                        Log.i(
                            "AddToBleDeviceMainDatabase",
                            context.getString(R.string.insert_chinese) + "bleDeviceSha256:" + nowEntity.bleDeviceSha256
                        )

                        sha256Buf.set(nowEntity.bleDeviceSha256)
                    } else {
                        Log.e(
                            "AddToBleDeviceMainDatabase",
                            context.getString(R.string.insert_data_error_chinese)
                        )
                        sha256Buf.set(null)
                    }
                }
                isComplete.set(true)
            })

            thread.start()
            while (!isComplete.get()) {
                Thread.sleep(50)
            }

            return sha256Buf.get()
        }

        /**
         * 通过Log输出BleDeviceMainDatabase数据库数据,可以在AndroidStudio的"Logcat"查看Log打印
         *
         * @param context 上下文,可以使用Activity作为上下文
         */
        fun logShowBleDeviceMainDatabase(context: Context?) {
            Thread(Runnable {
                val database: BleDeviceMainDatabase = BleDeviceMainDatabase.getDatabase(context)
                    ?: return@Runnable

                val list = database.bleDeviceMainDao().allGet()
                //遍历显示所有实体
                for (entity in list) {
                    Log.i(
                        "ShowBleDeviceMainDatabase",
                        "------------------------------------------------------"
                    )
                    Log.i(
                        "ShowBleDeviceMainDatabase",
                        "|" + "bleDeviceId:" + "[" + entity.bleDeviceId + "]"
                    )
                    Log.i(
                        "ShowBleDeviceMainDatabase",
                        "|" + "bleDeviceName:" + entity.bleDeviceName
                    )
                    Log.i(
                        "ShowBleDeviceMainDatabase",
                        "|" + "bleDeviceIconId:" + entity.bleDeviceIconId
                    )
                    Log.i(
                        "ShowBleDeviceMainDatabase",
                        "|" + "lastActiveTimestamp:" + entity.lastActiveTimestamp
                    )
                    Log.i(
                        "ShowBleDeviceMainDatabase",
                        "|" + "bleDeviceSha256:" + entity.bleDeviceSha256
                    )
                }
                Log.i(
                    "ShowBleDeviceMainDatabase",
                    "------------------------------------------------------"
                )
            }).start()
        }
    }
}