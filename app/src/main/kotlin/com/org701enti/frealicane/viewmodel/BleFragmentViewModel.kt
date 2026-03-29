package com.org701enti.frealicane.viewmodel

import androidx.lifecycle.ViewModel
import com.org701enti.bluetoothfocuser.BluetoothDeviceModel
import java.util.concurrent.atomic.AtomicReference

class BleFragmentViewModel: ViewModel() {
    val scanResultList: MutableList<BluetoothDeviceModel> = ArrayList()
    var isScanningBluetooth = false
    var isAllowNotifyChanged: AtomicReference<Boolean> = AtomicReference(java.lang.Boolean.TRUE)
}