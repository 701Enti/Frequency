package com.org701enti.bluetoothfocuser;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class BluetoothUI {

    private int frameworkType;//控制框架类型,通过StandardSync.FRAMEWORK_ ... 以继续选择
    private List<Object> dataList = new ArrayList<>();//数据列表,可以是DataInnerUI列表等,视frameworkType而定
    URI uriLink = null;//uri链接
    URL urlLink = null;//url链接

    /**
     * 内部生成式UI框架构造方法(frameworkType = FRAMEWORK_INNER_UI - 通过基本控制模型列表生成UI数据)
     * @param deviceSha256 连接的蓝牙设备广播数据的SHA-256校验码
     * @param controlBasicModelList 控制基本模型列表,在BluetoothControl读取设备完成后可取得
     */
    public BluetoothUI(@NonNull String deviceSha256,List<ControlBasicModelBluetooth> controlBasicModelList) {

    }

    /**
     * 本地离线网页UI框架构造方法(frameworkType = FRAMEWORK_OFFLINE_WEB_PAGE 通过URI载入UI数据)
     * @param deviceSha256 连接的蓝牙设备广播数据的SHA-256校验码
     * @param uriLink uri链接
     */
    public BluetoothUI(@NonNull String deviceSha256, URI uriLink) {

    }

    /**
     * 在线互联网网页UI框架构造方法(frameworkType = FRAMEWORK_ONLINE_WEB_PAGE 通过URL载入UI数据)
     * @param deviceSha256 连接的蓝牙设备广播数据的SHA-256校验码
     * @param urlLink url链接
     */
    public BluetoothUI(@NonNull String deviceSha256, URL urlLink) {

    }


    /**
     * 内部生成式UI数据
     */
    public class DataInnerUI{
        private int controlWayId;//控制方式ID(推子,按钮,开关等)
        private int serviceIconId;//服务图标ID
        private int characteristicIconId;//特征图标ID
        private int styleBackgroundColor;//控制控件主题颜色

        /**
         * 构造方法(默认初始化参数)
         */
        public DataInnerUI() {
        }

        /**
         * 完全构造方法
         *
         * @param controlWayId               控制方式ID(推子,按钮,开关等)
         * @param serviceIconId              服务图标ID
         * @param characteristicIconId       特征图标ID
         * @param styleBackgroundColor       控制控件主题颜色
         */
        public DataInnerUI(int controlWayId, int serviceIconId, int characteristicIconId, int styleBackgroundColor) {
            this.controlWayId = controlWayId;
            this.serviceIconId = serviceIconId;
            this.characteristicIconId = characteristicIconId;
            this.styleBackgroundColor = styleBackgroundColor;
        }

        public void setControlWayId(int controlWayId) {
            this.controlWayId = controlWayId;
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

        public int getControlWayId() {
            return controlWayId;
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
