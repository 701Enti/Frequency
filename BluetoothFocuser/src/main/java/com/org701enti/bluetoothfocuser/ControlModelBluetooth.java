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

public class ControlModelBluetooth {
    private UUID uuidService;//所属服务的UUID
    private UUID uuidCharacteristic;//所属特征的UUID
    private int controlTargetDataTypeId;//控制目标数据的类型ID

    private int dataValue;//数值数据
    private int maxDataValue;//数值数据最大值
    private int minDataValue;//数值数据最最小值
    private byte[] dataBytes;//字节数据
    private String dataText;//文本数据

    public ControlModelBluetooth() {
    }

    public ControlModelBluetooth(UUID uuidService, UUID uuidCharacteristic) {
        this.maxDataValue = Integer.MAX_VALUE;
        this.minDataValue = Integer.MIN_VALUE;

        this.uuidService = uuidService;
        this.uuidCharacteristic = uuidCharacteristic;
    }

    public ControlModelBluetooth(int controlTargetDataTypeId, UUID uuidService, UUID uuidCharacteristic) {
        this.maxDataValue = Integer.MAX_VALUE;
        this.minDataValue = Integer.MIN_VALUE;

        this.controlTargetDataTypeId = controlTargetDataTypeId;
        this.uuidService = uuidService;
        this.uuidCharacteristic = uuidCharacteristic;
    }

    public ControlModelBluetooth(int controlTargetDataTypeId, UUID uuidService, UUID uuidCharacteristic, int dataValue, int maxDataValue, int minDataValue) {
        this.maxDataValue = maxDataValue;
        this.minDataValue = minDataValue;

        this.dataValue = dataValue;

        this.controlTargetDataTypeId = controlTargetDataTypeId;
        this.uuidService = uuidService;
        this.uuidCharacteristic = uuidCharacteristic;
    }

    public ControlModelBluetooth(int controlTargetDataTypeId, UUID uuidService, UUID uuidCharacteristic, byte[] dataBytes) {
        this.maxDataValue = Integer.MAX_VALUE;
        this.minDataValue = Integer.MIN_VALUE;

        this.dataBytes = dataBytes;

        this.controlTargetDataTypeId = controlTargetDataTypeId;
        this.uuidService = uuidService;
        this.uuidCharacteristic = uuidCharacteristic;
    }

    public ControlModelBluetooth(int controlTargetDataTypeId, UUID uuidService, UUID uuidCharacteristic, String dataText) {
        this.maxDataValue = Integer.MAX_VALUE;
        this.minDataValue = Integer.MIN_VALUE;

        this.dataText = dataText;

        this.controlTargetDataTypeId = controlTargetDataTypeId;
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

    public int getControlTargetDataTypeId() {
        return controlTargetDataTypeId;
    }

    public void setControlTargetDataTypeId(int controlTargetDataTypeId) {
        this.controlTargetDataTypeId = controlTargetDataTypeId;
    }

    public int getDataValue() {
        return dataValue;
    }

    public void setDataValue(int dataValue) {
        this.dataValue = dataValue;
    }

    public int getMaxDataValue() {
        return maxDataValue;
    }

    public void setMaxDataValue(int maxDataValue) {
        this.maxDataValue = maxDataValue;
    }

    public int getMinDataValue() {
        return minDataValue;
    }

    public void setMinDataValue(int minDataValue) {
        this.minDataValue = minDataValue;
    }

    public byte[] getDataBytes() {
        return dataBytes;
    }

    public void setDataBytes(byte[] dataBytes) {
        this.dataBytes = dataBytes;
    }

    public String getDataText() {
        return dataText;
    }

    public void setDataText(String dataText) {
        this.dataText = dataText;
    }
}
