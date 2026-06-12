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

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothControl {

    String TAG = new String("BluetoothControl");

    //基本控制模型列表
    private List<ControlBasicModelBluetooth> controlModelList = new ArrayList<ControlBasicModelBluetooth>();

    private String deviceSha256;//设备的广播数据的SHA-256校验码
    private String deviceName;//设备名,可能为null

    public BluetoothGattDataAccessCallback callback;//数据请求回调
    private BluetoothGuess bluetoothGuess = null;//未知猜测,直接用于对应设备未知数据猜测和收集

    private BluetoothGatt gatt = null; //蓝牙BLE-GATT实例


    /**
     * BluetoothControl启动控制部署
     *
     * @param deviceSha256 连接的蓝牙设备广播数据的SHA-256校验码,即操作gatt实例以进行蓝牙相关控制的确认凭证
     * @param callback     蓝牙GATT数据请求回调,该回调必须具备访问对应控制设备的资源的能力,并且没有资源冲突问题
     */
    public BluetoothControl(@NonNull String deviceSha256, @Nullable String deviceName, @NonNull BluetoothGattDataAccessCallback callback) {
        this.deviceSha256 = deviceSha256;
        this.deviceName = deviceName;
        this.callback = callback;
        this.bluetoothGuess = new BluetoothGuess(callback.provideStandardSync());

        new Thread(() -> {
            //遍历存储设备所有特征到controlModelList
            List<BluetoothGattService> listServices = null;
            listServices = callback.getAllServicesBluetoothGatt(this.deviceSha256);
            if (listServices == null) {
                return;
            }
            for (int s = 0; s < listServices.size(); s++) {
                //获取角标为s的服务的所有特征
                List<BluetoothGattCharacteristic> listCharacteristics = callback.getThisServiceAllCharacteristicsBluetoothGatt(this.deviceSha256, listServices.get(s));
                if (listCharacteristics != null) {
                    //遍历存储角标为s的服务的所有特征
                    for (int c = 0; c < listCharacteristics.size(); c++) {
                        //先创建基本模型(初步构造)
                        ControlBasicModelBluetooth model = new ControlBasicModelBluetooth(listServices.get(s).getUuid(), listCharacteristics.get(c).getUuid());
                        //先保存到列表,因为之后读数据会调用dataUpdate,根据模型的特征服务UUID确定更新保存位置,这个位置必须预先存在
                        this.controlModelList.add(model);
                        //读取当前的特征数据,读取等待需要阻塞
                        if(model.getDataBytes() == null){
                            int checkResult = controlRead(model);
                            if(checkResult != StandardSync.RESULT_OK){
                                if(this.deviceName!=null){
                                    Log.e(TAG, "BluetoothControl: 断言失败 - "+this.deviceName+" - "+StandardSync.stringOf(checkResult));
                                }
                                else {
                                    Log.e(TAG, "BluetoothControl: 断言失败 - "+this.deviceSha256+" - "+StandardSync.stringOf(checkResult));
                                }
                            }
                            else {
                                //等待读取完成
                                for (int d=0;d<100;d++){
                                    try {
                                        if(model.getDataBytes() != null){
                                            break;
                                        }
                                        Thread.sleep(10);
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                                //尝试补充模型数据
                                if(model.getDataBytes() != null) {
                                    model.setDataType(bluetoothGuess.dataTypeByCharacteristicUuid(model.getUuidCharacteristic(), model.getDataBytes().length));
                                    model.setMinDataValue(bluetoothGuess.minDataValueByDataType(model.getDataType()));
                                    model.setMaxDataValue(bluetoothGuess.maxDataValueByDataType(model.getDataType()));
                                }
                            }
                        }
                    }
                }
            }

            callback.onBluetoothControlInitFinished();

        }).start();


    }

    /**
     * 检查设备的状态,建议在发现读写操作发生异常之后运行检查
     *
     * @return 结果码, 通过StandardSync.RESULT_...以枚举对比(+OK:设备通讯环境正常,+FAIL_DEVICE_CHANGED:连接设备已切换,+FAIL_DEVICE_STATE:当前设备不是可通讯状态)
     */
    public int deviceStatusCheck() {
        if (!callback.isDeviceSha256EqualsTo(this.deviceSha256)) {
            return StandardSync.RESULT_FAIL_DEVICE_CHANGED;
        }
        if (callback.requireGattState() != BluetoothGatt.STATE_CONNECTED) {
            return StandardSync.RESULT_FAIL_DEVICE_STATE;
        }

        return StandardSync.RESULT_OK;
    }

    /**
     * 获取当前模型在内部列表的最大索引位置
     *
     * @return 模型在内部列表的最大索引位置
     */
    public int getMaxIndex() {
        if (deviceStatusCheck() == StandardSync.RESULT_OK) {
            return controlModelList.size() - 1;
        }
        return -1;
    }

    /**
     * 搜索控制模型
     *
     * @param index 模型在内部列表的索引位置
     * @return 控制模型, 不存在/异常 = null
     */
    public ControlBasicModelBluetooth searchBasicModel(int index) {
        if (deviceStatusCheck() == StandardSync.RESULT_OK) {
            return controlModelList.get(index);
        }
        return null;
    }


    /**
     * 搜索控制模型
     *
     * @param uuidService        服务UUID
     * @param uuidCharacteristic 特征UUID
     * @return 控制模型, 不存在/异常 = null
     */
    public ControlBasicModelBluetooth searchBasicModel(UUID uuidService, UUID uuidCharacteristic) {
        if (deviceStatusCheck() == StandardSync.RESULT_OK) {
            for (ControlBasicModelBluetooth model : controlModelList) {
                if (model.getUuidService() == uuidService && model.getUuidCharacteristic() == uuidCharacteristic) {
                    return model;
                }
            }
        }
        return null;
    }


    /**
     * 更新模型数据
     *
     * @param index 模型在内部列表的索引位置
     * @param data  新的数据
     * @return 结果码, 通过StandardSync.RESULT_...以枚举对比(+ FAIL_CHARACTERISTIC_NOT_EXIST不存在的更新目标)
     */
    public int dataUpdate(int index, byte[] data) {
        //检查参数和设备
        if (data == null) {
            return StandardSync.RESULT_FAIL_PARAM;
        }
        if (!callback.isDeviceSha256EqualsTo(this.deviceSha256)) {
            return StandardSync.RESULT_FAIL_DEVICE_CHANGED;
        }
        if (callback.requireGattState() != BluetoothGatt.STATE_CONNECTED) {
            return StandardSync.RESULT_FAIL_DEVICE_STATE;
        }

        //尝试获取模型
        ControlBasicModelBluetooth model = null;
        model = searchBasicModel(index);
        if (model == null) {
            return StandardSync.RESULT_FAIL_CHARACTERISTIC_NOT_EXIST;
        }

        //保存数据
        model.setDataBytes(data);
        StringBuilder dataHex = new StringBuilder();
        for(byte b:data){
            dataHex.append("[").append(String.format("0x%02X", b)).append("]");
        }
        if(this.deviceName!=null){
            Log.i(TAG, "dataUpdate: 数据 - "+dataHex.toString()+"更新完成 - "+this.deviceName);
        }
        else {
            Log.i(TAG, "dataUpdate: 数据 - "+dataHex.toString()+"更新完成 - "+this.deviceSha256);
        }
        return StandardSync.RESULT_OK;
    }

    /**
     * 更新模型数据
     *
     * @param uuidService        服务UUID
     * @param uuidCharacteristic 特征UUID
     * @param data               新的数据
     * @return 结果码, 通过StandardSync.RESULT_...以枚举对比(+FAIL_CHARACTERISTIC_NOT_EXIST不存在的更新目标)
     */
    public int dataUpdate(UUID uuidService, UUID uuidCharacteristic, byte[] data) {
        //检查参数和设备
        if (data == null) {
            return StandardSync.RESULT_FAIL_PARAM;
        }
        if (!callback.isDeviceSha256EqualsTo(this.deviceSha256)) {
            return StandardSync.RESULT_FAIL_DEVICE_CHANGED;
        }
        if (callback.requireGattState() != BluetoothGatt.STATE_CONNECTED) {
            return StandardSync.RESULT_FAIL_DEVICE_STATE;
        }

        //尝试获取模型
        ControlBasicModelBluetooth model = null;
        model = searchBasicModel(uuidService, uuidCharacteristic);
        if (model == null) {
            return StandardSync.RESULT_FAIL_CHARACTERISTIC_NOT_EXIST;
        }

        //保存数据
        model.setDataBytes(data);
        StringBuilder dataHex = new StringBuilder();
        for(byte b:data){
            dataHex.append("[").append(String.format("0x%02X", b)).append("]");
        }
        if(this.deviceName!=null){
            Log.i(TAG, "dataUpdate: 数据 - "+dataHex.toString()+"更新完成 - "+this.deviceName);
        }
        else {
            Log.i(TAG, "dataUpdate: 数据 - "+dataHex.toString()+"更新完成 - "+this.deviceSha256);
        }
        return StandardSync.RESULT_OK;
    }


    /**
     * 控制写入到蓝牙设备
     *
     * @param model     控制模型
     * @param data      字节数据,如果持有其他类型数据,可以使用StandardSync的相关转换
     * @param writeType 写入执行类型 通过 BluetoothGattCharacteristic.WRITE_TYPE...以枚举选择
     * @return 结果码, 通过StandardSync.RESULT_...以枚举对比
     */
    public int controlWrite(ControlBasicModelBluetooth model, byte[] data, int writeType) {
        //检查参数和设备
        if (model == null || data == null) {
            return StandardSync.RESULT_FAIL_PARAM;
        }
        if (!callback.isDeviceSha256EqualsTo(this.deviceSha256)) {
            return StandardSync.RESULT_FAIL_DEVICE_CHANGED;
        }
        if (callback.requireGattState() != BluetoothGatt.STATE_CONNECTED) {
            return StandardSync.RESULT_FAIL_DEVICE_STATE;
        }

        //缓存数据
        model.setDataBytes(data);

        BluetoothGattService service = null;
        BluetoothGattCharacteristic characteristic = null;

        //尝试获取模型对应服务
        service = callback.getServiceBluetoothGatt(this.deviceSha256, model.getUuidService());
        if (service != null) {
            characteristic = callback.getCharacteristicsBluetoothGatt(this.deviceSha256, model.getUuidCharacteristic(), service);
        } else {
            return StandardSync.RESULT_FAIL_SERVICE_NOT_EXIST;
        }

        //检查是否成功获取模型对应特征
        if (characteristic == null) {
            return StandardSync.RESULT_FAIL_CHARACTERISTIC_NOT_EXIST;
        }


        //保存数据
        StringBuilder dataHex = new StringBuilder();
        for(byte b:data){
            dataHex.append("[").append(String.format("0x%02X", b)).append("]");
        }
        Log.d(TAG, "controlWrite:写入 - "
                + "服务 "
                + StandardSync.getBluetoothSimplifiedUuid(service.getUuid(),true,true)
                + " - "
                + "特征 "
                + StandardSync.getBluetoothSimplifiedUuid(characteristic.getUuid(),true,true)
                + " - "
                + "数据 "
                +dataHex.toString()
        );


        //通过回调接口,请求外部访问GATT写入
        if (callback.writeCharacteristic(this.deviceSha256, model.getDataBytes(), writeType, characteristic)) {
            return StandardSync.RESULT_OK;
        } else {
            return StandardSync.RESULT_FAIL_UNKNOWN;
        }
    }

    /**
     * 控制写入到蓝牙设备(将使用默认写入执行类型)
     *
     * @param model 控制模型
     * @param data  字节数据,如果持有其他类型数据,可以使用StandardSync的相关转换
     * @return 结果码, 通过StandardSync.RESULT_...以枚举对比
     */
    public int controlWrite(ControlBasicModelBluetooth model, byte[] data) {
        return controlWrite(model, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
    }


    /**
     * (非立即生效)请求控制读取蓝牙设备特征到BluetoothControl内部列表缓存
     *
     * @param model 控制模型
     * @return 检查结果码, 通过StandardSync.RESULT_...以枚举对比,这里只是发送前的检查结果,不是最终读取结果标识
     */
    public int controlRead(ControlBasicModelBluetooth model) {
        //检查参数和设备
        if (model == null) {
            return StandardSync.RESULT_FAIL_PARAM;
        }
        if (!callback.isDeviceSha256EqualsTo(this.deviceSha256)) {
            return StandardSync.RESULT_FAIL_DEVICE_CHANGED;
        }
        if (callback.requireGattState() != BluetoothGatt.STATE_CONNECTED) {
            return StandardSync.RESULT_FAIL_DEVICE_STATE;
        }

        BluetoothGattService service = null;
        BluetoothGattCharacteristic characteristic = null;

        //尝试获取模型对应服务
        service = callback.getServiceBluetoothGatt(this.deviceSha256, model.getUuidService());
        if (service != null) {
            characteristic = callback.getCharacteristicsBluetoothGatt(this.deviceSha256, model.getUuidCharacteristic(), service);
        } else {
            return StandardSync.RESULT_FAIL_SERVICE_NOT_EXIST;
        }

        //检查是否成功获取模型对应特征
        if (characteristic == null) {
            return StandardSync.RESULT_FAIL_CHARACTERISTIC_NOT_EXIST;
        }

        Log.d(TAG, "controlRead:读取 - "
                + "服务 "
                + StandardSync.getBluetoothSimplifiedUuid(service.getUuid(),true,true)
                + " - "
                + "特征 "
                + StandardSync.getBluetoothSimplifiedUuid(characteristic.getUuid(),true,true)
        );

        if((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) == 0){
            Log.w(TAG, "controlRead: 拒绝访问的特征");
            return StandardSync.RESULT_FAIL_ACCESS_DENIED;
        }

        //通过回调接口,请求外部访问GATT读取,请求之后外部回调会存储数据
        if (callback.readCharacteristic(this.deviceSha256, characteristic)) {
            return StandardSync.RESULT_OK;
        } else {
            return StandardSync.RESULT_FAIL_UNKNOWN;
        }
    }


    /**
     * (立即生效)启用Notify
     *
     * @param model 控制模型
     * @return 检查结果码, 通过StandardSync.RESULT_...以枚举对比,这里只是发送前的检查结果,不是最终读取结果标识
     */
    @SuppressLint("MissingPermission")
    public int controlEnableNotify(ControlBasicModelBluetooth model) {
        if(gatt == null) return StandardSync.RESULT_FAIL_DEVICE_STATE;

        //配置GATT
        BluetoothGattService service = gatt.getService(model.getUuidService());
        if(service == null)return StandardSync.RESULT_FAIL_SERVICE_NOT_EXIST;
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(model.getUuidCharacteristic());
        if(characteristic == null)return StandardSync.RESULT_FAIL_CHARACTERISTIC_NOT_EXIST;
        gatt.setCharacteristicNotification(characteristic,true);

        //写入CCCD
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(StandardSync.CCCD_UUID);
        if(descriptor != null){
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);
        }

        return StandardSync.RESULT_OK;
    }

    public BluetoothGuess getBluetoothGuess() {
        return bluetoothGuess;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getDeviceSha256() {
        return deviceSha256;
    }

    public BluetoothGatt getGatt() {
        return gatt;
    }

    public void setGatt(BluetoothGatt gatt) {
        this.gatt = gatt;
    }

    /**
     * 蓝牙GATT数据请求回调
     */
    public interface BluetoothGattDataAccessCallback {

        /**
         * 提供StandardSync(不能为null)
         * @return StandardSync实例(不为null)
         */
        @NonNull
        public StandardSync provideStandardSync();

        /**
         * 获取GATT的状态
         *
         * @return GATT的状态
         */
        public int requireGattState();

        /**
         * 判断存储的设备的广播数据的SHA-256校验码是否与当前连接的一致
         *
         * @param deviceSha256 设备的广播数据的SHA-256校验码
         * @return true = 一致
         */
        boolean isDeviceSha256EqualsTo(String deviceSha256);

        /**
         * 当BluetoothControl构造完成,将调用该方法
         */
        public void onBluetoothControlInitFinished();

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
         *
         * @param deviceSha256   设备的广播数据的SHA-256校验码
         * @param data           写入的数据
         * @param writeType      写入类型,输入WRITE_TYPE...以枚举选择
         * @param characteristic 特征的实例
         * @return 成功发起 = true 失败/设备已离线 = false
         */
        public boolean writeCharacteristic(String deviceSha256, byte[] data, int writeType, BluetoothGattCharacteristic characteristic);

    }
}
