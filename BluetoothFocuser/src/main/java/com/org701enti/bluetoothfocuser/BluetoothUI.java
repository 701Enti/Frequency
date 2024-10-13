package com.org701enti.bluetoothfocuser;

import android.view.View;

import androidx.annotation.NonNull;

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
    public final static int CONTROL_WAY_INPUT_BYTE = 1;//输入一个字节
    public final static int CONTROL_WAY_INPUT_BYTES = 2;//输入多个字节
    public final static int CONTROL_WAY_INPUT_TEXT = 3;//输入文本

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
     * @param bluetoothControl BluetoothControl实例
     */
    public BluetoothUI(@NonNull BluetoothControl bluetoothControl) {
        this.bluetoothControl = bluetoothControl;


    }

    /**
     * 本地离线网页UI框架构造方法(frameworkType = FRAMEWORK_OFFLINE_WEB_PAGE 通过URI载入UI数据)
     * @param bluetoothControl BluetoothControl实例
     * @param uriLink uri链接
     */
    public BluetoothUI(@NonNull BluetoothControl bluetoothControl, URI uriLink) {
        this.bluetoothControl = bluetoothControl;


    }

    /**
     * 在线互联网网页UI框架构造方法(frameworkType = FRAMEWORK_ONLINE_WEB_PAGE 通过URL载入UI数据)
     * @param bluetoothControl BluetoothControl实例
     * @param urlLink url链接
     */
    public BluetoothUI(@NonNull BluetoothControl bluetoothControl, URL urlLink) {
        this.bluetoothControl = bluetoothControl;


    }


    /**
     * 内部生成式UI单元,一个小控件对应一个单元,生成式UI由多个单元组成
     */
    public class InnerUiUnit {
        private int indexControlModel;//控制模型的索引位置,用于在BluetoothControl实例获取控制模型
        private int controlWay;//控制方式
        private int serviceIconId;//服务图标ID
        private int characteristicIconId;//特征图标ID
        private int styleBackgroundColor;//控制控件主题颜色
        private View unitView = null;//单元的View引用

        /**
         * 标准构造方法
         * @param indexControlModel  控制模型的索引位置,用于在BluetoothControl实例获取控制模型
         */
        public InnerUiUnit(int indexControlModel) {
            this.indexControlModel = indexControlModel;






        }

        /**
         * 自定义完全构造方法
         * @param indexControlModel  控制模型的索引位置,用于在BluetoothControl实例获取控制模型
         * @param controlWay               控制方式ID(推子,按钮,开关等)
         * @param serviceIconId              服务图标ID
         * @param characteristicIconId       特征图标ID
         * @param styleBackgroundColor       控制控件主题颜色
         */
        public InnerUiUnit(int indexControlModel, int controlWay, int serviceIconId, int characteristicIconId, int styleBackgroundColor) {
            this.indexControlModel =indexControlModel;
            this.controlWay = controlWay;
            this.serviceIconId = serviceIconId;
            this.characteristicIconId = characteristicIconId;
            this.styleBackgroundColor = styleBackgroundColor;
        }

        public int getIndexControlModel() {
            return indexControlModel;
        }

        public View getUnitView() {
            return unitView;
        }

        public void setUnitView(View unitView) {
            this.unitView = unitView;
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

        public void setStyleBackgroundColor(int styleBackgroundColor) {
            this.styleBackgroundColor = styleBackgroundColor;
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

        public int getStyleBackgroundColor() {
            return styleBackgroundColor;
        }
    }
}
