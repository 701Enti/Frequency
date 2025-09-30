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
import java.util.HashMap;
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
    private InputStream YamlServiceUuids = null;//服务UUID的YAML输入流
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
            this.YamlServiceUuids = assetManager.open("public/assigned_numbers/uuids/service_uuids.yaml");
            this.YamlFormatTypes = assetManager.open("public/assigned_numbers/core/formattypes.yaml");
            this.yamlAdTypes = assetManager.open("public/assigned_numbers/core/ad_types.yaml");
            this.yamlCharacteristicDataBasicType = assetManager.open("bluetooth-relevant-mapper/characteristic_data_basic_type.yaml");
        } catch (IOException e) {
            this.YamlAppearanceValues = null;
            this.YamlCharacteristicUuids = null;
            this.YamlServiceUuids = null;
            this.YamlFormatTypes = null;
            this.yamlAdTypes = null;
            this.yamlCharacteristicDataBasicType = null;
            Log.e(TAG, "useStandardFileInAssets:存在不可用的标准文件:", e);
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
            if (inputStream != null) {
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
            for (Map<Object, Object> parentItem : resultList) {
                //将listName作为Key,获取这个父条目的Value
                Object value = parentItem.get(listKey);
                if (value != null) {
                    //确定Value实际上是一个List
                    if (value instanceof List<?> list) {
                        resultList = new ArrayList<Map<Object, Object>>();//重置结果列表
                        //遍历存储这个List的每个Map条目到结果列表
                        for (Object item : list) {
                            if (item instanceof Map map) {
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

        /**
         * (可链式调用)获取结果列表
         * 先getResultList()接着.get()获取在ResultList中需要的Item
         * 一般结果Item只有一个,所以get(0) 得到一个Item,
         * 最后一步,继续get(需要的键),比如我最终要date这个key的值,
         * 就是. getResultList().get(0).get("date"),再强转成需要的类型
         *
         * @return 结果列表
         */
        public List<Map<Object, Object>> getResultList() {
            return resultList;
        }
    }


    /**
     * 获取简化的16位UUID,结果诸如"0x0001","0001"(addPrefix=false),"0x2900"等(含有字母根据设定匹配大小写形式)
     *
     * @param uuid        UUID实例,需要符合规范的UUID,否则无法获取
     * @param addPrefix   是否需要添加"0x"前缀
     * @param toUpperCase 如果UUID中含有字母,确保输出大写字母(如果添加"0x"前缀,其中的x不大写)
     * @return 根据参数简化的16位UUID/ uuid.toString读到的完整128位UUID,无前缀和大小写处理(格式不符合标准简化形式) / null(输入uuid实例为空)
     */
    public static String getBluetoothSimplifiedUuid(UUID uuid, boolean addPrefix, boolean toUpperCase) {
        if (uuid == null) {
            return null;
        }
        String origin = uuid.toString();
        if (origin.startsWith(BLUETOOTH_UUID128_PREFIX) && (origin.endsWith(BLUETOOTH_UUID128_SUFFIX) || origin.endsWith(BLUETOOTH_UUID128_SUFFIX_LOWERCASE))) {
            String cut;
            if (toUpperCase) {
                cut = origin.substring(4, 8).toUpperCase();
            } else {
                cut = origin.substring(4, 8);
            }
            if (addPrefix) {
                return "0x" + cut;
            } else {
                return cut;
            }
        } else {
            return origin;
        }
    }

    /**
     * 如果有,删除byte数组前部的0x00,如[0x00][0x13][0x35]会被切成[0x13][0x35],[0x26][0x34]输入所得为原值且引用一致,因为前部没有0x00
     * @param bytesInput 输入数组
     * @return 输出数组
     */
    public static byte[] cutZeroInTheFrontOf(byte[] bytesInput){
        //截取前部为0x00数据
        ArrayList<Byte> buf = new ArrayList<>();
        boolean getFlag = false;
        for (byte b : bytesInput) {
            if (b != 0x00) {
                getFlag = true;
            }
            if (getFlag) {
                buf.add(b);
            }
        }
        if(!buf.isEmpty()){
            byte[] bytesOutput = new byte[buf.size()];
            for(int j=0;j< buf.size();j++){
                bytesOutput[j] = buf.get(j);
            }
            return bytesOutput;
        }
        return bytesInput;
    }


    //您可以使用编辑器自带的多行编辑功能处理以下映射的编辑
    private static final Map<Integer, String> valueToStringMap = new HashMap<>();

    /**
     * 获取本类定义的常量的字符形式表示
     *
     * @param value 本类定义的常量
     * @return 字符形式表示
     */
    public static String stringOf(int value) {
        return new String(valueToStringMap.get(value));
    }


    //每个区从整十整百开始,两个区之间必须至少间隔500

    //标准依据 1 - 100
    final public static int STANDARD_ACCORDING_FILE_IN_ASSETS = 1;

    static {
        valueToStringMap.put(STANDARD_ACCORDING_FILE_IN_ASSETS, "STANDARD_ACCORDING_FILE_IN_ASSETS");
    }


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

    final public static String PREFIX_SHOW_DATA_TYPE_IS_UINT = "DATA_TYPE_UINT";

    static {
        valueToStringMap.put(DATA_TYPE_UNKNOWN, "DATA_TYPE_UNKNOWN");
        valueToStringMap.put(DATA_TYPE_OFFSET_FROM_YAML, "DATA_TYPE_OFFSET_FROM_YAML");
        valueToStringMap.put(DATA_TYPE_BOOLEAN, "DATA_TYPE_BOOLEAN");
        valueToStringMap.put(DATA_TYPE_UINT2, "DATA_TYPE_UINT2");
        valueToStringMap.put(DATA_TYPE_UINT4, "DATA_TYPE_UINT4");
        valueToStringMap.put(DATA_TYPE_UINT8, "DATA_TYPE_UINT8");
        valueToStringMap.put(DATA_TYPE_UINT12, "DATA_TYPE_UINT12");
        valueToStringMap.put(DATA_TYPE_UINT16, "DATA_TYPE_UINT16");
        valueToStringMap.put(DATA_TYPE_UINT24, "DATA_TYPE_UINT24");
        valueToStringMap.put(DATA_TYPE_UINT32, "DATA_TYPE_UINT32");
        valueToStringMap.put(DATA_TYPE_UINT48, "DATA_TYPE_UINT48");
        valueToStringMap.put(DATA_TYPE_UINT64, "DATA_TYPE_UINT64");
        valueToStringMap.put(DATA_TYPE_UINT128, "DATA_TYPE_UINT128");
        valueToStringMap.put(DATA_TYPE_SINT8, "DATA_TYPE_SINT8");
        valueToStringMap.put(DATA_TYPE_SINT12, "DATA_TYPE_SINT12");
        valueToStringMap.put(DATA_TYPE_SINT16, "DATA_TYPE_SINT16");
        valueToStringMap.put(DATA_TYPE_SINT24, "DATA_TYPE_SINT24");
        valueToStringMap.put(DATA_TYPE_SINT32, "DATA_TYPE_SINT32");
        valueToStringMap.put(DATA_TYPE_SINT48, "DATA_TYPE_SINT48");
        valueToStringMap.put(DATA_TYPE_SINT64, "DATA_TYPE_SINT64");
        valueToStringMap.put(DATA_TYPE_SINT128, "DATA_TYPE_SINT128");
        valueToStringMap.put(DATA_TYPE_FLOAT32, "DATA_TYPE_FLOAT32");
        valueToStringMap.put(DATA_TYPE_FLOAT64, "DATA_TYPE_FLOAT64");
        valueToStringMap.put(DATA_TYPE_MED_SFLOAT16, "DATA_TYPE_MED_SFLOAT16");
        valueToStringMap.put(DATA_TYPE_MED_SFLOAT32, "DATA_TYPE_MED_SFLOAT32");
        valueToStringMap.put(DATA_TYPE_UINT16_ARRAY_2, "DATA_TYPE_UINT16_ARRAY_2");
        valueToStringMap.put(DATA_TYPE_UTF8_STRING, "DATA_TYPE_UTF8_STRING");
        valueToStringMap.put(DATA_TYPE_UTF16_STRING, "DATA_TYPE_UTF16_STRING");
        valueToStringMap.put(DATA_TYPE_STRUCT, "DATA_TYPE_STRUCT");
        valueToStringMap.put(DATA_TYPE_MED_ASN1_STRUCTURE, "DATA_TYPE_MED_ASN1_STRUCTURE");
    }


    //运行框架区 1000 到 1500
    final public static int FRAMEWORK_UNKNOWN = 0;
    final public static int FRAMEWORK_INNER_UI = 1001;
    final public static int FRAMEWORK_OFFLINE_WEB_PAGE = 1002;
    final public static int FRAMEWORK_ONLINE_WEB_PAGE = 1003;

    static {
        valueToStringMap.put(FRAMEWORK_UNKNOWN, "FRAMEWORK_UNKNOWN");
        valueToStringMap.put(FRAMEWORK_INNER_UI, "FRAMEWORK_INNER_UI");
        valueToStringMap.put(FRAMEWORK_OFFLINE_WEB_PAGE, "FRAMEWORK_OFFLINE_WEB_PAGE");
        valueToStringMap.put(FRAMEWORK_ONLINE_WEB_PAGE, "FRAMEWORK_ONLINE_WEB_PAGE ");
    }


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
    final public static int RESULT_FAIL_ACCESS_DENIED = -11;

    static {
        valueToStringMap.put(RESULT_UNKNOWN, "RESULT_UNKNOWN");
        valueToStringMap.put(RESULT_OK, "RESULT_OK");
        valueToStringMap.put(RESULT_WAITING, "RESULT_WAITING");
        valueToStringMap.put(RESULT_THROW, "RESULT_THROW");
        valueToStringMap.put(RESULT_CATCH, "RESULT_CATCH");
        valueToStringMap.put(RESULT_FAIL_UNKNOWN, "RESULT_FAIL_UNKNOWN");
        valueToStringMap.put(RESULT_FAIL_PARAM, "RESULT_FAIL_PARAM");
        valueToStringMap.put(RESULT_FAIL_DEVICE_STATE, "RESULT_FAIL_DEVICE_STATE");
        valueToStringMap.put(RESULT_FAIL_DEVICE_CHANGED, "RESULT_FAIL_DEVICE_CHANGED");
        valueToStringMap.put(RESULT_FAIL_SERVICE_NOT_EXIST, "RESULT_FAIL_SERVICE_NOT_EXIST");
        valueToStringMap.put(RESULT_FAIL_CHARACTERISTIC_NOT_EXIST, "RESULT_FAIL_CHARACTERISTIC_NOT_EXIST");
        valueToStringMap.put(RESULT_FAIL_ACCESS_DENIED, "RESULT_FAIL_ACCESS_DENIED");
    }


    //静态数据和常量

    //蓝牙标准128位UUID前缀和后缀
    final private static String BLUETOOTH_UUID128_PREFIX = "0000";
    final private static String BLUETOOTH_UUID128_SUFFIX = "-0000-1000-8000-00805F9B34FB";
    final private static String BLUETOOTH_UUID128_SUFFIX_LOWERCASE = "-0000-1000-8000-00805f9b34fb";

    //部分类型最小值模板
    final public static byte[] MIN_DATA_VALUE_BOOLEAN = {0x00};
    final public static byte[] MIN_DATA_VALUE_UINT2 = {0x00};
    final public static byte[] MIN_DATA_VALUE_UINT4 = {0x00};
    final public static byte[] MIN_DATA_VALUE_UINT8 = {0x00};
    final public static byte[] MIN_DATA_VALUE_UINT16 = {0x00, 0x00};
    final public static byte[] MIN_DATA_VALUE_UINT24 = {0x00, 0x00, 0x00};
    final public static byte[] MIN_DATA_VALUE_UINT32 = {0x00, 0x00, 0x00, 0x00};
    final public static byte[] MIN_DATA_VALUE_UINT48 = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    final public static byte[] MIN_DATA_VALUE_UINT64 = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    final public static byte[] MIN_DATA_VALUE_UINT128 = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    final public static byte[] MIN_DATA_VALUE_SINT8 = {(byte) 0x80};
    final public static byte[] MIN_DATA_VALUE_SINT12 = {(byte) 0x08, 0x00};
    final public static byte[] MIN_DATA_VALUE_SINT16 = {(byte) 0x80, 0x00};
    final public static byte[] MIN_DATA_VALUE_SINT24 = {(byte) 0x80, 0x00, 0x00};
    final public static byte[] MIN_DATA_VALUE_SINT32 = {(byte) 0x80, 0x00, 0x00, 0x00};
    final public static byte[] MIN_DATA_VALUE_SINT48 = {(byte) 0x80, 0x00, 0x00, 0x00, 0x00, 0x00};
    final public static byte[] MIN_DATA_VALUE_SINT64 = {(byte) 0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    final public static byte[] MIN_DATA_VALUE_SINT128 = {(byte) 0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

    //部分类型最大值模板
    final public static byte[] MAX_DATA_VALUE_BOOLEAN = {0x01};
    final public static byte[] MAX_DATA_VALUE_UINT2 = {0x03};
    final public static byte[] MAX_DATA_VALUE_UINT4 = {0x0F};
    final public static byte[] MAX_DATA_VALUE_UINT8 = {(byte) 0xFF};
    final public static byte[] MAX_DATA_VALUE_UINT12 = {0x0F, (byte) 0xFF};
    final public static byte[] MAX_DATA_VALUE_UINT16 = {(byte) 0xFF, (byte) 0xFF};
    final public static byte[] MAX_DATA_VALUE_UINT24 = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
    final public static byte[] MAX_DATA_VALUE_UINT32 = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
    final public static byte[] MAX_DATA_VALUE_UINT48 = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
    final public static byte[] MAX_DATA_VALUE_UINT64 = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
    final public static byte[] MAX_DATA_VALUE_UINT128 = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
    final public static byte[] MAX_DATA_VALUE_SINT8 = {(byte) 0x7F};
    final public static byte[] MAX_DATA_VALUE_SINT16 = {(byte) 0x7F, (byte) 0xFF};
    final public static byte[] MAX_DATA_VALUE_SINT24 = {(byte) 0x7F, (byte) 0xFF, (byte) 0xFF};
    final public static byte[] MAX_DATA_VALUE_SINT32 = {(byte) 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
    final public static byte[] MAX_DATA_VALUE_SINT48 = {(byte) 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
    final public static byte[] MAX_DATA_VALUE_SINT64 = {(byte) 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
    final public static byte[] MAX_DATA_VALUE_SINT128 = {(byte) 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};

    public int getStandardAccording() {
        return standardAccording;
    }

    public InputStream getYamlAppearanceValues() throws IOException {
        YamlAppearanceValues.reset();
        return YamlAppearanceValues;
    }

    public InputStream getYamlCharacteristicUuids() throws IOException {
        YamlCharacteristicUuids.reset();
        return YamlCharacteristicUuids;
    }

    public InputStream getYamlServiceUuids() {
        return YamlServiceUuids;
    }

    public InputStream getYamlFormatTypes() throws IOException {
        YamlFormatTypes.reset();
        return YamlFormatTypes;
    }

    public InputStream getYamlAdTypes() throws IOException {
        yamlAdTypes.reset();
        return yamlAdTypes;
    }

    public InputStream getYamlCharacteristicDataBasicType() throws IOException {
        yamlCharacteristicDataBasicType.reset();
        return yamlCharacteristicDataBasicType;
    }
}
