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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import data.DeviceBle.DeviceBleDatabase;
import data.DeviceBle.DeviceBleEntity;

public class MainActivity extends AppCompatActivity {

////权限检查和提取
    public static final int AndroidVersion = Build.VERSION.SDK_INT;
    //    权限申请码定义
    private static final int REQUEST_COARSE_LOCATION = 100;
    private static final int REQUEST_FINE_LOCATION = 101;
    private static final int REQUEST_BLUETOOTH_SCAN = 102;
    private static final int REQUEST_BLUETOOTH_ADVERTISE = 103;
    private static final int REQUEST_BLUETOOTH_CONNECT = 104;

    //    权限申请标志
    private static boolean PermissionRequestingFlag = false;

    /**(含阻塞,请使用非主线程调用)插入或更新到BleDeviceMainDatabase数据库
     *
     * @param targetModel 目标蓝牙设备模型
     * @param context 上下文,可以使用Activity作为上下文
     * @return 操作设备的bleDeviceSha256,发生异常为null
     */
    @SuppressLint("MissingPermission")
    static String AddToBleDeviceMainDatabase(BleFragment.BluetoothDeviceModel targetModel, Context context) throws InterruptedException {
        if(targetModel == null){
            return null;
        }
        if(targetModel.getDeviceSha256() == null){
            return null;
        }
        //禁止在主线程执行,因为本方法内含阻塞,会阻塞调用线程,应该使用其他非服务线程调用
        if (Thread.currentThread().getName().equals("main")) {
            throw new InterruptedException(context.getString(R.string.permissionapplycheckthreaderr));
        }

        AtomicReference<String> sha256Buf = new AtomicReference<>();
        AtomicReference<Boolean> isComplete = new AtomicReference<>(Boolean.FALSE);

        Thread thread = new Thread(()->{
            //检查
            DeviceBleDatabase.BleDeviceMainDatabase database = null;
            database = DeviceBleDatabase.BleDeviceMainDatabase.getDatabase(context);

            if (database == null){
                isComplete.set(true);
                return;
            }

            //缓存当前时间戳
            long currentTimestamp = System.currentTimeMillis();

            //缓存名称
            String nameBuf = null;
            if(targetModel.getDevice() == null){//是伪造设备
                nameBuf = context.getString(R.string.fakedevice_chinese);
            }
            else {//不是伪造设备
                if(targetModel.getDevice().getName() != null){
                    nameBuf = targetModel.getDevice().getName();
                }
                else {//未知设备
                    nameBuf = context.getString(R.string.unknowndevice_chinese);
                }
            }

            //获取操作实体和目标动作
            DeviceBleEntity.BleDeviceMainEntity updateTargetEntity =
                    database.bleDeviceMainDao().getByNameThenBleDeviceSha256(nameBuf,targetModel.getDeviceSha256());
            if(updateTargetEntity != null){
                //已经存在,进行更新操作
                updateTargetEntity.setBleDeviceIconId(targetModel.getIconID());//设置图标ID
                updateTargetEntity.setLastActiveTimestamp(currentTimestamp);//设置最近活动时间戳为当前时间
                database.bleDeviceMainDao().update(updateTargetEntity);
                Log.i("AddToBleDeviceMainDatabase",context.getString(R.string.updatedeviceinformation));

                Log.i("AddToBleDeviceMainDatabase",context.getString(R.string.update_chinese) + "bleDeviceId:" + "[" + updateTargetEntity.getBleDeviceId() + "]");
                Log.i("AddToBleDeviceMainDatabase",context.getString(R.string.update_chinese) + "bleDeviceName:" + updateTargetEntity.getBleDeviceName());
                Log.i("AddToBleDeviceMainDatabase",context.getString(R.string.update_chinese) + "bleDeviceIconId:" + updateTargetEntity.getBleDeviceIconId());
                Log.i("AddToBleDeviceMainDatabase",context.getString(R.string.update_chinese) + "lastActiveTimestamp:" + updateTargetEntity.getLastActiveTimestamp());
                Log.i("AddToBleDeviceMainDatabase",context.getString(R.string.update_chinese) + "bleDeviceSha256:" + updateTargetEntity.getBleDeviceSha256());

                sha256Buf.set(updateTargetEntity.getBleDeviceSha256());
            }
            else{
                //新设备,进行插入操作
                DeviceBleEntity.BleDeviceMainEntity entityNew = new DeviceBleEntity.BleDeviceMainEntity();
                entityNew.setBleDeviceName(nameBuf);//设置设备名称
                entityNew.setBleDeviceIconId(targetModel.getIconID());//设置图标ID
                entityNew.setLastActiveTimestamp(currentTimestamp);//设置最近活动时间戳为当前时间
                entityNew.setBleDeviceSha256(targetModel.getDeviceSha256());//设置设备的SHA-256唯一性与安全校验码
                database.bleDeviceMainDao().insert(entityNew);
                Log.i("AddToBleDeviceMainDatabase",context.getString(R.string.insertnewdeviceinformation));

                //检查是否成功
                DeviceBleEntity.BleDeviceMainEntity nowEntity=
                        database.bleDeviceMainDao().getByNameThenBleDeviceSha256(entityNew.getBleDeviceName(), entityNew.getBleDeviceSha256());
                if(nowEntity != null){
                    Log.i("AddToBleDeviceMainDatabase",context.getString(R.string.completeinsertdatafollowing_chinese));
                    Log.i("AddToBleDeviceMainDatabase",context.getString(R.string.insert_chinese) + "bleDeviceId:" + "[" + nowEntity.getBleDeviceId() + "]");
                    Log.i("AddToBleDeviceMainDatabase",context.getString(R.string.insert_chinese) + "bleDeviceName:" + nowEntity.getBleDeviceName());
                    Log.i("AddToBleDeviceMainDatabase",context.getString(R.string.insert_chinese) + "bleDeviceIconId:" + nowEntity.getBleDeviceIconId());
                    Log.i("AddToBleDeviceMainDatabase",context.getString(R.string.insert_chinese) + "lastActiveTimestamp:" + nowEntity.getLastActiveTimestamp());
                    Log.i("AddToBleDeviceMainDatabase",context.getString(R.string.insert_chinese) + "bleDeviceSha256:" + nowEntity.getBleDeviceSha256());

                    sha256Buf.set(nowEntity.getBleDeviceSha256());
                }
                else {
                    Log.e("AddToBleDeviceMainDatabase",context.getString(R.string.insertdataerror_chinese));
                    sha256Buf.set(null);
                }
            }
            isComplete.set(true);

        });

        thread.start();
        while (!isComplete.get()){
            Thread.sleep(50);
        }

        return sha256Buf.get();
    }

    /**通过Log输出BleDeviceMainDatabase数据库数据,可以在AndroidStudio的"Logcat"查看Log打印
     *
     * @param context 上下文,可以使用Activity作为上下文
     */
    public static void LogShowBleDeviceMainDatabase(Context context){
        new Thread(()->{
            DeviceBleDatabase.BleDeviceMainDatabase database = null;
            database = DeviceBleDatabase.BleDeviceMainDatabase.getDatabase(context);
            if (database == null){
                return;
            }

            List<DeviceBleEntity.BleDeviceMainEntity> list =database.bleDeviceMainDao().allGet();
            //遍历显示所有实体
            for(DeviceBleEntity.BleDeviceMainEntity entity : list){
                Log.i("ShowBleDeviceMainDatabase","------------------------------------------------------");
                Log.i("ShowBleDeviceMainDatabase","|"+"bleDeviceId:" + "[" + entity.getBleDeviceId() + "]");
                Log.i("ShowBleDeviceMainDatabase","|"+"bleDeviceName:" + entity.getBleDeviceName());
                Log.i("ShowBleDeviceMainDatabase","|"+"bleDeviceIconId:" + entity.getBleDeviceIconId());
                Log.i("ShowBleDeviceMainDatabase","|"+"lastActiveTimestamp:" + entity.getLastActiveTimestamp());
                Log.i("ShowBleDeviceMainDatabase","|"+"bleDeviceSha256:" + entity.getBleDeviceSha256());
            }

            Log.i("ShowBleDeviceMainDatabase","------------------------------------------------------");
        }).start();
    }



    //Fragment管理
    FragmentManager managerFragmentMain = null;

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

        //配置fragment
        if(savedInstanceState == null){
            managerFragmentMain = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = managerFragmentMain.beginTransaction();

            BleFragment bleFragment = BleFragment.newInstance();
            fragmentTransaction.add(R.id.container_ble,bleFragment,getString(R.string.tag_blemaintransaction));
            fragmentTransaction.hide(bleFragment);
            fragmentTransaction.commitNow();
        }

        InitMainUI();
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        如果用户已经拒绝这个权限请求
        if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            //显示一个提示框,希望用户改变主意
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.hint_chinese);
            builder.setMessage(R.string.appneedpermission_chinese);
            //配置授予按钮
            builder.setPositiveButton(R.string.give_chinese, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //给用户再次的选择,用户点击"授予",会弹出系统的应用信息,里面有权限管理,但是用户这时可能又矛盾地没有允许对应权限
                    //如果用户一直这样做,最终会一直在当前这个if里循环,直到正式同意权限或点击这里创建的提示框的"拒绝"
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getString(R.string.packagename), null);
                    intent.setData(uri);
                    startActivity(intent);
                    PermissionRequestingFlag = false;//重置请求中标识,关闭独立线程的阻塞
                }
            });
            //配置拒绝按钮
            builder.setNegativeButton(R.string.reject_chinese, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    PermissionRequestingFlag = false;//重置请求中标识,关闭独立线程的阻塞
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();//弹出提示框
        }
        else {//如果同意对应请求
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

            PermissionRequestingFlag = false;//重置请求中标识,关闭独立线程的阻塞
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



////UI-底部导航栏
    private  final Handler[] HandlerMainBottomNavView = {null};//缓存ThreadMainBottomNavView线程handler

    //选择标签的监听
    public class MainBottomNavigationListener implements NavigationBarView.OnItemSelectedListener{
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {

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

            //主线程执行
            HideFragment(getString(R.string.tag_blemaintransaction));

            switch (item.getItemId()){
                case R.id.NavigationDevice:

                    break;
                case R.id.NavigationBLE:

                    ShowFragment(getString(R.string.tag_blemaintransaction));

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

            return true;
        }
    }
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

    /**
     * 隐藏掉指定的Fragment,通过TAG
     * @param tag 在add时注册的TAG
     * @param <F>设置Fragment的类型
     */
    public <F> void HideFragment(@Nullable String tag){
        FragmentTransaction transaction = managerFragmentMain.beginTransaction();
        F fragment = (F)managerFragmentMain.findFragmentByTag(tag);
        if(fragment != null){
            transaction.hide((Fragment) fragment);
            transaction.commit();
        }
    }
    /**
     * 隐藏掉指定的Fragment,通过Fragment的容器ID
     * @param id 布局文件中设置的Fragment的容器ID
     * @param <F> 设置Fragment的类型
     */
    public <F> void HideFragment(int id){
        FragmentTransaction transaction = managerFragmentMain.beginTransaction();
        F fragment = (F)managerFragmentMain.findFragmentById(id);
        if(fragment != null){
            transaction.hide((Fragment) fragment);
            transaction.commit();
        }
    }
    /**
     * 显示出指定的Fragment,通过TAG
     * @param tag 在add时注册的TAG
     * @param <F>设置Fragment的类型
     */
    public <F> void ShowFragment(@Nullable String tag){
        FragmentTransaction transaction = managerFragmentMain.beginTransaction();
        F fragment = (F)managerFragmentMain.findFragmentByTag(tag);
        if(fragment != null){
            transaction.show((Fragment) fragment);
            transaction.commit();
        }
    }
    /**
     * 显示出指定的Fragment,通过Fragment的容器ID
     * @param id 布局文件中设置的Fragment的容器ID
     * @param <F> 设置Fragment的类型
     */
    public <F> void ShowFragment(int id){
        FragmentTransaction transaction = managerFragmentMain.beginTransaction();
        F fragment = (F)managerFragmentMain.findFragmentById(id);
        if(fragment != null){
            transaction.show((Fragment) fragment);
            transaction.commit();
        }
    }

////主UI
    private void InitMainUI(){
        InitMainBottomNavigation();
    }
}