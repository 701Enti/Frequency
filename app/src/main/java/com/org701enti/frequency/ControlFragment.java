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

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ListAdapter;

import com.org701enti.bluetoothfocuser.BluetoothUI;
import com.org701enti.bluetoothfocuser.StandardSync;

import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ControlFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ControlFragment extends Fragment {

    String TAG = new String("ControlFragment");

    public ControlFragment() {
        // Required empty public constructor
    }

    //运行需求
    private ControlFragmentRunWant controlFragmentRunWant;

    public ControlFragmentRunWant getControlFragmentRunWant() {
        return controlFragmentRunWant;
    }

    public interface ControlFragmentRunWant{
        @NonNull
        public List<MainActivity.ControlBaseBluetooth> getControlBaseListBluetooth();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            controlFragmentRunWant = activity.getControlFragmentFunctionRun();
        } else {
            throw new RuntimeException("must be attached by MainActivity but not by" + context.toString());
        }
    }

    public static ControlFragment newInstance() {
        ControlFragment fragment = new ControlFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_control, container, false);
        //初始化其他布局
        initInnerUiConsole(view);
        initWebPageConsole(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


    }


    public void consoleShowBluetooth(MainActivity.ControlBaseBluetooth base){
        if(base == null){
            return;
        }
        BluetoothUI bluetoothUI = base.getBluetoothUI();
        if (bluetoothUI != null){
            //针对控制基础的性质配置控制台状态
           switch (bluetoothUI.getFrameworkType()){
               case StandardSync.FRAMEWORK_INNER_UI -> {
                   setVisibilityInnerUiConsole(View.VISIBLE);
                   setVisibilityWebPageConsole(View.GONE);
                   this.bluetoothInnerUiAdapter.selectShowAccording(base.getBluetoothUI().getDataList());
                   setAdapterBluetoothInnerUi();
               }
               case StandardSync.FRAMEWORK_OFFLINE_WEB_PAGE -> {
                   setVisibilityInnerUiConsole(View.GONE);
                   setVisibilityWebPageConsole(View.VISIBLE);


               }
               case StandardSync.FRAMEWORK_ONLINE_WEB_PAGE -> {
                   setVisibilityInnerUiConsole(View.GONE);
                   setVisibilityWebPageConsole(View.VISIBLE);


               }
               default -> {
                   setVisibilityInnerUiConsole(View.GONE);
                   setVisibilityWebPageConsole(View.GONE);
               }
           }
        }
    }



    //网页控制台相关,包括离线和在线,蓝牙和WIFI控制行为由网页控制
    private WebView webPageConsole = null;
    private URI bluetoothOfflineWebPageURI = null;
    private URL bluetoothOnlineWebPageURL = null;
    private URI wifiOfflineWebPageURI = null;
    private URL wifiOnlineWebPageURL = null;

    private void initWebPageConsole(View view){
        this.webPageConsole = view.findViewById(R.id.WebPageConsole);
        setVisibilityWebPageConsole(View.GONE);
    }
    private void setVisibilityWebPageConsole(int visibility){
        if(webPageConsole != null){
            webPageConsole.setVisibility(visibility);
        }
    }
    public WebView getWebPageConsole() {
        return webPageConsole;
    }

    //内部生成式UI控制台相关,可以由蓝牙,WIFI等不同适配器作用(这里使用适配器单例模式便于管理)
    private GridView innerUiConsole = null;
    private BluetoothInnerUiAdapter bluetoothInnerUiAdapter = new BluetoothInnerUiAdapter();//蓝牙内部生成式UI适配器单例

    private void initInnerUiConsole(View view){
       this.innerUiConsole = view.findViewById(R.id.InnerUiConsole);
       setVisibilityInnerUiConsole(View.GONE);
    }
    private void setVisibilityInnerUiConsole(int visibility){
        if(innerUiConsole != null){
            innerUiConsole.setVisibility(visibility);
        }
    }
    private void setAdapterBluetoothInnerUi(){
        if(innerUiConsole != null){
            innerUiConsole.setAdapter(bluetoothInnerUiAdapter);
        }
    }
    public GridView getInnerUiConsole() {
        return innerUiConsole;
    }

    public class BluetoothInnerUiAdapter extends BaseAdapter{
        private List<Object> unitList = Collections.emptyList();

        public void selectShowAccording(List<Object> unitList){
            if(unitList != null){
                this.unitList = unitList;
                this.notifyDataSetChanged();
            }
        }

        @Override
        public int getCount() {
            return unitList.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View unitView;
            BluetoothUI.InnerUiUnit unit = (BluetoothUI.InnerUiUnit) unitList.get(position);
            if (convertView == null){
                //对每个之前未制作的控件都会制作并设置布局参数
                unitView = unit.makeUnitView(null,false,requireActivity());
            }
            else {
                unitView = convertView;
            }
            //由于所有控件的视图更新,操作逻辑已经在每个都在makeUnitView注册自己独有的回调,不需要在这里完成
            return unitView;
        }
    }

















}