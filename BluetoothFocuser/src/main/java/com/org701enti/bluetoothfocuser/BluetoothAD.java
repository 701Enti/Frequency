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

import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;

import java.util.ArrayList;
import java.util.List;

public class BluetoothAD {

    public class AdvertisingStruct{
        private byte atypiaAdType;//(非标准警告:协议规定为无符号,但是byte是有符号的)AD类型标识值,根据SIG小组的规范,该值标识了下面AdData的唯一含义
        private byte[] adData;//(非标准警告:协议规定为无符号,但是byte是有符号的)AD数据段,数据含义由adType标识
        private int indexID;//(自定义的变量,不是协议栈标准)引导ID,由于AD结构地ADtype可重复性,一个广播中可能有多个同一类型的数据段,IndexID标记了本条数据在重复序列的角标(可0)

        /**
         * (非标准警告)请不要认为这就是真实的ADtype,协议中ADtype是无符号的,不要直接打印和if常量比对
         * @return (非标准警告:协议规定为无符号,但是byte是有符号的)
         */
        public byte getAtypiaAdType() {
            return atypiaAdType;
        }

        /**
         * (标准的,可以直接根据规范映射数值,不用考虑符号)数据项即要搜索的结构的所属ADtype
         * @return (标准的,可以直接根据规范映射数值,不用考虑符号)
         */
        public int getStandardAdType() {
          return getAtypiaAdType() & 0xFF;
        }


        /**
         * (非标准警告)请不要认为这就是真实的ADdata,协议中ADdata可能是无符号的,不要直接打印和if常量比对
         * @return (非标准警告:协议规定为无符号,但是byte是有符号的)
         */
        public byte[] getAdData() {
            return adData;
        }

        /**
         * (非标准警告)请不要认为这就是真实的ADdata,协议中ADdata可能是无符号的,不要直接写入字面值
         * @return (非标准警告:协议规定为无符号,但是byte是有符号的)
         */
        public void setAdData(byte[] adData) {
            this.adData = adData;
        }

        public int getIndexID() {
            return indexID;
        }

        /**
         * 构造方法,创建数据项即新AD结构,但是不会添加到列表,并且填写数据需要外部操作adData
         * @param atypiaAdType (非标准警告:协议规定为无符号,但是byte是有符号的)数据项即要添加的结构的所属AdType
         * @param structList (只读)要加入的列表,只是由于识别,不会真的把创建的结构加入列表
         */
        public AdvertisingStruct(byte atypiaAdType, List<AdvertisingStruct> structList){
            this.atypiaAdType = atypiaAdType;
            this.indexID = AllotIndexID(this.atypiaAdType,structList);
        }

        /**
         * 分配新的IndexID,IndexID从0开始,新的同类添加,需要有一个自增ID,如第一个为0,第二个为1,第三个为2...
         * @param atypiaAdType (非标准警告:协议规定为无符号,但是byte是有符号的)数据项即要添加的结构的所属AdType
         * @param structList 要加入的列表
         * @return 分配的IndexID
         */
        public int AllotIndexID(byte atypiaAdType,List<AdvertisingStruct> structList){
            int ret = 0;
            if(structList != null){
                for(AdvertisingStruct struct : structList){
                    //分配IndexID发生在将新结构写入主列表之前,显然我们发现分配的IndexID其实就是添加之前列表同类的总个数
                    if(struct.atypiaAdType == atypiaAdType){
                        ret++;
                    }
                }
            }
            return ret;
        }
    }


    //      继承该抽象类以创建排列规则类
    public abstract class ArrangeRule{
        /**
         * 广播数据解析成列表后
         */
        public abstract void OnListCreated(List<AdvertisingStruct> list);

        /**
         * 触发搜索后但实际搜索运行之前
         *
         * @return
         */
        public abstract int[] OnSearchStart();

        /**
         * 数据项搜索完成后
         */
        public abstract void OnSearchFinish();
    }

    //      设置列表排列规则,可以基于以下观测结果()   (并不意味必须改变其在列表的位置)
    //1.规定的ADtype数量多,广播包含的也可能多,在周围蓝牙设备过多的情况下,无规则地保存和检索数据以及对数据过度封装会浪费资源
    //2.关注焦点的有限性,人对于事物特点的关注位置数量是有限的,屏幕单个界面在UI绘制下对数据的显示面积是有限的,广播中实际容易被用户识别的数据很少
    //3.关注焦点的粘滞性,UI对于数据的索取往往专一化,对特定类型数据访问频率非常大,而对其他大部分数据往往置之不理
    //4.用户操作的不定向性,我们不能依照以上发现就否绝低频访问的存在,而是引入某种合适策略来适应用户的意向变化,除非外部程序认为这是非必要的
    //       由于规则的主观局限性,没有规则能够适应所有用户场景,因此允许规则自定义,可以在BluetoothAD构造方法中安装自定义规则
    //自定义规则与预设规则的实现思想都是通过一定比较(不一定是focusLevel)挑选高价值数据优先索引,但并不意味必须改变其在列表的位置
    //添加和运行逻辑也一致,主要实现 {广播数据解析成列表后}(生命周期中仅一次) {触发搜索后但实际搜索运行之前}(生命周期中随机可0) {数据搜索完成后}(生命周期中随机可0)
    //       如果已经注册规则,搜索方法首先会在规则提供的焦点表中索引,如果没有发现,或者本来就没有注册规则,会继续遍历整个主列表MainAdvertisingList进行线性搜索
    //因此,规则可以维护一个焦点表,也可以改变数据项在MainAdvertisingList的位置,甚至删除某些数据项,他们本质都是一种排序分级

    //       以下是内部预置规则,外部类创建BluetoothAD对象时需要规则对象注册,可以选择内部预置规则或者自己写规则,当然也可以不注册规则,这样只能线性搜索
    //根据最少依赖原则,请不要在自定义规则使用过于复杂的策略和算法,以及过多的列表引用


    //内部预置规则-排名排序
    //排名排序规则(创建需要提供焦点表最大角标即最后入围角标)
    //1.创建MainAdvertisingList即主列表时,在OnListCreated初始化严格定长的焦点表,刚开始焦点表中角标数据全部设置为0
    //2.维护这个焦点表,仅存储关注的AdvertisingStruct对象在主列表的角标位置,在OnSearchStart传递给搜索方法
    //3.当搜索完成,在OnSearchFinish,如果发现搜索到的AdvertisingStruct对象的角标还不在焦点表中,
    //  将其添加到焦点表第一名即角标[0],其他焦点全部退步一名,最后一名即为最后入围角标的在退步一名之后会被挤出焦点表,直到它再次被搜索
    //  如果发现它已经在焦点表中,将其进步一名,即与前一名交换数据,除非它是第一名
//    public class RuleRankingArrange extends ArrangeRule{
//        /**
//         * 广播数据解析成列表后
//         */
//        @Override
//        public void OnListCreated(List<AdvertisingStruct> list) {
//
//        }
//
//        /**
//         * 触发搜索后但实际搜索运行之前
//         *
//         * @return
//         */
//        @Override
//        public int[] OnSearchStart() {
//
//        }
//
//        /**
//         * 数据项搜索完成后
//         */
//        @Override
//        public void OnSearchFinish() {
//
//        }
//    }

    //内部预置规则-通报排序
    //通报排序规则(创建需要提供有长度修饰的通报表)
    //1.创建MainAdvertisingList即主列表时,在OnListCreated初始化不限制长度的焦点表,初始化为无元素空集
    //2.维护这个焦点表,仅存储关注的AdvertisingStruct对象在主列表的角标位置,在OnSearchStart传递给搜索方法
    //3.外部类可以通过NotifyAdd和NotifyDelete将某个AdvertisingStruct数据段在主列表MainAdvertisingList的角标在焦点表添加或删除
//    public class RuleNotifyArrange extends ArrangeRule{
//        /**
//         * 广播数据解析成列表后
//         */
//        @Override
//        public void OnListCreated(List<AdvertisingStruct> list) {
//
//        }
//
//        /**
//         * 触发搜索后但实际搜索运行之前
//         *
//         * @return
//         */
//        @Override
//        public int[] OnSearchStart() {
//
//        }
//
//        /**
//         * 数据项搜索完成后
//         */
//        @Override
//        public void OnSearchFinish() {
//
//        }
//    }

    /**
     * 抽取蓝牙广播数据包的内容整合并添加到结构列表中
     * @param rawCode 蓝牙广播数据包内容
     * @param structList 选中的结构列表
     */
    public void RawCodeAddToStructList(byte[] rawCode,List<AdvertisingStruct> structList){
        if(rawCode == null || structList == null) {
            return;
        }
        if(rawCode.length == 0){
            return;
        }
        //以下业务逻辑涉及蓝牙协议栈的AD广播包结构定义
        AdvertisingStruct structBuf = null;//缓存新结构的引用
        List<Byte> dataList = null;
        byte length = 0;//表示接下来adType和adData的总长度
        int  indexAdType = 0;//表示接下来adType的index
        int  dataCounter = 0;//用于处理adData的存储计数
        //遍历整个rawCode
        for(int index = 0;index < rawCode.length;index++){
            if(length == 0){
                //发现length=0确定本位一定表示length字段基于以下事实
                //1.由于length表示这个结构接下来adType和adData的总长度,adType必须存在并占用1字节,
                //  所以实际数据中的length=0是禁止的,及时真的存在这种情况,
                //  我们发现它会保持length=0,直到有效结构出现,这样不会产生任何影响
                //2.在解析完一个AD结构后,程序会复位length=0
                //3.在解析完一个AD结构后,如果此时遍历继续活跃,那么接下来一定有一个将要解析的完整结构,以 length表达 开头
                //4.所以开始解析新结构时,length=0是必然的,此时index标识字节的含义为length也是必然的
                //但是如果广播数据包被恶意篡改或编码错误,会引起无法解析或乱码,这种情况一般是人为的,不予考虑
                length = rawCode[index];
                indexAdType = index + 1;//对于正常结构,length后面必定为AdType,即下一个index位置
            }
            else{
                //综上,这里index一定标识的是adType或adData
                //  length表达他们的总长度,但是如果在else最后加上length--;那么length将表示未处理字节数,
                //因为我们每次index引用一个字节,处理一个字节,length每次减一个,现在就表示还有多少要处理的
                //  表示还有多少要处理的肯定可以为0,因为我们有最后一次,那恰好我们又隐式地复位了length = 0
                //  这样我们可以提高程序的复用性,也不需要任何其他缓存处理什么时候一个结构解析完成即处理其与index的偏移关系
                //而且length不可能被减成负数,因为在else保证不为0,length读到的时候就为负数也是不现实的
                if(index == indexAdType){//如果现在标识的是AdType
                    structBuf = new AdvertisingStruct(rawCode[index],structList);//创建新结构并申请一个indexID标识,同AdType加入新结构
                    dataList = new ArrayList<>();
                }
                else{//如果现在标识的是AdData
                    dataList.add(rawCode[index]);
                    dataCounter++;
                }
                length--;//隐式地复位了length = 0
                if(length == 0){

                    //保存adData
                    byte[] byteData = new byte[dataList.size()];
                    for (int i=0;i<dataList.size();i++){
                        byteData[i] = dataList.get(i);
                    }
                    structBuf.setAdData(byteData);

                    //复位其他变量
                    dataCounter = 0;

                    //完成一个结构解析后,将其加入到列表
                    structList.add(structBuf);
                }
            }
        }
    }


    //主列表
    private List<AdvertisingStruct> MainAdvertisingList = new ArrayList<>();
    //排列规则,可能调用构造方法时注册为null,这是允许的
    ArrangeRule arrangeRule = null;

    /**
     * BluetoothAD的构造方法
     * @param scanResult 扫描结果,会从其中读取蓝牙广播数据
     * @param arrangeRule 排列规则实例,用于提供搜索效率,填写null以不使用排列规则,键入Rule...查看内部预置规则类,继承ArrangeRule以创建自定义排列规则类
     */
    public BluetoothAD(ScanResult scanResult, ArrangeRule arrangeRule){
        //注册规则,可能调用时注册为null,这是允许的
        this.arrangeRule = arrangeRule;

        //解析蓝牙广播数据并缓存到主列表
        if(scanResult != null){
            ScanRecord scanRecord = scanResult.getScanRecord();
            if(scanRecord != null){
                byte[] rawCode = scanRecord.getBytes();
                RawCodeAddToStructList(rawCode,MainAdvertisingList);
            }
        }

        //如果this.arrangeRule实例不为空,调用OnListCreated()实现
        if(this.arrangeRule != null){
            this.arrangeRule.OnListCreated(MainAdvertisingList);
        }
    }

    /**
     * 搜索需求的结构(含indexID索引)适用明确了搜索的adType可能存在多个数据段,并且严格要求准确性
     * @param standardAdType (标准的,可以直接根据规范映射数值,不用考虑符号)数据项即要搜索的结构的所属ADtype
     * @param indexID 一个广播中可能有多个同一AdType的数据段,IndexID标记了本条数据在重复序列的角标(可0),即同类的中的区分标记
     * @return null 遇到错误或没有完全匹配的发现 / 搜索结果即AdvertisingStruct实例
     */
    public AdvertisingStruct Search(int standardAdType,int indexID){
       
        //将标准AdType数据转换为非标准的byte
        if(standardAdType < 0 || standardAdType > 255){
            return null;
        }
        byte atypiaAdType = (byte)standardAdType;
        
        //如果已经设置了排序规则,先索引排序规则给出的焦点表
        if(this.arrangeRule != null){
            int[] focusList = arrangeRule.OnSearchStart();
            if(focusList != null){
                AdvertisingStruct focusStruct;
                for (int focus : focusList){
                    try {
                        focusStruct = MainAdvertisingList.get(focus);
                        if(focusStruct.getAtypiaAdType() == atypiaAdType && focusStruct.getIndexID() == indexID){
                            return focusStruct;
                        }
                    }
                    catch (IndexOutOfBoundsException e){
                        return null;
                    }
                }
            }
        }

        //没有设置排序规则或者没有在焦点表的索引下发现,线性搜索
        for(AdvertisingStruct struct : MainAdvertisingList){
            if(struct.getAtypiaAdType() == atypiaAdType && struct.getIndexID() == indexID){
                return struct;
            }
        }

        //还没有搜索到,返回空
        return null;
    }

    /**
     * 搜索需求的结构(无indexID索引),适用明确了搜索的adType只存在单个数据段,如果有多个同一AdType的数据段,谁先被搜索到谁输出
     *
     * @param standardAdType (标准的,可以直接根据规范映射数值,不用考虑符号)数据项即要搜索的结构的所属ADtype
     * @return null 遇到错误或没有匹配的发现,尽管没有indexID的索引 / 搜索结果即AdvertisingStruct实例
     */
    public AdvertisingStruct Search(int standardAdType){

        //将标准AdType数据转换为非标准的byte
        if(standardAdType < 0 || standardAdType > 255){
            return null;
        }
        byte atypiaAdType = (byte)standardAdType;

        //如果已经设置了排序规则,先索引排序规则给出的焦点表
        if(this.arrangeRule != null){
            int[] focusList = arrangeRule.OnSearchStart();
            if(focusList != null){
                AdvertisingStruct focusStruct;
                for (int focus : focusList){
                    try {
                        focusStruct = MainAdvertisingList.get(focus);
                        if(focusStruct.getAtypiaAdType() == atypiaAdType){
                            return focusStruct;
                        }
                    }
                    catch (IndexOutOfBoundsException e){
                        return null;
                    }
                }
            }
        }

        //没有设置排序规则或者没有在焦点表的索引下发现,线性搜索
        for(AdvertisingStruct struct : MainAdvertisingList){
            if(struct.getAtypiaAdType() == atypiaAdType){
                return struct;
            }
        }

        //还没有搜索到,返回空
        return null;
    }
}
