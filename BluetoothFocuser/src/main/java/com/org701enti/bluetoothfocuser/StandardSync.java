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
    //每个区从整十整百开始,两个区之间必须至少间隔500
    //1-100不允许使用

    //数据类型标识区 100 到 500
    final public static int DATA_TYPE_UNKNOWN = 0;
    final public static int DATA_TYPE_BYTE_ARRAY = 101;
    final public static int DATA_TYPE_ONE_BYTE = 102;
    final public static int DATA_TYPE_TEXT = 103;
    final public static int DATA_TYPE_SINT32 = 104;
    final public static int DATA_TYPE_SINT16 = 105;
    final public static int DATA_TYPE_SINT8 = 106;
    final public static int DATA_TYPE_UINT32 = 107;
    final public static int DATA_TYPE_UINT16 = 108;
    final public static int DATA_TYPE_UINT8 = 109;
    final public static int DATA_TYPE_SFLOAT = 110;
    final public static int DATA_TYPE_FLOAT = 111;

    //运行框架区 1000 到 1500
    final public static int FRAMEWORK_UNKNOWN = 0;
    final public static int FRAMEWORK_INNER_UI = 1001;
    final public static int FRAMEWORK_OFFLINE_WEB_PAGE = 1002;
    final public static int FRAMEWORK_ONLINE_WEB_PAGE = 1003;


    //执行结果标识区 -1 到 -500
    final public static int RESULT_UNKNOWN = 0;//未知结果
    final public static int RESULT_OK = -1;
    final public static int RESULT_WAITING = -2;
    final public static int RESULT_THROWED = -3;
    final public static int RESULT_CATCHED = -4;
    final public static int RESULT_FAIL_UNKNOWN = -5;
    final public static int RESULT_FAIL_PARAM = -6;
    final public static int RESULT_FAIL_DEVICE_STATE = -7;
    final public static int RESULT_FAIL_DEVICE_CHANGED = -8;
    final public static int RESULT_FAIL_SERVICE_NOT_EXIST = -9;
    final public static int RESULT_FAIL_CHARACTERISTIC_NOT_EXIST = -10;

}
