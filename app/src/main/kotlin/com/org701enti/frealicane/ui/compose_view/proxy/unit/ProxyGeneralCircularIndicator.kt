package com.org701enti.frealicane.ui.compose_view.proxy.unit
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import com.org701enti.frealicane.suit.event.StableEventBus
import com.org701enti.frealicane.ui.compose.unit.GeneralCircularIndicator

object ProxyGeneralCircularIndicator{
    @JvmStatic
    fun setContent(
        view: ComposeView,
        makerId: String,
        makerStateToActive: Int,
        makerStateToStop: Int,
        eventBus: StableEventBus,
    ) {
        view.setContent {
            GeneralCircularIndicator(
                makerId = makerId,
                makerStateToActive = makerStateToActive,
                makerStateToStop = makerStateToStop,
                eventBus = eventBus
            )
        }
    }
}