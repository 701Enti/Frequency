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

package com.org701enti.bluetoothfocuser;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothControl {
    final static int FRAMEWORK_INNER_UI = 0;
    final static int FRAMEWORK_OFFLINE_WEB_PAGE = 1;
    final static int FRAMEWORK_ONLINE_WEB_PAGE = 2;

    private String deviceSha256;//设备的广播数据的SHA-256校验码
    private int controlFrameworkTypeId;//控制框架类型ID,这是一个枚举,通过BluetoothControl.FRAMEWORK_ ... 以继续选择


    private List<ControlModelBluetooth> controlModelList = new ArrayList<>();
    private List<ControlModelWithInnerUiBluetooth> controlModelWithInnerUiList = new ArrayList<>();


    public BluetoothControl(String deviceSha256, int controlFrameworkTypeId, BluetoothGattDataAccessCallback callback) {
        this.deviceSha256 = deviceSha256;
        this.controlFrameworkTypeId = controlFrameworkTypeId;

        //遍历存储设备所有特征到controlModelList
        List<BluetoothGattService> listServices = null;
        listServices = callback.getAllServicesBluetoothGatt(this.deviceSha256);
        if(listServices == null){
            return;
        }
        for (int s = 0; s < listServices.size(); s++) {
            //获取角标为s的服务的所有特征
            List<BluetoothGattCharacteristic> listCharacteristics = callback.getThisServiceAllCharacteristicsBluetoothGatt(this.deviceSha256,listServices.get(s));
            if(listCharacteristics == null){
                return;
            }
            //遍历存储角标为s的服务的所有特征
            for(int c = 0; c<listCharacteristics.size();c++){
                ControlModelBluetooth model = new ControlModelBluetooth(listServices.get(s).getUuid(),listCharacteristics.get(c).getUuid());
                this.controlModelList.add(model);
            }
        }

    }


    /**
     * 蓝牙GATT数据请求回调
     */
    public interface BluetoothGattDataAccessCallback {
        /**
         * 获取蓝牙设备所有服务
         *
         * @param deviceSha256 设备的广播数据的SHA-256校验码
         * @return 蓝牙设备服务实例列表
         */
        public List<BluetoothGattService> getAllServicesBluetoothGatt(String deviceSha256);

        /**
         * 获取蓝牙设备单个服务中的所有特征
         *
         * @param deviceSha256 设备的广播数据的SHA-256校验码
         * @param service      这个服务的实例
         * @return 这个服务中的所有特征实例列表
         */
        public List<BluetoothGattCharacteristic> getThisServiceAllCharacteristicsBluetoothGatt(String deviceSha256, BluetoothGattService service);

        /**
         * 获取蓝牙设备单个服务
         *
         * @param deviceSha256 设备的广播数据的SHA-256校验码
         * @param serviceUuid  服务的UUID
         * @return 蓝牙设备服务实例
         */
        public BluetoothGattService getServiceBluetoothGatt(String deviceSha256, UUID serviceUuid);

        /**
         * 获取蓝牙设备单个特征
         *
         * @param deviceSha256       设备的广播数据的SHA-256校验码
         * @param characteristicUuid 特征的UUID
         * @param service            特征所在的服务的实例
         * @return 蓝牙设备特征实例
         */
        public BluetoothGattCharacteristic getCharacteristicsBluetoothGatt(String deviceSha256, UUID characteristicUuid, BluetoothGattService service);
    }


    /**
     * 伴随内部生成式UI的控制模型
     */
    public class ControlModelWithInnerUiBluetooth extends ControlModelBluetooth {
        int controlWayId;//控制方式ID(推子,按钮,开关等)
        int serviceIconId;//服务图标ID
        int characteristicIconId;//特征图标ID
        int styleBackgroundColor;//控制控件主题颜色

        /**
         * 构造方法
         *
         * @param controlWayId          控制方式ID(推子,按钮,开关等)
         * @param serviceIconId         服务图标ID
         * @param characteristicIconId  特征图标ID
         * @param styleBackgroundColor  控制控件主题颜色
         * @param controlModelBluetooth 父级控制模型,如果当前未获得,可以设置为空,之后需要再通过syncControlModelBluetooth设置即可
         */
        public ControlModelWithInnerUiBluetooth(int controlWayId, int serviceIconId, int characteristicIconId, int styleBackgroundColor, @Nullable ControlModelBluetooth controlModelBluetooth) {
            super();
            this.controlWayId = controlWayId;
            this.serviceIconId = serviceIconId;
            this.characteristicIconId = characteristicIconId;
            this.styleBackgroundColor = styleBackgroundColor;
            if (controlModelBluetooth != null) {
                super.setUuidService(controlModelBluetooth.getUuidService());
                super.setUuidCharacteristic(controlModelBluetooth.getUuidCharacteristic());
                super.setControlTargetDataTypeId(controlModelBluetooth.getControlTargetDataTypeId());

                super.setDataValue(controlModelBluetooth.getDataValue());
                super.setMaxDataValue(controlModelBluetooth.getMaxDataValue());
                super.setMinDataValue(controlModelBluetooth.getMinDataValue());
                super.setDataBytes(controlModelBluetooth.getDataBytes());
                super.setDataText(controlModelBluetooth.getDataText());
            }
        }

        /**
         * 同步父级控制模型到本模型
         *
         * @param controlModelBluetooth 父级控制模型
         */
        public void syncControlModelBluetooth(ControlModelBluetooth controlModelBluetooth) {
            if (controlModelBluetooth != null) {
                super.setUuidService(controlModelBluetooth.getUuidService());
                super.setUuidCharacteristic(controlModelBluetooth.getUuidCharacteristic());
                super.setControlTargetDataTypeId(controlModelBluetooth.getControlTargetDataTypeId());

                super.setDataValue(controlModelBluetooth.getDataValue());
                super.setMaxDataValue(controlModelBluetooth.getMaxDataValue());
                super.setMinDataValue(controlModelBluetooth.getMinDataValue());
                super.setDataBytes(controlModelBluetooth.getDataBytes());
                super.setDataText(controlModelBluetooth.getDataText());
            }
        }
    }
}
