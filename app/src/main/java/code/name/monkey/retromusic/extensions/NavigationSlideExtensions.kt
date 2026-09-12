package code.name.monkey.retromusic.extensions

import androidx.navigation.NavOptions
import code.name.monkey.retromusic.R

/**
 * NavOptions بتعمل أنيميشن "Slide Up" للصفحة الجديدة (بتدخل من تحت لفوق)،
 * ولما ترجع بالـ back، الصفحة بتخرج تاني لتحت (Slide Down).
 * مستخدمة في الانتقال لصفحة "See all songs" من صفحة تفاصيل الفنان.
 */
val slideUpNavOptions: NavOptions
    get() = NavOptions.Builder()
        .setEnterAnim(R.anim.nav_slide_up_enter)
        .setExitAnim(R.anim.nav_slide_up_exit)
        .setPopEnterAnim(R.anim.nav_slide_up_pop_enter)
        .setPopExitAnim(R.anim.nav_slide_up_pop_exit)
        .build()
