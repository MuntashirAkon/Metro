package code.name.monkey.retromusic.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.appbar.MaterialToolbar

/**
 * Toolbar عادي بيضيف حاجة واحدة بس: تينت حقيقي وثابت لأيقونة الـ overflow
 * (التلات نقط)، شغال بنفس فلسفة [setNavigationIconTint] الأصلية الموجودة
 * جوه AppCompat/Toolbar.
 *
 * السبب اللي محتاجين الكلاس ده عشانه: Android بيوفر tint list داخلي
 * ومستمر لأيقونة السهم (بيتطبق تلقائيًا كل مرة الأيقونة تتغيّر، حتى لو
 * التغيير جه من جوه النظام نفسه)، لكن معندوش حاجة زيها خالص لأيقونة الـ
 * overflow. فأي تلوين بيتعمل من بره الـ Toolbar (زي ما كنا بنعمل في
 * الفراجمنت) بيبقى عرضة لإعادة الضبط الداخلية بتاعة النظام.
 *
 * هنا بنعمل نفس الحيلة، لكن جوه الكلاس نفسه: بنحفظ الـ tint في متغير،
 * وبنعمل override لـ [setOverflowIcon] (الميثود اللي أي حد - إحنا أو
 * النظام - بينده عليها لما الأيقونة تتغيّر) عشان تفضل تطبّق آخر تينت
 * محفوظ تلقائيًا في كل مرة.
 */
class TintableToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.toolbarStyle
) : MaterialToolbar(context, attrs, defStyleAttr) {

    private var overflowIconTintList: ColorStateList? = null

    /** بديل مباشر لـ setNavigationIconTint، لكن لأيقونة الـ overflow. */
    fun setOverflowIconTint(@ColorInt color: Int) {
        setOverflowIconTintList(ColorStateList.valueOf(color))
    }

    fun setOverflowIconTintList(tintList: ColorStateList?) {
        overflowIconTintList = tintList
        applyStoredTint()
    }

    // بينده عليها النظام نفسه كل مرة يعيد فيها بناء/ضبط زرار الـ overflow،
    // مش بس لما إحنا نستدعيها يدويًا. عشان كده هي المكان الصح نطبّق فيه
    // الـ tint المحفوظ، بدل أي listener أو post{} من بره.
    override fun setOverflowIcon(icon: Drawable?) {
        super.setOverflowIcon(icon)
        applyStoredTint()
    }

    private fun applyStoredTint() {
        val tintList = overflowIconTintList ?: return
        val icon = super.getOverflowIcon() ?: return
        val wrapped = DrawableCompat.wrap(icon.mutate())
        DrawableCompat.setTintList(wrapped, tintList)
        // بنستخدم super هنا عمدًا، مش super.setOverflowIcon العادي ولا
        // this.setOverflowIcon، عشان منعملش recursion لا نهائي على
        // الـ override بتاعنا.
        super.setOverflowIcon(wrapped)
    }
}
