package com.org701enti.frealicane.suit.event

import androidx.compose.runtime.Stable
import com.org701enti.frealicane.MainActivity
import org.greenrobot.eventbus.EventBus

@Stable
open class StableEventBus(private val originEventBus: EventBus) {
    fun post(event: Any) = originEventBus.post(event);
    fun register(subscriber: Any) = originEventBus.register(subscriber);
    fun unregister(subscriber: Any) = originEventBus.unregister(subscriber);

    companion object {
        fun create(eventBus: EventBus) = StableEventBus(eventBus)
    }
}

@Stable
object StableDeviceStateEventBus : StableEventBus(MainActivity.DEVICE_STATE_EVENT_BUS)