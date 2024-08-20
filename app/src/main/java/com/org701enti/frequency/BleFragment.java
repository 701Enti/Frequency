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

import android.animation.ObjectAnimator;
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

import com.airbnb.lottie.LottieAnimationView;
import com.org701enti.bluetoothfocuser.BluetoothAD;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BleFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BleFragment extends Fragment {

    public BleFragment() {
        // Required empty public constructor
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
        View view = inflater.inflate(R.layout.ble_fragment, container, false);

        //创建其他布局
        CreateRecyclerViewBluetooth(view);

        //加载布局文件
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
    private static List<BluetoothDeviceModel> bluetoothDevicesList = new ArrayList<>();
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
        private BluetoothDevice device;

        //设备的外观图标ID,其实就是外观值的bit6到bit15
        //详见(2.6.2)https://www.bluetooth.com/wp-content/uploads/Files/Specification/HTML/Assigned_Numbers/out/en/Assigned_Numbers.pdf
        private int iconID;

        private int deviceDistance;//与设备的距离(单位:米)

        public BluetoothDeviceModel(BluetoothDevice device,int deviceDistance) {
            this.device = device;
            this.deviceDistance = deviceDistance;
            this.iconID = 0;
        }

        public BluetoothDeviceModel(BluetoothDevice device,int deviceDistance,int iconID){
            this.device = device;
            this.deviceDistance = deviceDistance;
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
                    bluetoothDeviceRecyclerViewAdapter.notifyItemChanged(i);//提示信息更新,需要RecyclerView刷新显示
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
            BluetoothDeviceModel deviceModel = new BluetoothDeviceModel(result.getDevice(),deviceDistance,iconID);//生成这个蓝牙设备的基本信息模型

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
        recyclerViewBluetooth = view.findViewById(R.id.RecyclerViewBluetooth);
        recyclerViewBluetooth.setAdapter(bluetoothDeviceRecyclerViewAdapter);
        recyclerViewBluetooth.setLayoutManager(new LinearLayoutManager(requireActivity()));
        recyclerViewBluetooth.addItemDecoration(new ItemDecorationRecyclerViewBluetooth(90));
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
            View viewHandle = inflaterBuf.inflate(R.layout.recyclerviewbluetooth, parent, false);
            return new ViewHolder(viewHandle);
        }

        /**
         * ViewHolder池,持有相关View引用,防止findViewById更多调用来优化性能
         */
        public class ViewHolder extends RecyclerView.ViewHolder {
            //对象缓存
            public TextView deviceName = null;
            public ImageView deviceIcon = null;
            public TextView deviceDistance = null;

            public ObjectAnimator fadeInDeviceDistance = null;

            //在构造方法将各种View引用缓存到ViewHolder池
            public ViewHolder(View viewHandle) {
                //super调用父类RecyclerView.ViewHolder构造方法,并传递了参数viewHandle
                //即自定义布局R.layout.recyclerviewbluetooth的实例,因此自定义布局文件的配置会对效果产生影响
                //如果其中开头的layout_width,layout_height选择了match_parent,会导致绘制间距非常大,难以修正
                super(viewHandle);

                //元素View相关
                deviceName = viewHandle.findViewById(R.id.DeviceNameRecyclerViewBluetooth);
                deviceIcon = viewHandle.findViewById(R.id.DeviceIconRecyclerViewBluetooth);
                deviceDistance = viewHandle.findViewById(R.id.DeviceDistanceRecyclerViewBluetooth);

                //动画效果
                if(deviceDistance != null) {
                    fadeInDeviceDistance = ObjectAnimator.ofFloat(deviceDistance,"alpha",0F,1F);
                    fadeInDeviceDistance.setDuration(1000);
                    fadeInDeviceDistance.setInterpolator(new DecelerateInterpolator());
                }
            }
        }

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
     * (用于开发测试)测试蓝牙设备看板,将虚拟一个蓝牙设备存储到列表,测试功能,其中BluetoothDevice设置为空
     * @param deviceDistance 设备距离
     * @param iconID 设备外观图标ID
     */
    public void TestAddBluetoothDeviceRecyclerView(int deviceDistance,int iconID){
        //创建设备模型
        BluetoothDeviceModel deviceModel = new BluetoothDeviceModel(null,deviceDistance,iconID);//生成这个蓝牙设备的基本信息模型
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
        lottieAnimationBluetoothScanning = view.findViewById(R.id.LottieAnimationBluetoothScanning);
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
        public boolean onDown(@NonNull MotionEvent e) {
            super.onDown(e);

            fadeInMainTextViewBLE.start();

            if(isScanningBluetooth){
                //随机到任意播放位置
                Random random = new Random();
                lottieAnimationBluetoothScanning.setProgress(random.nextFloat());
            }
            return true;
        }
    }
    @SuppressLint("ClickableViewAccessibility")
    private void InitGestureBluetoothScanningAnimation(View view){
        gestureBluetoothScanningAnimation = new GestureDetector(requireActivity(),new ListenerGestureBluetoothScanningAnimation());
        LottieAnimationView detectView = view.findViewById(R.id.LottieAnimationBluetoothScanning);

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