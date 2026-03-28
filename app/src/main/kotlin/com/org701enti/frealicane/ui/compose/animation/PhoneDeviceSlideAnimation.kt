package com.org701enti.frealicane.ui.compose.animation

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * 动画:设备图标固定在底部中央,手机图标水平平滑往复滑动
 * 两端减速实现丝滑效果
 * @param phoneRes 手机图标资源
 * @param deviceRes 设备图标资源
 * @param containerWidthPx 装载动画的容器宽度(像素)
 * @param progressMin 动画进度最小值(0f - 1f),0f->可到达容器最左边
 * @param progressMax 动画进度最大值(0f - 1f),1f->可到达容器最右边
 * @param startDelay 延迟多少毫秒后开始播放
 * @param duration 手机图标往返总时间,动画周期时间
 */


@Composable
fun PhoneDeviceSlideAnimation(
    modifier: Modifier = Modifier,
    phoneRes: Int,
    deviceRes: Int,
    containerWidthPx: Int,
    progressMin: Float = 0f,
    progressMax: Float = 1f,
    startDelay: Int = 1000,
    duration: Int = 4000
) {
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "slide")
        val progress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = duration

                    progressMin at 0
                    progressMax at duration / 4 * 2 using FastOutSlowInEasing

                    progressMax at duration / 4 * 2
                    progressMin at duration using FastOutSlowInEasing

                },
                initialStartOffset = StartOffset(startDelay)
            ),
            label = "progress",
        )

        // 底部设备，居中+下边距
        Image(
            painter = painterResource(deviceRes),
            contentDescription = "Device",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )

        var imageWidthPhone by remember {
            mutableIntStateOf(0)
        }

        // 手机：只根据外部传入的宽度滑动
        Image(
            painter = painterResource(phoneRes),
            contentDescription = "Phone",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .onSizeChanged {
                    imageWidthPhone = it.width
                }
                .offset {
                    IntOffset(
                        x = (progress * containerWidthPx).toInt() - imageWidthPhone / 2,
                        y = 0
                    )
                }
        )
    }
}
