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

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import com.org701enti.bluetoothfocuser.BluetoothControl
import com.org701enti.bluetoothfocuser.StandardSync
import com.org701enti.frealicane.BleFragment.BleFragmentRunWant
import com.org701enti.frealicane.ControlFragment.ControlFragmentRunWant
import com.org701enti.frealicane.core.datastore.DarkModeSetting
import data.DeviceBle.DeviceBleDatabase.BleDeviceMainDatabase
import data.DeviceBle.DeviceBleEntity.BleDeviceMainEntity
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.atomic.AtomicReference
import com.org701enti.bluetoothfocuser.BluetoothDeviceModel
import com.org701enti.bluetoothfocuser.BluetoothUI
import com.org701enti.bluetoothfocuser.ControlBase

class MainActivity : AppCompatActivity() {
    var logTag: String = "MainActivity"
    private var standardSync: StandardSync? = null
    private var recentBackTime: Long = 0 //上一次点击返回键/触发返回操作的时间
    private var backPageNameStack = mutableListOf<String>()

    val fragmentNameStart = "DeviceFragment" //App的起始页面
    private val fragmentNavigationIdStart = R.id.NavigationDevice //起始页对应的NavigationID
    val fragmentSwitchFuncStart = ::switchDeviceFragment

    //Fragment
    private var bleFragment: BleFragment? = null
    private var controlFragment: ControlFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        when (DarkModeSetting.getMode(this)) {
            DarkModeSetting.USE_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            DarkModeSetting.USE_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

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


        if (savedInstanceState == null) {
            //创建fragment
            bleFragment = BleFragment.newInstance()
            controlFragment = ControlFragment.newInstance()
        } else {
            //恢复fragment
            bleFragment = supportFragmentManager.findFragmentByTag("BleFragment") as BleFragment?
            controlFragment =
                supportFragmentManager.findFragmentByTag("controlFragment") as ControlFragment?
        }


        //配置fragment
        if (savedInstanceState == null) {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            if (bleFragment != null && controlFragment != null) {
                fragmentTransaction
                    .add(
                        R.id.main_fragment_container_in_main,
                        bleFragment!!,
                        "BleFragment"
                    )
                    .add(
                        R.id.main_fragment_container_in_main,
                        controlFragment!!,
                        "ControlFragment"
                    )
                    .commitNow()
            }
        }

        //初始化控件
        initMainBottomNavigation()

        //选择到起始页
        selectItemMainBottomNavigation(fragmentNavigationIdStart)

        //设置返回建逻辑
        onBackPressedDispatcher.addCallback {

            if (backPageNameStack.size == 1) {
                if (System.currentTimeMillis() - recentBackTime < 2000) {
                    finish()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "再返回一次退出" + getString(R.string.app_name),
                        Toast.LENGTH_SHORT
                    ).show()
                    recentBackTime = System.currentTimeMillis()
                }
                return@addCallback
            } else {
                backPageNameStack.removeLastOrNull()
            }

            //判断需要返回到的位置
            val itemId = when (backPageNameStack.lastOrNull()) {
                "DeviceFragment" -> {
                    switchDeviceFragment()
                    R.id.NavigationDevice
                }

                "BleFragment" -> {
                    switchBleFragment()
                    R.id.NavigationBle
                }

                "ControlFragment" -> {
                    switchControlFragment()
                    R.id.NavigationControl
                }

                "WifiFragment" -> {
                    switchWifiFragment()
                    R.id.NavigationWifi
                }

                "MeFragment" -> {
                    switchMeFragment()
                    R.id.NavigationMe
                }

                else -> {
                    fragmentSwitchFuncStart()
                    fragmentNavigationIdStart
                }
            }

            //仅更新UI,临时删除监听,避免触发Selected逻辑导致switch方法意外调用引发返回栈加入操作
            mainBottomNavView?.setOnItemSelectedListener(null)
            selectItemMainBottomNavigation(itemId)
            mainBottomNavView?.setOnItemSelectedListener(MainBottomNavigationListener())
        }
    }


    //全局事件类型
    //基本事件类型,建议继承以适配相关通用型业务
    abstract class BaseEvent(val makerId: String, val makerState: Int, val eventType: String?) {
        val makeTimestamp: Long = System.currentTimeMillis()
    }

    //设备状态相关事件类型
    class DeviceStateEvent(makerId: String, makerState: Int) :
        BaseEvent(makerId, makerState, MainActivity.DeviceStateEvent::class.simpleName) {
        companion object {
            //状态唯一决定,不同时对于两个或多个状态
            //已连接 -> 控制面板部署中 -> 控制面板部署完成
            const val DEVICE_STATE_CONNECTED: Int = 100 //已连接
            const val DEVICE_STATE_DISCONNECTED: Int = 101 //连接已断开
            const val DEVICE_STATE_CONTROL_PLATE_DEPLOYING: Int = 102 //控制面板部署中
            const val DEVICE_STATE_CONTROL_PLATE_DEPLOYED: Int = 103 //控制面板部署完成
        }
    }


    //BleFragment需求操作实现
    inner class BleFragmentFunctionRun : BleFragmentRunWant {
        @SuppressLint("MissingPermission")
        override fun startControl(deviceModel: BluetoothDeviceModel) {
            if (deviceModel.device == null) {
                Log.i(logTag, "BleFragmentRunWant - startControl: 存在参数为空,无法启动控制")
                return
            }
            //检查是否将会重复创建同一设备的连接
            for (base in controlBaseListBluetooth) {
                if (base.deviceModel.deviceSha256 == deviceModel.deviceSha256) {
                    Log.w(
                        logTag,
                        "BleFragmentRunWant - startControl: 阻止了用户对同一个设备重复连接的请求 : [${deviceModel.device?.name}]  [${deviceModel.deviceSha256}]"
                    )
                    return
                }
            }
            //创建并保存ControlBaseBluetooth实例
            val controlBaseBluetooth = ControlBaseBluetooth(deviceModel)
            controlBaseListBluetooth.add(controlBaseBluetooth)
            //运行连接,自动进行ControlBaseBluetooth实例的完善
            deviceModel.device?.connectGatt(this@MainActivity, true, controlBaseBluetooth)
        }

        override fun getControlBaseBluetooth(sha256: String?): ControlBaseBluetooth? {
            if (controlBaseListBluetooth.isNotEmpty() && sha256 != null) {
                for (base in controlBaseListBluetooth) {
                    if (base.deviceModel.deviceSha256 == sha256) {
                        return base
                    }
                }
            }
            return null
        }

        override fun provideStandardSync(): StandardSync {
            return standardSync!!
        }
    }

    //ControlFragment需求操作实现
    inner class ControlFragmentFunctionRun(override var controlBaseListBluetooth: List<ControlBase>) :
        ControlFragmentRunWant


    //蓝牙控制基础类的实例列表
    private val controlBaseListBluetooth: MutableList<ControlBaseBluetooth> = ArrayList()

    //Fragment需求操作实例的分配
    val bleFragmentFunctionRun: BleFragmentFunctionRun = BleFragmentFunctionRun()
    val controlFragmentFunctionRun: ControlFragmentFunctionRun =
        ControlFragmentFunctionRun(controlBaseListBluetooth)

    ////UI-底部导航栏
    private var mainBottomNavView: BottomNavigationView? = null

    inner class MainBottomNavigationListener : NavigationBarView.OnItemSelectedListener {

        override fun onNavigationItemSelected(item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.NavigationDevice -> {
                    switchDeviceFragment()
                    backPageNameStack.add("DeviceFragment")
                }

                R.id.NavigationBle -> {
                    switchBleFragment()
                    backPageNameStack.add("BleFragment")
                }

                R.id.NavigationControl -> {
                    switchControlFragment()
                    backPageNameStack.add("ControlFragment")
                }

                R.id.NavigationWifi -> {
                    switchWifiFragment()
                    backPageNameStack.add("WifiFragment")
                }

                R.id.NavigationMe -> {
                    switchMeFragment()
                    backPageNameStack.add("MeFragment")
                }

                else -> {}
            }
            return true
        }
    }

    /**
     * 底部导航栏选中操作,会触发内部selected逻辑,引发onNavigationItemSelected回调
     *
     * @param itemId 输入R.id.Navigation...选择需要的切换
     */
    fun selectItemMainBottomNavigation(itemId: Int) {
        mainBottomNavView?.selectedItemId = itemId
    }

    /**
     * 初始化底部导航栏
     */
    private fun initMainBottomNavigation() {
        mainBottomNavView = findViewById<BottomNavigationView>(R.id.main_bottom_navigation_in_main)
        mainBottomNavView?.setOnItemSelectedListener(MainBottomNavigationListener())
    }

    private fun switchDeviceFragment() {
//        val device: Fragment = deviceFragment ?: return
        val ble: Fragment = bleFragment ?: return
        val control: Fragment = controlFragment ?: return
//        val wifi: Fragment = wifiFragment ?: return
//        val me: Fragment = meFragment ?: return

        val transaction = supportFragmentManager.beginTransaction()
        transaction
//                .show(device)
            .hide(ble)
            .hide(control)
//                .hide(wifi)
//                .hide(me)
            .commit()

    }

    private fun switchBleFragment() {
//        val device: Fragment = deviceFragment ?: return
        val ble: Fragment = bleFragment ?: return
        val control: Fragment = controlFragment ?: return
//        val wifi: Fragment = wifiFragment ?: return
//        val me: Fragment = meFragment ?: return

        val transaction = supportFragmentManager.beginTransaction()
        transaction
//                .hide(device)
            .show(ble)
            .hide(control)
//                .hide(wifi)
//                .hide(me)
            .commit()
        bluetoothPermissionCheck()

    }

    private fun switchControlFragment() {
//        val device: Fragment = deviceFragment ?: return
        val ble: Fragment = bleFragment ?: return
        val control: Fragment = controlFragment ?: return
//        val wifi: Fragment = wifiFragment ?: return
//        val me: Fragment = meFragment ?: return

        val transaction = supportFragmentManager.beginTransaction()
        transaction
//                .hide(device)
            .hide(ble)
            .show(control)
//                .hide(wifi)
//                .hide(me)
            .commit()

//        if (readBlePendingCount == 0 && writeBlePendingCount == 0) {
//            val runWant: ControlFragmentRunWant = controlFragment?.controlFragmentRunWant!!
//            controlFragment?.consoleShowBluetooth(runWant.controlBaseListBluetooth[0])
//        }

        val runWant: ControlFragmentRunWant = controlFragment?.controlFragmentRunWant!!
        controlFragment?.consoleShowBluetooth(runWant.controlBaseListBluetooth[0])

    }

    private fun switchWifiFragment() {
//        val device: Fragment = deviceFragment ?: return
        val ble: Fragment = bleFragment ?: return
        val control: Fragment = controlFragment ?: return
//        val wifi: Fragment = wifiFragment ?: return
//        val me: Fragment = meFragment ?: return

        val transaction = supportFragmentManager.beginTransaction()
        transaction
//                .hide(device)
            .hide(ble)
            .hide(control)
//                .show(wifi)
//                .hide(me)
            .commit()

    }

    private fun switchMeFragment() {
//        val device: Fragment = deviceFragment ?: return
        val ble: Fragment = bleFragment ?: return
        val control: Fragment = controlFragment ?: return
//        val wifi: Fragment = wifiFragment ?: return
//        val me: Fragment = meFragment ?: return

        val transaction = supportFragmentManager.beginTransaction()
        transaction
//                .hide(device)
            .hide(ble)
            .hide(control)
//                .hide(wifi)
//                .show(me)
            .commit()

    }


    inner class ControlBaseBluetooth(deviceModel: BluetoothDeviceModel) : ControlBase(deviceModel) {

        override fun provideStandardSync(): StandardSync {
            return standardSync!!
        }

        override fun onBluetoothControlInitFinished() {
            bluetoothControl?.let { bluetoothControl ->
                this.bluetoothUI = BluetoothUI(bluetoothControl,this@MainActivity)
                DEVICE_STATE_EVENT_BUS.post(
                    DeviceStateEvent(
                        deviceModel.deviceSha256,
                        DeviceStateEvent.DEVICE_STATE_CONTROL_PLATE_DEPLOYED
                    )
                )
                runOnUiThread {
                    selectItemMainBottomNavigation(R.id.NavigationControl)
                }
            }
        }


        //BluetoothGattCallback实现
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            super.gattState = newState

            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {
                    DEVICE_STATE_EVENT_BUS.post(
                        DeviceStateEvent(
                            deviceModel.deviceSha256,
                            DeviceStateEvent.DEVICE_STATE_CONNECTED
                        )
                    )
                    if (this.deviceModel.device?.name != null) {
                        Log.i(logTag, "onConnectionStateChange: 已连接到 - " + this.deviceModel.device?.name)
                    } else {
                        Log.i(
                            logTag,
                            "onConnectionStateChange: 已连接到 - " + this.deviceModel.deviceSha256
                        )
                    }
                    gatt.discoverServices() //如果状态为已经连接,就扫描服务
                }

                BluetoothGatt.STATE_DISCONNECTED -> DEVICE_STATE_EVENT_BUS.post(
                    DeviceStateEvent(
                        deviceModel.deviceSha256,
                        DeviceStateEvent.DEVICE_STATE_DISCONNECTED
                    )
                )

                else -> {}
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            DEVICE_STATE_EVENT_BUS.post(
                DeviceStateEvent(
                    deviceModel.deviceSha256,
                    DeviceStateEvent.DEVICE_STATE_CONTROL_PLATE_DEPLOYING
                )
            )
            bluetoothControl = BluetoothControl(deviceModel.deviceSha256, this.deviceModel.device?.name, this)
            super.bluetoothControl?.gatt = gatt //缓存实例引用
        }
    }


    companion object {
        ////全局事件总线单例
        val DEVICE_STATE_EVENT_BUS: EventBus = EventBus.builder().build()

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

    //权限管理
    /**
     * 检查蓝牙权限授予状态,如果未授予且未被拒绝,请求用户授予
     */
    private fun bluetoothPermissionCheck() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
//            <!--兼容Android12+蓝牙相关-->
            XXPermissions.with(this)
                .permission(PermissionLists.getBluetoothScanPermission())
                .permission(PermissionLists.getBluetoothConnectPermission())
                .request { _, deniedList ->
                    if (deniedList.isNotEmpty()) {
                        //显示一个提示框,希望用户改变主意
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle(R.string.hint_chinese)
                        builder.setMessage(R.string.app_need_permission_chinese)
                        //配置授予按钮
                        builder.setPositiveButton(R.string.give_chinese) { _, _ ->
                            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(intent)
                        }
                        val dialog = builder.create()
                        dialog.show() //弹出提示框
                    }
                }
        } else {
//            <!--兼容Android11及以下蓝牙相关-->
            XXPermissions.with(this)
                .permission(PermissionLists.getAccessFineLocationPermission())
                .request { _, deniedList ->
                    if (deniedList.isNotEmpty()) {
                        //显示一个提示框,希望用户改变主意
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle(R.string.hint_chinese)
                        builder.setMessage(R.string.app_need_permission_chinese)
                        //配置授予按钮
                        builder.setPositiveButton(R.string.give_chinese) { _, _ ->
                            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(intent)
                        }
                        val dialog = builder.create()
                        dialog.show() //弹出提示框
                    }
                }
        }

    }


}