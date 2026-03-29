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
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.webkit.WebViewFeature
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.org701enti.bluetoothfocuser.BluetoothAD
import com.org701enti.bluetoothfocuser.BluetoothAD.AdvertisingStruct
import com.org701enti.bluetoothfocuser.BluetoothDeviceModel
import com.org701enti.bluetoothfocuser.StandardSync
import com.org701enti.frealicane.BleFragment.ScanResultRecyclerViewAdapter.ScanResultItemViewHolder
import com.org701enti.frealicane.MainActivity.ControlBaseBluetooth
import com.org701enti.frealicane.core.datastore.DarkModeSetting
import com.org701enti.frealicane.utils.ViewConfigUtils
import com.org701enti.frealicane.suit.event.StableDeviceStateEventBus
import com.org701enti.frealicane.ui.compose.animation.PhoneDeviceSlideAnimation
import com.org701enti.frealicane.ui.compose.unit.GeneralCircularIndicator
import com.org701enti.frealicane.viewmodel.BleFragmentViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow

/**
 * A simple [Fragment] subclass.
 * Use the [BleFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BleFragment() : Fragment() {
    var logTag: String = "BleFragment"

    //运行需求
    var bleFragmentRunWant: BleFragmentRunWant? = null

    interface BleFragmentRunWant {
        /**
         * 开始设备控制
         *
         * @param deviceModel 设备模型
         */
        fun startControl(deviceModel: BluetoothDeviceModel)

        /** 获取指定设备的控制基础
         * @param sha256 设备的SHA-256校验码
         */
        fun getControlBaseBluetooth(sha256: String?): ControlBaseBluetooth?

        /**
         * 提供StandardSync(不能为null)
         * @return StandardSync实例(不为null)
         */
        fun provideStandardSync(): StandardSync
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
        initBLE()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ble, container, false)
        //初始化其他布局
        initRecyclerViewBluetooth(view)
        initBleFragmentUI(view)
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bleFragmentRunWant = null;
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState != null && bleFragmentViewModel.isScanningBluetooth) {
            bleFragmentViewModel.isScanningBluetooth = false
            bluetoothScanStart()
        }
    }


    private val bleFragmentViewModel: BleFragmentViewModel by viewModels()

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var scanResultRecyclerView: RecyclerView? = null
    private var scanResultRecyclerViewAdapter: ScanResultRecyclerViewAdapter? = null
    private var scanClearJob: Job? = null

    private val BLE_SCAN_TIMEOUT = 5_000L //设备扫描超时时间(ms),长时间未扫描到的设备条目将被清理
    private val BLE_CHECK_SCAN_TIMEOUT_INTERVAL = 500L //检查设备扫描是否超时的间隔时间(ms)
    private val BLE_DISTANCE_UPDATE_INTERVAL = 1_000L //设备距离更新间隔时间(ms)

    private fun initBLE() {
        if (!isAdded) return
        val manager: BluetoothManager =
            requireActivity().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = manager.adapter
        if (bluetoothAdapter != null) {
            bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        }
    }

    /**
     * 检查蓝牙开启状态,如果未开启,弹出弹窗请求用户开启,如果用户设备不支持蓝牙,弹出提示
     */
    @SuppressLint("MissingPermission")
    fun bluetoothEnableCheck() {
        if (!isAdded) return
        if (bluetoothAdapter == null) { //已经初始化但还为空
            //用户设备不支持蓝牙,弹出提示
            val builder = android.app.AlertDialog.Builder(requireContext())
            builder.setTitle(R.string.bluetooth_support_lack_chinese)
            builder.setIcon(R.drawable.ble)
            builder.setMessage(R.string.user_device_hardware_not_support_chinese)
            builder.setPositiveButton(getString(R.string.cancel_chinese), null)
            val dialog = builder.create()
            dialog.show()
        } else {
            if (bluetoothAdapter?.state != BluetoothAdapter.STATE_ON) {
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivity(intent)
                    showNoticeMainText(getString(R.string.please_continue_control_chinese))
                }
            }
        }

    }

    /**
     * 启动蓝牙设备扫描
     */
    @SuppressLint("MissingPermission")
    fun bluetoothScanStart() {
        if (!bleFragmentViewModel.isScanningBluetooth) {
            if (bluetoothLeScanner == null) {
                //当以蓝牙关闭状态进入APP,InitBLE获取的bluetoothLeScanner将为空
                bluetoothEnableCheck() //检查蓝牙开启
                initBLE()
            }
            if (bluetoothLeScanner != null) {
                if (bluetoothAdapter?.state == BluetoothAdapter.STATE_ON) {

                    bluetoothLeScanner?.startScan(bluetoothScanCallback)

                    animationBluetoothScanning?.visibility = View.VISIBLE
                    showNoticeMainText(getString(R.string.vertical_slide_text_to_stop_sacn_chinese))

                    bleFragmentViewModel.isScanningBluetooth = true
                    if (bleFragmentViewModel.scanResultList.isNotEmpty()) {
                        bleFragmentViewModel.scanResultList.forEach { model : BluetoothDeviceModel ->
                            model.flagRecentScanTime = System.currentTimeMillis()
                        }
                    }

                    scanClearJob = lifecycleScope.launch {
                        while (true) {
                            clearOneScanTimeoutDeviceItem()
                            delay(BLE_CHECK_SCAN_TIMEOUT_INTERVAL)
                        }
                    }

                } else {
                    bluetoothEnableCheck() //检查蓝牙开启
                }
            }
        }
    }

    /**
     * 停止蓝牙设备扫描
     */
    @SuppressLint("MissingPermission")
    fun bluetoothScanStop() {
        if (bleFragmentViewModel.isScanningBluetooth) {
            if (bluetoothLeScanner == null) {
                bluetoothEnableCheck() //检查蓝牙开启
                initBLE()
            }
            if (bluetoothLeScanner != null) {
                if (bluetoothAdapter?.state == BluetoothAdapter.STATE_ON) {

                    bluetoothLeScanner?.stopScan(bluetoothScanCallback)

                    animationBluetoothScanning?.visibility = View.INVISIBLE
                    showNoticeMainText(getString(R.string.horizontal_slide_text_to_start_sacn_chinese))
                    bleFragmentViewModel.isScanningBluetooth = false

                    scanClearJob?.cancel()
                    scanClearJob = null
                } else {
                    bluetoothEnableCheck() //检查蓝牙开启
                }
            }
        }
    }

    /**
     * 清理一个扫描超时的设备条目,长时间未扫描到的设备条目将被清理(前提是当前允许Notify标志位为true)
     */
    private fun clearOneScanTimeoutDeviceItem() {
        if (bleFragmentViewModel.scanResultList.isNotEmpty() && bleFragmentViewModel.isAllowNotifyChanged.get()) {
            var i = 0
            while (i < bleFragmentViewModel.scanResultList.size) {
                if (System.currentTimeMillis() - bleFragmentViewModel.scanResultList[i].flagRecentScanTime > BLE_SCAN_TIMEOUT) {
                    bleFragmentViewModel.scanResultList.removeAt(i)
                    scanResultRecyclerViewAdapter?.notifyItemRemoved(i)
                }
                i++
            }
        }
    }

    /**
     * 粗略计算信号的传播距离2.4GHz
     *
     * @param powerTX 信号发射功率(单位:dBm)
     * @param powerRX 信号接收功率(单位:dBm)
     * @return 粗略估测距离(单位 : m)
     */
    fun distance2400MHZ(powerTX: Int, powerRX: Int): Double {
        //路径损耗(单位dB)L=powerTX - powerRX ,f=2.4GHz,根据自由空间路径损耗公式,最后粗略得到:
        return 10.0.pow(0.9944 * ((powerTX - powerRX) / 20.0) - 2.5)
    }


    //扫描回调
    private val bluetoothScanCallback: ScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {

            if (!isAdded) return

            super.onScanResult(callbackType, result)
            if (scanResultRecyclerViewAdapter == null) {
                return
            }

            //判断设备是否已经存在列表
            for (i in scanResultRecyclerViewAdapter?.modelList?.indices!!) {
                val model = scanResultRecyclerViewAdapter?.modelList?.getOrNull(i) ?: continue
                //如果已经存在相同设备在列表
                if ((result.device == model.device)) {
                    if (System.currentTimeMillis() - model.flagRecentDistanceUpdateTime > BLE_DISTANCE_UPDATE_INTERVAL) {
                        //更新设备的deviceDistance,但获取发射功率使用内部解析
                        val distanceNow: Int
                        var txPowerInt: Int  //Bluetooth SIG定义发射功率水平为sint8(有符号)(单位:dBm),与byte一致
                        txPowerInt = result.txPower //(使用内部解析以节约资源)获取蓝牙设备的信号发射功率水平(单位:dBm)
                        if (txPowerInt.toByte() == Byte.MAX_VALUE) { //如果无法读取
                            txPowerInt = 0
                        }
                        distanceNow = distance2400MHZ(txPowerInt, result.rssi).toInt()
                        model.deviceDistance = distanceNow

                        if (bleFragmentViewModel.isAllowNotifyChanged.get()) {
                            scanResultRecyclerViewAdapter?.notifyItemChanged(i) //提示信息更新,需要RecyclerView刷新显示
                        }
                    }

                    model.flagRecentScanTime = System.currentTimeMillis()
                    model.flagRecentDistanceUpdateTime = System.currentTimeMillis()

                    return
                }
            }

            //解析广播数据包
            val bluetoothAD = BluetoothAD(result, null)

            //获取iconID
            var iconID = 0
            val activity: Activity = requireActivity()
            if (isAdded) {
                if (activity is MainActivity) {
                    //获取StandardSync实例
                    val standardSync: StandardSync = bleFragmentRunWant?.provideStandardSync()!!
                    try {
                        if (standardSync.yamlAdTypes != null) {
                            //使用StandardSync的YamlResolver解析出当前标准的外观特征值的adType数值
                            //参考:assets/public/assigned_numbers/core/ad_types.yaml
                            val yamlResolver =
                                standardSync.YamlResolver(standardSync.yamlAdTypes)
                            val appearanceAdType = checkNotNull(
                                yamlResolver
                                    .enterThisMapList("ad_types")//进入ad_types列表
                                    .reserveTheItemsHave(
                                        "name",
                                        "Appearance"
                                    )//保留含有key为name,value为Appearance的键值对的条目
                                    .resultList[0]["value"] as Int?//获取第一个搜索结果并获取条目中key为value的键值对的值,并强制转换类型
                            )
                            //使用BluetoothAD在广播数据中提取外观特征值(通过adType搜索)
                            val structList = bluetoothAD.Search(appearanceAdType)
                            if (structList.isNotEmpty()) {
                                for (struct: AdvertisingStruct in structList) {
                                    //Appearance标准为2个字节
                                    if (struct.adData.size == 2) {
                                        iconID =
                                            (((struct.adData[1].toInt() and 0xFF) shl 8) or (struct.adData[0].toInt() and 0xFF)) ushr 6
                                    }
                                    Log.i(
                                        logTag,
                                        "onScanResult: 获取到" + result.device.name + "的iconID: " + iconID
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        if (result.device.name != null) {
                            Log.w(
                                logTag,
                                "onScanResult: 无法获取名为" + result.device.name + "的设备的iconID,因为:",
                                e
                            )
                        } else {
                            Log.w(
                                logTag,
                                "onScanResult: 无法获取SHA256为" + bluetoothAD.sha256StringAdvertising + "的设备的iconID,因为:",
                                e
                            )
                        }
                    }
                }
            }


            //获取deviceDistance
            val deviceDistance: Int
            var powerTX: Byte = 0 //Bluetooth SIG定义发射功率水平为sint8(有符号)(单位:dBm),与byte一致
            //如果设备广播数据包含了,获取发射功率,如果没有包含,将以定义时的初始化值0计算
            val structList = bluetoothAD.Search(0x0A)
            if (structList.isNotEmpty()) {
                powerTX = structList[0].adData.firstOrNull() ?: 0
            }
            deviceDistance = distance2400MHZ(powerTX.toInt(), result.rssi).toInt()

            //创建设备模型
            val deviceModel = BluetoothDeviceModel(
                result.device, deviceDistance,
                iconID, bluetoothAD.sha256StringAdvertising
            ) //生成这个蓝牙设备的基本信息模型

            deviceModel.flagRecentScanTime = System.currentTimeMillis()
            deviceModel.flagRecentDistanceUpdateTime = System.currentTimeMillis()

            //缓存到RecyclerView适配器内部列表
            val index = scanResultRecyclerViewAdapter?.itemCount
            scanResultRecyclerViewAdapter?.modelList?.add(
                index!!,
                deviceModel
            ) //添加信息到公共的表,RecyclerView将利用表中信息显示
            scanResultRecyclerViewAdapter?.notifyItemInserted(index!!) //提示信息更新,需要RecyclerView刷新显示
            Log.i(
                "BluetoothInfoReceiver",
                "[" + index + "]" + deviceModel.device?.name + " { iconID: " + deviceModel.iconID + " }"
            )
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            super.onBatchScanResults(results)
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
        private val dividerPaint: Paint = Paint()

        init {
            dividerPaint.color = dividerColor
            dividerPaint.strokeWidth = dividerHeightPx
            dividerPaint.style = Paint.Style.FILL
            dividerPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
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
            val saveCount =
                c.saveLayer(0f, 0f, parent.width.toFloat(), parent.height.toFloat(), null)
            for (i in 0 until parent.childCount) {
                val child = parent.getChildAt(i)
                c.drawRect(
                    child.left.toFloat(),
                    child.top.toFloat(),
                    child.right.toFloat(),
                    child.top + dividerHeightPx,
                    dividerPaint
                )
            }
            if (parent.childCount >= 1) {
                val lastChild = parent.getChildAt(parent.childCount - 1)
                c.drawRect(
                    lastChild.left.toFloat(),
                    lastChild.bottom.toFloat(),
                    lastChild.right.toFloat(),
                    lastChild.bottom + dividerHeightPx,
                    dividerPaint
                )
            }
            c.restoreToCount(saveCount)
        }
    }

    private fun initRecyclerViewBluetooth(view: View) {
        if (!isAdded) return
        //创建用于展示扫描结果的RecyclerView及Adapter
        scanResultRecyclerViewAdapter =
            ScanResultRecyclerViewAdapter(bleFragmentViewModel.scanResultList)
        scanResultRecyclerView = view.findViewById(R.id.scan_result_recycler_view_in_ble)
        scanResultRecyclerView?.let {
            //添加配置
            it.setAdapter(scanResultRecyclerViewAdapter)
            it.setLayoutManager(LinearLayoutManager(requireActivity()))
            //配置分隔线,高度固定为1dp
            val density = requireContext().resources.displayMetrics.density
            it.addItemDecoration(
                ItemDecorationRecyclerViewBluetooth(
                    1 * density,
                    ContextCompat.getColor(requireActivity(), R.color.item_decoration)
                )
            )
            //禁用变更动画
            val animator = it.itemAnimator
            if (animator is SimpleItemAnimator) {
                animator.supportsChangeAnimations = false
            }
        }
    }


    //BLE-RecyclerView的适配器类,用于RecyclerView展示扫描到的蓝牙设备
    inner class ScanResultRecyclerViewAdapter(val modelList: MutableList<BluetoothDeviceModel>?) :
        RecyclerView.Adapter<ScanResultItemViewHolder>() {
        @SuppressLint("MissingPermission")
        override fun onBindViewHolder(holder: ScanResultItemViewHolder, position: Int) {
            if (!isAdded) return

            //缓存最新targetModel到holder
            holder.targetModel = modelList?.getOrNull(position)
                ?: BluetoothDeviceModel(null, 0, "ERROR-绑定BluetoothDeviceModel失败")

            //为holder名下的子视图进行数据显示更新(子视图本身不会绑定targetModel)
            holder.targetModel?.let {
                //设备图标
                ViewConfigUtils.configImageViewDeviceIcon(it, holder.deviceIcon, requireActivity())

                //设备名
                ViewConfigUtils.configTextViewDeviceName(
                    it, holder.deviceName, ViewConfigUtils.calculateSuitableTextSizeSp(
                        it.device?.name?.length ?: 0
                    ), requireActivity()
                )

                //与设备的距离(通过信号强度估测)
                val distance = it.deviceDistance
                holder.deviceDistance?.text = when (distance) {
                    in 0..10 -> "0-10" + getString(R.string.meter_chinese)
                    in 11..50 -> "10-50" + getString(R.string.meter_chinese)
                    else -> ">50" + getString(R.string.meter_chinese)
                }
            }

            //为holder名下的根视图即扫描结果条目设置用户操作监听

            //点击条目显示对应详细信息弹窗
            holder.itemView.setOnClickListener {
                if (!isAdded) return@setOnClickListener

                val v: View = LayoutInflater.from(requireActivity())
                    .inflate(R.layout.dialog_of_more_info_scan_result_item, null)

                //向容器装载ComposeView

                //加载中转圈指示器-启动控制中
                val containerOfStartControlLoading: FrameLayout =
                    v.findViewById(R.id.container_of_start_control_loading_in_dialog_of_more_info_scan_result_item)
                val startControlLoading = ComposeView(requireContext())
                    .apply {
                        setViewTreeLifecycleOwner(requireActivity())
                        setContent {
                            holder.targetModel?.deviceSha256?.let { it1 ->
                                GeneralCircularIndicator(
                                    it1,
                                    MainActivity.DeviceStateEvent.DEVICE_STATE_CONNECTED,
                                    MainActivity.DeviceStateEvent.DEVICE_STATE_CONTROL_PLATE_DEPLOYED,
                                    StableDeviceStateEventBus,
                                    MainActivity.DeviceStateEvent::class.simpleName
                                )
                            }
                        }
                    }
                containerOfStartControlLoading.addView(startControlLoading)

                //构建dialog
                val builder = MaterialAlertDialogBuilder(requireActivity())
                builder.setView(v)
                val dialog: AlertDialog = builder.create()

                //设置dialog生命周期相关
                dialog.window?.decorView?.let {
                    it.setViewTreeLifecycleOwner(requireActivity())
                    it.setViewTreeSavedStateRegistryOwner(requireActivity())
                }
                dialog.setOnDismissListener {
                    dialog.window?.decorView?.let {
                        it.setViewTreeLifecycleOwner(null)
                        it.setViewTreeSavedStateRegistryOwner(null)
                    }
                }

                //当dialog展示时运行以下回调
                dialog.setOnShowListener {
                    if (!isAdded) return@setOnShowListener

                    //设备图标
                    val deviceIcon: ImageView? =
                        dialog.findViewById(R.id.device_icon_in_dialog_of_more_info_scan_result_item)
                    //设备名
                    val deviceName: TextView? =
                        dialog.findViewById(R.id.device_name_in_dialog_of_more_info_scan_result_item)

                    //启动控制按钮
                    val dialogStartControlButton: MaterialButton? =
                        dialog.findViewById(R.id.start_control_button_in_dialog_of_more_info_scan_result_item)
                    //添加到设备按钮
                    val dialogAddToDeviceButton: MaterialButton? =
                        dialog.findViewById(R.id.add_to_device_button_in_dialog_of_more_info_scan_result_item)
                    //置顶条目按钮
                    val dialogStickToTopButton: MaterialButton? =
                        dialog.findViewById(R.id.stick_to_top_button_in_dialog_of_more_info_scan_result_item)
                    //关闭dialog弹窗按钮
                    val dialogDismissButton: MaterialButton? =
                        dialog.findViewById(R.id.dismiss_button_in_dialog_of_more_info_scan_result_item)

                    //媒体面板 - 设备在互联网上的搜索结果
                    val dialogDeviceWebSearchOfMediaPanel: WebView? =
                        dialog.findViewById(R.id.device_web_search_of_media_panel_in_dialog_of_more_info_scan_result_item)

                    //媒体面板 - 面板容器
                    val dialogPanelContainerOfMediaPanel: FrameLayout? =
                        dialog.findViewById(R.id.panel_container_of_media_panel_in_dialog_of_more_info_scan_result_item)

                    //媒体面板 - 面板容器页面:"确认设备是你的"
                    // (可选择关闭"确认设备是你的"功能)
                    //用于确认设备所有者是不是自己,主要为了防止错误连接他人设备
                    //这只是一个确认辅助工具页,在控制/添加按钮点击时展示在MediaPanel
                    //如果你没有确认,他不会阻止继续操作,再点击一次按钮可正常进入
                    //提示文字(在容器顶部):
                    //1.提示:如果你不确认这是你的设备,请靠近设备并扫动手机
                    //      If you are not sure this is your device, please approach the device and wave your phone
                    //2.提示:已确认这是你的设备
                    //      It has been confirmed that this is your device
                    //动画(占整个容器):设备图标固定在底部中央,手机图标水平平滑往复滑动
                    val dialogPanelShowConfirmDeviceOwner = ComposeView(requireContext())
                        .apply {
                            setViewTreeLifecycleOwner(requireActivity())
                            setContent {
                                val isDarkMode =
                                    DarkModeSetting.shouldUseDark(context = LocalContext.current)
                                MaterialTheme(
                                    colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme(),
                                    content = {
                                        PhoneDeviceSlideAnimation(
                                            phoneRes = R.drawable.phone,
                                            deviceRes = R.drawable.device,
                                            containerWidthPx = dialogPanelContainerOfMediaPanel?.width
                                                ?: 0,
                                            progressMin = 0.25f,
                                            progressMax = 0.75f,
                                            duration = 2000
                                        )
                                        Text(
                                            text = "如果你不确认这是你的设备,请靠近设备并扫动手机",
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                )
                            }
                        }
                    dialogPanelContainerOfMediaPanel?.isInvisible = true
                    dialogPanelContainerOfMediaPanel?.addView(dialogPanelShowConfirmDeviceOwner)

                    holder.targetModel?.let { deviceModel ->
                        //设备图标
                        ViewConfigUtils.configImageViewDeviceIcon(
                            deviceModel,
                            deviceIcon,
                            requireActivity()
                        )
                        //设备名
                        ViewConfigUtils.configTextViewDeviceName(
                            deviceModel,
                            deviceName,
                            ViewConfigUtils.calculateSuitableTextSizeSp(deviceName?.length()!!),
                            requireActivity()
                        )

                        //媒体面板 - 设备在互联网上的搜索结果
                        if (deviceModel.device?.name != null) {
                            dialogDeviceWebSearchOfMediaPanel?.settings?.apply {
                                javaScriptEnabled = true

                                useWideViewPort = true
                                loadWithOverviewMode = true
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false

                                domStorageEnabled = true
                                databaseEnabled = true

                                cacheMode = WebSettings.LOAD_NO_CACHE

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                                        isAlgorithmicDarkeningAllowed = true
                                    }
                                }
                            }

                            dialogDeviceWebSearchOfMediaPanel?.clearCache(true)
                            dialogDeviceWebSearchOfMediaPanel?.clearHistory()
                            android.webkit.CookieManager.getInstance().removeAllCookies(null)

                            dialogDeviceWebSearchOfMediaPanel?.webViewClient = object :
                                WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    //为确保安全,非起始页操作交给系统页面
                                    if (request != null) {
                                        if (request.url.host?.endsWith("bing.com") == true) {
                                            return false
                                        }
                                        CustomTabsIntent.Builder().build()
                                            .launchUrl(requireActivity(), request.url)
                                        Toast.makeText(
                                            requireActivity(),
                                            "当前页面不再受到" + getString(R.string.app_name) + "控制",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    return true
                                }

                                @Deprecated("Deprecated in Java")
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    url: String?
                                ): Boolean {
                                    //为确保安全,非起始页操作交给系统页面
                                    if (Uri.parse(url).host?.endsWith("bing.com") == true) {
                                        return false
                                    }
                                    CustomTabsIntent.Builder().build()
                                        .launchUrl(requireActivity(), Uri.parse(url))
                                    Toast.makeText(
                                        requireActivity(),
                                        "当前页面不再受到" + getString(R.string.app_name) + "控制",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)

                                    if (DarkModeSetting.shouldUseDark(requireActivity()) && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                        //低版本SDK通过 注入 CSS 强制暗色
                                        val js = """
                                                 (function() {
                                                     var css = 'html { filter: invert(1) hue-rotate(180deg); }' +
                                                               'img, video, picture, svg, iframe { filter: invert(1) hue-rotate(180deg); }';
                                                     var style = document.createElement('style');
                                                     style.appendChild(document.createTextNode(css));
                                                     document.head.appendChild(style);
                                                 })();
                                        """.trimIndent()

                                        view?.evaluateJavascript(js, null)
                                    }
                                }
                            }
                            dialogDeviceWebSearchOfMediaPanel?.loadUrl(
                                "https://www.bing.com/search?q=${deviceModel.device?.name}",
                                mapOf("Cache-Control" to "no-cache")
                            )
                        }

//                        lifecycleScope.launch {
//                            while (deviceModel.deviceDistance > 1){
//                                delay(10)
//                            }
//                            if(deviceModel.device?.name != null){
//                                Toast
//                                    .makeText(requireActivity(),"已确认" + deviceModel.device?.name + "你的设备",Toast.LENGTH_LONG)
//                                    .show()
//                            }
//                            else{
//                                Toast
//                                    .makeText(requireActivity(),"已确认这是你的设备",Toast.LENGTH_LONG)
//                                    .show()
//                            }
//
//                        }

                        //配置按钮的点击事件监听
                        dialogStartControlButton?.setOnClickListener {
//                            dialogPanelContainerOfMediaPanel?.isInvisible = false
//                            dialogDeviceWebSearchOfMediaPanel?.isInvisible = true
                            scanResultItemOperationRun(
                                WANT_START_CONTROL,
                                holder.getBindingAdapterPosition(),
                                requireContext()
                            )
                        }
                        dialogAddToDeviceButton?.setOnClickListener {
//                        scanResultItemOperationRun(
//                            WANT_ADD_TO_DEVICE,
//                            holder.getBindingAdapterPosition(),
//                            requireContext()
//                        )
                        }
                        dialogStickToTopButton?.setOnClickListener {
                            scanResultItemOperationRun(
                                WANT_STICK_TO_TOP,
                                holder.getBindingAdapterPosition(),
                                requireContext()
                            )
                            dialog.dismiss()
                        }
                        dialogDismissButton?.setOnClickListener { dialog.dismiss() }
                    }

                }
                dialog.show()
            }

            //长按条目将对应条目置顶
            holder.itemView.setOnLongClickListener {
                scanResultItemOperationRun(
                    WANT_STICK_TO_TOP,
                    holder.getBindingAdapterPosition(),
                    requireContext()
                )
                true
            }
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
            var fadeInDeviceDistance: Unit = ObjectAnimator
                .ofFloat(deviceDistance, "alpha", 0f, 1f)
                .setDuration(1000)
                .setInterpolator(DecelerateInterpolator())

            //在构造方法将各种View引用缓存到ViewHolder池
            init {
                //元素View相关
                deviceName =
                    view.findViewById(R.id.device_name_in_item_of_scan_result_recycler_view)
                deviceIcon =
                    view.findViewById(R.id.device_icon_in_item_of_scan_result_recycler_view)
                deviceDistance =
                    view.findViewById(R.id.device_distance_in_item_of_scan_result_recycler_view)
            }
        }

        override fun getItemCount(): Int {
            return modelList?.size ?: 0
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
                //获取要读取操作列表中的的deviceModel实例
                val targetModel: BluetoothDeviceModel =
                    scanResultRecyclerViewAdapter?.modelList?.getOrNull(position)
                        ?: return
                try {
                    val confirmSha256 =
                        context?.let {
                            MainActivity.addToBleDeviceMainDatabase(
                                targetModel,
                                it
                            )
                        } //插入或更新到数据库
                    if (confirmSha256 != null) {
                        //确定加入数据库的数据未发生错误或篡改
                        if ((targetModel.deviceSha256 == confirmSha256)) {
                            lifecycleScope.launch {
                                //执行用户需要的操作
                                when (want) {
                                    WANT_ADD_TO_DEVICE -> {}
                                    WANT_STICK_TO_TOP -> {
                                        if (bleFragmentViewModel.isAllowNotifyChanged.get()) {
                                            val targetItem =
                                                scanResultRecyclerViewAdapter?.modelList?.removeAt(
                                                    position
                                                )
                                            if (targetItem != null) {
                                                scanResultRecyclerViewAdapter?.modelList?.add(
                                                    0,
                                                    targetItem
                                                )
                                            }
                                            //完成置顶效果需要RecyclerView刷新显示
                                            scanResultRecyclerViewAdapter?.notifyItemMoved(
                                                position,
                                                0
                                            )
                                            scanResultRecyclerViewAdapter?.notifyItemChanged(
                                                0
                                            )
                                            if (position != 0) {
                                                scanResultRecyclerViewAdapter?.notifyItemChanged(
                                                    1
                                                ) //更新被挤下去的条目
                                            }
                                        }
                                    }

                                    WANT_START_CONTROL -> {
                                        bluetoothScanStop()
                                        bleFragmentRunWant?.startControl(targetModel)
                                        targetModel.controlBase =
                                            bleFragmentRunWant?.getControlBaseBluetooth(
                                                targetModel.deviceSha256
                                            )
                                    }
                                }
                            }
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
    fun devicesListClearBluetooth() {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            if (scanResultRecyclerViewAdapter != null) {
                scanResultRecyclerViewAdapter?.modelList?.clear()
                scanResultRecyclerViewAdapter?.notifyDataSetChanged()
            }
        }
    }

    /**
     * (用于开发测试)测试蓝牙设备看板,将虚拟一个蓝牙设备存储到列表,测试功能,其中BluetoothDevice设置为空
     *
     * @param deviceDistance 设备距离
     * @param iconID         设备外观图标ID
     */
    fun testAddBluetoothDeviceRecyclerView(deviceDistance: Int, iconID: Int) {
        //根据时间生成伪设备SHA-256
        val sha256: String = fetchFakeSha256ByTime()
        //创建设备模型
        val deviceModel =
            BluetoothDeviceModel(null, deviceDistance, iconID, sha256) //生成这个蓝牙设备的基本信息模型
        //缓存到RecyclerView适配器内部列表
        val index = scanResultRecyclerViewAdapter?.itemCount
        index?.let {
            scanResultRecyclerViewAdapter?.modelList?.add(
                it,
                deviceModel
            ) //添加信息到公共的表,RecyclerView将利用表中信息显示
            scanResultRecyclerViewAdapter?.notifyItemInserted(it) //提示信息更新,需要RecyclerView刷新显示
        }

    }


    ////UI-主界面-BLE-MainTextViewBLE
    private var mainTextViewBLE: TextView? = null
    private var fadeOutMainTextViewBLE: ObjectAnimator? = null
    private var fadeInMainTextViewBLE: ObjectAnimator? = null

    private fun initMainTextViewBLE(view: View) {
        //获取实例
        mainTextViewBLE = view.findViewById(R.id.main_text_in_ble)
        mainTextViewBLE?.let {
            //创建消隐动画效果
            fadeOutMainTextViewBLE = ObjectAnimator.ofFloat(it, "alpha", 1f, 0f)
            fadeOutMainTextViewBLE?.setDuration(5000)
            //创建淡入动画效果
            fadeInMainTextViewBLE = ObjectAnimator.ofFloat(it, "alpha", 0f, 1f)
            fadeInMainTextViewBLE?.setDuration(2000)
        }

    }


    ////UI-蓝牙扫描动画
    private var animationBluetoothScanning: View? = null

    private fun initAnimationBluetoothScanning(view: View) {
        animationBluetoothScanning = view.findViewById(R.id.scan_state_indicator_in_ble)
    }


    ///UI-蓝牙扫描操作控制
    private var gestureMainText: GestureDetector? = null

    private inner class ListenerGestureMainText : SimpleOnGestureListener() {
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            super.onFling(e1, e2, velocityX, velocityY)
            fadeInMainTextViewBLE?.start()
            //如果以水平滑动为主
            if (abs(velocityX.toDouble()) > abs(velocityY.toDouble())) {
                if (!bleFragmentViewModel.isScanningBluetooth) {
                    bluetoothScanStart() //扫描启动
                }
            } else { //如果以垂直滑动为主
                if (bleFragmentViewModel.isScanningBluetooth) {
                    bluetoothScanStop()
                }
            }
            return true
        }

        override fun onDown(e: MotionEvent): Boolean { //如果为点击
            super.onDown(e)
            if (bleFragmentViewModel.isScanningBluetooth) {
                mainTextViewBLE?.text =
                    getString(R.string.vertical_slide_text_to_stop_sacn_chinese)
            } else {
                mainTextViewBLE?.text =
                    getString(R.string.horizontal_slide_text_to_start_sacn_chinese)
            }
            fadeInMainTextViewBLE?.start()
            return true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initGestureMainText(view: View) {
        if (!isAdded) return

        gestureMainText = GestureDetector(requireActivity(), ListenerGestureMainText())
        val detectView = view.findViewById<View>(R.id.main_text_in_ble)

        detectView.setOnTouchListener { _, motionEvent ->
            gestureMainText?.onTouchEvent(
                motionEvent
            ) == true
        }
    }


    /**
     * 在MainTextView显示文本作为告示(显示几秒后会逐渐消失)
     * @param text 填写需要展示的文本
     */
    private fun showNoticeMainText(text: String) {
        mainTextViewBLE?.text = text
        fadeInMainTextViewBLE?.start()
    }

    /**
     * 初始化BleFragment的UI界面
     *
     * @param view onCreateView返回的根视图实例
     */
    private fun initBleFragmentUI(view: View) {
        initAnimationBluetoothScanning(view)
        initMainTextViewBLE(view)

        initGestureMainText(view)
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
        const val WANT_START_CONTROL: Byte = 1 //开始设备控制
        const val WANT_ADD_TO_DEVICE: Byte = 2 //添加入"设备"
        const val WANT_STICK_TO_TOP: Byte = 3 //置顶,移动到列表顶部
        const val WANT_NONE: Byte = 4 //无操作

        /**
         * (用于开发测试)根据时间生成伪设备SHA-256
         */
        fun fetchFakeSha256ByTime(): String {
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