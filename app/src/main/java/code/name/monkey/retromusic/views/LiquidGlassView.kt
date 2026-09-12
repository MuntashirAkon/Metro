package code.name.monkey.retromusic.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils

/**
 * طبقة الأزرار الصلبة (Solid Tint)
 * تعتمد على تفتيح/تغميق لون الخلفية الأصلي بدون استخدام أي شفافية،
 * مع الاحتفاظ بحواف بيضاء رفيعة وثابتة لإعطاء التباين المطلوب.
 */
class LiquidGlassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var cornerRadiusPx: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    private var backdropColor: Int = Color.DKGRAY
    private val rect = RectF()

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        // الحفاظ على السمك الرفيع (Hairline) للحواف
        strokeWidth = 0.8f * resources.displayMetrics.density 
    }

    fun setBackdropColor(color: Int) {
        this.backdropColor = color
        setupPaints()
        invalidate()
    }

    private fun setupPaints() {
        if (width == 0 || height == 0) return

        // 1. تحديد لون التعبئة (تعديل درجة الإضاءة بدون شفافية)
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(backdropColor, hsl)

        // hsl[2] تمثل الإضاءة (Lightness) من 0.0 إلى 1.0
        // افتراضيًا دايمًا بنفتح، إلا لو الخلفية فاتحة جدًا أصلاً (قريبة من الأبيض)
        // بحيث التفتيح مش هايبان/هيدي فرق - في الحالة دي بس نغمق شوية عشان تفضل
        // الدائرة واضحة فوق الخلفية.
        val veryLightThreshold = 0.75f
        if (hsl[2] > veryLightThreshold) {
            hsl[2] = Math.max(0f, hsl[2] - 0.035f)
        } else {
            hsl[2] = Math.min(1f, hsl[2] + 0.05f)
        }
        
        // تعيين اللون الصلب الناتج (بدون أي Alpha/شفافية)
        fillPaint.color = ColorUtils.HSLToColor(hsl)

        // 2. إطار اللمعة: أبيض دائماً ولا يتحول للأسود إطلاقاً
        val topBorderColor = ColorUtils.setAlphaComponent(Color.WHITE, 65) 
        val bottomBorderColor = ColorUtils.setAlphaComponent(Color.WHITE, 10) 
        
        borderPaint.shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            topBorderColor, bottomBorderColor,
            Shader.TileMode.CLAMP
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rect.set(0f, 0f, w.toFloat(), h.toFloat())
        setupPaints() 
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val radius = if (cornerRadiusPx > 0f) cornerRadiusPx else height / 2f
        
        // رسم جسم الزر باللون الصلب المعدل
        canvas.drawRoundRect(rect, radius, radius, fillPaint)
        
        // رسم الإطار اللامع الأبيض
        val inset = borderPaint.strokeWidth / 2f
        val borderRect = RectF(inset, inset, width - inset, height - inset)
        val borderRadius = radius - inset
        
        canvas.drawRoundRect(borderRect, borderRadius, borderRadius, borderPaint)
    }
}
