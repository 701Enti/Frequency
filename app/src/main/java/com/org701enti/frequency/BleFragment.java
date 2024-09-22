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

package com.org701enti.frequency;

import static android.content.Context.BLUETOOTH_SERVICE;
import static android.content.Context.WINDOW_SERVICE;

import static com.org701enti.frequency.MainActivity.AddToBleDeviceMainDatabase;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
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
import android.graphics.Rect;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.org701enti.bluetoothfocuser.BluetoothAD;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BleFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BleFragment extends Fragment {

    public BleFragment() {
        // Required empty public constructor
    }

    private BleFragmentRunUserWant bleFragmentRunUserWant;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof MainActivity){
            MainActivity activity = (MainActivity) context;
            bleFragmentRunUserWant = activity.getBleFragmentFunctionRun();
        }
        else{
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

        if(bluetoothAdapter == null || bluetoothLeScanner == null){//已经初始化但还为空
            //用户设备不支持蓝牙,弹出提示
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            builder.setTitle(R.string.error_chinese);
            builder.setMessage(R.string.userdevicehardwareunsupport);
            builder.setPositiveButton(getString(R.string.cancel_chinese), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });
            builder.setNegativeButton(getString(R.string.returnapp_chinese), new DialogInterface.OnClickListener() {
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

        //创建其他布局
        CreateRecyclerViewBluetooth(view);

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
    private List<BluetoothDeviceModel> bluetoothDevicesList = new ArrayList<>();
    private BluetoothDeviceRecyclerViewAdapter bluetoothDeviceRecyclerViewAdapter = null;
    private RecyclerView recyclerViewBluetooth = null;
    private boolean isScanningBluetooth = false;

    private void InitBLE() {
        //获取BLEadapter实例
        BluetoothManager BLEmanager = (BluetoothManager) requireActivity().getSystemService(BLUETOOTH_SERVICE);
        if (BLEmanager != null) {
            bluetoothAdapter = BLEmanager.getAdapter();
            if(bluetoothAdapter!=null){
                bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            }
        }
    }

    /**
     *
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
                        MainTextViewBLE.setText(R.string.pleasecontinuewhenbluetoothenabled_chinese);
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
    public void BluetoothScanStart(List<ScanFilter> filters, ScanSettings settings){
        if(!isScanningBluetooth){

            //我们这里有必要解释为什么使用两次bluetoothLeScanner的null判断
            //(请您见下面BluetoothScanStop方法中的注释)

            if(bluetoothLeScanner == null){
                //当以蓝牙关闭状态进入APP,InitBLE获取的bluetoothLeScanner将为空
                BluetoothOpenCheck();//检查蓝牙开启
                InitBLE();
            }
            if(bluetoothLeScanner != null){
                if(bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON){
                    bluetoothLeScanner.startScan(filters,settings,bluetoothScanCallback);
                    lottieAnimationBluetoothScanning.playAnimation();
                    MainTextViewBLE.setText(getString(R.string.verticalslidevavetostopsacn_chinese_chinese));
                    isScanningBluetooth = true;
                    fadeOutMainTextViewBLE.start();
                }
                else{
                    BluetoothOpenCheck();//检查蓝牙开启
                }
            }
        }
    }

    /**
     * 启动蓝牙设备扫描,无配置
     */
    @SuppressLint("MissingPermission")
    public void BluetoothScanStart(){
        if(!isScanningBluetooth){

            //我们这里有必要解释为什么使用两次bluetoothLeScanner的null判断
            //(请您见下面BluetoothScanStop方法中的注释)

            if(bluetoothLeScanner == null){
                //当以蓝牙关闭状态进入APP,InitBLE获取的bluetoothLeScanner将为空
                BluetoothOpenCheck();//检查蓝牙开启
                InitBLE();
            }
            if(bluetoothLeScanner != null){
                if(bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON){
                    bluetoothLeScanner.startScan(bluetoothScanCallback);
                    lottieAnimationBluetoothScanning.playAnimation();
                    MainTextViewBLE.setText(getString(R.string.verticalslidevavetostopsacn_chinese_chinese));
                    isScanningBluetooth = true;
                    fadeOutMainTextViewBLE.start();
                }
                else{
                    BluetoothOpenCheck();//检查蓝牙开启
                }
            }
        }
    }

    /**
     * 停止蓝牙设备扫描
     */
    @SuppressLint("MissingPermission")
    public void BluetoothScanStop(){
        if(isScanningBluetooth){

            //我们这里有必要解释为什么使用两次bluetoothLeScanner的null判断
            //在当用户以蓝牙关闭状态进入APP,InitBLE获取的bluetoothLeScanner将为空
            //我们期望选择这个时机来恢复正轨,因为用户现在非常明确他是希望使用蓝牙功能的,不是有意的
            //如果此时执行该方法,进入==null这个代码块,我们会请求系统弹出"是否打开蓝牙",之后我们重新初始化
            //但是我们需要澄清的是,重新初始化后,未必还是成功的,所以大概率是无法继续!= null这个代码块的
            //这是因为请求打开蓝牙需要用户操作之后,才会可能启动,相对是异步的,所以等蓝牙启动后,早就已经结束方法了
            //那么非常显然的,如果用户再次触发执行这个方法,我们便顺理成章地在用户完成打开蓝牙的状态下正常初始化了
            //之后使用两次bluetoothLeScanner的null判断的优势就出现了,我们可以继续!= null这个代码块,正常启动需求
            //这样我们在用户不知情的情况下完成了两个任务,如果我们判断一次== null再加上else就草草结束,您会发现用户必须额外再次触发一次该方法在初始化后正式运行

            if(bluetoothLeScanner == null){
                BluetoothOpenCheck();//检查蓝牙开启
                InitBLE();
            }
            if(bluetoothLeScanner != null){
                if(bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON){
                    bluetoothLeScanner.stopScan(bluetoothScanCallback);
                    lottieAnimationBluetoothScanning.pauseAnimation();
                    MainTextViewBLE.setText(getString(R.string.horizontalslidevavetostartsacn_chinese));
                    isScanningBluetooth = false;
                    fadeOutMainTextViewBLE.start();
                }
                else{
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

        public BluetoothDeviceModel(BluetoothDevice device,int deviceDistance,@NonNull String deviceSha256) {
            this.device = device;
            this.deviceDistance = deviceDistance;
            this.deviceSha256 = deviceSha256;
            this.iconID = 0;
        }

        public BluetoothDeviceModel(BluetoothDevice device,int deviceDistance,int iconID,@NonNull String deviceSha256){
            this.device = device;
            this.deviceDistance = deviceDistance;
            this.deviceSha256 = deviceSha256;
            this.iconID = iconID;
        }

        public BluetoothDevice getDevice() {
            return device;
        }
        public int getIconID(){ return iconID;}

        public int getDeviceDistance() {
            return deviceDistance;
        }

        public void setDeviceDistance(int deviceDistance) {
            this.deviceDistance = deviceDistance;
        }

        public String getDeviceSha256() {
            return deviceSha256;
        }
    }

    /**
     * 粗略计算信号的传播距离2.4GHz
     * @param powerTX 信号发射功率(单位:dBm)
     * @param powerRX 信号接收功率(单位:dBm)
     * @return 粗略估测距离(单位:m)
     */
    public double Distance2400MHZ(int powerTX,int powerRX){
        //路径损耗(单位dB)L=powerTX - powerRX ,f=2.4GHz,根据自由空间路径损耗公式,最后粗略得到:
        return Math.pow(  10D , 0.9944D * ((powerTX - powerRX)/20D)-2.5D  );
    }


    //扫描回调
    AtomicReference<Boolean> isAllowNotifyChanged = new AtomicReference<>(Boolean.TRUE);
    final ScanCallback bluetoothScanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if(result == null || bluetoothDeviceRecyclerViewAdapter == null){
                return;
            }

            //判断设备是否已经存在列表
            for(int i=0;i < bluetoothDeviceRecyclerViewAdapter.getModelList().size();i++){
                BluetoothDeviceModel model = bluetoothDeviceRecyclerViewAdapter.getModelList().get(i);
                //如果已经存在相同设备在列表
                if(result.getDevice().equals(model.getDevice())){
                    //更新设备的deviceDistance,但使用内部解析
                    int distanceNow = 0;

                    int powerTXint = 0;//Bluetooth SIG定义发射功率水平为sint8(有符号)(单位:dBm),与byte一致
                    powerTXint =  result.getTxPower(); //(使用内部解析以节约资源)获取蓝牙设备的信号发射功率水平(单位:dBm)
                    if(powerTXint == Byte.MAX_VALUE){//如果无法读取
                        powerTXint = 0;
                    }
                    distanceNow = (int) Distance2400MHZ(powerTXint,result.getRssi());

                    model.setDeviceDistance(distanceNow);
                    if(isAllowNotifyChanged.get()){
                        bluetoothDeviceRecyclerViewAdapter.notifyItemChanged(i);//提示信息更新,需要RecyclerView刷新显示
                    }
                    return;
                }
            }

            //解析广播数据包
            BluetoothAD bluetoothAD = new BluetoothAD(result,null);

            //获取iconID
            int iconID = 0;
            BluetoothAD.AdvertisingStruct structAppearance = bluetoothAD.Search(0x19);
            if(structAppearance != null){
                if(structAppearance.getAdData().length >= 2){
                    iconID = ((structAppearance.getAdData()[1] << 8 | structAppearance.getAdData()[0]) & 0xFFC0) >> 6;
                }
            }

            //获取deviceDistance
            int deviceDistance = 0;
            byte powerTX = 0;//Bluetooth SIG定义发射功率水平为sint8(有符号)(单位:dBm),与byte一致
            BluetoothAD.AdvertisingStruct structPowerTX = bluetoothAD.Search(0x0A);//蓝牙设备的信号发射功率水平(单位:dBm)的结构数据
            if(structPowerTX != null){//如果设备广播数据包含了,获取,如果没有包含,以powerTX = 0dBm计算
                powerTX = structPowerTX.getAdData()[0];
            }
            deviceDistance = (int) Distance2400MHZ(powerTX,result.getRssi());

            //创建设备模型
            BluetoothDeviceModel deviceModel = new BluetoothDeviceModel(result.getDevice(),deviceDistance,
                    iconID,bluetoothAD.getSha256StringAdvertising());//生成这个蓝牙设备的基本信息模型

            //缓存到RecyclerView适配器内部列表
            int index = bluetoothDeviceRecyclerViewAdapter.getItemCount();
            bluetoothDeviceRecyclerViewAdapter.getModelList().add(index,deviceModel);//添加信息到公共的表,RecyclerView将利用表中信息显示
            bluetoothDeviceRecyclerViewAdapter.notifyItemInserted(index);//提示信息更新,需要RecyclerView刷新显示
            Log.i("BluetoothInfoReceiver","[" + index + "]" + deviceModel.getDevice().getName());
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            if(results != null){
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);


        }
    };

    ////UI-蓝牙设备看板
    public class ItemDecorationRecyclerViewBluetooth extends RecyclerView.ItemDecoration{
        private final  int verticalSpaceHeight;

        public ItemDecorationRecyclerViewBluetooth(int verticalSpaceHeight){
            this.verticalSpaceHeight = verticalSpaceHeight;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            outRect.top = verticalSpaceHeight;
        }
    }
    private void CreateRecyclerViewBluetooth(View view){
        bluetoothDeviceRecyclerViewAdapter = new BluetoothDeviceRecyclerViewAdapter(bluetoothDevicesList);
        recyclerViewBluetooth = view.findViewById(R.id.RecyclerViewBluetoothBLE);
        recyclerViewBluetooth.setAdapter(bluetoothDeviceRecyclerViewAdapter);
        recyclerViewBluetooth.setLayoutManager(new LinearLayoutManager(requireActivity()));
        recyclerViewBluetooth.addItemDecoration(new ItemDecorationRecyclerViewBluetooth(30));
    }


    //BLE-RecyclerView的适配器类,用于RecyclerView展示扫描到的蓝牙设备
    public class BluetoothDeviceRecyclerViewAdapter extends RecyclerView.Adapter<BluetoothDeviceRecyclerViewAdapter.ViewHolder>
    {
        private List<BluetoothDeviceModel> modelList;

        public BluetoothDeviceRecyclerViewAdapter(List<BluetoothDeviceModel> modelList) {
            this.modelList = modelList;
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BluetoothDeviceModel targetModel = null;
            targetModel = modelList.get(position);//获取要读取操作列表中的的deviceModel实例
            //holder包含了需要刷新区域的对应View引用,实际存储在之前实例化的ViewHolder池中

            if(targetModel != null){
                //将目标模型的位置记录到holder池
                holder.positionBuf = holder.getBindingAdapterPosition();

                //设备图标
                int iconID = targetModel.getIconID();
                Bitmap iconBitmap = null;
                try(InputStream iconInput = requireActivity().getAssets().open("bluetoothdeviceicon/btac" + iconID + ".png")) {
                    iconBitmap = BitmapFactory.decodeStream(iconInput);
                    if(iconBitmap == null){
                        holder.deviceIcon.setImageResource(R.drawable.ble);
                    }
                }
                catch (IOException e) {
                    holder.deviceIcon.setImageResource(R.drawable.ble);
                }
                if(iconBitmap != null){
                    holder.deviceIcon.setImageBitmap(iconBitmap);
                }

                //设备名
                String name = null;
                if(targetModel.getDevice() != null){
                    name = targetModel.getDevice().getName();
                }
                if(name == null){
                    holder.deviceName.setText(getString(R.string.unknowndevice_chinese));
                }
                else {
                    //确定显示的字符尺寸
                    int len = name.length();
                    if(len <= 12){
                        holder.deviceName.setTextSize(TypedValue.COMPLEX_UNIT_SP,24f - 4f*1);
                    }
                    else if (len <= 16) {
                        holder.deviceName.setTextSize(TypedValue.COMPLEX_UNIT_SP,24f - 4f*2);
                    }
                    else if (len <= 20) {
                        holder.deviceName.setTextSize(TypedValue.COMPLEX_UNIT_SP,24f - 4f*3);
                    }
                    else{
                        holder.deviceName.setTextSize(TypedValue.COMPLEX_UNIT_SP,24f - 4f*4);
                    }

                    holder.deviceName.setText(name);
                }

                //与设备的距离
                int distance = targetModel.getDeviceDistance();
                String showDistance = distance + getString(R.string.meter_rice_chinese);
                holder.deviceDistance.setText(showDistance);
                holder.fadeInDeviceDistance.start();

            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflaterBuf = LayoutInflater.from(parent.getContext());
            //实例化自定义布局R.layout.recyclerviewbluetooth
            View view = inflaterBuf.inflate(R.layout.scan_result_show_bluetooth, parent, false);
            return new ViewHolder(view);
        }

        /**
         * ViewHolder池,持有相关View引用,防止findViewById更多调用来优化性能
         */
        public class ViewHolder extends RecyclerView.ViewHolder {
            //独立View元素缓存
            public TextView deviceName = null;
            public ImageView deviceIcon = null;
            public TextView deviceDistance = null;

            //记录
            public int positionBuf = -1;

            public ObjectAnimator fadeInDeviceDistance = null;

            //用户操作相关
            float progress = 0;//扫描动画播放进度
            String nameBuf = getString(R.string.unknowndevice_chinese);
            RelativeLayout.LayoutParams deviceNameParams = null;//
            //以下坐标单位均为像素px
            float startX = 0;//开始按下时的横坐标
            float currentX = 0;//当前手指滑动位置的横坐标
            float displayX = 0;//屏幕的最大横坐标
            int deviceNameBufX = 0;//deviceName的横坐标缓存
            int deviceNameBufY = 0;//deviceName的纵坐标缓存
            //操作需求
            byte user_want = WANT_NONE;
            //动画恢复运行标识
            AtomicReference<Boolean> isRunningRecover = null;

            //在构造方法将各种View引用缓存到ViewHolder池
            public ViewHolder(View view) {
                //super调用父类RecyclerView.ViewHolder构造方法,并传递了参数viewHandle
                //即自定义布局R.layout.recyclerviewbluetooth的实例,因此自定义布局文件的配置会对效果产生影响
                //如果其中开头的layout_width,layout_height选择了match_parent,会导致绘制间距非常大,难以修正
                super(view);

                //元素View相关
                deviceName = view.findViewById(R.id.DeviceNameRecyclerViewBluetooth);
                deviceIcon = view.findViewById(R.id.DeviceIconRecyclerViewBluetooth);
                deviceDistance = view.findViewById(R.id.DeviceDistanceRecyclerViewBluetooth);

                //动画效果
                if(deviceDistance != null) {
                    fadeInDeviceDistance = ObjectAnimator.ofFloat(deviceDistance,"alpha",0F,1F);
                    fadeInDeviceDistance.setDuration(1000);
                    fadeInDeviceDistance.setInterpolator(new DecelerateInterpolator());
                }


                //在行单元触发对应结果的控制面板,不松手继续滑动选择指定操作
                view.setOnTouchListener(new View.OnTouchListener() {
                    @SuppressLint({"ClickableViewAccessibility", "MissingPermission"})
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        switch (event.getAction()){
                            //在开始按下时
                            case MotionEvent.ACTION_DOWN:{
                                //强制停止正在运行的动画,因为恢复动画是耗时的,如果未完成,用户又开始操作,触发"按下",所以我们必须停止它
                                //同时现在保存上次的缓存还未覆盖,可以立即恢复坐标即跳过动画完成恢复
                                //如果是第一次运行,缓存是没有数据的,这样我们不应该恢复
                                if(isRunningRecover == null){
                                    isRunningRecover = new AtomicReference<>(Boolean.FALSE);
                                }
                                else {
                                    if(isRunningRecover.get()){//需要强制停止
                                        isRunningRecover.set(false);//强制停止正在运行的恢复动画
                                    }
                                }

                                lottieAnimationBluetoothScanning.pauseAnimation();//保证停止动画
                                isAllowNotifyChanged.set(false);//禁止列表对已存在条目的数据刷新

                                //进行坐标的快速复位,保证坐标的正常复位,安全地继续操作
                                if(deviceNameParams != null){
                                    deviceNameParams.leftMargin = deviceNameBufX;
                                    deviceNameParams.topMargin = deviceNameBufY;
                                    deviceName.setLayoutParams(deviceNameParams);
                                }

                                //切换到中间进度(实际为25%,因为50%-100%是0%-50%的倒放)
                                lottieAnimationBluetoothScanning.setProgress(0.25F);
                                //重置变量
                                user_want = WANT_NONE;
                                //缓存设备名称
                                BluetoothDeviceModel targetModel = null;
                                targetModel = modelList.get(positionBuf);//获取要读取操作列表中的的deviceModel实例
                                if(targetModel != null) {
                                    BluetoothDevice device = null;
                                    device = targetModel.getDevice();
                                    if(device != null){
                                        if(device.getName() != null)
                                            nameBuf = device.getName();
                                        else
                                            nameBuf = getString(R.string.unknowndevice_chinese);
                                    }
                                }
                                //获取屏幕的最大横坐标(单位像素px)
                                DisplayMetrics displayMetrics = new DisplayMetrics();
                                WindowManager windowManager = (WindowManager)requireActivity().getSystemService(WINDOW_SERVICE);
                                windowManager.getDefaultDisplay().getMetrics(displayMetrics);
                                displayX = displayMetrics.widthPixels;
                                //记录开始按下的坐标
                                startX = event.getX();
                                //记录开始的deviceName横纵坐标
                                deviceNameParams = (RelativeLayout.LayoutParams) deviceName.getLayoutParams();
                                deviceNameBufX = deviceNameParams.leftMargin;
                                deviceNameBufY = deviceNameParams.topMargin;
                                //显示主文本提示
                                MainTextViewBLE.setText(getString(R.string.horizontalslidedevicetoselectoperaction_chinese));
                                MainTextViewBLE.setAlpha(1F);
                                break;
                            }

                            //以下对View元素的设置是一时的,一但发生RecyclerView的视图重构会马上重新绘制,这些状态就丢失了
                            // 当然在我们恢复对列表已存在条目数据更新的允许之后
                            //在线设备被扫描到就会更新视图,不用等到视图重构,所以会马上恢复,但是还是可以停留几秒,因为扫描是耗时的
                            //如果这个设备一直扫描不到即离线了,恢复概率会大大降低

                            //在滑动时
                            case MotionEvent.ACTION_MOVE:{
                                currentX = event.getX();
                                if(displayX != 0){
                                    progress = 0.25F + 0.25F * (currentX - startX) / (displayX/2);
                                    if(progress >= 0F && progress <= 0.5F){

                                        //设置动画进度在25%根据滑动距离和方向偏移±25%
                                        //因为50%-100%的片段其实是0%-50%的倒放,使用一半即可
                                        lottieAnimationBluetoothScanning.setProgress(progress);

                                        //显示实时的触摸横坐标偏移
                                        String dxShow = (int)(currentX - startX) + "PX";
                                        deviceDistance.setText(dxShow);


                                        //防止用户手指遮挡deviceName,在滑动时偏移其横纵坐标
                                        deviceNameParams.leftMargin = deviceNameBufX + (int)(currentX - startX);//偏移触摸横坐标的偏移值
                                        deviceNameParams.alignWithParent = false;
                                        if(!(progress > 0.20F && progress < 0.30F)){
                                            deviceNameParams.topMargin = 0;
                                        }
                                        deviceName.setLayoutParams(deviceNameParams);


                                        //选择操作,期间蓝牙设备图标会变成操作的标识图标
                                        //   |             |             |
                                        //   0%           25%           50%
                                        // |0-15||15-20||20-30||30-35||35-50|
                                        //加入设备 将其置顶 无效/取消 检查信息 开始控制
                                        //-------------------------------------
                                        if(progress <= 0.15F){
                                            user_want = WANT_ADD_TO_DEVICE;
                                            deviceIcon.setImageResource(R.drawable.addtodevice);//加入设备
                                            deviceName.setText(R.string.addtodevice_chinese);
                                        }
                                        if(progress > 0.15F && progress <= 0.20F){
                                            user_want = WANT_STICK_TO_TOP;
                                            deviceIcon.setImageResource(R.drawable.sticktotop);//将其置顶
                                            deviceName.setText(R.string.sticktotop_chinese);
                                        }
                                        if(progress > 0.20F && progress < 0.30F){
                                            //如果用户之前移动到了有效选项又回来,显示取消,否则不更改内容
                                            if(user_want != WANT_NONE){
                                                user_want = WANT_NONE;
                                                deviceIcon.setImageResource(R.drawable.undo);//取消
                                                deviceName.setText(R.string.cancel_chinese);
                                            }
                                        }
                                        if(progress >= 0.30F && progress < 0.35F){
                                            user_want = WANT_CHECK_INFORMATION;
                                            deviceIcon.setImageResource(R.drawable.checkinformation);//检查信息
                                            deviceName.setText(R.string.checkinformation_chinese);
                                        }
                                        if(progress >= 0.35F){
                                            user_want = WANT_START_CONTROL;
                                            deviceIcon.setImageResource(R.drawable.startcontrol);//开始控制
                                            deviceName.setText(R.string.startcontrol_chinese);
                                        }
                                    }
                                }
                                break;
                            }

                            //在松开时
                            case MotionEvent.ACTION_UP:
                            {
                                //使用isRunningRecover.set控制动画是否要继续
                                isRunningRecover.set(true);//在下次触发"在开始按下时",会重置为false
                                RecoverAnimation();
                                OperationRun(user_want,modelList,positionBuf,requireActivity());
                                break;
                            }

                            //在取消时,取消可能是由于其他事件切入,如来电和用户应用切换,系统警告等等
                            case MotionEvent.ACTION_CANCEL:{
                                //如果用户之前移动到了有效选项,显示取消,否则不显示内容
                                if(user_want != WANT_NONE){
                                    user_want = WANT_NONE;
                                    deviceIcon.setImageResource(R.drawable.undo);
                                    deviceName.setText(R.string.cancel_chinese);
                                }

                                //使用isRunningRecover.set控制恢复动画是否要继续
                                isRunningRecover.set(true);//在下次触发"在开始按下时",会重置为false
                                RecoverAnimation();

                                break;
                            }
                        }

                        return true;
                    }
                });
            }


            /**
             * 恢复deviceName在滑动之前的坐标,以及扫描状态,伴随更新动画,同步更新之前的偏移显示等
             * (使用isRunningRecover.set控制动画是否要继续)
             */
            private void RecoverAnimation(){
                if(deviceNameParams == null || nameBuf == null || deviceName == null || deviceDistance == null){
                    return;
                }

                //考虑用户没有滑动超出20%-30%无效区域的情况,但是确实移动了deviceName,所以也应该进行横坐标恢复,但是不恢复纵坐标,因为现在纵坐标还没有变化
                ValueAnimator animatorX = ValueAnimator.ofInt((int)(currentX-startX),0);
                animatorX.addUpdateListener(animation -> {
                    if(!isRunningRecover.get()){
                        return;//如果外部需要退出动画,退出
                    }

                    deviceNameParams.leftMargin = deviceNameBufX + (int)animation.getAnimatedValue();
                    deviceName.setLayoutParams(deviceNameParams);
                    //同步更新之前的偏移显示(之前的偏移显示:String dxShow = (int)(currentX - startX) + "PX")
                    String dxShow = (int)animation.getAnimatedValue() + "PX";
                    deviceDistance.setText(dxShow);
                });
                animatorX.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationEnd(@NonNull Animator animation) {
//                      考虑用户没有滑动超出20%-30%无效区域的情况,但是确实移动了deviceName,所以也应该进行横坐标恢复,但是不恢复纵坐标,因为现在纵坐标还没有变化
                        if(!deviceName.getText().equals(nameBuf)){
                            deviceName.setText(nameBuf);//恢复deviceName之前显示的设备名
                            ValueAnimator animatorY = ValueAnimator.ofInt(0,deviceNameBufY);
                            animatorY.addUpdateListener(animaton->{
                                if(!isRunningRecover.get()){
                                    return;//如果外部需要退出动画,退出
                                }

                                deviceNameParams.topMargin = (int)animatorY.getAnimatedValue();
                                deviceName.setLayoutParams(deviceNameParams);
                            });
                            animatorY.setDuration(100);
                            animatorY.setInterpolator(new AccelerateInterpolator());
                            animatorY.addListener(new Animator.AnimatorListener() {
                                @Override
                                public void onAnimationEnd(@NonNull Animator animation) {
                                    if(isScanningBluetooth) {
                                        lottieAnimationBluetoothScanning.playAnimation();
                                    }
                                    isAllowNotifyChanged.set(true);//允许列表对已存在条目的数据刷新
                                }
                                @Override
                                public void onAnimationStart(@NonNull Animator animation) {}
                                @Override
                                public void onAnimationCancel(@NonNull Animator animation) {}
                                @Override
                                public void onAnimationRepeat(@NonNull Animator animation) {}
                            });
                            animatorY.start();
                        }
                        else {
                            if(isScanningBluetooth) {
                                lottieAnimationBluetoothScanning.playAnimation();
                            }
                            isAllowNotifyChanged.set(true);//允许列表对已存在条目的数据刷新
                        }
                    }
                    @Override
                    public void onAnimationStart(@NonNull Animator animation) {}
                    @Override
                    public void onAnimationCancel(@NonNull Animator animation) {}
                    @Override
                    public void onAnimationRepeat(@NonNull Animator animation) {}
                });
                animatorX.setDuration(200);
                animatorX.setInterpolator(new DecelerateInterpolator());
                animatorX.start();
            }
        }


        /**
         * @param want 用户操作枚举 WANT_X
         * @param list 蓝牙设备模型列表,可以是实体BluetoothDeviceRecyclerViewAdapter中的,也可以是外部自己拼接的
         * @param position 选择操作的模型单元在list的位置
         * @param context 上下文,可以使用Activity作为上下文
         */
        public void OperationRun(byte want,List<BluetoothDeviceModel> list,int position,Context context){
            if(want == WANT_NONE){
                return;
            }

            //添加设备数据到数据库并继续处理
            new Thread(new Runnable() {
                @Override
                public void run() {
                    BluetoothDeviceModel targetModel = null;
                    targetModel = list.get(position);//获取要读取操作列表中的的deviceModel实例
                    if(targetModel == null){
                        return;
                    }

                    try {
                        String confirmSha256 = AddToBleDeviceMainDatabase(targetModel, context);//插入或更新到数据库
                        if(confirmSha256 != null){
                            //确定数据未发生错误或篡改
                            if(targetModel.getDeviceSha256().equals(confirmSha256)){
                                //执行用户需要的操作
                                switch (want){
                                    case WANT_ADD_TO_DEVICE:

                                        break;
                                    case WANT_STICK_TO_TOP:

                                        break;
                                    case WANT_CHECK_INFORMATION:

                                        break;
                                    case WANT_START_CONTROL:
                                        bleFragmentRunUserWant.StartControl(targetModel.getDevice());
                                        break;
                                }
                            }

                        }
                    }
                    catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();
        }

//        /**添加入"设备"
//         *
//         * @param targetSha256 目标设备在数据库的Sha256校验码
//         * @param context 上下文,可以使用Activity作为上下文
//         */
//        private static void AddToDevice(String targetSha256,Context context){
//
//        }
//
//        /**
//         * 置顶对应显示行单元到列表顶部
//         */
//        private static void StickToTop(String targetSha256,int originPosition,List<BluetoothDeviceModel> list,Context context){
//
//        }
//
//        /**
//         * 检查设备信息
//         */
//        private static void CheckInformation(String targetSha256,Context context){
//
//        }
//





        public List<BluetoothDeviceModel> getModelList() {
            return modelList;
        }

        @Override
        public int getItemCount() {
            if(modelList != null){
                return modelList.size();
            }
            else{
                return 0;
            }
        }
    }

    //用户操作
    
    //用户操作枚举
    final static byte WANT_ADD_TO_DEVICE = -2;//添加入"设备"
    final static byte WANT_STICK_TO_TOP = -1;//置顶对应显示行单元到列表顶部
    final static byte WANT_NONE = 0;//无操作
    final static byte WANT_CHECK_INFORMATION = 1;//检查设备信息
    final static byte WANT_START_CONTROL = 2;//开始设备控制

    //用户操作接口
    public interface BleFragmentRunUserWant {
        /**开始设备控制
         *
         * @param device BluetoothDevice实例
         * @param sha256 设备的SHA-256校验码
         */
        void StartControl(BluetoothDevice device,String sha256);
    }
    
    

    /**
     * (内部回归主线程处理)清除蓝牙设备列表并请求列表刷新
     */
    public void  DevicesListClearBluetooth(){
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void run() {
                if (bluetoothDeviceRecyclerViewAdapter != null) {
                    bluetoothDeviceRecyclerViewAdapter.getModelList().clear();
                    bluetoothDeviceRecyclerViewAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    /**
     * (用于开发测试)根据时间生成伪设备SHA-256
     */
    public static String FetchFakeSha256ByTime(){
        long timestamp = System.currentTimeMillis();
        //填充到byte数组,低位到高位
        byte[] rawCode = new byte[8];
        for(int i=0;i<8;i++){
            rawCode[i] = (byte)(timestamp >> 8 * i);
        }
        return BluetoothAD.RawCodeFetchSha256String(rawCode);
    }


    /**
     * (用于开发测试)测试蓝牙设备看板,将虚拟一个蓝牙设备存储到列表,测试功能,其中BluetoothDevice设置为空
     * @param deviceDistance 设备距离
     * @param iconID 设备外观图标ID
     */
    public void TestAddBluetoothDeviceRecyclerView(int deviceDistance,int iconID){
        //根据时间生成伪设备SHA-256
        String sha256 = FetchFakeSha256ByTime();
        if(sha256 == null){
            return;
        }
        //创建设备模型
        BluetoothDeviceModel deviceModel =
                new BluetoothDeviceModel(null,deviceDistance,iconID,sha256);//生成这个蓝牙设备的基本信息模型
        //缓存到RecyclerView适配器内部列表
        int index = bluetoothDeviceRecyclerViewAdapter.getItemCount();
        bluetoothDeviceRecyclerViewAdapter.getModelList().add(index,deviceModel);//添加信息到公共的表,RecyclerView将利用表中信息显示
        bluetoothDeviceRecyclerViewAdapter.notifyItemInserted(index);//提示信息更新,需要RecyclerView刷新显示
    }



////UI-主界面-BLE-MainTextViewBLE
    private TextView MainTextViewBLE = null;
    private ObjectAnimator fadeOutMainTextViewBLE = null;
    private ObjectAnimator fadeInMainTextViewBLE = null;
    private void InitMainTextViewBLE(View view){
        //获取实例
        MainTextViewBLE = view.findViewById(R.id.MainTextViewBLE);
        //创建消隐动画效果
        fadeOutMainTextViewBLE = ObjectAnimator.ofFloat(MainTextViewBLE,"alpha",1F,0F);
        fadeOutMainTextViewBLE.setDuration(5000);
        //创建淡入动画效果
        fadeInMainTextViewBLE = ObjectAnimator.ofFloat(MainTextViewBLE,"alpha",0F,1F);
        fadeInMainTextViewBLE.setDuration(2000);
    }


////UI-蓝牙扫描动画
    private LottieAnimationView lottieAnimationBluetoothScanning = null;
    private void InitAnimationBluetoothScanning(View view){
        lottieAnimationBluetoothScanning = view.findViewById(R.id.LottieAnimationBluetoothScanningBLE);
    }


///UI-蓝牙扫描操作控制(基于动画实例)
    private GestureDetector gestureBluetoothScanningAnimation = null;
    private class ListenerGestureBluetoothScanningAnimation extends GestureDetector.SimpleOnGestureListener{
        @Override
        public boolean onFling(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
            super.onFling(e1, e2, velocityX, velocityY);

            fadeInMainTextViewBLE.start();

            //如果以水平滑动为主
            if(Math.abs(velocityX) > Math.abs(velocityY)){
                if(!isScanningBluetooth){
                    BluetoothScanStart();//扫描启动
                }
                else{
                    //如果已经启动,调节播放速度
                    float speedAnimation = lottieAnimationBluetoothScanning.getSpeed();
                    if(velocityX < 0 && speedAnimation + 1F <= 7){//向左滑动,正向时加速/反向时减速
                        speedAnimation+= 1F;
                        lottieAnimationBluetoothScanning.setSpeed(speedAnimation);
                    }
                    if(velocityX > 0 && speedAnimation - 1F > 0){//向右滑动,正向时减速/反向时加速
                        speedAnimation-= 1F;
                        lottieAnimationBluetoothScanning.setSpeed(speedAnimation);
                    }
                    else if(velocityX > 0 && speedAnimation - 0.1F > 0){//向右滑动,正向时减速/反向时加速
                        speedAnimation-= 0.1F;
                        lottieAnimationBluetoothScanning.setSpeed(speedAnimation);
                    }
                }
            }
            else {//如果以垂直滑动为主
                if(isScanningBluetooth){
                    BluetoothScanStop();
                }
            }
            return true;
        }

        @Override
        public boolean onDown(@NonNull MotionEvent e) {//如果为点击
            super.onDown(e);



            if(isScanningBluetooth){
                //随机到任意播放位置
                Random random = new Random();
                lottieAnimationBluetoothScanning.setProgress(random.nextFloat());

                MainTextViewBLE.setText(getString(R.string.verticalslidevavetostopsacn_chinese_chinese));
            }
            else {
                MainTextViewBLE.setText(getString(R.string.horizontalslidevavetostartsacn_chinese));
            }

            fadeInMainTextViewBLE.start();
            return true;
        }
    }
    @SuppressLint("ClickableViewAccessibility")
    private void InitGestureBluetoothScanningAnimation(View view){
        gestureBluetoothScanningAnimation = new GestureDetector(requireActivity(),new ListenerGestureBluetoothScanningAnimation());
        LottieAnimationView detectView = view.findViewById(R.id.LottieAnimationBluetoothScanningBLE);

        detectView.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return gestureBluetoothScanningAnimation.onTouchEvent(motionEvent);
            }
        });
    }

    /**
     * 初始化BleFragment的UI界面
     * @param view onCreateView返回的根视图实例
     */
    private void InitBleFragmentUI(View view){
        InitAnimationBluetoothScanning(view);
        InitMainTextViewBLE(view);

        InitGestureBluetoothScanningAnimation(view);

    }







}