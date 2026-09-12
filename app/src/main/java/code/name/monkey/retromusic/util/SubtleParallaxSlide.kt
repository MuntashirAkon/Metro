package code.name.monkey.retromusic.util

import android.animation.Animator
import android.animation.ObjectAnimator
import android.view.View
import android.view.ViewGroup
import androidx.transition.TransitionValues
import androidx.transition.Visibility

/**
 * Transition بديلة لـ MaterialSharedAxis، بتحرك الـ View مسافة بسيطة أفقيًا
 * (Parallax خفيف) من غير ما تلمس الـ alpha خالص.
 *
 * الهدف: نستخدمها على الصفحة اللي *تحت* (اللي مش بتخرج من الشاشة فعليًا)
 * عشان تدي إحساس حركة/عمق من غير أي خطر إن الشفافية تكشف اللي وراها.
 *
 * forward = true  => بتتحرك لليسار وقت الدخول للصفحة الجديدة (زي الاتجاه في X)
 * forward = false => بتتحرك لليمين وقت الرجوع
 */
class SubtleParallaxSlide @JvmOverloads constructor(
    private val forward: Boolean = true,
    private val distancePx: Float = 0f
) : Visibility() {

    private fun resolveDistance(sceneRoot: ViewGroup): Float {
        // لو مفيش مسافة متحددة، نستخدم 15% من عرض الشاشة كقيمة افتراضية معقولة
        return if (distancePx > 0f) distancePx else sceneRoot.width * 0.15f
    }

    override fun onAppear(
        sceneRoot: ViewGroup,
        view: View,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ): Animator {
        val distance = resolveDistance(sceneRoot)
        val startX = if (forward) -distance else distance
        view.translationX = startX
        return ObjectAnimator.ofFloat(view, View.TRANSLATION_X, startX, 0f)
    }

    override fun onDisappear(
        sceneRoot: ViewGroup,
        view: View,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ): Animator {
        val distance = resolveDistance(sceneRoot)
        val endX = if (forward) -distance else distance
        return ObjectAnimator.ofFloat(view, View.TRANSLATION_X, view.translationX, endX)
            .apply {
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        // نرجّع الـ translation لصفر عشان لو الـ View اتعاد استخدامه
                        // (Fragment reuse) ميفضلش مزاح من مكانه
                        view.translationX = 0f
                    }
                })
            }
    }
}
