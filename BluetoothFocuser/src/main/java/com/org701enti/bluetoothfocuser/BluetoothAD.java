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

import android.bluetooth.le.ScanResult;

public class BluetoothAD {

    public class AdvertisingStruct{
        private int adType;//AD类型标识值,根据SIG小组的规范,该值标识了下面AdData的唯一含义
        private Byte[] adData;
        private int indexID;//引导ID,由于AD结构地ADtype可重复性,一个广播中可能有多个同一类型的数据段,IndexID标记了本条数据在重复序列的角标(可0)
        private int maxIndexID;//最大引导ID,标记重复序列中最后一个重复数据的IndexID,假如广播数据中只有一个AdType=0x01的数据,那您只能访问一个该类型数据,它的MaxInde=0,IndexID=0
        private int focusLevel;//关注水平,某个AdvertisingStruct结构数据的被重视程度越高,关注水平值的数值越大,数值越大的AdvertisingStruct数据段在搜索时更加优先

        //设置关注水平排列规则,主要可以基于以下观测结果
        //1.规定的ADtype数量多,广播包含的也可能多,在周围蓝牙设备过多的情况下,无规则地保存和检索数据以及对数据过度封装会浪费资源
        //2.关注焦点的有限性,人对于事物特点的关注位置数量是有限的,屏幕单个界面在UI绘制下对数据的显示面积是有限的,广播中实际容易被用户识别的数据很少
        //3.关注焦点的粘滞性,UI对于数据的索取往往专一化,对特定类型数据访问频率非常大,而对其他大部分数据往往置之不理
        //4.用户操作的不定向性,我们不能依照以上发现就否绝低频访问的存在,而是引入排名策略来适应用户的意向变化
        //由于规则的主观局限性,没有规则能够适应所有用户场景,因此引入规则自定义策略,可以在BluetoothAD构造方法中安装自定义规则
        //自定义规则与默认规则的实现思想都是通过一定比较(不一定是focusLevel)挑选高价值数据放在列表之前
        //添加和运行逻辑也一致,主要实现 {广播数据解析后}(生命周期中仅一次) {触发搜索后但实际搜索运行之前}(生命周期中随机可0) {数据搜索完成后}(生命周期中随机可0)


    }


    public BluetoothAD(ScanResult result){

    }
}
