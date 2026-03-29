package com.org701enti.bluetoothfocuser

import android.bluetooth.BluetoothDevice

//蓝牙设备列表模型类
class BluetoothDeviceModel {
    var device: BluetoothDevice? //可能包含设备名称,信号强度等多种数据和方法
        private set

    var deviceSha256: String ////设备的SHA-256唯一性与安全校验码
        private set

    //设备的外观图标ID,其实就是外观值的bit6到bit15
    //详见(2.6.2)https://www.bluetooth.com/wp-content/uploads/Files/Specification/HTML/Assigned_Numbers/out/en/Assigned_Numbers.pdf
    var iconID: Int
        private set

    var deviceDistance: Int //与设备的距离(单位:米)

    var flagRecentScanTime: Long //(标志值,不要用于业务显示)设备最近被扫描到的时间(ms),当扫描操作启动/重启时,他被设置为当前时间

    var flagRecentDistanceUpdateTime: Long //(标志值,不要用于业务显示)设备最近更新距离数据的时间(ms)

    var controlBase: ControlBase? //控制基础(创建GATT连接后获得并绑定到此)

    constructor(device: BluetoothDevice?, deviceDistance: Int, deviceSha256: String) {
        this.device = device
        this.deviceDistance = deviceDistance
        this.flagRecentScanTime = 0
        this.flagRecentDistanceUpdateTime = 0
        this.deviceSha256 = deviceSha256
        this.iconID = 0
        controlBase = null
    }

    constructor(
        device: BluetoothDevice?,
        deviceDistance: Int,
        iconID: Int,
        deviceSha256: String
    ) {
        this.device = device
        this.deviceDistance = deviceDistance
        this.flagRecentScanTime = 0
        this.flagRecentDistanceUpdateTime = 0
        this.deviceSha256 = deviceSha256
        this.iconID = iconID
        controlBase = null
    }
}