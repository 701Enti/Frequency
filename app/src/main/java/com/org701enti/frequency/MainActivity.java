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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final int AndroidVersion = Build.VERSION.SDK_INT;
    //    权限申请码定义
    private static final int REQUEST_COARSE_LOCATION = 100;
    private static final int REQUEST_FINE_LOCATION = 101;
    private static final int REQUEST_BLUETOOTH_SCAN = 102;
    private static final int REQUEST_BLUETOOTH_ADVERTISE = 103;
    private static final int REQUEST_BLUETOOTH_CONNECT = 104;

    //    权限申请标志
    private static boolean PermissionRequestingFlag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        InitBLE();
        InitUI();


    }


    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        如果用户已经拒绝这个权限请求
        if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            //显示一个提示框,希望用户改变主意
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.hint_chinese);
            builder.setMessage(R.string.requestpermission_chinese);
            builder.setPositiveButton(R.string.give_chinese, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //给用户再次的选择,用户点击"授予",会弹出系统的应用信息,里面有权限管理,但是用户这时可能又矛盾地没有允许对应权限
                    //如果用户一直这样做,最终会一直在当前这个if里循环,直到正式同意权限或点击这里创建的提示框的"拒绝"
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getString(R.string.packagename), null);
                    intent.setData(uri);
                    startActivity(intent);
                    PermissionRequestingFlag = false;//重置请求中标识
                }
            });
            builder.setNegativeButton(R.string.reject_chinese, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    PermissionRequestingFlag = false;//重置请求中标识
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {//如果同意对应请求请求
            PermissionRequestingFlag = false;//重置请求中标识
            switch (requestCode) {
                case REQUEST_COARSE_LOCATION:

                    break;
                case REQUEST_FINE_LOCATION:

                    break;
                case REQUEST_BLUETOOTH_SCAN:

                    break;
                case REQUEST_BLUETOOTH_ADVERTISE:

                    break;
                case REQUEST_BLUETOOTH_CONNECT:

                    break;
                default:
                    break;

            }
        }
    }


    /**
     * (含阻塞,必须使用非主线程调用)权限检查,如果权限未授予或拒绝,会进行对应权限申请工作
     * @param requestCode 权限申请码,参考MainActivity开头的权限申请码定义
     */
    private void PermissionApplyCheck(int requestCode) throws InterruptedException {
        //禁止在主线程执行,因为本方法内含阻塞,会阻塞调用线程,应该使用其他非服务线程调用
        if (Thread.currentThread().getName().equals("main")) {
            throw new InterruptedException(getString(R.string.permissionapplycheckthreaderr));
        }
        //权限申请时使用主线程请求,调用本方法的线程处理请求时的阻塞,防止连续请求
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                switch (requestCode) {
                    case REQUEST_COARSE_LOCATION:
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            PermissionRequestingFlag = true;
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, requestCode);
                        }
                        break;
                    case REQUEST_FINE_LOCATION:
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            PermissionRequestingFlag = true;
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, requestCode);
                        }
                        break;
                    case REQUEST_BLUETOOTH_SCAN:
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                            PermissionRequestingFlag = true;
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, requestCode);
                        }
                        break;
                    case REQUEST_BLUETOOTH_ADVERTISE:
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                            PermissionRequestingFlag = true;
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_ADVERTISE}, requestCode);
                        }
                        break;
                    case REQUEST_BLUETOOTH_CONNECT:
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            PermissionRequestingFlag = true;
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, requestCode);
                        }
                        break;
                    default:
                        break;
                }
            }
        });
        while (PermissionRequestingFlag) {
            Thread.sleep(50);
        }
    }


    //////////////////////////////////////蓝牙业务

    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothLeScanner bluetoothLeScanner = null;
    final Handler[] HandlerBluetooth = {null};//缓存蓝牙处理线程handler
    private static List<BluetoothDeviceModel> bluetoothDevicesList = new ArrayList<>();
    private BluetoothDeviceRecyclerViewAdapter bluetoothDeviceRecyclerViewAdapter = null;
    private RecyclerView recyclerViewBluetooth = null;


    //RecyclerView的适配器类,用于RecyclerView展示扫描到的蓝牙设备
    public class BluetoothDeviceRecyclerViewAdapter extends RecyclerView.Adapter<BluetoothDeviceRecyclerViewAdapter.ViewHolder>
    {
        private  List<BluetoothDeviceModel> devicesList;

        public BluetoothDeviceRecyclerViewAdapter(List<BluetoothDeviceModel> devicesList) {
            this.devicesList = devicesList;
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BluetoothDeviceModel deviceModel = null;
            deviceModel = devicesList.get(position);
            if(deviceModel != null){

                //设备图标
                int iconID = deviceModel.getIconID();
                try {
                    InputStream iconInput = getAssets().open("bluetoothdeviceicon/btac" + iconID + ".png");
                    holder.DeviceIcon.setImageBitmap(BitmapFactory.decodeStream(iconInput));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }


                //设备名
                String name = deviceModel.getDevice().getName();
                if(name == null){
                    holder.DeviceName.setText(getString(R.string.unknowndevice_chinese));
                }
                else {
                    //确定显示的字符尺寸
                    int len = name.length();
                    if(len <= 8){
                        holder.DeviceName.setTextSize(TypedValue.COMPLEX_UNIT_SP,32f);
                    }
                    else if (len <= 8 * 3) {
                        holder.DeviceName.setTextSize(TypedValue.COMPLEX_UNIT_SP,32f - 8f*1);
                    }
                    else if (len <= 8 * 6) {
                        holder.DeviceName.setTextSize(TypedValue.COMPLEX_UNIT_SP,32f - 8f*2);
                    }
                    else{
                        holder.DeviceName.setTextSize(TypedValue.COMPLEX_UNIT_SP,5f);
                    }

                    holder.DeviceName.setText(name);
                }



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
            public TextView DeviceName;
            public ImageView DeviceIcon;

            //在构造方法将各种View引用缓存到ViewHolder池
            public ViewHolder(View viewHandle) {
                //super调用父类RecyclerView.ViewHolder构造方法,并传递了参数viewHandle
                //即自定义布局R.layout.recyclerviewbluetooth的实例,因此自定义布局文件的配置会对效果产生影响
                //如果其中开头的layout_width,layout_height选择了match_parent,会导致绘制间距非常大,难以修正
                super(viewHandle);

                DeviceName = viewHandle.findViewById(R.id.DeviceNameRecyclerViewBluetooth);
                DeviceIcon = viewHandle.findViewById(R.id.DeviceIconRecyclerViewBluetooth);

            }
        }

        public List<BluetoothDeviceModel> getDevicesList() {
            return devicesList;
        }

        @Override
        public int getItemCount() {
            if(devicesList != null){
                return devicesList.size();
            }
            else{
                return 0;
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

    //蓝牙设备列表模型类
    public class BluetoothDeviceModel {
        private BluetoothDevice device;
        private int iconID;

        public BluetoothDeviceModel(BluetoothDevice device) {
            this.device = device;
            this.iconID = 0;
        }

        public BluetoothDeviceModel(BluetoothDevice device,int iconID){
            this.device = device;
            this.iconID = iconID;
        }

        public BluetoothDevice getDevice() {
            return device;
        }
        public int getIconID(){ return iconID;}
    }

//    扫描回调
    ScanCallback bluetoothScanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);



            BluetoothDeviceModel deviceModel = new BluetoothDeviceModel(result.getDevice());//生成这个蓝牙设备的基本信息模型
            if(bluetoothDeviceRecyclerViewAdapter != null){//缓存到RecyclerView适配器内部列表
                int index = bluetoothDeviceRecyclerViewAdapter.getItemCount();
                bluetoothDeviceRecyclerViewAdapter.getDevicesList().add(index,deviceModel);//添加信息到公共的表,RecyclerView将利用表中信息显示
                bluetoothDeviceRecyclerViewAdapter.notifyItemInserted(index);//提示信息更新,需要RecyclerView刷新显示
                Log.i("BluetoothInfoReceiver","[" + index + "]" + deviceModel.getDevice().getName());
            }





        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    /**
     * 启动蓝牙设备扫描,含配置
     */
    @SuppressLint("MissingPermission")
    public void BluetoothScanStart(List<ScanFilter> filters, ScanSettings settings){
        if(bluetoothAdapter != null && bluetoothLeScanner != null){
            bluetoothLeScanner.startScan(filters,settings,bluetoothScanCallback);
        }
    }

    /**
     * 启动蓝牙设备扫描,无配置
     */
    @SuppressLint("MissingPermission")
    public void BluetoothScanStart(){
        if(bluetoothAdapter != null && bluetoothLeScanner != null){
            bluetoothLeScanner.startScan(bluetoothScanCallback);
        }
    }

    /**
     * 停止蓝牙设备扫描
     */
    @SuppressLint("MissingPermission")
    public void BluetoothScanStop(){
        if(bluetoothAdapter != null && bluetoothLeScanner != null){
            bluetoothLeScanner.stopScan(bluetoothScanCallback);
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
                    bluetoothDeviceRecyclerViewAdapter.getDevicesList().clear();
                    bluetoothDeviceRecyclerViewAdapter.notifyDataSetChanged();
                }
            }
        });
    }


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
        BluetoothManager BLEmanager = (BluetoothManager) this.getSystemService(BLUETOOTH_SERVICE);
        if (BLEmanager != null) {
            bluetoothAdapter = BLEmanager.getAdapter();
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        }
    }




    //////底部导航栏业务
    public final Handler[] HandlerMainBottomNavView = {null};//缓存ThreadMainBottomNavView线程handler

    //选择监听
    public class MainBottomNavigationListener implements NavigationBarView.OnItemSelectedListener{
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            //主线程执行
            if(recyclerViewBluetooth != null){
                if(item.getItemId() == R.id.NavigationBLE)
                    recyclerViewBluetooth.setVisibility(View.VISIBLE);
                else
                    recyclerViewBluetooth.setVisibility(View.GONE);
            }

            //独立线程ThreadMainBottomNavView执行
            HandlerMainBottomNavView[0].post(new Runnable() {
                @Override
                public void run() {
                    switch (item.getItemId()){
                        case R.id.NavigationDevice:

                            break;
                        case R.id.NavigationBLE:
//                          权限检查
                            try {
                                PermissionApplyCheck(REQUEST_COARSE_LOCATION);
                                PermissionApplyCheck(REQUEST_FINE_LOCATION);
                                if(AndroidVersion >= Build.VERSION_CODES.S){
                                    PermissionApplyCheck(REQUEST_BLUETOOTH_SCAN);
                                    PermissionApplyCheck(REQUEST_BLUETOOTH_ADVERTISE);
                                    PermissionApplyCheck(REQUEST_BLUETOOTH_CONNECT);
                                }
                            }
                            catch (InterruptedException Inter){
                                Thread.currentThread().interrupt();
                            }

                            BluetoothOpenCheck();//检查蓝牙开启
                            BluetoothScanStart();//扫描启动

                            break;
                        case R.id.NavigationControl:

                            break;
                        case R.id.NavigationWIFI:

                            break;
                        case R.id.NavigationMe:

                            break;
                        default:
                            break;
                    }
                }
            });
            return true;
        }
    }



    /**
     * 初始化底部导航栏
     */
    private void InitMainBottomNavigation(){
        BottomNavigationView mainBottomNavView = findViewById(R.id.MainBottomNavigation);
        //创建一个线程处理底部导航栏业务(含Looper)
        Thread ThreadMainBottomNavView = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                HandlerMainBottomNavView[0] = new Handler(Looper.myLooper());
                Looper.loop();
            }
        });
        ThreadMainBottomNavView.start();//启动该线程

        mainBottomNavView.setOnItemSelectedListener(new MainBottomNavigationListener());
    }


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
        recyclerViewBluetooth = findViewById(R.id.RecyclerViewBluetooth);
        recyclerViewBluetooth.setAdapter(bluetoothDeviceRecyclerViewAdapter);
        recyclerViewBluetooth.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewBluetooth.addItemDecoration(new ItemDecorationRecyclerViewBluetooth(30));
    }

    private void InitUI(){
        InitMainBottomNavigation();
        InitRecyclerViewBluetooth();

    }
}