package code.name.monkey.retromusic.fragments.artists

import android.graphics.drawable.Drawable
import android.view.View
import androidx.transition.Transition

/**
 * بيلوّن الـ Container المشترك (fragment_container) بس أثناء مدة الأنيميشن نفسه —
 * بيتلوّن لحظة بداية الترانزيشن (onTransitionStart) ويرجع لخلفيته الأصلية أول
 * ما الترانزيشن يخلص أو يتلغي (onTransitionEnd / onTransitionCancel)، عشان
 * التلوين ميفضلش عالق على باقي صفحات التطبيق.
 * مستخدمة في AbsArtistDetailsFragment و ArtistAllSongsFragment.
 */
internal fun Transition.colorContainerDuringTransition(
    container: View?,
    colorProvider: () -> Int
) {
    if (container == null) return
    addListener(object : Transition.TransitionListener {
        private var originalBackground: Drawable? = null

        override fun onTransitionStart(transition: Transition) {
            originalBackground = container.background
            container.setBackgroundColor(colorProvider())
        }

        override fun onTransitionEnd(transition: Transition) {
            container.background = originalBackground
        }

        override fun onTransitionCancel(transition: Transition) {
            container.background = originalBackground
        }

        override fun onTransitionPause(transition: Transition) {}
        override fun onTransitionResume(transition: Transition) {}
    })
}
