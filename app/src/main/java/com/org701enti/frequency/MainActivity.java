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

import android.app.AlertDialog;
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

import android.Manifest;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

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

        InitUI();


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
            //主线程执行




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










////主UI
    private void InitUI(){
        InitMainBottomNavigation();
    }
}