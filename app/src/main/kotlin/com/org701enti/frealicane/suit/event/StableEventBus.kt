package com.org701enti.frealicane.suit.event

import androidx.compose.runtime.Stable
import com.org701enti.frealicane.MainActivity
import org.greenrobot.eventbus.EventBus

@Stable
class StableEventBus(private val eventBus: EventBus) {
    fun post(event: Any) = eventBus.post(event);
    fun register(subscriber: Any) = eventBus.register(subscriber);
    fun unregister(subscriber: Any) = eventBus.unregister(subscriber);

    companion object {
        fun create(eventBus: EventBus) = StableEventBus(eventBus)
    }
}

@Stable
object StableDeviceStateEventBus {
    val instance = StableEventBus.create(MainActivity.DEVICE_STATE_EVENT_BUS)
}