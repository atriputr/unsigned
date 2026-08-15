package com.example.un_signed

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * A rich gesture detector supporting up to two long-press durations plus tap/double-tap.
 *
 * Behaviour:
 *   • hold ≥ [longPress1Ms] → fires [onLongPress1]
 *   • continue holding to ≥ [longPress2Ms] → fires [onLongPress2] (if provided)
 *   • release before [longPress1Ms] → waits [doubleTapWindowMs] to see if another tap arrives
 *       └ second tap → [onDoubleTap]
 *       └ no second tap → [onSingleTap]
 *
 * When a long press fires, the tap detection is suppressed for that gesture cycle.
 */
fun Modifier.quickGestures(
    scope: CoroutineScope,
    doubleTapWindowMs: Long = 280L,
    longPress1Ms: Long = 3000L,
    longPress2Ms: Long? = null,
    onSingleTap: () -> Unit = {},
    onDoubleTap: () -> Unit = {},
    onLongPress1: () -> Unit = {},
    onLongPress2: () -> Unit = {}
): Modifier = this.pointerInput(longPress1Ms, longPress2Ms) {
    // Job used to fire a delayed single-tap (cancelled if a second tap comes in time)
    var pendingSingleTap: Job? = null

    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)

        // Phase 1: wait for release OR for longPress1Ms to elapse
        val phase1Release = withTimeoutOrNull(longPress1Ms) {
            waitForUpOrCancellation()
        }

        if (phase1Release != null) {
            // Released before long-press threshold — treat as a tap
            // Cancel any pending single-tap from prior tap and check for double-tap
            val prior = pendingSingleTap
            if (prior != null && prior.isActive) {
                // This is the 2nd tap arriving within the window → double tap
                prior.cancel()
                pendingSingleTap = null
                onDoubleTap()
            } else {
                pendingSingleTap = scope.launch {
                    delay(doubleTapWindowMs)
                    onSingleTap()
                }
            }
        } else {
            // longPress1 threshold reached — fire it
            onLongPress1()

            // Phase 2 (optional): keep holding for longPress2Ms
            if (longPress2Ms != null && longPress2Ms > longPress1Ms) {
                val remaining = longPress2Ms - longPress1Ms
                val phase2Release = withTimeoutOrNull(remaining) {
                    waitForUpOrCancellation()
                }
                if (phase2Release == null) {
                    onLongPress2()
                    waitForUpOrCancellation()   // wait for final release
                }
            } else {
                waitForUpOrCancellation()
            }
        }
    }
}
