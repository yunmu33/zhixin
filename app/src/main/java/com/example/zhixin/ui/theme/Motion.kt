package com.example.zhixin.ui.theme

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/** 页面切换一趟用多久（毫秒）。 */
private const val PAGE_MS = 250

/** 日历翻页 / 换一天用的时长，比整页切换再快一点。 */
private const val DATE_MS = 220

/**
 * 页面之间的滑动切换：
 * - forward = true 进新页（比如主页 → 设置）：新页从右边滑进来，旧页往左挪一点淡出
 * - forward = false 退回上一页：反过来走
 */
fun pageTransition(forward: Boolean): ContentTransform = ContentTransform(
    targetContentEnter = slideInHorizontally(tween(PAGE_MS)) {
        if (forward) it / 3 else -it / 5
    } + fadeIn(tween(PAGE_MS)),
    initialContentExit = slideOutHorizontally(tween(PAGE_MS)) {
        if (forward) -it / 5 else it / 3
    } + fadeOut(tween(PAGE_MS))
)

/**
 * 底部四个 tab 之间的切换：轻轻横移一点点 + 淡入淡出，
 * 往右点就往右滑（跟手指的方向一致）。
 */
fun tabTransition(toRight: Boolean): ContentTransform = ContentTransform(
    targetContentEnter = slideInHorizontally(tween(PAGE_MS)) {
        if (toRight) it / 8 else -it / 8
    } + fadeIn(tween(PAGE_MS)),
    initialContentExit = slideOutHorizontally(tween(PAGE_MS)) {
        if (toRight) -it / 8 else it / 8
    } + fadeOut(tween(PAGE_MS))
)

/**
 * 日历 / 日期切换：轻轻横移一点点 + 淡入淡出。
 * forward = 往后翻一天（或后一个月 / 下一年）就往左滑，往前翻就往右滑，跟手指方向对上。
 */
fun dateTransition(forward: Boolean): ContentTransform = ContentTransform(
    targetContentEnter = slideInHorizontally(tween(DATE_MS)) {
        if (forward) it / 6 else -it / 6
    } + fadeIn(tween(DATE_MS)),
    initialContentExit = slideOutHorizontally(tween(DATE_MS)) {
        if (forward) -it / 6 else it / 6
    } + fadeOut(tween(DATE_MS))
)

/** 只是淡入淡出，不做位移（锁屏页那种）。 */
fun fadeTransition(): ContentTransform = ContentTransform(
    targetContentEnter = fadeIn(tween(PAGE_MS)),
    initialContentExit = fadeOut(tween(PAGE_MS))
)

/**
 * 弹窗冒出来的样子：从稍小一点点放大到原样，同时淡入。
 * 给 AlertDialog 的 modifier 用就行，不用改弹窗里的布局。
 */
@Composable
fun dialogEnter(): Modifier {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.92f,
        animationSpec = tween(220),
        label = "dialogScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(170),
        label = "dialogAlpha"
    )
    return Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.alpha = alpha
    }
}

/**
 * 铺满屏的覆盖层（心情选择、记一记、看日记这些）冒出来的样子：
 * 从右边轻轻滑进来一点点，同时淡入。
 */
@Composable
fun overlayEnter(): Modifier {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val offset by animateFloatAsState(
        targetValue = if (visible) 0f else 0.06f,
        animationSpec = tween(240),
        label = "overlayOffset"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(200),
        label = "overlayAlpha"
    )
    return Modifier.graphicsLayer {
        translationX = offset * size.width
        this.alpha = alpha
    }
}

/** 弹窗里的内容：稍微往上浮一点，比整块弹窗慢半拍。 */
@Composable
fun dialogContentEnter(): EnterTransition =
    fadeIn(tween(220, delayMillis = 60)) +
        slideInVertically(tween(260, delayMillis = 40)) { it / 6 }

/**
 * 日历 / 列表里新出现的内容（记完心情后的表情、写完的日记、新打卡记录）：
 * 从小一点点弹出来，同时淡入，看着是「冒出来」而不是凭空蹦出来。
 */
@Composable
fun popIn(delayMs: Int = 0): Modifier {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.4f,
        animationSpec = tween(320, delayMillis = delayMs),
        label = "popScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(260, delayMillis = delayMs),
        label = "popAlpha"
    )
    return Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.alpha = alpha
    }
}
