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

public class StandardSync {

    final public static int DATA_TYPE_UNKNOWN = 0;

    //数据类型标识
    final public static int DATA_TYPE_SINT32 = BluetoothGattCharacteristic.FORMAT_SINT32;
    final public static int DATA_TYPE_SINT16 = BluetoothGattCharacteristic.FORMAT_SINT16;
    final public static int DATA_TYPE_SINT8 = BluetoothGattCharacteristic.FORMAT_SINT8;
    final public static int DATA_TYPE_UINT32 = BluetoothGattCharacteristic.FORMAT_UINT32;
    final public static int DATA_TYPE_UINT16 = BluetoothGattCharacteristic.FORMAT_UINT16;
    final public static int DATA_TYPE_UINT8 = BluetoothGattCharacteristic.FORMAT_UINT8;

    final public static int DATA_TYPE_FLOAT_LINE = 49;//整数型和浮点型分割线

    final public static int DATA_TYPE_SFLOAT = BluetoothGattCharacteristic.FORMAT_SFLOAT;
    final public static int DATA_TYPE_FLOAT = BluetoothGattCharacteristic.FORMAT_FLOAT;

    final public static int DATA_TYPE_OTHER_LINE = 99;//浮点型和非数值类分割线

    final public static int DATA_TYPE_ONE_BYTE = 101;
    final public static int DATA_TYPE_BYTE_ARRAY = 102;
    final public static int DATA_TYPE_TEXT = 103;

    //执行结果标识
    final public static int RESULT_UNKNOWN = 0;//未知结果
    final public static int RESULT_OK = -1;
    final public static int RESULT_WAITING = -2;
    final public static int RESULT_THROWED = -3;
    final public static int RESULT_CATCHED = -4;
    final public static int RESULT_FAIL_UNKNOWN = -5;
    final public static int RESULT_FAIL_PARAM = -6;
    final public static int RESULT_FAIL_DEVICE_DISCONNECTED = -7;
    final public static int RESULT_FAIL_DEVICE_CHANGED = -8;









}
