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
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
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
import android.view.MenuItem;
import android.webkit.PermissionRequest;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.security.Permission;

public class MainActivity extends AppCompatActivity {

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

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,@NonNull int[]grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        如果用户已经拒绝这个权限请求
        if(grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED){
            //显示一个提示框,希望用户改变主意
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.hint_chinese);
            builder.setMessage(R.string.requestpermission_chinese);
            builder.setPositiveButton(R.string.give_chinese, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //给用户再次的选择,用户点击"授予",弹出系统的权限申请框,但是用户这时可能又矛盾地点击这个权限请求框的拒绝
                    //如果用户一直这样做,最终会一直在当前这个if里循环,直到正式同意权限或点击这里创建的提示框的"拒绝"
                    PermissionRequestingFlag = false;//重置请求中标识
                    try {
                        PermissionApplyCheck(requestCode);
                    }
                    catch (InterruptedException Inter){
                        Thread.currentThread().interrupt();
                    }

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
        }
        else{//如果同意对应请求请求
            PermissionRequestingFlag = false;//重置请求中标识
            switch (requestCode){
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
     * 权限检查,如果权限未授予或拒绝,会进行对应权限申请工作
     * @param requestCode 权限申请码,参考MainActivity开头的权限申请码定义
     */
    private void PermissionApplyCheck(int requestCode) throws InterruptedException {
        Handler handler = new Handler(Looper.getMainLooper());
        switch (requestCode){
            case REQUEST_COARSE_LOCATION:
                if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},requestCode);
                        }
                    });
                }
                else return;
                break;
            case REQUEST_FINE_LOCATION:
                if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, requestCode);
                        }
                        });
                }
                else return;
                break;
            case REQUEST_BLUETOOTH_SCAN:
                if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(MainActivity.this,new  String[]{Manifest.permission.BLUETOOTH_SCAN},requestCode);
                }
                else return;
                break;
            case REQUEST_BLUETOOTH_ADVERTISE:
                if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(MainActivity.this,new  String[]{Manifest.permission.BLUETOOTH_ADVERTISE},requestCode);
                }
                else return;
                break;
            case REQUEST_BLUETOOTH_CONNECT:
                if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(MainActivity.this,new  String[]{Manifest.permission.BLUETOOTH_CONNECT},requestCode);
                }
                else return;
                break;
            default:
                break;
        }
        PermissionRequestingFlag = true;
        while (PermissionRequestingFlag){
                Thread.sleep(50);
        }
    }


    private void InitUI(){
        BottomNavigationView mainBottomNavView = findViewById(R.id.MainBottomNavigation);
        mainBottomNavView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
          @SuppressLint("NonConstantResourceId")
          @Override
          public boolean onNavigationItemSelected(@NonNull MenuItem item) {
              switch (item.getItemId()){
                  case R.id.NavigationDevice:

                      break;
                  case R.id.NavigationBLE:
                      new Thread(() -> {
                          try {
//                              PermissionApplyCheck(REQUEST_COARSE_LOCATION);
                              PermissionApplyCheck(REQUEST_FINE_LOCATION);
//                              PermissionApplyCheck(REQUEST_BLUETOOTH_SCAN);
//                              PermissionApplyCheck(REQUEST_BLUETOOTH_ADVERTISE);
//                              PermissionApplyCheck(REQUEST_BLUETOOTH_CONNECT);
                          }
                          catch (InterruptedException Inter){
                              Thread.currentThread().interrupt();
                          }
                      }).start();
                      break;
                  case R.id.NavigationControl:

                      break;
                  case R.id.NavigationWIFI:

                      break;
                  case R.id.NavigationMe:

                      break;
                  default:
                      return false;
              }
              return true;
          }
      });
    }
}