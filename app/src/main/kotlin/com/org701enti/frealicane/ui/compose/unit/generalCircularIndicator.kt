package com.org701enti.frealicane.ui.compose.unit

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.org701enti.frealicane.MainActivity
import com.org701enti.frealicane.suit.event.StableEventBus
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * 通用圆形指示器，根据 EventBus 事件状态决定是否显示旋转动画(事件类型必须为 MainActivity.BaseEvent 的实现类,例如 MainActivity.DeviceStateEvent)
 * @param makerId 要监听的事件 makerId，当收到此 makerId 的事件时会检查状态
 * @param makerStateToActive 当事件的 makerState 与此值相同时，指示器开始旋转
 * @param makerStateToStop 当事件的 makerState 与此值相同时，指示器停止旋转
 * @param eventBus 选择需要监听的事件总线
 * @param eventType (可选,不需要可填null)选择仅在为该类型的事件发生时处理,忽略其他类型的事件(例如填入MainActivity.DeviceStateEvent::class.simpleName,仅DeviceStateEvent发生时处理)
 * @param modifier 用于修饰此指示器的布局或样式
 * @param size 指示器的尺寸大小
 * @param strokeWidth 指示器圆形进度条的线宽
 * @param color 指示器的颜色
 */
@Composable
fun GeneralCircularIndicator(
    makerId: String,
    makerStateToActive: Int,
    makerStateToStop: Int,
    eventBus: StableEventBus,
    eventType: String?,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    strokeWidth: Dp = 4.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    var isActive by remember { mutableStateOf(false) }

    LaunchedEffect(eventBus) {
        val channel = Channel<Unit>()

        val subscriber = object {
            @Subscribe
            fun onEvent(event: MainActivity.BaseEvent) {
                if (eventType != null) {
                    if (event.eventType != eventType) return
                }
                if (event.makerId.isNotBlank() && event.makerId == makerId) {
                    if (event.makerState == makerStateToActive) isActive = true
                    if (event.makerState == makerStateToStop) isActive = false
                }
            }
        }
        eventBus.register(subscriber)
        try {
            channel.receive()
        } finally {
            channel.close()
            eventBus.unregister(subscriber)
        }
    }

    if (isActive) {
        CircularProgressIndicator(
            modifier = modifier.size(size),
            strokeWidth = strokeWidth,
            color = color
        )
    } else {
        Spacer(modifier = modifier.size(size))
    }
}