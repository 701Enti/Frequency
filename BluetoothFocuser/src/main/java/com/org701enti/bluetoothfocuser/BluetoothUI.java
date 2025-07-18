package com.org701enti.bluetoothfocuser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothUI {

    String TAG = new String("BluetoothUI");

    private int frameworkType;//控制框架类型,通过StandardSync.FRAMEWORK_ ... 以继续选择
    private List<Object> dataList = new ArrayList<>();//数据列表,可以是列表InnerUiUnit等,视frameworkType而定
    private BluetoothControl bluetoothControl = null;
    private URI uriLink = null;//uri链接
    private URL urlLink = null;//url链接

    //未知的控制方式 0
    public final static int CONTROL_WAY_UNKNOWN = 0;

    //输入式 1 - 10
    public final static int CONTROL_WAY_INPUT_BYTE = 1;//输入数据大小占一个字节
    public final static int CONTROL_WAY_INPUT_BYTES = 2;//输入数据大小占多个字节
    public final static int CONTROL_WAY_INPUT_TEXT = 3;//输入文本

    //结构式 11 - 20
    public final static int CONTROL_WAY_STRUCT = 11;//结构化数据

    //按钮式 21 - 30
    public final static int CONTROL_WAY_BUTTON_ONE = 21;//一个按钮
    public final static int CONTROL_WAY_BUTTONS_MAP_BYTE_BIN = 22;//一组按钮,只控制一个字节,每个按钮对应一个位
    public final static int CONTROL_WAY_BUTTONS_MAP_BYTES_BIN = 23;//一组按钮,控制多个字节,每个按钮对应一个位

    //开关式 41 - 50
    public final static int CONTROL_WAY_SWITCH_ONE = 41;//一个开关
    public final static int CONTROL_WAY_SWITCHES_MAP_BYTE_BIN = 42;//一组开关,只控制一个字节,每个开关对应一个位
    public final static int CONTROL_WAY_SWITCHES_MAP_BYTES_BIN = 43;//一组开关,控制多个字节,每个开关对应一个位

    //选项式 61 - 70
    public final static int CONTROL_WAY_SELECT_PATTERN_VALUE = 61;//选择预设的典型值

    //滑动式 81 - 90
    public final static int CONTROL_WAY_SLIDE_FREE_FADER_ONE = 81;//一个自由推子

    /**
     * 内部生成式UI框架构造方法(frameworkType = FRAMEWORK_INNER_UI - 通过基本控制模型列表生成UI数据)
     *
     * @param bluetoothControl BluetoothControl实例
     * @param context          上下文
     */
    public BluetoothUI(@NonNull BluetoothControl bluetoothControl, Context context) {
        this.frameworkType = StandardSync.FRAMEWORK_INNER_UI;
        this.bluetoothControl = bluetoothControl;
        //尝试根据BluetoothControl数据,创建数据列表,并生成UI及其控制绑定
        for (int i = 0; i <= this.bluetoothControl.getMaxIndex(); i++) {
            ControlBasicModelBluetooth controlModel = bluetoothControl.search(i);
            if (controlModel != null) {
                BluetoothGuess guess = this.bluetoothControl.getBluetoothGuess();
                int controlWay = guess.controlWayByDataType(controlModel.getDataType());//通过DataType猜测controlWay
                int serviceId = guess.serviceId(controlModel.getUuidService());//推测服务ID
                int characteristicId = guess.characteristicId(controlModel.getUuidCharacteristic());//推测特征ID
                //生成对应UI控制单元
                InnerUiUnit unit = new InnerUiUnit(i,controlWay,serviceId,characteristicId);
                //保存到数据列表
                this.dataList.add(i, unit);
            }
        }

        if(bluetoothControl.getDeviceName()!=null){
            Log.i(TAG, "就绪,使用内部生成式UI,共计 "+dataList.size()+" 个控件单元 - " +bluetoothControl.getDeviceName());
        }
        else {
            Log.i(TAG, "就绪,使用内部生成式UI,共计 "+dataList.size()+" 个控件单元 - " +bluetoothControl.getDeviceSha256());
        }
    }

    /**
     * 本地离线网页UI框架构造方法(frameworkType = FRAMEWORK_OFFLINE_WEB_PAGE 通过URI载入UI数据)
     *
     * @param bluetoothControl BluetoothControl实例
     * @param uriLink          uri链接
     */
    public BluetoothUI(@NonNull BluetoothControl bluetoothControl, URI uriLink) {
        this.frameworkType = StandardSync.FRAMEWORK_OFFLINE_WEB_PAGE;
        this.bluetoothControl = bluetoothControl;


    }

    /**
     * 在线互联网网页UI框架构造方法(frameworkType = FRAMEWORK_ONLINE_WEB_PAGE 通过URL载入UI数据)
     *
     * @param bluetoothControl BluetoothControl实例
     * @param urlLink          url链接
     */
    public BluetoothUI(@NonNull BluetoothControl bluetoothControl, URL urlLink) {
        this.frameworkType = StandardSync.FRAMEWORK_ONLINE_WEB_PAGE;
        this.bluetoothControl = bluetoothControl;


    }

    /**
     * (请使用UI线程调用)配置自由单个推子控件的视图,包括与BluetoothControl的回调式调度配置
     *
     * @param view    自由单个推子控件的视图,需要外部预先通过inflater加载好XML视图
     * @param unit    这个控件的数据保存在的InnerUiUnit实例
     * @param context 上下文,需要是有Assets访问能力的Activity Fragment等,Assets需要有特定文件资源提供
     */
    public void ViewConfigSlideFreeFaderOne(View view, InnerUiUnit unit, Context context) {
        if (view == null || unit == null || context == null) {
            return;
        }
        //获取数据源
        ControlBasicModelBluetooth model = this.bluetoothControl.search(unit.getIndexControlModel());
        if (model == null) {
            return;
        }
        //获取控件引用
        SeekBar faderSeekBar = view.findViewById(R.id.ControlFaderSeekBar);
        ImageView targetIcon = view.findViewById(R.id.ControlTargetIcon);
        if (faderSeekBar == null || targetIcon == null) {
            return;
        }
        //根据DataList保存的内容更新视图
        //设置图标
        Bitmap bitmapTargetIcon = null;
        try (InputStream iconInput = context.getAssets().open("bluetoothserviceicon/btsu" + unit.getServiceId() + ".png")) {
            bitmapTargetIcon = BitmapFactory.decodeStream(iconInput);
        } catch (IOException e) {
            return;
        }
        if (bitmapTargetIcon != null) {
            targetIcon.setImageBitmap(bitmapTargetIcon);
        }
        //设置推子相关
        byte[] minData = model.getMinDataValue();
        byte[] maxData = model.getMaxDataValue();
        byte[] nowData = model.getDataBytes();
        if (minData != null && maxData != null && nowData != null) {
            BigInteger lower;
            BigInteger upper;
            BigInteger nowValue;
            if(StandardSync.stringOf(model.getDataType()).startsWith(StandardSync.PREFIX_SHOW_DATA_TYPE_IS_UINT)){
                lower = new BigInteger(1,minData);
                upper = new BigInteger(1,maxData);
                nowValue = new BigInteger(1,nowData);
            }
            else{
                lower = new BigInteger(minData);
                upper = new BigInteger(maxData);
                nowValue = new BigInteger(nowData);
            }

            if (upper.compareTo(lower) >= 0 && nowValue.compareTo(lower) >= 0 && nowValue.compareTo(upper) <= 0) {
                BigDecimal range = new BigDecimal(upper.subtract(lower));//计算最小到最大的区间长度
                //设置推子显示当前数据指代的进度
                BigInteger offset = nowValue.subtract(lower);//当前的值的偏移量
                BigDecimal percentage = new BigDecimal(offset)
                        .divide(range, 10, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                faderSeekBar.setProgress(percentage.toBigInteger().intValue(), true);
                //设置推子的用户操作配置
                ControlBasicModelBluetooth finalModel = model;
                BigInteger finalLower = lower;
                BigInteger finalUpper = upper;
                faderSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            BigDecimal setRate = new BigDecimal(progress)
                                    .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
                            BigInteger setValue = setRate.multiply(range).toBigInteger().add(finalLower);
                            if (setValue.compareTo(finalLower) >= 0 && setValue.compareTo(finalUpper) <= 0) {
                                byte[] bytesValue = setValue.toByteArray();
                                if(bytesValue!=null){
                                    //如果bytesValue前部含0x00,可能是由于.toByteArray()为避免输出被识别为负数所致,这里不需要这些额外添加的前缀0x00,而应该截去
                                    bluetoothControl.controlWrite(finalModel,StandardSync.cutZeroInTheFrontOf(bytesValue));
                                }
                            }
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
            }
        }
    }


    /**
     * 内部生成式UI单元,一个小控件对应一个单元,生成式UI由多个单元组成
     */
    public class InnerUiUnit {
        private int indexControlModel;//控制模型的索引位置,用于在BluetoothControl实例获取控制模型
        private int controlWay;//控制方式

        private int serviceId;//服务ID(规定值为其服务简化16位服务UUID的值减去标准定义的第一个服务的简化16位服务UUID的值)
        private int characteristicId;//特征ID(规定值为其特征简化16位特征UUID的值减去标准定义的第一个特征的简化16位服务UUID的值)

        private final static int DEFAULT_SERVICE_ID = 48;
        private final static int DEFAULT_CHARACTERISTIC_ID = 0;


        /**
         * 构造方法
         *
         * @param indexControlModel    控制模型的索引位置,用于在BluetoothControl实例获取控制模型
         * @param controlWay           控制方式ID(推子,按钮,开关等)
         * @param serviceId        服务ID(规定值为服务简化16位服务UUID的值减去标准定义的第一个服务的简化16位服务UUID的值)
         * @param characteristicId 特征ID(规定值为特征简化16位服务UUID的值减去标准定义的第一个特征的简化16位服务UUID的值)
         */
        public InnerUiUnit(int indexControlModel, int controlWay, int serviceId, int characteristicId) {
            this.indexControlModel = indexControlModel;
            this.controlWay = controlWay;
            this.serviceId = serviceId;
            this.characteristicId = characteristicId;
        }

        /**
         * 通过自身数据制作对应内部生成式UI单元的View视图
         *
         * @param root         需要附加到的根视图,不需要时可以为null
         * @param attachToRoot 是否需要附加到根视图,当root==null时,此参数无效
         * @param context      上下文,需要有提供LayoutInflater实例的能力,不需要相关布局XML提供的能力
         * @return 内部生成式UI单元的View视图
         */
        @SuppressLint("ResourceType")
        @Nullable
        public View makeUnitView(@Nullable ViewGroup root, boolean attachToRoot, Context context) {
            View unitView = null;
            LayoutInflater inflater = LayoutInflater.from(context);
            switch (this.getControlWay()) {
//                    case CONTROL_WAY_INPUT_BYTE -> {
//
//                    }
//                    case CONTROL_WAY_INPUT_BYTES -> {
//
//                    }
//                    case CONTROL_WAY_INPUT_TEXT -> {
//
//                    }
//                    case CONTROL_WAY_STRUCT ->{
//
//                    }
//                    case CONTROL_WAY_BUTTON_ONE ->{
//
//                    }
//                    case CONTROL_WAY_BUTTONS_MAP_BYTE_BIN ->{
//
//                    }
//                    case CONTROL_WAY_BUTTONS_MAP_BYTES_BIN ->{
//
//                    }
//                    case CONTROL_WAY_SWITCH_ONE ->{
//
//                    }
//                    case CONTROL_WAY_SWITCHES_MAP_BYTE_BIN ->{
//
//                    }
//                    case CONTROL_WAY_SWITCHES_MAP_BYTES_BIN ->{
//
//                    }
//                    case CONTROL_WAY_SELECT_PATTERN_VALUE ->{
//
//                    }
                case CONTROL_WAY_SLIDE_FREE_FADER_ONE -> {
                    unitView = inflater.inflate(R.layout.fader_show_control, root, attachToRoot);
                    ViewConfigSlideFreeFaderOne(unitView, this, context);
                }
                default -> {
                    unitView = new TextView(context);
                }
            }
            return unitView;
        }

        public int getIndexControlModel() {
            return indexControlModel;
        }

        public void setControlWay(int controlWay) {
            this.controlWay = controlWay;
        }

        public void setServiceId(int serviceId) {
            this.serviceId = serviceId;
        }

        public void setCharacteristicId(int characteristicId) {
            this.characteristicId = characteristicId;
        }

        public int getControlWay() {
            return controlWay;
        }

        public int getServiceId() {
            return serviceId;
        }

        public int getCharacteristicId() {
            return characteristicId;
        }
    }


    public int getFrameworkType() {
        return frameworkType;
    }

    public List<Object> getDataList() {
        return dataList;
    }
}
