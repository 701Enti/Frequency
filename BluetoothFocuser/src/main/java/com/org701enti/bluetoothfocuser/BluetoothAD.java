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

import java.util.List;

public class BluetoothAD {

    public class AdvertisingStruct{
        private int adType;//AD类型标识值,根据SIG小组的规范,该值标识了下面AdData的唯一含义
        private Byte[] adData;
        private int indexID;//引导ID,由于AD结构地ADtype可重复性,一个广播中可能有多个同一类型的数据段,IndexID标记了本条数据在重复序列的角标(可0)
        private int maxIndexID;//最大引导ID,标记重复序列中最后一个重复数据的IndexID,假如广播数据中只有一个AdType=0x01的数据,那您只能访问一个该类型数据,它的MaxInde=0,IndexID=0
    }

    //主列表
    private List<AdvertisingStruct> MainAdvertisingList;


    public abstract class ArrangeRule(){
//        设置列表排列规则,可以基于以下观测结果()   (并不意味必须改变其在列表的位置)
        //1.规定的ADtype数量多,广播包含的也可能多,在周围蓝牙设备过多的情况下,无规则地保存和检索数据以及对数据过度封装会浪费资源
        //2.关注焦点的有限性,人对于事物特点的关注位置数量是有限的,屏幕单个界面在UI绘制下对数据的显示面积是有限的,广播中实际容易被用户识别的数据很少
        //3.关注焦点的粘滞性,UI对于数据的索取往往专一化,对特定类型数据访问频率非常大,而对其他大部分数据往往置之不理
        //4.用户操作的不定向性,我们不能依照以上发现就否绝低频访问的存在,而是引入某种合适策略来适应用户的意向变化,除非外部程序认为这是非必要的

//        由于规则的主观局限性,没有规则能够适应所有用户场景,因此允许规则自定义,可以在BluetoothAD构造方法中安装自定义规则
        //自定义规则与默认规则的实现思想都是通过一定比较(不一定是focusLevel)挑选高价值数据优先索引,但并不意味必须改变其在列表的位置
        //添加和运行逻辑也一致,主要实现 {广播数据解析成列表后}(生命周期中仅一次) {触发搜索后但实际搜索运行之前}(生命周期中随机可0) {数据搜索完成后}(生命周期中随机可0)

//        搜索方法首先会在规则提供的焦点表中索引,如果没有,会继续遍历整个主列表MainAdvertisingList进行线性搜索
//        因此,规则可以维护一个焦点表,也可以改变数据项在MainAdvertisingList的位置,甚至删除某些数据项,他们本质都是一种排序分级

        /**
         * 广播数据解析成列表后
         */
        public abstract void OnListCreated();

        /**
         * 触发搜索后但实际搜索运行之前
         */
        public abstract void OnSearchStart();

        /**
         * 数据项搜索完成后
         */
        public abstract void OnSearchFinish();
    }



    //默认排列规则之一 排名排序
    public class RankingArrange extends ArrangeRule{
        //排名排序规则(创建需要提供焦点表最大角标即最后入围角标)
        //1.创建MainAdvertisingList即主列表时,在OnListCreated初始化严格定长的焦点表,刚开始焦点表中角标数据全部设置为0
        //2.维护这个焦点表,仅存储关注的AdvertisingStruct对象在主列表的角标位置,在OnSearchStart传递给搜索方法
        //3.当搜索完成,在OnSearchFinish,如果发现搜索到的AdvertisingStruct对象的角标还不在焦点表中,
        //  将其添加到焦点表第一名即角标[0],其他焦点全部退步一名,最后一名即为最后入围角标的在退步一名之后会被挤出焦点表,直到它再次被搜索
        //  如果发现它已经在焦点表中,将其进步一名,即与前一名交换数据,除非它是第一名
    }

    //默认排列规则之一 通报排序
    public class NotifyArrange extends ArrangeRule{
        //通报排序规则(创建需要提供有长度修饰的通报表)
        //1.创建MainAdvertisingList即主列表时,在OnListCreated初始化不限制长度的焦点表,初始化为无元素空集
        //2.维护这个焦点表,仅存储关注的AdvertisingStruct对象在主列表的角标位置,在OnSearchStart传递给搜索方法
        //3.外部类可以通过NotifyAdd和NotifyDelete将某个AdvertisingStruct数据段在主列表MainAdvertisingList的角标在焦点表添加或删除



    }

    public class








    public BluetoothAD(ScanResult result){

    }
}
