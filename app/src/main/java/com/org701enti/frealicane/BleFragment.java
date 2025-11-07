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

package com.org701enti.frealicane;

import static android.content.Context.BLUETOOTH_SERVICE;
import static com.org701enti.frealicane.MainActivity.AddToBleDeviceMainDatabase;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.org701enti.bluetoothfocuser.BluetoothAD;
import com.org701enti.bluetoothfocuser.StandardSync;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BleFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BleFragment extends Fragment {

    String TAG = new String("BleFragment");

    public BleFragment() {
        // Required empty public constructor
    }

    //运行需求
    private BleFragmentRunWant bleFragmentRunWant;

    public BleFragmentRunWant getBleFragmentRunWant() {
        return bleFragmentRunWant;
    }

    public interface BleFragmentRunWant {
        /**
         * 开始设备控制
         *
         * @param device BluetoothDevice实例
         * @param sha256 设备的SHA-256校验码
         */
        void startControl(BluetoothDevice device, String sha256);

        MainActivity.ControlBaseBluetooth getControlBaseBluetooth(String sha256);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            bleFragmentRunWant = activity.getBleFragmentFunctionRun();
        } else {
            throw new RuntimeException("must be attached by MainActivity but not by" + context.toString());
        }
    }

    public static BleFragment newInstance() {
        BleFragment fragment = new BleFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        InitBLE();

        if (bluetoothAdapter == null || bluetoothLeScanner == null) {//已经初始化但还为空
            //用户设备不支持蓝牙,弹出提示
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            builder.setTitle(R.string.error_chinese);
            builder.setMessage(R.string.user_device_hardware_unsupport);
            builder.setPositiveButton(getString(R.string.cancel_chinese), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });
            builder.setNegativeButton(getString(R.string.return_app_chinese), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ble, container, false);

        //初始化其他布局
        initRecyclerViewBluetooth(view);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        InitBleFragmentUI(view);
    }

    ////蓝牙内部处理业务
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothLeScanner bluetoothLeScanner = null;
    private RecyclerView scanResultRecyclerView = null;
    private List<BluetoothDeviceModel> scanResultList = new ArrayList<>();
    private ScanResultRecyclerViewAdapter scanResultRecyclerViewAdapter = null;
    private boolean isScanningBluetooth = false;

    private void InitBLE() {
        //获取BLEadapter实例
        BluetoothManager BLEmanager = (BluetoothManager) requireActivity().getSystemService(BLUETOOTH_SERVICE);
        if (BLEmanager != null) {
            bluetoothAdapter = BLEmanager.getAdapter();
            if (bluetoothAdapter != null) {
                bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            }
        }
    }

    /**
     * 检查蓝牙开启状态,如果未开启,打开蓝牙
     */
    @SuppressLint("MissingPermission")
    public void BluetoothOpenCheck() {
        //检查并启动
        //用于可能弹出打开蓝牙的询问框,回归主线程处理
        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.getState() != BluetoothAdapter.STATE_ON) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    //请求用户启动蓝牙
                    @Override
                    public void run() {
                        bluetoothAdapter.enable();
                        MainTextViewBLE.setText(R.string.please_continue_when_bluetooth_enabled_chinese);
                        fadeInMainTextViewBLE.start();
                    }
                });
            }
        }
    }

    /**
     * 启动蓝牙设备扫描,含配置
     */
    @SuppressLint("MissingPermission")
    public void BluetoothScanStart(List<ScanFilter> filters, ScanSettings settings) {
        if (!isScanningBluetooth) {

            //我们这里有必要解释为什么使用两次bluetoothLeScanner的null判断
            //(请您见下面BluetoothScanStop方法中的注释)

            if (bluetoothLeScanner == null) {
                //当以蓝牙关闭状态进入APP,InitBLE获取的bluetoothLeScanner将为空
                BluetoothOpenCheck();//检查蓝牙开启
                InitBLE();
            }
            if (bluetoothLeScanner != null) {
                if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
                    bluetoothLeScanner.startScan(filters, settings, bluetoothScanCallback);
                    AnimationBluetoothScanning.setVisibility(View.VISIBLE);
                    MainTextViewBLE.setText(getString(R.string.vertical_slide_text_to_stop_sacn_chinese));
                    isScanningBluetooth = true;
                    fadeOutMainTextViewBLE.start();
                } else {
                    BluetoothOpenCheck();//检查蓝牙开启
                }
            }
        }
    }

    /**
     * 启动蓝牙设备扫描,无配置
     */
    @SuppressLint("MissingPermission")
    public void BluetoothScanStart() {
        if (!isScanningBluetooth) {

            //我们这里有必要解释为什么使用两次bluetoothLeScanner的null判断
            //(请您见下面BluetoothScanStop方法中的注释)

            if (bluetoothLeScanner == null) {
                //当以蓝牙关闭状态进入APP,InitBLE获取的bluetoothLeScanner将为空
                BluetoothOpenCheck();//检查蓝牙开启
                InitBLE();
            }
            if (bluetoothLeScanner != null) {
                if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
                    bluetoothLeScanner.startScan(bluetoothScanCallback);
                    AnimationBluetoothScanning.setVisibility(View.VISIBLE);
                    MainTextViewBLE.setText(getString(R.string.vertical_slide_text_to_stop_sacn_chinese));
                    isScanningBluetooth = true;
                    fadeOutMainTextViewBLE.start();
                } else {
                    BluetoothOpenCheck();//检查蓝牙开启
                }
            }
        }
    }

    /**
     * 停止蓝牙设备扫描
     */
    @SuppressLint("MissingPermission")
    public void BluetoothScanStop() {
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
                BluetoothOpenCheck();//检查蓝牙开启
                InitBLE();
            }
            if (bluetoothLeScanner != null) {
                if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
                    bluetoothLeScanner.stopScan(bluetoothScanCallback);
                    AnimationBluetoothScanning.setVisibility(View.INVISIBLE);
                    MainTextViewBLE.setText(getString(R.string.horizontal_slide_text_to_start_sacn_chinese));
                    isScanningBluetooth = false;
                    fadeOutMainTextViewBLE.start();
                } else {
                    BluetoothOpenCheck();//检查蓝牙开启
                }
            }

        }
    }

    //蓝牙设备列表模型类
    public class BluetoothDeviceModel {
        private BluetoothDevice device;//可能包含设备名称,信号强度等多种数据和方法

        private String deviceSha256;////设备的SHA-256唯一性与安全校验码

        //设备的外观图标ID,其实就是外观值的bit6到bit15
        //详见(2.6.2)https://www.bluetooth.com/wp-content/uploads/Files/Specification/HTML/Assigned_Numbers/out/en/Assigned_Numbers.pdf
        private int iconID;

        private int deviceDistance;//与设备的距离(单位:米)

        private MainActivity.ControlBaseBluetooth controlBaseBluetooth;//控制基础(创建GATT连接后获得并绑定到此)

        public BluetoothDeviceModel(BluetoothDevice device, int deviceDistance, @NonNull String deviceSha256) {
            this.device = device;
            this.deviceDistance = deviceDistance;
            this.deviceSha256 = deviceSha256;
            this.iconID = 0;
            controlBaseBluetooth = null;
        }

        public BluetoothDeviceModel(BluetoothDevice device, int deviceDistance, int iconID, @NonNull String deviceSha256) {
            this.device = device;
            this.deviceDistance = deviceDistance;
            this.deviceSha256 = deviceSha256;
            this.iconID = iconID;
            controlBaseBluetooth = null;
        }

        public BluetoothDevice getDevice() {
            return device;
        }

        public int getIconID() {
            return iconID;
        }

        public int getDeviceDistance() {
            return deviceDistance;
        }

        public void setDeviceDistance(int deviceDistance) {
            this.deviceDistance = deviceDistance;
        }

        public String getDeviceSha256() {
            return deviceSha256;
        }

        public MainActivity.ControlBaseBluetooth getControlBaseBluetooth() {
            return controlBaseBluetooth;
        }

        public void setControlBaseBluetooth(MainActivity.ControlBaseBluetooth controlBaseBluetooth) {
            this.controlBaseBluetooth = controlBaseBluetooth;
        }
    }

    /**
     * 粗略计算信号的传播距离2.4GHz
     *
     * @param powerTX 信号发射功率(单位:dBm)
     * @param powerRX 信号接收功率(单位:dBm)
     * @return 粗略估测距离(单位 : m)
     */
    public double Distance2400MHZ(int powerTX, int powerRX) {
        //路径损耗(单位dB)L=powerTX - powerRX ,f=2.4GHz,根据自由空间路径损耗公式,最后粗略得到:
        return Math.pow(10D, 0.9944D * ((powerTX - powerRX) / 20D) - 2.5D);
    }


    //扫描回调
    AtomicReference<Boolean> isAllowNotifyChanged = new AtomicReference<>(Boolean.TRUE);
    final ScanCallback bluetoothScanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (result == null || scanResultRecyclerViewAdapter == null) {
                return;
            }

            //判断设备是否已经存在列表
            for (int i = 0; i < scanResultRecyclerViewAdapter.getModelList().size(); i++) {
                BluetoothDeviceModel model = scanResultRecyclerViewAdapter.getModelList().get(i);
                //如果已经存在相同设备在列表
                if (result.getDevice().equals(model.getDevice())) {

                    //更新设备的deviceDistance,但使用内部解析
                    int distanceNow = 0;
                    int powerTXint = 0;//Bluetooth SIG定义发射功率水平为sint8(有符号)(单位:dBm),与byte一致
                    powerTXint = result.getTxPower(); //(使用内部解析以节约资源)获取蓝牙设备的信号发射功率水平(单位:dBm)
                    if (powerTXint == Byte.MAX_VALUE) {//如果无法读取
                        powerTXint = 0;
                    }
                    distanceNow = (int) Distance2400MHZ(powerTXint, result.getRssi());
                    model.setDeviceDistance(distanceNow);

                    if (isAllowNotifyChanged.get()) {
                        scanResultRecyclerViewAdapter.notifyItemChanged(i);//提示信息更新,需要RecyclerView刷新显示
                    }
                    return;
                }
            }

            //解析广播数据包
            BluetoothAD bluetoothAD = new BluetoothAD(result, null);

            //获取iconID
            int iconID = 0;
            Activity activity = getActivity();
            if (activity != null) {
                if (activity instanceof MainActivity mainActivity) {
                    //获取StandardSync实例
                    StandardSync standardSync = mainActivity.getStandardSync();
                    if (standardSync != null) {
                        try {
                            if (standardSync.getYamlAdTypes() != null) {
                                //使用StandardSync的YamlResolver解析出需要的Value,即当前标准的Appearance的adType数值
                                StandardSync.YamlResolver yamlResolver = standardSync.new YamlResolver(standardSync.getYamlAdTypes());
                                Integer appearanceAdType = (Integer)
                                        yamlResolver
                                                .enterThisMapList("ad_types")
                                                .reserveTheItemsHave("name", "Appearance")
                                                .getResultList().get(0).get("value");
                                assert appearanceAdType != null;
                                //使用BluetoothAD在广播数据中提取数据
                                List<BluetoothAD.AdvertisingStruct> structList = bluetoothAD.Search(appearanceAdType);
                                if (!structList.isEmpty()) {
                                    for (BluetoothAD.AdvertisingStruct struct : structList) {
                                        if (struct.getAdData().length == 2) {
                                            iconID = ((struct.getAdData()[1] & 0xFF) << 8 | (struct.getAdData()[0] & 0xFF)) >>> 10;
                                        }
                                        if (struct.getAdData().length == 3) {
                                            iconID = ((struct.getAdData()[2] & 0xFF) << 16 | (struct.getAdData()[1] & 0xFF) << 8 | (struct.getAdData()[0] & 0xFF)) >>> 18;
                                        }
                                        Log.i(TAG, "onScanResult: 获取到" + result.getDevice().getName() + "的iconID: " + iconID);
                                    }
                                }

                            }
                        } catch (AssertionError | NullPointerException | IndexOutOfBoundsException |
                                 IOException e) {
                            if (result.getDevice().getName() != null) {
                                Log.w(TAG, "onScanResult: 无法获取" + result.getDevice().getName() + "的iconID,因为:", e);
                            } else {
                                Log.w(TAG, "onScanResult: 无法获取" + bluetoothAD.getSha256StringAdvertising() + "的iconID,因为:", e);
                            }

                        }
                    }
                }
            }


            //获取deviceDistance
            int deviceDistance = 0;
            byte powerTX = 0;//Bluetooth SIG定义发射功率水平为sint8(有符号)(单位:dBm),与byte一致
            //如果设备广播数据包含了,获取发射功率,如果没有包含,将以定义时的初始化值0计算
            List<BluetoothAD.AdvertisingStruct> structList = bluetoothAD.Search(0x0A);
            if (!structList.isEmpty()) {
                if (structList.get(0) != null) {
                    powerTX = structList.get(0).getAdData()[0];
                }
            }
            deviceDistance = (int) Distance2400MHZ(powerTX, result.getRssi());

            //创建设备模型
            BluetoothDeviceModel deviceModel = new BluetoothDeviceModel(result.getDevice(), deviceDistance,
                    iconID, bluetoothAD.getSha256StringAdvertising());//生成这个蓝牙设备的基本信息模型

            //缓存到RecyclerView适配器内部列表
            int index = scanResultRecyclerViewAdapter.getItemCount();
            scanResultRecyclerViewAdapter.getModelList().add(index, deviceModel);//添加信息到公共的表,RecyclerView将利用表中信息显示
            scanResultRecyclerViewAdapter.notifyItemInserted(index);//提示信息更新,需要RecyclerView刷新显示
            Log.i("BluetoothInfoReceiver", "[" + index + "]" + deviceModel.getDevice().getName() + " { iconID: " + deviceModel.getIconID() + " }");
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            if (results != null) {
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);


        }
    };

    ////UI-蓝牙设备看板
    public class ItemDecorationRecyclerViewBluetooth extends RecyclerView.ItemDecoration {
        private final float dividerHeightPx;
        private final Paint dividerPaint;

        public ItemDecorationRecyclerViewBluetooth(float dividerHeightPx, int dividerColor) {
            this.dividerHeightPx = dividerHeightPx;
            this.dividerPaint = new Paint();
            this.dividerPaint.setColor(dividerColor);
            this.dividerPaint.setStrokeWidth(dividerHeightPx);
            this.dividerPaint.setStyle(Paint.Style.FILL);
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            outRect.bottom = (int) dividerHeightPx;
        }

        @Override
        public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            for (int i = 0; i < parent.getChildCount() - 1; i++) {//列表最后一项不添加分隔线
                View child = parent.getChildAt(i);
                c.drawRect(
                        child.getLeft(),
                        child.getBottom(),
                        child.getRight(),
                        child.getBottom() + dividerHeightPx,
                        dividerPaint
                );
            }
        }
    }

    private void initRecyclerViewBluetooth(View view) {
        scanResultRecyclerViewAdapter = new ScanResultRecyclerViewAdapter(scanResultList);
        scanResultRecyclerView = view.findViewById(R.id.scan_result_recycler_view_in_ble);
        scanResultRecyclerView.setAdapter(scanResultRecyclerViewAdapter);
        scanResultRecyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        float density = requireContext().getResources().getDisplayMetrics().density;
        scanResultRecyclerView.addItemDecoration(new ItemDecorationRecyclerViewBluetooth(1 * density, R.color.light_gray));//分隔线高度固定为1dp

        //禁用变更动画
        RecyclerView.ItemAnimator animator = scanResultRecyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
    }


    //BLE-RecyclerView的适配器类,用于RecyclerView展示扫描到的蓝牙设备
    public class ScanResultRecyclerViewAdapter extends RecyclerView.Adapter<ScanResultRecyclerViewAdapter.ScanResultItemViewHolder> {
        private List<BluetoothDeviceModel> modelList;

        public ScanResultRecyclerViewAdapter(List<BluetoothDeviceModel> modelList) {
            this.modelList = modelList;
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onBindViewHolder(@NonNull ScanResultItemViewHolder holder, int position) {
            BluetoothDeviceModel targetModel = null;
            targetModel = modelList.get(position);//获取要读取操作列表中的的deviceModel实例
            //holder包含了需要刷新区域的对应View引用,实际存储在之前实例化的ViewHolder池中
            if (targetModel != null) {
                //当前View配置
                configImageViewDeviceIcon(targetModel, holder.deviceIcon);//设备图标
                configTextViewDeviceName(targetModel, holder.deviceName, calculateSuitableTextSizeSp(holder.deviceName.length()));//设备名
                int distance = targetModel.getDeviceDistance();//与设备的距离
                String showDistance = distance + getString(R.string.meter_chinese);
                holder.deviceDistance.setText(showDistance);
            }
            //保存targetModel到holder
            holder.setTargetModel(targetModel);
        }

        @NonNull
        @Override
        public ScanResultItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            //实例化自定义布局R.layout.recyclerviewbluetooth
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_of_scan_result_recycler_view, parent, false);
            ScanResultItemViewHolder holder = new ScanResultItemViewHolder(view);

            //触控事件注册
            view.setOnClickListener(v -> {
                //显示详细信息弹窗
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
                View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_of_more_info_scan_result_item, null);
                builder.setView(dialogView);

                //设备图标
                ImageView deviceIcon = dialogView.findViewById(R.id.device_icon_in_dialog_of_more_info_scan_result_item);
                configImageViewDeviceIcon(holder.targetModel, deviceIcon);

                //设备名
                TextView deviceName = dialogView.findViewById(R.id.device_name_in_dialog_of_more_info_scan_result_item);
                configTextViewDeviceName(holder.targetModel, deviceName, calculateSuitableTextSizeSp(deviceName.length()));

                //底部按钮
                MaterialButton startControlButton = dialogView.findViewById(R.id.start_control_button_in_dialog_of_more_info_scan_result_item);
                MaterialButton addToDeviceButton = dialogView.findViewById(R.id.add_to_device_button_in_dialog_of_more_info_scan_result_item);
                MaterialButton stickToTopButton = dialogView.findViewById(R.id.stick_to_top_button_in_dialog_of_more_info_scan_result_item);
                MaterialButton undoButton = dialogView.findViewById(R.id.undo_button_in_dialog_of_more_info_scan_result_item);

                //配置dialog
                androidx.appcompat.app.AlertDialog dialog = builder.create();
                dialog.show();

                //设置事件监听
                startControlButton.setOnClickListener(v1 -> {
                    scanResultItemOperationRun(WANT_START_CONTROL, holder.getBindingAdapterPosition(), requireContext(), new OperationRunListener() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onFailure() {

                        }
                    });
                });
                addToDeviceButton.setOnClickListener(v2 -> {
                    scanResultItemOperationRun(WANT_ADD_TO_DEVICE, holder.getBindingAdapterPosition(), requireContext(), new OperationRunListener() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onFailure() {

                        }
                    });
                });
                stickToTopButton.setOnClickListener(v3 -> {
                    scanResultItemOperationRun(WANT_STICK_TO_TOP, holder.getBindingAdapterPosition(), requireContext(),null);
                    dialog.dismiss();
                });
                undoButton.setOnClickListener(v4 -> dialog.dismiss());

            });

            view.setOnLongClickListener(v -> {
                scanResultItemOperationRun(WANT_STICK_TO_TOP, holder.getBindingAdapterPosition(), requireContext(),null);
                return true;
            });

            return holder;
        }

        /**
         * ViewHolder池,持有相关View引用,防止findViewById更多调用来优化性能
         */
        public class ScanResultItemViewHolder extends RecyclerView.ViewHolder {
            //数据源
            private BluetoothDeviceModel targetModel = null;

            //View元素
            public TextView deviceName = null;
            public ImageView deviceIcon = null;
            public TextView deviceDistance = null;

            //动画效果
            public ObjectAnimator fadeInDeviceDistance = null;

            //在构造方法将各种View引用缓存到ViewHolder池
            public ScanResultItemViewHolder(View view) {
                //super调用父类RecyclerView.ViewHolder构造方法,并传递了参数viewHandle
                //即自定义布局R.layout.recyclerviewbluetooth的实例,因此自定义布局文件的配置会对效果产生影响
                //如果其中开头的layout_width,layout_height选择了match_parent,会导致绘制间距非常大,难以修正
                super(view);

                //元素View相关
                deviceName = view.findViewById(R.id.device_name_in_item_of_scan_result_recycler_view);
                deviceIcon = view.findViewById(R.id.device_icon_in_item_of_scan_result_recycler_view);
                deviceDistance = view.findViewById(R.id.device_distance_in_item_of_scan_result_recycler_view);

                //动画效果
                if (deviceDistance != null) {
                    fadeInDeviceDistance = ObjectAnimator.ofFloat(deviceDistance, "alpha", 0F, 1F);
                    fadeInDeviceDistance.setDuration(1000);
                    fadeInDeviceDistance.setInterpolator(new DecelerateInterpolator());
                }
            }

            public BluetoothDeviceModel getTargetModel() {
                return targetModel;
            }

            public void setTargetModel(BluetoothDeviceModel targetModel) {
                this.targetModel = targetModel;
            }
        }

        public List<BluetoothDeviceModel> getModelList() {
            return modelList;
        }

        @Override
        public int getItemCount() {
            if (modelList != null) {
                return modelList.size();
            } else {
                return 0;
            }
        }

        public float calculateSuitableTextSizeSp(int length) {
            if (length <= 12) {
                return 24f - 4f * 1;
            } else if (length <= 16) {
                return 24f - 4f * 2;
            } else if (length <= 20) {
                return 24f - 4f * 3;
            } else {
                return 24f - 4f * 4;
            }
        }

        /***
         * 配置DeviceIcon组件以展示需要的内容
         * @param targetModel 选择数据来源的BluetoothDeviceModel实例
         * @param deviceIcon 对该ImageView实例执行配置
         */
        private void configImageViewDeviceIcon(BluetoothDeviceModel targetModel, ImageView deviceIcon) {
            int iconID = targetModel.getIconID();
            Bitmap iconBitmap = null;
            try (InputStream iconInput = requireActivity().getAssets().open("bluetoothdeviceicon/btac" + iconID + ".png")) {
                iconBitmap = BitmapFactory.decodeStream(iconInput);
                if (iconBitmap == null) {
                    deviceIcon.setImageResource(R.drawable.ble);
                }
            } catch (IOException e) {
                deviceIcon.setImageResource(R.drawable.ble);
            }
            if (iconBitmap != null) {
                deviceIcon.setImageBitmap(iconBitmap);
            }
        }

        /***
         * 配置DeviceName组件以展示需要的内容
         * @param targetModel 选择数据来源的BluetoothDeviceModel实例
         * @param deviceName 对该TextView实例执行配置
         * @param sizeSP 字体大小,单位sp
         */
        @SuppressLint("MissingPermission")
        private void configTextViewDeviceName(BluetoothDeviceModel targetModel, TextView deviceName, float sizeSP) {
            String name = null;
            if (targetModel.getDevice() != null) {
                name = targetModel.getDevice().getName();
            }
            if (name == null) {
                deviceName.setText(getString(R.string.unknown_device_chinese));
            } else {
                //确定显示的字符尺寸
                deviceName.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSP);
                deviceName.setText(name);
            }
        }

    }

    //用户操作枚举-->
    final static byte WANT_START_CONTROL = 1;//开始设备控制
    final static byte WANT_ADD_TO_DEVICE = 2;//添加入"设备"
    final static byte WANT_STICK_TO_TOP = 3;//置顶,移动到列表顶部
    final static byte WANT_NONE = 4;//无操作

    public interface OperationRunListener{
        void onSuccess();
        void onFailure();
    }

    /**
     * 对扫描结果条目运行需要的操作,执行操作就会将设备信息加入数据库
     *
     * @param want     用户操作枚举 WANT_X
     * @param position 选择操作的模型单元在list的位置
     * @param context  上下文,可以使用Activity作为上下文
     */
    public void scanResultItemOperationRun(byte want, int position, Context context,@Nullable OperationRunListener listener) {
        if (want == WANT_NONE) {
            return;
        }

        //添加设备数据到数据库并继续处理-在独立线程执行
        new Thread(new Runnable() {
            @Override
            public void run() {
                BluetoothDeviceModel targetModel = null;
                targetModel = scanResultRecyclerViewAdapter.getModelList().get(position);//获取要读取操作列表中的的deviceModel实例
                if (targetModel == null) {
                    return;
                }
                try {
                    String confirmSha256 = AddToBleDeviceMainDatabase(targetModel, context);//插入或更新到数据库
                    if (confirmSha256 != null) {
                        //确定加入数据库的数据未发生错误或篡改
                        if (targetModel.getDeviceSha256().equals(confirmSha256)) {

                            BluetoothDeviceModel finalTargetModel = targetModel;

                            //在主线程执行
                            Handler handler = new Handler(Looper.getMainLooper());
                            Runnable taskMainThread = new Runnable() {
                                @Override
                                public void run() {
                                    //执行用户需要的操作
                                    switch (want) {
                                        case WANT_ADD_TO_DEVICE: {

                                        }
                                        break;
                                        case WANT_STICK_TO_TOP: {
                                            if (position >= 0 && position < scanResultRecyclerViewAdapter.getModelList().size()) {
                                                if (isAllowNotifyChanged.get()) {
                                                    BluetoothDeviceModel targetItem = scanResultRecyclerViewAdapter.getModelList().remove(position);
                                                    scanResultRecyclerViewAdapter.getModelList().add(0, targetItem);
                                                    //完成置顶效果需要RecyclerView刷新显示
                                                    scanResultRecyclerViewAdapter.notifyItemMoved(position, 0);
                                                    scanResultRecyclerViewAdapter.notifyItemChanged(0);
                                                    if (position != 0) {
                                                        scanResultRecyclerViewAdapter.notifyItemChanged(1);//更新被挤下去的条目
                                                    }
                                                }
                                            }
                                        }
                                        break;
                                        case WANT_START_CONTROL: {
                                            BluetoothScanStop();
                                            bleFragmentRunWant.startControl(finalTargetModel.getDevice(), finalTargetModel.getDeviceSha256());
                                            finalTargetModel.setControlBaseBluetooth(bleFragmentRunWant.getControlBaseBluetooth(finalTargetModel.getDeviceSha256()));
                                            new Thread(() -> {
                                                //等待ControlBase部署完成-独立线程执行
                                                for (int t = 0; t < 1000; t++) {
                                                    try {
                                                        if (finalTargetModel.getControlBaseBluetooth() != null) {
                                                            if (finalTargetModel.getControlBaseBluetooth().getGatt() != null) {
                                                                break;
                                                            }
                                                        }
                                                        Thread.sleep(100);
                                                    } catch (InterruptedException e) {
                                                        throw new RuntimeException(e);
                                                    }
                                                }
                                            }
                                            ).start();
                                        }
                                        break;
                                    }
                                }
                            };
                            handler.post(taskMainThread);
                        }

                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    /**
     * (内部回归主线程处理)清除蓝牙设备列表并请求列表刷新
     */
    public void DevicesListClearBluetooth() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void run() {
                if (scanResultRecyclerViewAdapter != null) {
                    scanResultRecyclerViewAdapter.getModelList().clear();
                    scanResultRecyclerViewAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    /**
     * (用于开发测试)根据时间生成伪设备SHA-256
     */
    public static String FetchFakeSha256ByTime() {
        long timestamp = System.currentTimeMillis();
        //填充到byte数组,低位到高位
        byte[] rawCode = new byte[8];
        for (int i = 0; i < 8; i++) {
            rawCode[i] = (byte) (timestamp >> 8 * i);
        }
        return BluetoothAD.RawCodeFetchSha256String(rawCode);
    }


    /**
     * (用于开发测试)测试蓝牙设备看板,将虚拟一个蓝牙设备存储到列表,测试功能,其中BluetoothDevice设置为空
     *
     * @param deviceDistance 设备距离
     * @param iconID         设备外观图标ID
     */
    public void TestAddBluetoothDeviceRecyclerView(int deviceDistance, int iconID) {
        //根据时间生成伪设备SHA-256
        String sha256 = FetchFakeSha256ByTime();
        if (sha256 == null) {
            return;
        }
        //创建设备模型
        BluetoothDeviceModel deviceModel =
                new BluetoothDeviceModel(null, deviceDistance, iconID, sha256);//生成这个蓝牙设备的基本信息模型
        //缓存到RecyclerView适配器内部列表
        int index = scanResultRecyclerViewAdapter.getItemCount();
        scanResultRecyclerViewAdapter.getModelList().add(index, deviceModel);//添加信息到公共的表,RecyclerView将利用表中信息显示
        scanResultRecyclerViewAdapter.notifyItemInserted(index);//提示信息更新,需要RecyclerView刷新显示
    }


    ////UI-主界面-BLE-MainTextViewBLE
    private TextView MainTextViewBLE = null;
    private ObjectAnimator fadeOutMainTextViewBLE = null;
    private ObjectAnimator fadeInMainTextViewBLE = null;

    private void InitMainTextViewBLE(View view) {
        //获取实例
        MainTextViewBLE = view.findViewById(R.id.main_text_in_ble);
        //创建消隐动画效果
        fadeOutMainTextViewBLE = ObjectAnimator.ofFloat(MainTextViewBLE, "alpha", 1F, 0F);
        fadeOutMainTextViewBLE.setDuration(5000);
        //创建淡入动画效果
        fadeInMainTextViewBLE = ObjectAnimator.ofFloat(MainTextViewBLE, "alpha", 0F, 1F);
        fadeInMainTextViewBLE.setDuration(2000);
    }


    ////UI-蓝牙扫描动画
    private View AnimationBluetoothScanning = null;

    private void InitAnimationBluetoothScanning(View view) {
        AnimationBluetoothScanning = view.findViewById(R.id.scan_state_indicator_in_ble);
    }


    ///UI-蓝牙扫描操作控制(基于动画实例)
    private GestureDetector gestureMainText = null;

    private class ListenerGestureMainText extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
            super.onFling(e1, e2, velocityX, velocityY);

            fadeInMainTextViewBLE.start();

            //如果以水平滑动为主
            if (Math.abs(velocityX) > Math.abs(velocityY)) {
                if (!isScanningBluetooth) {
                    BluetoothScanStart();//扫描启动
                }
            } else {//如果以垂直滑动为主
                if (isScanningBluetooth) {
                    BluetoothScanStop();
                }
            }
            return true;
        }

        @Override
        public boolean onDown(@NonNull MotionEvent e) {//如果为点击
            super.onDown(e);


            if (isScanningBluetooth) {
                MainTextViewBLE.setText(getString(R.string.vertical_slide_text_to_stop_sacn_chinese));
            } else {
                MainTextViewBLE.setText(getString(R.string.horizontal_slide_text_to_start_sacn_chinese));
            }

            fadeInMainTextViewBLE.start();
            return true;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void InitGestureMainText(View view) {
        gestureMainText = new GestureDetector(requireActivity(), new ListenerGestureMainText());
        View detectView = view.findViewById(R.id.main_text_in_ble);

        detectView.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return gestureMainText.onTouchEvent(motionEvent);
            }
        });
    }

    /**
     * 初始化BleFragment的UI界面
     *
     * @param view onCreateView返回的根视图实例
     */
    private void InitBleFragmentUI(View view) {
        InitAnimationBluetoothScanning(view);
        InitMainTextViewBLE(view);

        InitGestureMainText(view);

    }


}