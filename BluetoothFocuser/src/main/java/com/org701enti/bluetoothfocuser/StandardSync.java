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

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class StandardSync {

    String TAG = "StandardSync";

    private int standardAccording;//标准依据,通过 StandardSync.STANDARD_ACCORDING_...以枚举对比

    private InputStream YamlAppearanceValues = null;//外观值YAML输入流
    private InputStream YamlCharacteristicUuids = null;//特征UUID的YAML输入流
    private InputStream YamlFormatTypes = null;//数据类型的YAML输入流
    private InputStream yamlAdTypes = null;//广播类型的YAML输入流
    private InputStream yamlCharacteristicDataBasicType = null;//特征数据基本类型的YAML输入流

    /**
     * @param standardAccording 标准依据,通过 StandardSync.STANDARD_ACCORDING_...以枚举选择
     */
    public StandardSync(int standardAccording, Context context) {
        this.standardAccording = standardAccording;
        switch (standardAccording) {
            case STANDARD_ACCORDING_FILE_IN_ASSETS -> {
                useStandardFileInAssets(context);
            }
            default -> {
                useStandardFileInAssets(context);
            }
        }
    }

    public void useStandardFileInAssets(Context context) {
        try {
            AssetManager assetManager = context.getAssets();
            this.YamlAppearanceValues = assetManager.open("public/assigned_numbers/core/appearance_values.yaml");
            this.YamlCharacteristicUuids = assetManager.open("public/assigned_numbers/uuids/characteristic_uuids.yaml");
            this.YamlFormatTypes = assetManager.open("public/assigned_numbers/core/formattypes.yaml");
            this.yamlAdTypes = assetManager.open("public/assigned_numbers/core/ad_types.yaml");
            this.yamlCharacteristicDataBasicType = assetManager.open("bluetoothstandardmap/characteristic_data_basic_type.yaml");
        } catch (IOException e) {
            this.YamlAppearanceValues = null;
            this.YamlCharacteristicUuids = null;
            this.YamlFormatTypes = null;
            this.yamlAdTypes = null;
            this.yamlCharacteristicDataBasicType = null;
            Log.e(TAG, "useStandardFileInAssets:存在不可用的标准文件:",e);
        }
    }

    /**
     * YAML解析器
     */
    public class YamlResolver {
        private List<Map<Object, Object>> resultList = null; //转换结果列表
        Yaml yaml = null;//第三方库snakeYaml - YAML实例

        /**
         * 构造方法
         *
         * @param inputStream 选择需要进行解析的YAML输入流,打开YAML文件,或使用Standard.get...获取需要的标准流
         */
        public YamlResolver(InputStream inputStream) {
            yaml = new Yaml();
            resultList = new ArrayList<Map<Object, Object>>();
            if(inputStream != null){
                resultList.add(yaml.load(inputStream));
            }
        }

        /**
         * (可链式调用)进入这个Map列表
         *
         * @param listKey 列表名/列表这个值(对于所在的父列表而言)对应的键
         * @return YamlResolver实例, 使用本类方法其他方法继续,
         * 或getResultList()接着.get()获取在ResultList中需要的Item,
         * 一般结果Item只有一个,所以get(0)得到一个Item,
         * 最后一步,继续get(需要的键),比如我最终要date这个key的值,
         * 就是.getResultList().get(0).get("date"),再强转成需要的类型
         */
        public YamlResolver enterThisMapList(Object listKey) {
            if (resultList == null || listKey == null) {
                return this;
            }
            //遍历每一个父条目
            for(Map<Object, Object> parentItem:resultList){
                //将listName作为Key,获取这个父条目的Value
                Object value = parentItem.get(listKey);
                if(value != null){
                    //确定Value实际上是一个List
                    if(value instanceof List<?> list){
                        resultList = new ArrayList<Map<Object, Object>>();//重置结果列表
                        //遍历存储这个List的每个Map条目到结果列表
                        for (Object item:list){
                            if(item instanceof Map map){
                                //这个条目是一个Map
                                resultList.add(map);
                            }
                        }
                    }
                }
            }
            return this;
        }

        /**
         * (可链式调用)保留拥有这个Key并Value相等的条目即Item
         *
         * @param key   这个键
         * @param value 键的值
         * @return YamlResolver实例, 使用本类方法其他方法继续,
         * 或getResultList()接着.get()获取在ResultList中需要的Item,
         * 一般结果Item只有一个,所以get(0)得到一个Item,
         * 最后一步,继续get(需要的键),比如我最终要date这个key的值,
         * 就是.getResultList().get(0).get("date"),再强转成需要的类型
         */
        public YamlResolver reserveTheItemsHave(Object key, Object value) {
            if (resultList == null || key == null || value == null) {
                return this;
            }
            resultList = resultList.stream()
                    .filter(item -> Objects.equals(item.get(key), value))
                    .collect(Collectors.toList());
            return this;
        }

        /**(可链式调用)获取结果列表
         * 先getResultList()接着.get()获取在ResultList中需要的Item
         * 一般结果Item只有一个,所以get(0) 得到一个Item,
         * 最后一步,继续get(需要的键),比如我最终要date这个key的值,
         * 就是. getResultList().get(0).get("date"),再强转成需要的类型
         * @return 结果列表
         */
        public List<Map<Object, Object>> getResultList() {
            return resultList;
        }
    }


    /**
     * 获取简化的16位UUID,结果诸如"0x0001","0001"(addPrefix=false),"0x2900"等(蓝牙相关)
     * @param uuid UUID实例,需要符合规范的UUID,否则无法获取
     * @param addPrefix 是否需要添加"0x"前缀
     * @return 简化的16位UUID / null(格式错误/输入为空)
     */
    public static String getBluetoothSimplifiedUuid(UUID uuid,boolean addPrefix){
        if(uuid == null){
            return null;
        }
        String string = uuid.toString();
        if(string.startsWith(BLUETOOTH_UUID128_PREFIX) && string.endsWith(BLUETOOTH_UUID128_SUFFIX)){
            if(addPrefix){
                return "0x" + string.substring(4,8);
            }
            else {
                return string.substring(4,8);
            }
        }
        else {
            return null;
        }
    }



    //标准依据 1 - 100
    final public static int STANDARD_ACCORDING_FILE_IN_ASSETS = 1;


    //每个区从整十整百开始,两个区之间必须至少间隔500
    //1-100不允许使用

    //数据类型标识区 100 到 500
    //以YAML文档formattypes.yaml中定义加上偏移量100按顺序映射如下
    final public static int DATA_TYPE_UNKNOWN = 0;
    final public static int DATA_TYPE_OFFSET_FROM_YAML = 100;//从YAML文档formattypes.yaml提取值需要加上的固有偏移量
    final public static int DATA_TYPE_BOOLEAN = DATA_TYPE_OFFSET_FROM_YAML + 0x01;
    final public static int DATA_TYPE_UINT2 = DATA_TYPE_OFFSET_FROM_YAML + 0x02;
    final public static int DATA_TYPE_UINT4 = DATA_TYPE_OFFSET_FROM_YAML + 0x03;
    final public static int DATA_TYPE_UINT8 = DATA_TYPE_OFFSET_FROM_YAML + 0x04;
    final public static int DATA_TYPE_UINT12 = DATA_TYPE_OFFSET_FROM_YAML + 0x05;
    final public static int DATA_TYPE_UINT16 = DATA_TYPE_OFFSET_FROM_YAML + 0x06;
    final public static int DATA_TYPE_UINT24 = DATA_TYPE_OFFSET_FROM_YAML + 0x07;
    final public static int DATA_TYPE_UINT32 = DATA_TYPE_OFFSET_FROM_YAML + 0x08;
    final public static int DATA_TYPE_UINT48 = DATA_TYPE_OFFSET_FROM_YAML + 0x09;
    final public static int DATA_TYPE_UINT64 = DATA_TYPE_OFFSET_FROM_YAML + 0x0A;
    final public static int DATA_TYPE_UINT128 = DATA_TYPE_OFFSET_FROM_YAML + 0x0B;
    final public static int DATA_TYPE_SINT8 = DATA_TYPE_OFFSET_FROM_YAML + 0x0C;
    final public static int DATA_TYPE_SINT12 = DATA_TYPE_OFFSET_FROM_YAML + 0x0D;
    final public static int DATA_TYPE_SINT16 = DATA_TYPE_OFFSET_FROM_YAML + 0x0E;
    final public static int DATA_TYPE_SINT24 = DATA_TYPE_OFFSET_FROM_YAML + 0x0F;
    final public static int DATA_TYPE_SINT32 = DATA_TYPE_OFFSET_FROM_YAML + 0x10;
    final public static int DATA_TYPE_SINT48 = DATA_TYPE_OFFSET_FROM_YAML + 0x11;
    final public static int DATA_TYPE_SINT64 = DATA_TYPE_OFFSET_FROM_YAML + 0x12;
    final public static int DATA_TYPE_SINT128 = DATA_TYPE_OFFSET_FROM_YAML + 0x13;
    final public static int DATA_TYPE_FLOAT32 = DATA_TYPE_OFFSET_FROM_YAML + 0x14;
    final public static int DATA_TYPE_FLOAT64 = DATA_TYPE_OFFSET_FROM_YAML + 0x15;
    final public static int DATA_TYPE_MED_SFLOAT16 = DATA_TYPE_OFFSET_FROM_YAML + 0x16;
    final public static int DATA_TYPE_MED_SFLOAT32 = DATA_TYPE_OFFSET_FROM_YAML + 0x17;
    final public static int DATA_TYPE_UINT16_ARRAY_2 = DATA_TYPE_OFFSET_FROM_YAML + 0x18;
    final public static int DATA_TYPE_UTF8_STRING = DATA_TYPE_OFFSET_FROM_YAML + 0x19;
    final public static int DATA_TYPE_UTF16_STRING = DATA_TYPE_OFFSET_FROM_YAML + 0x1A;
    final public static int DATA_TYPE_STRUCT = DATA_TYPE_OFFSET_FROM_YAML + 0x1B;
    final public static int DATA_TYPE_MED_ASN1_STRUCTURE = DATA_TYPE_OFFSET_FROM_YAML + 0x1C;


    //运行框架区 1000 到 1500
    final public static int FRAMEWORK_UNKNOWN = 0;
    final public static int FRAMEWORK_INNER_UI = 1001;
    final public static int FRAMEWORK_OFFLINE_WEB_PAGE = 1002;
    final public static int FRAMEWORK_ONLINE_WEB_PAGE = 1003;


    //执行结果标识区 -1 到 -500
    final public static int RESULT_UNKNOWN = 0;//未知结果
    final public static int RESULT_OK = -1;
    final public static int RESULT_WAITING = -2;
    final public static int RESULT_THROW = -3;
    final public static int RESULT_CATCH = -4;
    final public static int RESULT_FAIL_UNKNOWN = -5;
    final public static int RESULT_FAIL_PARAM = -6;
    final public static int RESULT_FAIL_DEVICE_STATE = -7;
    final public static int RESULT_FAIL_DEVICE_CHANGED = -8;
    final public static int RESULT_FAIL_SERVICE_NOT_EXIST = -9;
    final public static int RESULT_FAIL_CHARACTERISTIC_NOT_EXIST = -10;


    final private static String BLUETOOTH_UUID128_PREFIX = "0000";
    final private static String BLUETOOTH_UUID128_SUFFIX = "-0000-1000-8000-00805F9B34FB";


    public InputStream getYamlAppearanceValues() {
        return YamlAppearanceValues;
    }

    public InputStream getYamlCharacteristicUuids() {
        return YamlCharacteristicUuids;
    }

    public InputStream getYamlFormatTypes() {
        return YamlFormatTypes;
    }

    public int getStandardAccording() {
        return standardAccording;
    }

    public InputStream getYamlAdTypes() {
        return yamlAdTypes;
    }

    public InputStream getYamlCharacteristicDataBasicType() {
        return yamlCharacteristicDataBasicType;
    }
}
