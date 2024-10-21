package com.org701enti.bluetoothfocuser;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class BluetoothUI {
    private int frameworkType;//控制框架类型,通过StandardSync.FRAMEWORK_ ... 以继续选择
    private List<Object> dataList = new ArrayList<>();//数据列表,可以是列表InnerUiUnit等,视frameworkType而定
    BluetoothControl bluetoothControl = null;
    URI uriLink = null;//uri链接
    URL urlLink = null;//url链接

    //未知的控制方式 0
    public final static int CONTROL_WAY_UNKNOWN = 0;

    //输入式 1 - 10
    public final static int CONTROL_WAY_INPUT_BYTE = 1;//输入数据大小占一个字节
    public final static int CONTROL_WAY_INPUT_BYTES = 2;//输入数据大小占多个字节
    public final static int CONTROL_WAY_INPUT_TEXT = 3;//输入文本

    //结构式 11 - 20
    public final static int CONTROL_WAY_STRUCT = 11;//结构化数据

    //按钮式 21 - 30
    public final static int CONTROL_WAY_BUTTON_ONE = 21;//一个按钮
    public final static int CONTROL_WAY_BUTTONS_MAP_BYTE_BIN = 22;//一组按钮,只控制一个字节,每个按钮对应一个位
    public final static int CONTROL_WAY_BUTTONS_MAP_BYTES_BIN = 23;//一组按钮,控制多个字节,每个按钮对应一个位

    //开关式 41 - 50
    public final static int CONTROL_WAY_SWITCH_ONE = 41;//一个开关
    public final static int CONTROL_WAY_SWITCHES_MAP_BYTE_BIN = 42;//一组开关,只控制一个字节,每个开关对应一个位
    public final static int CONTROL_WAY_SWITCHES_MAP_BYTES_BIN = 43;//一组开关,控制多个字节,每个开关对应一个位

    //选项式 61 - 70
    public final static int CONTROL_WAY_SELECT_PATTERN_VALUE = 61;//选择预设的典型值

    //滑动式 81 - 90
    public final static int CONTROL_WAY_SLIDE_FREE_FADER_ONE = 81;//一个自由推子

    /**
     * 内部生成式UI框架构造方法(frameworkType = FRAMEWORK_INNER_UI - 通过基本控制模型列表生成UI数据)
     *
     * @param bluetoothControl BluetoothControl实例
     * @param context          上下文
     */
    public BluetoothUI(@NonNull BluetoothControl bluetoothControl, Context context) {
        this.frameworkType = StandardSync.FRAMEWORK_INNER_UI;
        this.bluetoothControl = bluetoothControl;
        //尝试根据BluetoothControl数据,创建数据列表,并生成UI及其控制绑定
        for (int i = 0; i <= this.bluetoothControl.getMaxIndex(); i++) {
            ControlBasicModelBluetooth controlModel = bluetoothControl.search(i);
            if (controlModel != null) {
                //对每个特征生成对应UI控制单元
                InnerUiUnit unit = new InnerUiUnit(i);
                unit.setControlWay(this.bluetoothControl.getBluetoothGuess().controlWayByDataType(controlModel.getDataType()));
//                //生成内部UI
//                switch (unit.getControlWay()){
//                    case CONTROL_WAY_INPUT_BYTE ->
//                    case CONTROL_WAY_INPUT_BYTES ->
//                    case CONTROL_WAY_INPUT_TEXT ->
//                    case CONTROL_WAY_STRUCT ->
//                    case CONTROL_WAY_BUTTON_ONE ->
//                    case CONTROL_WAY_BUTTONS_MAP_BYTE_BIN ->
//                    case CONTROL_WAY_BUTTONS_MAP_BYTES_BIN ->
//                    case CONTROL_WAY_SWITCH_ONE ->
//                    case CONTROL_WAY_SWITCHES_MAP_BYTE_BIN ->
//                    case CONTROL_WAY_SWITCHES_MAP_BYTES_BIN ->
//                    case CONTROL_WAY_SELECT_PATTERN_VALUE ->
//                    case CONTROL_WAY_SLIDE_FREE_FADER_ONE ->
//                    default -> null;
//                };
                //保存到数据列表
                this.dataList.add(i, unit);
            }
        }


    }

    /**
     * 本地离线网页UI框架构造方法(frameworkType = FRAMEWORK_OFFLINE_WEB_PAGE 通过URI载入UI数据)
     *
     * @param bluetoothControl BluetoothControl实例
     * @param uriLink          uri链接
     */
    public BluetoothUI(@NonNull BluetoothControl bluetoothControl, URI uriLink) {
        this.frameworkType = StandardSync.FRAMEWORK_OFFLINE_WEB_PAGE;
        this.bluetoothControl = bluetoothControl;


    }

    /**
     * 在线互联网网页UI框架构造方法(frameworkType = FRAMEWORK_ONLINE_WEB_PAGE 通过URL载入UI数据)
     *
     * @param bluetoothControl BluetoothControl实例
     * @param urlLink          url链接
     */
    public BluetoothUI(@NonNull BluetoothControl bluetoothControl, URL urlLink) {
        this.frameworkType = StandardSync.FRAMEWORK_ONLINE_WEB_PAGE;
        this.bluetoothControl = bluetoothControl;


    }

    /**
     * 配置自由单个推子控件的视图,包括与BluetoothControl的回调式调度配置
     * @param view 自由单个推子控件的视图,需要外部预先通过inflater加载好XML视图
     * @param indexDataList 这个控件的数据保存在DataList的索引位置
     * @param context 上下文,需要是有Assets访问能力的Activity Fragment等,Assets需要有特定文件资源提供
     */
    public void ViewConfigSlideFreeFaderOne(View view, int indexDataList,Context context) {
        if (view == null || indexDataList >= this.dataList.size() || indexDataList < 0) {
            return;
        }

        //获取数据源
        ControlBasicModelBluetooth basicModel = null;
        if (this.dataList.get(indexDataList) instanceof InnerUiUnit unit) {
            basicModel = this.bluetoothControl.search(unit.getIndexControlModel());
            if (basicModel == null) {
                return;
            }
        } else {
            return;
        }
        //获取控件引用
        SeekBar faderSeekBar = view.findViewById(R.id.ControlFaderSeekBar);
        ImageView targetIcon = view.findViewById(R.id.ControlTargetIcon);
        if (faderSeekBar == null || targetIcon == null) {
            return;
        }
        //根据DataList保存的内容更新视图
        Bitmap bitmapTargetIcon = null;
        try (InputStream iconInput = context.getAssets().open("bluetoothserviceicon/btsu" + unit.getServiceIconId() + ".png")) {
            bitmapTargetIcon = BitmapFactory.decodeStream(iconInput);
        } catch (IOException e) {
            return;
        }
        if (bitmapTargetIcon != null) {
            targetIcon.setImageBitmap(bitmapTargetIcon);
        }



    }


    /**
     * 内部生成式UI单元,一个小控件对应一个单元,生成式UI由多个单元组成
     */
    public class InnerUiUnit {
        private int indexControlModel;//控制模型的索引位置,用于在BluetoothControl实例获取控制模型
        private int controlWay;//控制方式

        private int serviceIconId;//服务图标ID
        private int characteristicIconId;//特征图标ID

        /**
         * 标准构造方法
         *
         * @param indexControlModel 控制模型的索引位置,用于在BluetoothControl实例获取控制模型
         */
        public InnerUiUnit(int indexControlModel) {
            this.indexControlModel = indexControlModel;
            this.controlWay = BluetoothUI.CONTROL_WAY_UNKNOWN;
            this.serviceIconId = 0;
            this.characteristicIconId = 0;
        }

        /**
         * 自定义完全构造方法
         *
         * @param indexControlModel    控制模型的索引位置,用于在BluetoothControl实例获取控制模型
         * @param controlWay           控制方式ID(推子,按钮,开关等)
         * @param serviceIconId        服务图标ID
         * @param characteristicIconId 特征图标ID
         */
        public InnerUiUnit(int indexControlModel, int controlWay, int serviceIconId, int characteristicIconId) {
            this.indexControlModel = indexControlModel;
            this.controlWay = controlWay;
            this.serviceIconId = serviceIconId;
            this.characteristicIconId = characteristicIconId;
        }

        public int getIndexControlModel() {
            return indexControlModel;
        }

        public void setControlWay(int controlWay) {
            this.controlWay = controlWay;
        }

        public void setServiceIconId(int serviceIconId) {
            this.serviceIconId = serviceIconId;
        }

        public void setCharacteristicIconId(int characteristicIconId) {
            this.characteristicIconId = characteristicIconId;
        }

        public int getControlWay() {
            return controlWay;
        }

        public int getServiceIconId() {
            return serviceIconId;
        }

        public int getCharacteristicIconId() {
            return characteristicIconId;
        }
    }
}
