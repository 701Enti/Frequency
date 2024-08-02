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
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.Context;
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
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

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
     * (必须使用非主线程调用)权限检查,如果权限未授予或拒绝,会进行对应权限申请工作
     * @param requestCode 权限申请码,参考MainActivity开头的权限申请码定义
     */
    private void PermissionApplyCheck(int requestCode) throws InterruptedException {
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
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, requestCode);
                        } else return;
                        break;
                    case REQUEST_FINE_LOCATION:
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, requestCode);
                        } else return;
                        break;
                    case REQUEST_BLUETOOTH_SCAN:
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, requestCode);
                        } else return;
                        break;
                    case REQUEST_BLUETOOTH_ADVERTISE:
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_ADVERTISE}, requestCode);
                        } else return;
                        break;
                    case REQUEST_BLUETOOTH_CONNECT:
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, requestCode);
                        } else return;
                        break;
                    default:
                        break;
                }
            }
        });
        PermissionRequestingFlag = true;
        while (PermissionRequestingFlag) {
            Thread.sleep(50);
        }
    }


    //////////////////////////////////////蓝牙业务

    public BluetoothAdapter bluetoothAdapter = null;
    public BluetoothGatt BLEgatt = null;
    final  Handler[] HandlerBluetooth = {null};//缓存蓝牙处理线程handler
    private List<BluetoothDeviceModel> bluetoothDevicesList;


    //蓝牙设备列表模型类
    public class BluetoothDeviceModel {
        private BluetoothDevice device;

        public BluetoothDeviceModel(BluetoothDevice device) {
            this.device = device;
        }

        public BluetoothDevice getDevice() {
            return device;
        }
    }

    //RecyclerView的适配器类,用于RecyclerView展示扫描到的蓝牙设备
    public class BluetoothDeviceRecyclerViewAdapter extends RecyclerView.Adapter<BluetoothDeviceRecyclerViewAdapter.ViewHolder> {
        private List<BluetoothDeviceModel> devicesList;

        public BluetoothDeviceRecyclerViewAdapter(List<BluetoothDeviceModel> devicesList) {
            this.devicesList = devicesList;
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BluetoothDeviceModel device = devicesList.get(position);
            holder.DeviceName.setText(device.getDevice().getName() == null ? getString(R.string.unknowndevice_chinese) : device.getDevice().getName());

        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflaterBuf = LayoutInflater.from(parent.getContext());
            View viewHandle = inflaterBuf.inflate(R.layout.activity_main,parent,false);
            return new ViewHolder(viewHandle);
        }

        /**
         * ViewHolder池,持有相关View引用,防止findViewById更多调用来优化性能
         */
        public class ViewHolder extends RecyclerView.ViewHolder{
            public TextView DeviceName;

            //在构造方法将各种View引用缓存到ViewHolder池
            public ViewHolder(View viewHandle){
                super(viewHandle);
                DeviceName = viewHandle.findViewById(R.id.DeviceNameRecyclerViewBluetooth);
            }
        }

        @Override
        public int getItemCount() {
            return devicesList.size();
        }
    }


    /**
     * 蓝牙信息的系统广播接收器(一个对象),这里指的系统广播是Android中的一种信息传递机制,并不是蓝牙协议栈的蓝牙广播
     */
    private final BroadcastReceiver BluetoothInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
//            接收时post到蓝牙相关处理线程HandlerBluetooth来异步执行
            HandlerBluetooth[0].post(new Runnable() {
                @Override
                public void run() {
                    String action = intent.getAction();//获取状态
                    if(BluetoothDevice.ACTION_FOUND.equals(action)){
                        //处理接收到的系统广播信息
                        BluetoothDeviceModel device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        bluetoothDevicesList.add(device);

                    }
                    else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){

                    }
                }
            });
        }
    }


    private void InitBLE(){
        //获取BLEadapter实例
        BluetoothManager BLEmanager = (BluetoothManager) this.getSystemService(this.BLUETOOTH_SERVICE);
        if(BLEmanager != null){
            bluetoothAdapter = BLEmanager.getAdapter();
        }


        //创建蓝牙相关处理线程(含Looper)
        Thread HandlerBluetooth = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                HandlerBluetooth[0] = new Handler(Looper.myLooper());
                Looper.loop();
            }
        });
        HandlerBluetooth.start();//启动该线程

        //注册蓝牙数据的系统广播接收器,接收蓝牙扫描到的信息
        //这里的广播指的是系统广播,并非蓝牙协议栈里的蓝牙广播概念,系统广播还可以传输其他信息如网络状态变更和应用自定义广播等
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(BluetoothInfoReceiver,filter);
    }

    /**
     * 检查蓝牙开启状态,如果未开启,打开蓝牙
     */
    @SuppressLint("MissingPermission")
    public void BluetoothOpenCheck(){
        if(bluetoothAdapter != null){
            if(!bluetoothAdapter.isEnabled()){
                bluetoothAdapter.enable();
            }
        }
    }

    /**
     * 启动蓝牙设备扫描
     */
    @SuppressLint("MissingPermission")
    public void BluetoothScanStart(){

        bluetoothAdapter.startDiscovery();
    }


    private void InitUI(){
        BottomNavigationView mainBottomNavView = findViewById(R.id.MainBottomNavigation);
        final Handler[] HandlerMainBottomNavView = {null};//缓存ThreadMainBottomNavView线程handler
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

        //设置导航栏业务(将在ThreadMainBottomNavView线程运行)
        mainBottomNavView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
          @SuppressLint("NonConstantResourceId")
          @Override
          public boolean onNavigationItemSelected(@NonNull MenuItem item) {
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
//                          蓝牙启动检查
                          BluetoothOpenCheck();




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
      });
    }
}