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
package com.org701enti.frealicane

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.BaseAdapter
import android.widget.GridView
import androidx.fragment.app.Fragment
import com.org701enti.bluetoothfocuser.BluetoothUI.InnerUiUnit
import com.org701enti.bluetoothfocuser.StandardSync
import com.org701enti.frealicane.MainActivity.ControlBaseBluetooth
import java.net.URI
import java.net.URL

/**
 * A simple [Fragment] subclass.
 * Use the [ControlFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ControlFragment : Fragment() {
    var logTag: String = "ControlFragment"

    //运行需求
    var controlFragmentRunWant: ControlFragmentRunWant? = null

    interface ControlFragmentRunWant {
        var controlBaseListBluetooth: List<ControlBaseBluetooth>
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainActivity) {
            controlFragmentRunWant = context.controlFragmentFunctionRun
        } else {
            throw RuntimeException("must be attached by MainActivity but not by$context")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_control, container, false)
        //初始化其他布局
        initInnerUiConsole(view)
        initWebPageConsole(view)
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        controlFragmentRunWant = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }


    fun consoleShowBluetooth(base: ControlBaseBluetooth?) {
        val bluetoothUI = base?.bluetoothUI
        when (bluetoothUI?.frameworkType) {
            StandardSync.FRAMEWORK_INNER_UI -> {
                setVisibilityInnerUiConsole(View.VISIBLE)
                setVisibilityWebPageConsole(View.GONE)
                bluetoothInnerUiAdapter.selectShowAccording(
                    base.bluetoothUI?.dataList
                )
                setAdapterBluetoothInnerUi()
            }

            StandardSync.FRAMEWORK_OFFLINE_WEB_PAGE -> {
                setVisibilityInnerUiConsole(View.GONE)
                setVisibilityWebPageConsole(View.VISIBLE)
            }

            StandardSync.FRAMEWORK_ONLINE_WEB_PAGE -> {
                setVisibilityInnerUiConsole(View.GONE)
                setVisibilityWebPageConsole(View.VISIBLE)
            }

            else -> {
                setVisibilityInnerUiConsole(View.GONE)
                setVisibilityWebPageConsole(View.GONE)
            }
        }
    }


    //网页控制台相关,包括离线和在线,蓝牙和WIFI控制行为由网页控制
    var webPageConsole: WebView? = null
        private set
    private val bluetoothOfflineWebPageURI: URI? = null
    private val bluetoothOnlineWebPageURL: URL? = null
    private val wifiOfflineWebPageURI: URI? = null
    private val wifiOnlineWebPageURL: URL? = null

    private fun initWebPageConsole(view: View) {
        this.webPageConsole = view.findViewById(R.id.web_page_console_in_control)
        setVisibilityWebPageConsole(View.GONE)
    }

    private fun setVisibilityWebPageConsole(visibility: Int) {
        webPageConsole?.visibility = visibility
    }


    //内部生成式UI控制台相关,可以由蓝牙,WIFI等不同适配器作用
    //这里使用适配器[单例模式]便于管理,如果有新的适配器定义,请在此处初始化一个单例
    private var innerUiConsole: GridView? =
        null //innerUiConsole是一个GridView实例,通过initInnerUiConsole方法设置的父级view取里面的(R.id.InnerUiConsole)
    private val bluetoothInnerUiAdapter = BluetoothInnerUiAdapter() //蓝牙内部生成式UI适配器单例

    /**
     * 初始化innerUiConsole
     * 设置的父级view将被用来获取一个id为(R.id.InnerUiConsole)的GridView,即innerUiConsole(初始化后默认为隐藏显示)
     * @param view 父级view,决定了innerUiConsole显示位置
     */
    private fun initInnerUiConsole(view: View) {
        this.innerUiConsole = view.findViewById(R.id.inner_ui_console_in_control)
        setVisibilityInnerUiConsole(View.GONE)
    }

    /**
     * 设置innerUiConsole的可见性
     * @param visibility 可见性,这是一个枚举,通过View.xxx
     */
    private fun setVisibilityInnerUiConsole(visibility: Int) {
        innerUiConsole?.visibility = visibility
    }

    /**
     * 设置innerUiConsole的适配器为BluetoothInnerUiAdapter
     * 作为一个GridView,innerUiConsole需要适配器才能工作
     */
    private fun setAdapterBluetoothInnerUi() {
        innerUiConsole?.adapter = bluetoothInnerUiAdapter
    }

    ////[GridView适配器]蓝牙内部生成式UI适配器,用于Grid展示的适配
    inner class BluetoothInnerUiAdapter : BaseAdapter() {
        private var unitList : MutableList<Any> = emptyList<Any>().toMutableList()//显示数据列表

        /**
         * 设置显示数据列表
         * [要求列表中每个Object都是BluetoothUI.InnerUiUnit实例,这样每个条目对应一个小控件,即GridView中一个子单元]
         * @param unitList 显示是依据什么数据,提供显示数据列表,可随时切换,切换后立即更新
         */
        fun selectShowAccording(unitList: MutableList<Any>?) {
            this.unitList = (unitList ?: emptyList()).toMutableList()
            this.notifyDataSetChanged()
        }

        override fun getCount(): Int {
            return unitList.size //GridView需要条目数量,条目数量为数据列表中的实例个数
        }

        override fun getItem(position: Int): Any? {
            return null //GridView需要目标位置的条目数据,这里不需要这个功能
        }

        override fun getItemId(position: Int): Long {
            return 0 //GridView需要目标位置的条目id,这里不需要这个功能
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
            //GridView需要目标位置的条目view,这里我们要自己根据数据列表制作view实例
            //[要求列表中每个Object都是BluetoothUI.InnerUiUnit实例]
            //[由于所有控件的视图更新,操作逻辑已经在每个都在makeUnitView注册自己独有的回调,不需要在这里完成]
            //如果已经制作了,使用之前的
            //对每个之前未制作的控件都会制作并设置布局参数
            if(!isAdded) return null
            val unit = unitList[position] as InnerUiUnit
            return convertView ?: unit.makeUnitView(null, false, requireActivity()) //提供view实例
        }
    }


    companion object {
        @JvmStatic
        fun newInstance(): ControlFragment {
            val fragment = ControlFragment()
            val args = Bundle()

            fragment.arguments = args
            return fragment
        }
    }
}