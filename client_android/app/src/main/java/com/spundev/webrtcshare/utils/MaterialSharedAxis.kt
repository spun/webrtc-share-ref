package com.spundev.webrtcshare.utils

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseInExpo
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/*
 These functions try to recreate the MaterialSharedAxis transition from material-components.

 MaterialSharedAxis overrides the default interpolator from MaterialVisibility with the attribute
 ?attr/motionEasingEmphasizedInterpolator from material-components. When this interpolator is
 applied to the AnimatorSet with AnimatorSet#setInterpolator, it overrides the interpolator of all
 its child animators.

 Because motionEasingEmphasizedInterpolator is defined using a path with two cubic Bézier curves, we
 don't have an equivalent easing. For our implementation we can use [FastOutSlowInEasing] that also
 speeds up quickly and slows down gradually (see the FadeThroughProvider recreation to know how this
 interpolator is applied).
*/

/**
 * [EnterTransition] that slides and fades in
 */
fun <T> AnimatedContentTransitionScope<T>.materialSharedAxisIn(
    slideDirection: AnimatedContentTransitionScope.SlideDirection,
    durationInMillis: Int = 450,
    // Ask for Density until this issue is fixed (b/272110324)
    density: Density
): EnterTransition {

    val slideDistance = with(density) { 30.dp.roundToPx() }

    // Recreate SlideDistanceProvider from material-components
    val primary = slideIntoContainer(
        towards = slideDirection,
        animationSpec = tween(durationInMillis, 0, FastOutSlowInEasing)
    ) {
        when {
            it > 0 -> slideDistance
            it < 0 -> -slideDistance
            else -> it
        }
    }

    // Recreate FadeThroughProvider (default threshold of 0.35) from material-components
    // The original FadeThroughProvider uses a ValueAnimator to calculate the alpha value for a
    // given progress/fraction. The ValueAnimator will use the motionEasingEmphasizedInterpolator to
    // generate the progress values, but the function used to calculate the values (lerp) has a
    // "threshold" where progress from the start is ignored and the initial value of 0 is returned.
    // Because of this, although the animator interpolator has a curve similar to
    // [FastOutSlowInEasing], the easing of the alpha values is in reality the interpolator with the
    // initial 35% removed.
    // Since our fadeIn accepts a delay, we need to pass an easing that has a curve similar to the
    // remaining 65% of motionEasingEmphasizedInterpolator, and that easing is EaseOutExpo.
    val delayFadeIn = (durationInMillis * 0.35f).toInt()
    val durationFadeIn = durationInMillis - delayFadeIn
    val secondary = fadeIn(
        animationSpec = tween(
            durationMillis = durationFadeIn,
            delayMillis = delayFadeIn,
            easing = EaseOutExpo
        )
    )

    return primary + secondary
}

/**
 * [ExitTransition] that slides and fades out
 */
fun <T> AnimatedContentTransitionScope<T>.materialSharedAxisOut(
    slideDirection: AnimatedContentTransitionScope.SlideDirection,
    durationInMillis: Int = 450,
    // Ask for Density until this issue is fixed (b/272110324)
    density: Density
): ExitTransition {

    val slideDistance = with(density) { 30.dp.roundToPx() }

    // Recreate SlideDistanceProvider from material-components
    val primary = slideOutOfContainer(
        towards = slideDirection,
        animationSpec = tween(durationInMillis, 0, FastOutSlowInEasing)
    ) {
        when {
            it > 0 -> slideDistance
            it < 0 -> -slideDistance
            else -> it
        }
    }

    // Recreate FadeThroughProvider (default threshold of 0.35) from material-components
    // The original FadeThroughProvider uses a ValueAnimator to calculate the alpha value for a
    // given progress/fraction. The ValueAnimator will use the motionEasingEmphasizedInterpolator to
    // generate the progress values, but the function used to calculate the values (lerp) has a
    // "threshold" where progress is ignored when reached and the final value of 0 is returned.
    // Because of this, although the animator interpolator has a curve similar to
    // [FastOutSlowInEasing], the easing of the alpha values is in reality the interpolator with the
    // final 65% removed.
    // Since we are setting a duration with the threshold applied, we need to pass an easing that
    // has a curve similar to the initial 35% of motionEasingEmphasizedInterpolator, and that easing
    // is EaseInExpo.
    val durationFadeOut = (durationInMillis * 0.35f).toInt()
    val secondary = fadeOut(
        animationSpec = tween(
            durationMillis = durationFadeOut,
            delayMillis = 0,
            easing = EaseInExpo
        )
    )

    return primary + secondary
}
