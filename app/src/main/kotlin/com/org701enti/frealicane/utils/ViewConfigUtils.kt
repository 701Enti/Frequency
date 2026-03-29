package com.org701enti.frealicane.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.TypedValue
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.org701enti.bluetoothfocuser.BluetoothDeviceModel
import com.org701enti.frealicane.R
import java.io.IOException

class ViewConfigUtils {

    companion object{
        /***
         * 配置DeviceIcon组件以展示需要的内容
         * @param targetModel 选择数据来源的BluetoothDeviceModel实例
         * @param deviceIcon 对该ImageView实例执行配置,会根据深色模式设置切换原图颜色
         * @param context
         */
        fun configImageViewDeviceIcon(
            targetModel: BluetoothDeviceModel,
            deviceIcon: ImageView?,
            context: Context
        ) {
            val iconID = targetModel.iconID
            var iconBitmap: Bitmap? = null
            try {
                context.assets.open("bluetoothdeviceicon/btac$iconID.png")
                    .use { iconInput ->
                        iconBitmap = BitmapFactory.decodeStream(iconInput)
                        if (iconBitmap == null) {
                            deviceIcon?.setImageResource(R.drawable.ble)
                        }
                    }
            } catch (e: IOException) {
                deviceIcon?.setImageResource(R.drawable.ble)
            }
            if (iconBitmap != null) {
                deviceIcon?.setImageBitmap(iconBitmap)
            }

            //切换颜色
            deviceIcon?.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.icon_color)
            )
        }

        /***
         * 配置DeviceName组件以展示需要的内容,可以和calculateSuitableTextSizeSp()连用
         * @param targetModel 选择数据来源的BluetoothDeviceModel实例
         * @param view 对该TextView实例执行配置
         * @param sizeSP 字体大小,单位sp
         * @param context
         */
        @SuppressLint("MissingPermission")
        fun configTextViewDeviceName(
            targetModel: BluetoothDeviceModel,
            view: TextView?,
            sizeSP: Float,
            context: Context
        ) {
            val name: String? = targetModel.device?.name
            if (name == null) {
                view?.text = context.getString(R.string.unknown_device_chinese)
            } else {
                view?.text = name
            }
            //确定显示的字符尺寸
            view?.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSP)
        }

        /***
         * 计算字体合适尺寸大小
         * @param length 文字长度
         * @return 字体大小,单位sp
         */
        fun calculateSuitableTextSizeSp(length: Int): Float {
            return if (length <= 12) {
                24f - 4f * 1
            } else if (length <= 16) {
                24f - 4f * 2
            } else if (length <= 32) {
                24f - 4f * 3
            } else if (length <= 64) {
                24f - 4f * 4
            } else {
                24f - 4f * 5
            }
        }
    }
}