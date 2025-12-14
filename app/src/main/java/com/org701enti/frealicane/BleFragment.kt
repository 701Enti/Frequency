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

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnShowListener
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.org701enti.bluetoothfocuser.BluetoothAD
import com.org701enti.bluetoothfocuser.BluetoothAD.AdvertisingStruct
import com.org701enti.bluetoothfocuser.StandardSync
import com.org701enti.frealicane.BleFragment.ScanResultRecyclerViewAdapter.ScanResultItemViewHolder
import com.org701enti.frealicane.MainActivity.ControlBaseBluetooth
import com.org701enti.frealicane.suit.event.StableDeviceStateEventBus.instance
import com.org701enti.frealicane.ui.compose_view.proxy.unit.ProxyGeneralCircularIndicator.setContent
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

/**
 * A simple [Fragment] subclass.
 * Use the [BleFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BleFragment() : Fragment() {
    var logTag: String = "BleFragment"

    //运行需求
    lateinit var bleFragmentRunWant: BleFragmentRunWant

    interface BleFragmentRunWant {
        /**
         * 开始设备控制
         *
         * @param device BluetoothDevice实例
         * @param sha256 设备的SHA-256校验码
         */
        fun startControl(device: BluetoothDevice?, sha256: String?)

        fun getControlBaseBluetooth(sha256: String?): ControlBaseBluetooth?
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainActivity) {
            bleFragmentRunWant = context.bleFragmentFunctionRun
        } else {
            throw RuntimeException("must be attached by MainActivity but not by$context")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        InitBLE()

        if (bluetoothAdapter == null || bluetoothLeScanner == null) { //已经初始化但还为空
            //用户设备不支持蓝牙,弹出提示
            val builder = android.app.AlertDialog.Builder(requireActivity())
            builder.setTitle(R.string.error_chinese)
            builder.setMessage(R.string.user_device_hardware_unsupport)
            builder.setPositiveButton(getString(R.string.cancel_chinese)) { dialogInterface, i -> }
            builder.setNegativeButton(getString(R.string.return_app_chinese)) { dialogInterface, i -> }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ble, container, false)

        //初始化其他布局
        initRecyclerViewBluetooth(view)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        InitBleFragmentUI(view)
    }

    ////蓝牙内部处理业务
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var scanResultRecyclerView: RecyclerView? = null
    private val scanResultList: MutableList<BluetoothDeviceModel> = ArrayList()
    private var scanResultRecyclerViewAdapter: ScanResultRecyclerViewAdapter? = null
    private var isScanningBluetooth = false

    private fun InitBLE() {
        //获取BLEadapter实例
        val BLEmanager: BluetoothManager? =
            requireActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if (BLEmanager != null) {
            bluetoothAdapter = BLEmanager.adapter
            if (bluetoothAdapter != null) {
                bluetoothLeScanner = bluetoothAdapter!!.bluetoothLeScanner
            }
        }
    }

    /**
     * 检查蓝牙开启状态,如果未开启,打开蓝牙
     */
    @SuppressLint("MissingPermission")
    fun BluetoothOpenCheck() {
        //检查并启动
        //用于可能弹出打开蓝牙的询问框,回归主线程处理
        if (bluetoothAdapter != null) {
            if (bluetoothAdapter!!.state != BluetoothAdapter.STATE_ON) {
                val handler = Handler(Looper.getMainLooper())
                handler.post(Runnable
                //请求用户启动蓝牙
                {
                    bluetoothAdapter!!.enable()
                    MainTextViewBLE!!.setText(R.string.please_continue_when_bluetooth_enabled_chinese)
                    fadeInMainTextViewBLE!!.start()
                })
            }
        }
    }

    /**
     * 启动蓝牙设备扫描,含配置
     */
    @SuppressLint("MissingPermission")
    fun BluetoothScanStart(filters: List<ScanFilter?>?, settings: ScanSettings?) {
        if (!isScanningBluetooth) {
            //我们这里有必要解释为什么使用两次bluetoothLeScanner的null判断
            //(请您见下面BluetoothScanStop方法中的注释)

            if (bluetoothLeScanner == null) {
                //当以蓝牙关闭状态进入APP,InitBLE获取的bluetoothLeScanner将为空
                BluetoothOpenCheck() //检查蓝牙开启
                InitBLE()
            }
            if (bluetoothLeScanner != null) {
                if (bluetoothAdapter!!.state == BluetoothAdapter.STATE_ON) {
                    bluetoothLeScanner!!.startScan(filters, settings, bluetoothScanCallback)
                    AnimationBluetoothScanning!!.visibility = View.VISIBLE
                    MainTextViewBLE!!.text =
                        getString(R.string.vertical_slide_text_to_stop_sacn_chinese)
                    isScanningBluetooth = true
                    fadeOutMainTextViewBLE!!.start()
                } else {
                    BluetoothOpenCheck() //检查蓝牙开启
                }
            }
        }
    }

    /**
     * 启动蓝牙设备扫描,无配置
     */
    @SuppressLint("MissingPermission")
    fun BluetoothScanStart() {
        if (!isScanningBluetooth) {
            //我们这里有必要解释为什么使用两次bluetoothLeScanner的null判断
            //(请您见下面BluetoothScanStop方法中的注释)

            if (bluetoothLeScanner == null) {
                //当以蓝牙关闭状态进入APP,InitBLE获取的bluetoothLeScanner将为空
                BluetoothOpenCheck() //检查蓝牙开启
                InitBLE()
            }
            if (bluetoothLeScanner != null) {
                if (bluetoothAdapter!!.state == BluetoothAdapter.STATE_ON) {
                    bluetoothLeScanner!!.startScan(bluetoothScanCallback)
                    AnimationBluetoothScanning!!.visibility = View.VISIBLE
                    MainTextViewBLE!!.text =
                        getString(R.string.vertical_slide_text_to_stop_sacn_chinese)
                    isScanningBluetooth = true
                    fadeOutMainTextViewBLE!!.start()
                } else {
                    BluetoothOpenCheck() //检查蓝牙开启
                }
            }
        }
    }

    /**
     * 停止蓝牙设备扫描
     */
    @SuppressLint("MissingPermission")
    fun BluetoothScanStop() {
        if (isScanningBluetooth) {
            //我们这里有必要解释为什么使用两次bluetoothLeScanner的null判断
            //在当用户以蓝牙关闭状态进入APP,InitBLE获取的bluetoothLeScanner将为空
            //我们期望选择这个时机来恢复正轨,因为用户现在非常明确他是希望使用蓝牙功能的,不是有意的
            //如果此时执行该方法,进入==null这个代码块,我们会请求系统弹出"是否打开蓝牙",之后我们重新初始化
            //但是我们需要澄清的是,重新初始化后,未必还是成功的,所以大概率是无法继续!= null这个代码块的
            //这是因为请求打开蓝牙需要用户操作之后,才会可能启动,相对是异步的,所以等蓝牙启动后,早就已经结束方法了
            //那么非常显然的,如果用户再次触发执行这个方法,我们便顺理成章地在用户完成打开蓝牙的状态下正常初始化了
            //之后使用两次bluetoothLeScanner的null判断的优势就出现了,我们可以继续!= null这个代码块,正常启动需求
            //这样我们在用户不知情的情况下完成了两个任务,如果我们判断一次== null再加上else就草草结束,您会发现用户必须额外再次触发一次该方法在初始化后正式运行

            if (bluetoothLeScanner == null) {
                BluetoothOpenCheck() //检查蓝牙开启
                InitBLE()
            }
            if (bluetoothLeScanner != null) {
                if (bluetoothAdapter!!.state == BluetoothAdapter.STATE_ON) {
                    bluetoothLeScanner!!.stopScan(bluetoothScanCallback)
                    AnimationBluetoothScanning!!.visibility = View.INVISIBLE
                    MainTextViewBLE!!.text =
                        getString(R.string.horizontal_slide_text_to_start_sacn_chinese)
                    isScanningBluetooth = false
                    fadeOutMainTextViewBLE!!.start()
                } else {
                    BluetoothOpenCheck() //检查蓝牙开启
                }
            }
        }
    }

    //蓝牙设备列表模型类
    inner class BluetoothDeviceModel {
        var device: BluetoothDevice? //可能包含设备名称,信号强度等多种数据和方法
            private set

        var deviceSha256: String ////设备的SHA-256唯一性与安全校验码
            private set

        //设备的外观图标ID,其实就是外观值的bit6到bit15
        //详见(2.6.2)https://www.bluetooth.com/wp-content/uploads/Files/Specification/HTML/Assigned_Numbers/out/en/Assigned_Numbers.pdf
        var iconID: Int
            private set

        var deviceDistance: Int //与设备的距离(单位:米)

        var controlBaseBluetooth: ControlBaseBluetooth? //控制基础(创建GATT连接后获得并绑定到此)

        constructor(device: BluetoothDevice?, deviceDistance: Int, deviceSha256: String) {
            this.device = device
            this.deviceDistance = deviceDistance
            this.deviceSha256 = deviceSha256
            this.iconID = 0
            controlBaseBluetooth = null
        }

        constructor(
            device: BluetoothDevice?,
            deviceDistance: Int,
            iconID: Int,
            deviceSha256: String
        ) {
            this.device = device
            this.deviceDistance = deviceDistance
            this.deviceSha256 = deviceSha256
            this.iconID = iconID
            controlBaseBluetooth = null
        }
    }

    /**
     * 粗略计算信号的传播距离2.4GHz
     *
     * @param powerTX 信号发射功率(单位:dBm)
     * @param powerRX 信号接收功率(单位:dBm)
     * @return 粗略估测距离(单位 : m)
     */
    fun Distance2400MHZ(powerTX: Int, powerRX: Int): Double {
        //路径损耗(单位dB)L=powerTX - powerRX ,f=2.4GHz,根据自由空间路径损耗公式,最后粗略得到:
        return 10.0.pow(0.9944 * ((powerTX - powerRX) / 20.0) - 2.5)
    }


    //扫描回调
    var isAllowNotifyChanged: AtomicReference<Boolean> = AtomicReference(java.lang.Boolean.TRUE)
    val bluetoothScanCallback: ScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            if (result == null || scanResultRecyclerViewAdapter == null) {
                return
            }

            //判断设备是否已经存在列表
            for (i in scanResultRecyclerViewAdapter!!.modelList!!.indices) {
                val model = scanResultRecyclerViewAdapter!!.modelList!![i]
                //如果已经存在相同设备在列表
                if ((result.device == model.device)) {
                    //更新设备的deviceDistance,但使用内部解析

                    var distanceNow = 0
                    var powerTXint = 0 //Bluetooth SIG定义发射功率水平为sint8(有符号)(单位:dBm),与byte一致
                    powerTXint = result.txPower //(使用内部解析以节约资源)获取蓝牙设备的信号发射功率水平(单位:dBm)
                    if (powerTXint == Byte.MAX_VALUE) { //如果无法读取
                        powerTXint = 0
                    }
                    distanceNow = Distance2400MHZ(powerTXint, result.rssi).toInt()
                    model.deviceDistance = distanceNow

                    if (isAllowNotifyChanged.get()) {
                        scanResultRecyclerViewAdapter!!.notifyItemChanged(i) //提示信息更新,需要RecyclerView刷新显示
                    }
                    return
                }
            }

            //解析广播数据包
            val bluetoothAD = BluetoothAD(result, null)

            //获取iconID
            var iconID = 0
            val activity: Activity? = activity
            if (activity != null) {
                if (activity is MainActivity) {
                    //获取StandardSync实例
                    val standardSync: StandardSync = activity.standardSync
                    if (standardSync != null) {
                        try {
                            if (standardSync.yamlAdTypes != null) {
                                //使用StandardSync的YamlResolver解析出需要的Value,即当前标准的Appearance的adType数值
                                val yamlResolver =
                                    standardSync.YamlResolver(standardSync.yamlAdTypes)
                                val appearanceAdType = checkNotNull(
                                    yamlResolver
                                        .enterThisMapList("ad_types")
                                        .reserveTheItemsHave("name", "Appearance")
                                        .resultList[0]["value"] as Int?
                                )
                                //使用BluetoothAD在广播数据中提取数据
                                val structList = bluetoothAD.Search(appearanceAdType)
                                if (!structList.isEmpty()) {
                                    for (struct: AdvertisingStruct in structList) {
                                        if (struct.adData.size == 2) {
                                            iconID =
                                                (((struct.adData[1].toInt() and 0xFF) shl 8) or (struct.adData[0].toInt() and 0xFF)) ushr 10
                                        }
                                        if (struct.adData.size == 3) {
                                            iconID =
                                                (((struct.adData[2].toInt() and 0xFF) shl 16) or ((struct.adData[1].toInt() and 0xFF) shl 8) or (struct.adData[0].toInt() and 0xFF)) ushr 18
                                        }
                                        Log.i(
                                            logTag,
                                            "onScanResult: 获取到" + result.device.name + "的iconID: " + iconID
                                        )
                                    }
                                }
                            }
                        } catch (e: AssertionError) {
                            if (result.device.name != null) {
                                Log.w(
                                    logTag,
                                    "onScanResult: 无法获取" + result.device.name + "的iconID,因为:",
                                    e
                                )
                            } else {
                                Log.w(
                                    logTag,
                                    "onScanResult: 无法获取" + bluetoothAD.sha256StringAdvertising + "的iconID,因为:",
                                    e
                                )
                            }
                        } catch (e: NullPointerException) {
                            if (result.device.name != null) {
                                Log.w(
                                    logTag,
                                    "onScanResult: 无法获取" + result.device.name + "的iconID,因为:",
                                    e
                                )
                            } else {
                                Log.w(
                                    logTag,
                                    "onScanResult: 无法获取" + bluetoothAD.sha256StringAdvertising + "的iconID,因为:",
                                    e
                                )
                            }
                        } catch (e: IndexOutOfBoundsException) {
                            if (result.device.name != null) {
                                Log.w(
                                    logTag,
                                    "onScanResult: 无法获取" + result.device.name + "的iconID,因为:",
                                    e
                                )
                            } else {
                                Log.w(
                                    logTag,
                                    "onScanResult: 无法获取" + bluetoothAD.sha256StringAdvertising + "的iconID,因为:",
                                    e
                                )
                            }
                        } catch (e: IOException) {
                            if (result.device.name != null) {
                                Log.w(
                                    logTag,
                                    "onScanResult: 无法获取" + result.device.name + "的iconID,因为:",
                                    e
                                )
                            } else {
                                Log.w(
                                    logTag,
                                    "onScanResult: 无法获取" + bluetoothAD.sha256StringAdvertising + "的iconID,因为:",
                                    e
                                )
                            }
                        }
                    }
                }
            }


            //获取deviceDistance
            var deviceDistance = 0
            var powerTX: Byte = 0 //Bluetooth SIG定义发射功率水平为sint8(有符号)(单位:dBm),与byte一致
            //如果设备广播数据包含了,获取发射功率,如果没有包含,将以定义时的初始化值0计算
            val structList = bluetoothAD.Search(0x0A)
            if (!structList.isEmpty()) {
                if (structList[0] != null) {
                    powerTX = structList[0]!!.adData[0]
                }
            }
            deviceDistance = Distance2400MHZ(powerTX.toInt(), result.rssi).toInt()

            //创建设备模型
            val deviceModel = BluetoothDeviceModel(
                result.device, deviceDistance,
                iconID, bluetoothAD.sha256StringAdvertising
            ) //生成这个蓝牙设备的基本信息模型

            //缓存到RecyclerView适配器内部列表
            val index = scanResultRecyclerViewAdapter!!.itemCount
            scanResultRecyclerViewAdapter!!.modelList!!.add(
                index,
                deviceModel
            ) //添加信息到公共的表,RecyclerView将利用表中信息显示
            scanResultRecyclerViewAdapter!!.notifyItemInserted(index) //提示信息更新,需要RecyclerView刷新显示
            Log.i(
                "BluetoothInfoReceiver",
                "[" + index + "]" + deviceModel.device!!.name + " { iconID: " + deviceModel.iconID + " }"
            )
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            super.onBatchScanResults(results)
            if (results != null) {
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
        }
    }

    ////UI-蓝牙设备看板
    inner class ItemDecorationRecyclerViewBluetooth(
        private val dividerHeightPx: Float,
        dividerColor: Int
    ) : RecyclerView.ItemDecoration() {
        private val dividerPaint: Paint

        init {
            this.dividerPaint = Paint()
            dividerPaint.color = dividerColor
            dividerPaint.strokeWidth = dividerHeightPx
            dividerPaint.style = Paint.Style.FILL
        }

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            outRect.bottom = dividerHeightPx.toInt()
        }

        override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            for (i in 0 until (parent.childCount - 1)) { //列表最后一项不添加分隔线
                val child = parent.getChildAt(i)
                c.drawRect(
                    child.left.toFloat(),
                    child.bottom.toFloat(),
                    child.right.toFloat(),
                    child.bottom + dividerHeightPx,
                    dividerPaint
                )
            }
        }
    }

    private fun initRecyclerViewBluetooth(view: View) {
        scanResultRecyclerViewAdapter = ScanResultRecyclerViewAdapter(scanResultList)
        scanResultRecyclerView = view.findViewById(R.id.scan_result_recycler_view_in_ble)
        scanResultRecyclerView.setAdapter(scanResultRecyclerViewAdapter)
        scanResultRecyclerView.setLayoutManager(LinearLayoutManager(requireActivity()))
        val density = requireContext().resources.displayMetrics.density
        scanResultRecyclerView.addItemDecoration(
            ItemDecorationRecyclerViewBluetooth(
                1 * density,
                R.color.light_gray
            )
        ) //分隔线高度固定为1dp

        //禁用变更动画
        val animator = scanResultRecyclerView.getItemAnimator()
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
    }


    //BLE-RecyclerView的适配器类,用于RecyclerView展示扫描到的蓝牙设备
    inner class ScanResultRecyclerViewAdapter(val modelList: MutableList<BluetoothDeviceModel>?) :
        RecyclerView.Adapter<ScanResultItemViewHolder>() {
        @SuppressLint("MissingPermission")
        override fun onBindViewHolder(holder: ScanResultItemViewHolder, position: Int) {
            //缓存最新targetModel到holder

            val targetModel = modelList!![position]
            holder.targetModel = targetModel

            //为holder名下的子视图进行数据显示更新(子视图本身不会绑定targetModel)
            if (holder.targetModel != null) {
                configImageViewDeviceIcon(holder.targetModel, holder.deviceIcon) //设备图标
                configTextViewDeviceName(
                    holder.targetModel, holder.deviceName, calculateSuitableTextSizeSp(
                        holder.deviceName!!.length()
                    )
                ) //设备名
                val distance = holder.targetModel!!.deviceDistance //与设备的距离
                val showDistance = distance.toString() + getString(R.string.meter_chinese)
                holder.deviceDistance!!.text = showDistance
            }

            //为holder名下的根视图即扫描结果条目设置用户操作监听

            //触控事件注册

            //点击条目显示对应详细信息弹窗
            holder.itemView.setOnClickListener({ v: View? ->
                val dialogView: View = LayoutInflater.from(requireContext())
                    .inflate(R.layout.dialog_of_more_info_scan_result_item, null)
                //设备图标
                val deviceIcon: ImageView =
                    dialogView.findViewById(R.id.device_icon_in_dialog_of_more_info_scan_result_item)
                configImageViewDeviceIcon(holder.targetModel, deviceIcon)

                //设备名
                val deviceName: TextView =
                    dialogView.findViewById(R.id.device_name_in_dialog_of_more_info_scan_result_item)
                configTextViewDeviceName(
                    holder.targetModel,
                    deviceName,
                    calculateSuitableTextSizeSp(deviceName.length())
                )

                //底部按钮
                val startControlButton: MaterialButton =
                    dialogView.findViewById(R.id.start_control_button_in_dialog_of_more_info_scan_result_item)
                val addToDeviceButton: MaterialButton =
                    dialogView.findViewById(R.id.add_to_device_button_in_dialog_of_more_info_scan_result_item)
                val stickToTopButton: MaterialButton =
                    dialogView.findViewById(R.id.stick_to_top_button_in_dialog_of_more_info_scan_result_item)
                val undoButton: MaterialButton =
                    dialogView.findViewById(R.id.undo_button_in_dialog_of_more_info_scan_result_item)

                //ComposeView动画层
                val startControlLoading: ComposeView =
                    dialogView.findViewById(R.id.start_control_loading_in_dialog_of_more_info_scan_result_item)
                setContent(
                    startControlLoading,
                    holder.targetModel!!.deviceSha256,
                    MainActivity.DeviceStateEvent.DEVICE_STATE_CONNECTING,
                    MainActivity.DeviceStateEvent.DEVICE_STATE_CONTROL_PLATE_DEPLOYED,
                    instance
                )

                //配置dialog
                val builder: MaterialAlertDialogBuilder =
                    MaterialAlertDialogBuilder(requireContext())
                builder.setView(dialogView)
                val dialog: AlertDialog = builder.create()
                dialog.setOnShowListener(OnShowListener { d: DialogInterface? ->

                    //为dialog视图手动设置viewTreeLifecycleOwner
                    if (dialog.getWindow() != null) {
                        dialog.getWindow()!!.getDecorView()
                            .setViewTreeLifecycleOwner(this@BleFragment)
                    }

                    //获取需要进一步设置的View
                    val dialogStartControlButton: MaterialButton? =
                        dialog.findViewById(R.id.start_control_button_in_dialog_of_more_info_scan_result_item)
                    val dialogAddToDeviceButton: MaterialButton? =
                        dialog.findViewById(R.id.add_to_device_button_in_dialog_of_more_info_scan_result_item)
                    val dialogStickToTopButton: MaterialButton? =
                        dialog.findViewById(R.id.stick_to_top_button_in_dialog_of_more_info_scan_result_item)
                    val dialogUndoButton: MaterialButton? =
                        dialog.findViewById(R.id.undo_button_in_dialog_of_more_info_scan_result_item)

                    //追加按钮的点击事件监听
                    try {
                        dialogStartControlButton!!.setOnClickListener(View.OnClickListener { v1: View? ->
                            scanResultItemOperationRun(
                                WANT_START_CONTROL,
                                holder.getBindingAdapterPosition(),
                                requireContext()
                            )
                        })
                        dialogAddToDeviceButton!!.setOnClickListener(View.OnClickListener { v2: View? ->
                            scanResultItemOperationRun(
                                WANT_ADD_TO_DEVICE,
                                holder.getBindingAdapterPosition(),
                                requireContext()
                            )
                        })
                        dialogStickToTopButton!!.setOnClickListener(View.OnClickListener { v3: View? ->
                            scanResultItemOperationRun(
                                WANT_STICK_TO_TOP,
                                holder.getBindingAdapterPosition(),
                                requireContext()
                            )
                            dialog.dismiss()
                        })
                        dialogUndoButton!!.setOnClickListener(View.OnClickListener { v4: View? -> dialog.dismiss() })
                    } catch (e: NullPointerException) {
                        throw RuntimeException()
                    }
                })
            })

            //长按条目将对应条目置顶
            holder.itemView.setOnLongClickListener({ v: View? ->
                scanResultItemOperationRun(
                    WANT_STICK_TO_TOP,
                    holder.getBindingAdapterPosition(),
                    requireContext()
                )
                true
            })
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ScanResultItemViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_of_scan_result_recycler_view, parent, false)
            return ScanResultItemViewHolder(view)
        }

        /**
         * ViewHolder池,持有相关View引用,防止findViewById更多调用来优化性能
         */
        inner class ScanResultItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            //数据源
            var targetModel: BluetoothDeviceModel? = null

            //View元素
            var deviceName: TextView? = null
            var deviceIcon: ImageView? = null
            var deviceDistance: TextView? = null

            //动画效果
            var fadeInDeviceDistance: ObjectAnimator? = null

            //在构造方法将各种View引用缓存到ViewHolder池
            init {
                //super调用父类RecyclerView.ViewHolder构造方法,并传递了参数viewHandle
                //即自定义布局R.layout.recyclerviewbluetooth的实例,因此自定义布局文件的配置会对效果产生影响
                //如果其中开头的layout_width,layout_height选择了match_parent,会导致绘制间距非常大,难以修正
                //元素View相关
                deviceName =
                    view.findViewById(R.id.device_name_in_item_of_scan_result_recycler_view)
                deviceIcon =
                    view.findViewById(R.id.device_icon_in_item_of_scan_result_recycler_view)
                deviceDistance =
                    view.findViewById(R.id.device_distance_in_item_of_scan_result_recycler_view)

                //动画效果
                if (deviceDistance != null) {
                    fadeInDeviceDistance = ObjectAnimator.ofFloat(deviceDistance, "alpha", 0f, 1f)
                    fadeInDeviceDistance.setDuration(1000)
                    fadeInDeviceDistance.setInterpolator(DecelerateInterpolator())
                }
            }
        }

        override fun getItemCount(): Int {
            if (modelList != null) {
                return modelList.size
            } else {
                return 0
            }
        }

        fun calculateSuitableTextSizeSp(length: Int): Float {
            if (length <= 12) {
                return 24f - 4f * 1
            } else if (length <= 16) {
                return 24f - 4f * 2
            } else if (length <= 20) {
                return 24f - 4f * 3
            } else {
                return 24f - 4f * 4
            }
        }

        /***
         * 配置DeviceIcon组件以展示需要的内容
         * @param targetModel 选择数据来源的BluetoothDeviceModel实例
         * @param deviceIcon 对该ImageView实例执行配置
         */
        private fun configImageViewDeviceIcon(
            targetModel: BluetoothDeviceModel?,
            deviceIcon: ImageView?
        ) {
            val iconID = targetModel!!.iconID
            var iconBitmap: Bitmap? = null
            try {
                requireActivity().assets.open("bluetoothdeviceicon/btac$iconID.png")
                    .use { iconInput ->
                        iconBitmap = BitmapFactory.decodeStream(iconInput)
                        if (iconBitmap == null) {
                            deviceIcon!!.setImageResource(R.drawable.ble)
                        }
                    }
            } catch (e: IOException) {
                deviceIcon!!.setImageResource(R.drawable.ble)
            }
            if (iconBitmap != null) {
                deviceIcon!!.setImageBitmap(iconBitmap)
            }
        }

        /***
         * 配置DeviceName组件以展示需要的内容
         * @param targetModel 选择数据来源的BluetoothDeviceModel实例
         * @param deviceName 对该TextView实例执行配置
         * @param sizeSP 字体大小,单位sp
         */
        @SuppressLint("MissingPermission")
        private fun configTextViewDeviceName(
            targetModel: BluetoothDeviceModel?,
            deviceName: TextView?,
            sizeSP: Float
        ) {
            var name: String? = null
            if (targetModel!!.device != null) {
                name = targetModel.device!!.name
            }
            if (name == null) {
                deviceName!!.text = getString(R.string.unknown_device_chinese)
            } else {
                //确定显示的字符尺寸
                deviceName!!.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSP)
                deviceName.text = name
            }
        }
    }

    /**
     * 对扫描结果条目运行需要的操作,执行操作就会将设备信息加入数据库
     *
     * @param want     用户操作枚举 WANT_X
     * @param position 选择操作的模型单元在list的位置
     * @param context  上下文,可以使用Activity作为上下文
     */
    fun scanResultItemOperationRun(want: Byte, position: Int, context: Context?) {
        if (want == WANT_NONE) {
            return
        }

        //添加设备数据到数据库并继续处理-在独立线程执行
        Thread(object : Runnable {
            override fun run() {
                var targetModel: BluetoothDeviceModel? = null
                targetModel =
                    scanResultRecyclerViewAdapter!!.modelList!![position] //获取要读取操作列表中的的deviceModel实例
                if (targetModel == null) {
                    return
                }
                try {
                    val confirmSha256 =
                        MainActivity.addToBleDeviceMainDatabase(targetModel, context) //插入或更新到数据库
                    if (confirmSha256 != null) {
                        //确定加入数据库的数据未发生错误或篡改
                        if ((targetModel.deviceSha256 == confirmSha256)) {
                            val finalTargetModel: BluetoothDeviceModel = targetModel

                            //在主线程执行
                            val handler = Handler(Looper.getMainLooper())
                            val taskMainThread: Runnable = object : Runnable {
                                override fun run() {
                                    //执行用户需要的操作
                                    when (want) {
                                        WANT_ADD_TO_DEVICE -> {}
                                        WANT_STICK_TO_TOP -> {
                                            if (position >= 0 && position < scanResultRecyclerViewAdapter!!.modelList!!.size) {
                                                if (isAllowNotifyChanged.get()) {
                                                    val targetItem =
                                                        scanResultRecyclerViewAdapter!!.modelList!!.removeAt(
                                                            position
                                                        )
                                                    scanResultRecyclerViewAdapter!!.modelList!!.add(
                                                        0,
                                                        targetItem
                                                    )
                                                    //完成置顶效果需要RecyclerView刷新显示
                                                    scanResultRecyclerViewAdapter!!.notifyItemMoved(
                                                        position,
                                                        0
                                                    )
                                                    scanResultRecyclerViewAdapter!!.notifyItemChanged(
                                                        0
                                                    )
                                                    if (position != 0) {
                                                        scanResultRecyclerViewAdapter!!.notifyItemChanged(
                                                            1
                                                        ) //更新被挤下去的条目
                                                    }
                                                }
                                            }
                                        }

                                        WANT_START_CONTROL -> {
                                            BluetoothScanStop()
                                            bleFragmentRunWant!!.startControl(
                                                finalTargetModel.device,
                                                finalTargetModel.deviceSha256
                                            )
                                            finalTargetModel.controlBaseBluetooth =
                                                bleFragmentRunWant!!.getControlBaseBluetooth(
                                                    finalTargetModel.deviceSha256
                                                )
                                        }
                                    }
                                }
                            }
                            handler.post(taskMainThread)
                        }
                    }
                } catch (e: InterruptedException) {
                    throw RuntimeException(e)
                }
            }
        }).start()
    }

    /**
     * (内部回归主线程处理)清除蓝牙设备列表并请求列表刷新
     */
    fun DevicesListClearBluetooth() {
        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable {
            @SuppressLint("NotifyDataSetChanged")
            override fun run() {
                if (scanResultRecyclerViewAdapter != null) {
                    scanResultRecyclerViewAdapter!!.modelList!!.clear()
                    scanResultRecyclerViewAdapter!!.notifyDataSetChanged()
                }
            }
        })
    }

    /**
     * (用于开发测试)测试蓝牙设备看板,将虚拟一个蓝牙设备存储到列表,测试功能,其中BluetoothDevice设置为空
     *
     * @param deviceDistance 设备距离
     * @param iconID         设备外观图标ID
     */
    fun TestAddBluetoothDeviceRecyclerView(deviceDistance: Int, iconID: Int) {
        //根据时间生成伪设备SHA-256
        val sha256: String = FetchFakeSha256ByTime() ?: return
        //创建设备模型
        val deviceModel =
            BluetoothDeviceModel(null, deviceDistance, iconID, sha256) //生成这个蓝牙设备的基本信息模型
        //缓存到RecyclerView适配器内部列表
        val index = scanResultRecyclerViewAdapter!!.itemCount
        scanResultRecyclerViewAdapter!!.modelList!!.add(
            index,
            deviceModel
        ) //添加信息到公共的表,RecyclerView将利用表中信息显示
        scanResultRecyclerViewAdapter!!.notifyItemInserted(index) //提示信息更新,需要RecyclerView刷新显示
    }


    ////UI-主界面-BLE-MainTextViewBLE
    private var MainTextViewBLE: TextView? = null
    private var fadeOutMainTextViewBLE: ObjectAnimator? = null
    private var fadeInMainTextViewBLE: ObjectAnimator? = null

    private fun InitMainTextViewBLE(view: View) {
        //获取实例
        MainTextViewBLE = view.findViewById(R.id.main_text_in_ble)
        //创建消隐动画效果
        fadeOutMainTextViewBLE = ObjectAnimator.ofFloat(MainTextViewBLE, "alpha", 1f, 0f)
        fadeOutMainTextViewBLE.setDuration(5000)
        //创建淡入动画效果
        fadeInMainTextViewBLE = ObjectAnimator.ofFloat(MainTextViewBLE, "alpha", 0f, 1f)
        fadeInMainTextViewBLE.setDuration(2000)
    }


    ////UI-蓝牙扫描动画
    private var AnimationBluetoothScanning: View? = null

    private fun InitAnimationBluetoothScanning(view: View) {
        AnimationBluetoothScanning = view.findViewById(R.id.scan_state_indicator_in_ble)
    }


    ///UI-蓝牙扫描操作控制(基于动画实例)
    private var gestureMainText: GestureDetector? = null

    private inner class ListenerGestureMainText() : SimpleOnGestureListener() {
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            super.onFling(e1, e2, velocityX, velocityY)

            fadeInMainTextViewBLE!!.start()

            //如果以水平滑动为主
            if (abs(velocityX.toDouble()) > abs(velocityY.toDouble())) {
                if (!isScanningBluetooth) {
                    BluetoothScanStart() //扫描启动
                }
            } else { //如果以垂直滑动为主
                if (isScanningBluetooth) {
                    BluetoothScanStop()
                }
            }
            return true
        }

        override fun onDown(e: MotionEvent): Boolean { //如果为点击
            super.onDown(e)


            if (isScanningBluetooth) {
                MainTextViewBLE!!.text =
                    getString(R.string.vertical_slide_text_to_stop_sacn_chinese)
            } else {
                MainTextViewBLE!!.text =
                    getString(R.string.horizontal_slide_text_to_start_sacn_chinese)
            }

            fadeInMainTextViewBLE!!.start()
            return true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun InitGestureMainText(view: View) {
        gestureMainText = GestureDetector(requireActivity(), ListenerGestureMainText())
        val detectView = view.findViewById<View>(R.id.main_text_in_ble)

        detectView.setOnTouchListener(object : OnTouchListener {
            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
                return gestureMainText!!.onTouchEvent(motionEvent)
            }
        })
    }

    /**
     * 初始化BleFragment的UI界面
     *
     * @param view onCreateView返回的根视图实例
     */
    private fun InitBleFragmentUI(view: View) {
        InitAnimationBluetoothScanning(view)
        InitMainTextViewBLE(view)

        InitGestureMainText(view)
    }


    companion object {
        @JvmStatic
        fun newInstance(): BleFragment {
            val fragment = BleFragment()
            val args = Bundle()

            fragment.arguments = args
            return fragment
        }

        //用户操作枚举-->
        val WANT_START_CONTROL: Byte = 1 //开始设备控制
        val WANT_ADD_TO_DEVICE: Byte = 2 //添加入"设备"
        val WANT_STICK_TO_TOP: Byte = 3 //置顶,移动到列表顶部
        val WANT_NONE: Byte = 4 //无操作

        /**
         * (用于开发测试)根据时间生成伪设备SHA-256
         */
        fun FetchFakeSha256ByTime(): String {
            val timestamp = System.currentTimeMillis()
            //填充到byte数组,低位到高位
            val rawCode = ByteArray(8)
            for (i in 0..7) {
                rawCode[i] = (timestamp shr (8 * i)).toByte()
            }
            return BluetoothAD.RawCodeFetchSha256String(rawCode)
        }
    }
}