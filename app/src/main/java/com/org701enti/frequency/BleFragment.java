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
import android.widget.ImageView;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.org701enti.bluetoothfocuser.BluetoothAD;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BleFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BleFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public BleFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment BleFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static BleFragment newInstance(String param1, String param2) {
        BleFragment fragment = new BleFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.ble_fragment, container, false);
    }






//    //切换到BLE界面
//            if(item.getItemId() == R.id.NavigationBLE){
//        if(bluetoothAdapter == null){//已经初始化但还为空
//            //用户设备不支持蓝牙,弹出提示
//            AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            builder.setTitle(R.string.error_chinese);
//            builder.setMessage(R.string.userdevicehardwareunsupport);
//            builder.setPositiveButton(getString(R.string.cancel_chinese), new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialogInterface, int i) {
//
//                }
//            });
//
//
//
//        }
//
//        if(isScanningBluetooth){
//            lottieAnimationBluetoothScanning.playAnimation();
//            MainTextViewBLE.setText(getString(R.string.verticalslidevavetostopsacn_chinese_chinese));
//        }
//        else{
//            lottieAnimationBluetoothScanning.pauseAnimation();
//            MainTextViewBLE.setText(getString(R.string.horizontalslidevavetostartsacn_chinese));
//        }
//
//        recyclerViewBluetooth.setVisibility(View.VISIBLE);
//        lottieAnimationBluetoothScanning.setVisibility(View.VISIBLE);
//        MainTextViewBLE.setVisibility(View.VISIBLE);
//
//        fadeInMainTextViewBLE.start();
//
//    }
//            else {
//        recyclerViewBluetooth.setVisibility(View.GONE);
//        lottieAnimationBluetoothScanning.setVisibility(View.GONE);
//        MainTextViewBLE.setVisibility(View.GONE);
//        lottieAnimationBluetoothScanning.pauseAnimation();
//    }







////蓝牙内部处理业务
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothLeScanner bluetoothLeScanner = null;
    final Handler[] HandlerBluetooth = {null};//缓存蓝牙处理线程handler
    private static List<BluetoothDeviceModel> bluetoothDevicesList = new ArrayList<>();
    private BluetoothDeviceRecyclerViewAdapter bluetoothDeviceRecyclerViewAdapter = null;
    private RecyclerView recyclerViewBluetooth = null;
    private boolean isScanningBluetooth = false;

    private void InitBLE() {
//        //创建蓝牙相关处理线程(含Looper)
//        Thread ThreadBluetooth = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                Looper.prepare();
//                HandlerBluetooth[0] = new Handler(Looper.myLooper());
//                Looper.loop();
//            }
//        });
//        ThreadBluetooth.start();//启动该线程

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
            if (!bluetoothAdapter.isEnabled()) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        bluetoothAdapter.enable();
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
        if(bluetoothAdapter != null && bluetoothLeScanner != null){
            bluetoothLeScanner.startScan(filters,settings,bluetoothScanCallback);
            lottieAnimationBluetoothScanning.playAnimation();
            MainTextViewBLE.setText(getString(R.string.verticalslidevavetostopsacn_chinese_chinese));
            isScanningBluetooth = true;
            fadeOutMainTextViewBLE.start();
        }
    }

    /**
     * 启动蓝牙设备扫描,无配置
     */
    @SuppressLint("MissingPermission")
    public void BluetoothScanStart(){
        if(bluetoothAdapter != null && bluetoothLeScanner != null){
            bluetoothLeScanner.startScan(bluetoothScanCallback);
            lottieAnimationBluetoothScanning.playAnimation();
            MainTextViewBLE.setText(getString(R.string.verticalslidevavetostopsacn_chinese_chinese));
            isScanningBluetooth = true;
            fadeOutMainTextViewBLE.start();
        }
    }

    /**
     * 停止蓝牙设备扫描
     */
    @SuppressLint("MissingPermission")
    public void BluetoothScanStop(){
        if(bluetoothAdapter != null && bluetoothLeScanner != null){
            bluetoothLeScanner.stopScan(bluetoothScanCallback);
            lottieAnimationBluetoothScanning.pauseAnimation();
            MainTextViewBLE.setText(getString(R.string.horizontalslidevavetostartsacn_chinese));
            isScanningBluetooth = false;
            fadeOutMainTextViewBLE.start();
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
    private void InitRecyclerViewBluetooth(){
        bluetoothDeviceRecyclerViewAdapter = new BluetoothDeviceRecyclerViewAdapter(bluetoothDevicesList);
        recyclerViewBluetooth = requireView().findViewById(R.id.RecyclerViewBluetooth);
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
                try {
                    InputStream iconInput = requireActivity().getAssets().open("bluetoothdeviceicon/btac" + iconID + ".png");
                    holder.deviceIcon.setImageBitmap(BitmapFactory.decodeStream(iconInput));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                //设备名
                String name = targetModel.getDevice().getName();
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
            public TextView deviceName;
            public ImageView deviceIcon;
            public TextView deviceDistance;

            //在构造方法将各种View引用缓存到ViewHolder池
            public ViewHolder(View viewHandle) {
                //super调用父类RecyclerView.ViewHolder构造方法,并传递了参数viewHandle
                //即自定义布局R.layout.recyclerviewbluetooth的实例,因此自定义布局文件的配置会对效果产生影响
                //如果其中开头的layout_width,layout_height选择了match_parent,会导致绘制间距非常大,难以修正
                super(viewHandle);

                deviceName = viewHandle.findViewById(R.id.DeviceNameRecyclerViewBluetooth);
                deviceIcon = viewHandle.findViewById(R.id.DeviceIconRecyclerViewBluetooth);
                deviceDistance = viewHandle.findViewById(R.id.DeviceDistanceRecyclerViewBluetooth);

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

////UI-主界面-BLE-MainTextViewBLE
    private TextView MainTextViewBLE = null;
    private ObjectAnimator fadeOutMainTextViewBLE = null;
    private ObjectAnimator fadeInMainTextViewBLE = null;
    private void InitMainTextViewBLE(){
        //获取实例
        MainTextViewBLE = requireActivity().findViewById(R.id.MainTextViewBLE);
        MainTextViewBLE.setVisibility(View.GONE);
        //创建消隐动画效果
        fadeOutMainTextViewBLE = ObjectAnimator.ofFloat(MainTextViewBLE,"alpha",1F,0F);
        fadeOutMainTextViewBLE.setDuration(5000);
        //创建淡入动画效果
        fadeInMainTextViewBLE = ObjectAnimator.ofFloat(MainTextViewBLE,"alpha",0F,1F);
        fadeInMainTextViewBLE.setDuration(2000);
    }


////UI-蓝牙扫描动画
    private LottieAnimationView lottieAnimationBluetoothScanning = null;
    private void InitAnimationBluetoothScanning(){
        lottieAnimationBluetoothScanning = requireActivity().findViewById(R.id.LottieAnimationBluetoothScanning);
        lottieAnimationBluetoothScanning.setVisibility(View.GONE);
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
    private void InitGestureBluetoothScanningAnimation(){
        gestureBluetoothScanningAnimation = new GestureDetector(requireActivity(),new ListenerGestureBluetoothScanningAnimation());
        LottieAnimationView detectView = requireActivity().findViewById(R.id.LottieAnimationBluetoothScanning);

        detectView.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return gestureBluetoothScanningAnimation.onTouchEvent(motionEvent);
            }
        });
    }


    private void InitBleFragmentUI(){
        InitRecyclerViewBluetooth();
        InitAnimationBluetoothScanning();
        InitMainTextViewBLE();

        InitGestureBluetoothScanningAnimation();
    }







}