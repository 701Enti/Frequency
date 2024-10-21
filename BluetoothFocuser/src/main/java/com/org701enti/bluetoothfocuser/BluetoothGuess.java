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

import static com.google.android.material.internal.ContextUtils.getActivity;
import static com.org701enti.bluetoothfocuser.StandardSync.DATA_TYPE_OFFSET_FROM_YAML;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.UUID;

public class BluetoothGuess {

    String TAG = "BluetoothGuess";
    StandardSync standardSync = null;


    public BluetoothGuess(@NonNull StandardSync standardSync) {
        this.standardSync = standardSync;
    }


    /**
     * 猜测数据的类型,通过特征的UUID和数据样本大小
     *
     * @param uuid       特征的UUID实例
     * @param dataLength (对大小不定类型,猜测时忽略此参数)存储这个数据的byte数组的.length结果值,即这个数据占据最多几个字节
     * @return 数据的类型, 根据StandardSync.DATA_TYPE_...枚举
     */
    public int dataTypeByCharacteristicUuid(UUID uuid, int dataLength) {
        int dataType = StandardSync.DATA_TYPE_UNKNOWN;
        if (uuid == null) {
            return dataType;
        }
        //获取简化UUID,不添加0x前缀,结果诸如"2900" "2A19"
        String stringSimplifiedUuid = StandardSync.getBluetoothSimplifiedUuid(uuid, false);
        if (stringSimplifiedUuid == null) {
            return dataType;
        } else {
            return dataTypeByCharacteristicUuid(stringSimplifiedUuid, dataLength);
        }
    }

    /**
     * 猜测数据的类型,通过特征的UUID和数据样本大小
     *
     * @param stringSimplifiedUuid 特征的UUID的16位16进制简化字符串表达,如"2900" "2A19"
     * @param dataLength           (对大小不定类型,猜测时忽略此参数)存储这个数据的byte数组的.length结果值,即这个数据占据最多几个字节
     * @return 数据的类型, 根据StandardSync.DATA_TYPE_...枚举
     */
    public int dataTypeByCharacteristicUuid(String stringSimplifiedUuid, int dataLength) {
        int dataType = StandardSync.DATA_TYPE_UNKNOWN;

        if (stringSimplifiedUuid == null) {
            return dataType;
        }

        try {
            //通过简化的特征UUID获取特征名称
            StandardSync.YamlResolver yamlResolver = standardSync.new YamlResolver(standardSync.getYamlCharacteristicUuids());
            String name = (String)
                    yamlResolver
                            .enterThisMapList("uuids")
                            .reserveTheItemsHave("uuid", Integer.parseInt(stringSimplifiedUuid, 16))
                            .getResultList().get(0).get("name");
            assert name != null;
            //通过特征名称获取基本类型
            yamlResolver = standardSync.new YamlResolver(standardSync.getYamlCharacteristicDataBasicType());
            String basicType = (String)
                    yamlResolver
                            .enterThisMapList("characteristic_data_basic_type")
                            .reserveTheItemsHave("name", name)
                            .getResultList().get(0).get("basic_type");
            assert basicType != null;
            //获取较完整类型名,如果需要补充类型名,例如 uint -> uint16,根据dataLength补充
            String type;//较完整类型名
            if (basicType.startsWith("uint") || basicType.startsWith("sint") || basicType.startsWith("float") || basicType.startsWith("medfloat")) {
                type = new String(basicType + String.valueOf(dataLength * 8));//dataLength为数据字节数
            } else {
                type = new String(basicType);
            }
            //根据较完整类型名,获取类型对应码
            yamlResolver = standardSync.new YamlResolver(standardSync.getYamlFormatTypes());
            Integer typeId = (Integer)
                    yamlResolver
                            .enterThisMapList("formattypes")
                            .reserveTheItemsHave("short_name", type)
                            .getResultList().get(0).get("value");
            assert typeId != null;
            //根据StandardSync的规定映射成StandardSync.DATA_TYPE_...枚举值即dataType值
            dataType = (int) typeId + DATA_TYPE_OFFSET_FROM_YAML;
        } catch (AssertionError | NullPointerException | IndexOutOfBoundsException |
                 NumberFormatException e) {
            Log.w(TAG, "dataTypeByCharacteristicUuid: 猜测时出现问题,因为:", e);
            return dataType;
        }

        return dataType;
    }


    /**
     * 猜测UI控制方式,通过数据的类型
     *
     * @param dataType 数据的类型,根据StandardSync.DATA_TYPE_...枚举
     * @return UI控制方式, 通过BluetoothUI.CONTROL_WAY_...枚举对比
     */
    public int controlWayByDataType(int dataType) {
        return switch (dataType) {
            case StandardSync.DATA_TYPE_BOOLEAN
                    -> BluetoothUI.CONTROL_WAY_SWITCH_ONE;
            case StandardSync.DATA_TYPE_UINT2, StandardSync.DATA_TYPE_UINT4
                    -> BluetoothUI.CONTROL_WAY_SELECT_PATTERN_VALUE;
            case StandardSync.DATA_TYPE_UINT8, StandardSync.DATA_TYPE_UINT16,
                 StandardSync.DATA_TYPE_UINT32, StandardSync.DATA_TYPE_SINT8,
                 StandardSync.DATA_TYPE_SINT16, StandardSync.DATA_TYPE_SINT32
                    -> BluetoothUI.CONTROL_WAY_SLIDE_FREE_FADER_ONE;
            case StandardSync.DATA_TYPE_UINT12, StandardSync.DATA_TYPE_UINT24,
                 StandardSync.DATA_TYPE_UINT48, StandardSync.DATA_TYPE_UINT64,
                 StandardSync.DATA_TYPE_UINT128, StandardSync.DATA_TYPE_SINT12,
                 StandardSync.DATA_TYPE_SINT24, StandardSync.DATA_TYPE_SINT48,
                 StandardSync.DATA_TYPE_SINT64, StandardSync.DATA_TYPE_SINT128,
                 StandardSync.DATA_TYPE_FLOAT32, StandardSync.DATA_TYPE_FLOAT64,
                 StandardSync.DATA_TYPE_MED_SFLOAT16, StandardSync.DATA_TYPE_MED_SFLOAT32,
                 StandardSync.DATA_TYPE_UINT16_ARRAY_2
                    -> BluetoothUI.CONTROL_WAY_INPUT_BYTES;
            case StandardSync.DATA_TYPE_UTF8_STRING, StandardSync.DATA_TYPE_UTF16_STRING
                    -> BluetoothUI.CONTROL_WAY_INPUT_TEXT;
            case StandardSync.DATA_TYPE_STRUCT, StandardSync.DATA_TYPE_MED_ASN1_STRUCTURE
                    -> BluetoothUI.CONTROL_WAY_STRUCT;
            default -> BluetoothUI.CONTROL_WAY_UNKNOWN;
        };
    }

    public byte[] minDataValueByDataType(int dataType){

    }

    public byte[] maxDataValueByDataType(int dataType){

    }

}
