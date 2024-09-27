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

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothControl {
    final public static int FRAMEWORK_INNER_UI = 0;
    final public static int FRAMEWORK_OFFLINE_WEB_PAGE = 1;
    final public static int FRAMEWORK_ONLINE_WEB_PAGE = 2;

    private String deviceSha256;//设备的广播数据的SHA-256校验码
    private int frameworkType;//控制框架类型ID,这是一个枚举,通过BluetoothControl.FRAMEWORK_ ... 以继续选择

    private List<ControlModelWithInnerUiBluetooth> controlModelWithInnerUiList = new ArrayList<>();


    public BluetoothControl(@NonNull String deviceSha256, int frameworkType,@NonNull BluetoothGattDataAccessCallback callback) {
        this.deviceSha256 = deviceSha256;
        this.frameworkType = frameworkType;

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

                //先创建基本模型
                ControlBasicModelBluetooth model = new ControlBasicModelBluetooth(listServices.get(s).getUuid(),listCharacteristics.get(c).getUuid());



//                写一个可以自己生成特征信息的ControlModelWithInnerUiBluetooth构造方法
//                如果获取到写入值类型,补充controlTargetDataTypeId到父级,否则不动






                //再根据基本模型创建含UI模型
                ControlModelWithInnerUiBluetooth modelWithUi = new ControlModelWithInnerUiBluetooth(


                        //只要一个基本模型就可以构造


                        ,model);




                this.controlModelWithInnerUiList.add(modelWithUi);
            }
        }

    }


    /**
     * 搜索控制模型
     * @param index 模型在内部列表的位置
     * @return 控制模型,父级是基本控制模型
     */
    public ControlModelWithInnerUiBluetooth search(int index){
        return controlModelWithInnerUiList.get(index);
    }


    /**
     * 搜索控制模型
     * @param uuidService 服务UUID
     * @param uuidCharacteristic 特征UUID
     * @return 控制模型,父级是基本控制模型
     */
    public ControlModelWithInnerUiBluetooth search(UUID uuidService, UUID uuidCharacteristic){
        for(ControlModelWithInnerUiBluetooth model:controlModelWithInnerUiList){
            if(model.getUuidService() == uuidService && model.getUuidCharacteristic() == uuidCharacteristic){
                return model;
            }
        }
        return null;
    }


    /**
     * 控制写入到蓝牙设备
     * @param model 可以是任何类型的控制模型
     * @param data 字节数据,如果持有其他类型数据,可以使用StandardSync的相关转换
     * @param writeType 写入执行类型 通过 BluetoothGattCharacteristic.WRITE_TYPE...以枚举选择
     * @return 结果码,通过StandardSync.RESULT_...以枚举对比
     */
    public int controlWrite(ControlBasicModelBluetooth model,byte[] data,int writeType){
        return StandardSync.RESULT_FAIL_PARAM;
        return StandardSync.RESULT_FAIL_DEVICE_CHANGED;
        return StandardSync.RESULT_FAIL_DEVICE_DISCONNECTED;
        return StandardSync.RESULT_OK;

    }

    /**
     * 控制写入到蓝牙设备(将使用默认写入执行类型)
     * @param model 可以是任何类型的控制模型
     * @param data 字节数据,如果持有其他类型数据,可以使用StandardSync的相关转换
     * @return 结果码,通过StandardSync.RESULT_...以枚举对比
     */
    public int controlWrite(ControlBasicModelBluetooth model,byte[] data){
        return controlWrite(model,data,BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
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

        /**
         * 发起读取特征请求
         *
         * @param deviceSha256   设备的广播数据的SHA-256校验码
         * @param characteristic 特征的实例
         * @return 成功发起 = true 失败/设备已离线 = false
         */
        public boolean readCharacteristic(String deviceSha256, BluetoothGattCharacteristic characteristic);

        /**
         * 发起写入特征请求
         * @param deviceSha256 设备的广播数据的SHA-256校验码
         * @param data 写入的数据
         * @param writeType 写入类型,输入WRITE_TYPE...以枚举选择
         * @param characteristic 特征的实例
         * @return 成功发起 = true 失败/设备已离线 = false
         */
        public boolean writeCharacteristic(String deviceSha256, byte[] data, int writeType, BluetoothGattCharacteristic characteristic);

    }


    /**
     * 含有内部生成式UI的控制模型,继承了基本控制模型
     */
    public class ControlModelWithInnerUiBluetooth extends ControlBasicModelBluetooth {
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
         * @param controlBasicModelBluetooth 父级控制模型,如果当前未获得,可以设置为空,之后需要再通过syncControlModelBluetooth设置即可
         */
        public ControlModelWithInnerUiBluetooth(int controlWayId, int serviceIconId, int characteristicIconId, int styleBackgroundColor, @Nullable ControlBasicModelBluetooth controlBasicModelBluetooth) {
            super();
            this.controlWayId = controlWayId;
            this.serviceIconId = serviceIconId;
            this.characteristicIconId = characteristicIconId;
            this.styleBackgroundColor = styleBackgroundColor;
            syncControlModelBluetooth(controlBasicModelBluetooth);
        }

        /**
         * 同步父级控制模型到本模型
         *
         * @param controlBasicModelBluetooth 父级控制模型
         */
        public void syncControlModelBluetooth(ControlBasicModelBluetooth controlBasicModelBluetooth) {
            if (controlBasicModelBluetooth != null) {
                super.setUuidService(controlBasicModelBluetooth.getUuidService());
                super.setUuidCharacteristic(controlBasicModelBluetooth.getUuidCharacteristic());
                super.setDataType(controlBasicModelBluetooth.getDataType());

                super.setMaxDataValue(controlBasicModelBluetooth.getMaxDataValue());
                super.setMinDataValue(controlBasicModelBluetooth.getMinDataValue());
                super.setDataBytes(controlBasicModelBluetooth.getDataBytes());
            }
        }
    }
}
