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

import java.util.UUID;

public class ControlBasicModelBluetooth {
    private UUID uuidService;//所属服务的UUID
    private UUID uuidCharacteristic;//所属特征的UUID

    private byte[] maxDataValue;//数值数据最大值
    private byte[] minDataValue;//数值数据最小值
    private byte[] dataBytes;//字节数据,任何数据都以字节存储

    private int dataType;//控制目标的数据的类型,将dataBytes中数据转换为这个类型


    public ControlBasicModelBluetooth() {
    }

    public ControlBasicModelBluetooth(UUID uuidService, UUID uuidCharacteristic) {
        this.maxDataValue = null;
        this.minDataValue = null;

        this.dataBytes = null;

        this.dataType = StandardSync.DATA_TYPE_UNKNOWN;
        this.uuidService = uuidService;
        this.uuidCharacteristic = uuidCharacteristic;
    }

    public ControlBasicModelBluetooth(int dataType, UUID uuidService, UUID uuidCharacteristic) {
        this.maxDataValue = null;
        this.minDataValue = null;

        this.dataBytes = null;

        this.dataType = dataType;
        this.uuidService = uuidService;
        this.uuidCharacteristic = uuidCharacteristic;
    }

    public ControlBasicModelBluetooth(int dataType, UUID uuidService, UUID uuidCharacteristic, byte[] dataBytes) {
        this.maxDataValue = null;
        this.minDataValue = null;

        this.dataBytes = dataBytes;

        this.dataType = dataType;
        this.uuidService = uuidService;
        this.uuidCharacteristic = uuidCharacteristic;
    }


    public ControlBasicModelBluetooth(int dataType, UUID uuidService, UUID uuidCharacteristic, byte[] maxDataValue, byte[] minDataValue) {
        this.maxDataValue = maxDataValue;
        this.minDataValue = minDataValue;

        this.dataBytes = null;

        this.dataType = dataType;
        this.uuidService = uuidService;
        this.uuidCharacteristic = uuidCharacteristic;
    }



    public ControlBasicModelBluetooth(int dataType, UUID uuidService, UUID uuidCharacteristic, byte[] dataBytes, byte[] maxDataValue, byte[] minDataValue) {
        this.maxDataValue = maxDataValue;
        this.minDataValue = minDataValue;

        this.dataBytes = dataBytes;

        this.dataType = dataType;
        this.uuidService = uuidService;
        this.uuidCharacteristic = uuidCharacteristic;
    }





    public UUID getUuidService() {
        return uuidService;
    }

    public void setUuidService(UUID uuidService) {
        this.uuidService = uuidService;
    }

    public UUID getUuidCharacteristic() {
        return uuidCharacteristic;
    }

    public void setUuidCharacteristic(UUID uuidCharacteristic) {
        this.uuidCharacteristic = uuidCharacteristic;
    }

    public int getDataType() {
        return dataType;
    }

    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    public byte[] getMaxDataValue() {
        return maxDataValue;
    }

    public void setMaxDataValue(byte[] maxDataValue) {
        this.maxDataValue = maxDataValue;
    }

    public byte[] getMinDataValue() {
        return minDataValue;
    }

    public void setMinDataValue(byte[] minDataValue) {
        this.minDataValue = minDataValue;
    }

    public byte[] getDataBytes() {
        return dataBytes;
    }

    public void setDataBytes(byte[] dataBytes) {
        this.dataBytes = dataBytes;
    }

}
