package code.name.monkey.retromusic.util

import android.app.AlertDialog
import android.content.Context
import android.os.SystemClock
import android.view.Choreographer
import android.widget.ScrollView
import android.widget.TextView

/**
 * أداة تشخيص مؤقتة (Debug only) - مش جزء من الفيتشر الأساسي.
 *
 * الهدف: نلاقي مكان التهنيج (jank) اللي بيسبب تجمد الأنيميشن بين
 * ArtistDetails و ArtistAllSongs، من غير الحاجة لـ adb/Logcat/Profiler
 * (لأن الشغل بيتعمل من الموبايل عن طريق GitHub Actions مباشرة).
 *
 * الاستخدام:
 * 1. JankProbe.mark("اسم النقطة") في أي سطر عايز تعرف امتى بيتنفذ.
 * 2. JankProbe.startFrameWatch() وقت ما المفروض الترانزيشن يبدأ، عشان يسجل
 *    أي فريم اتأخر عن 24ms (يعني drop فريم أو أكتر عند 60fps).
 * 3. Long-press على العنصر اللي حطيت عليه الـ listener هيطلع Dialog فيه
 *    كل النقط بالترتيب + الوقت بينهم بالمللي ثانية.
 * 4. اعمل Screenshot للـ Dialog وابعته.
 *
 * ملحوظة: الكلاس ده وكل الاستدعاءات بتاعته لازم تتشال بعد ما نخلص التشخيص.
 */
object JankProbe {

    private const val FRAME_SKIP_THRESHOLD_MS = 24L
    private const val FRAME_WATCH_DURATION_MS = 2500L

    private val entries = mutableListOf<Pair<String, Long>>()
    private var frameWatcherRunning = false

    @Synchronized
    fun mark(tag: String) {
        entries.add(tag to SystemClock.elapsedRealtime())
    }

    @Synchronized
    fun startFrameWatch() {
        if (frameWatcherRunning) return
        frameWatcherRunning = true
        mark("── frame_watch_start ──")
        val endAt = SystemClock.elapsedRealtime() + FRAME_WATCH_DURATION_MS
        var lastFrameNanos = 0L

        val callback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (lastFrameNanos != 0L) {
                    val deltaMs = (frameTimeNanos - lastFrameNanos) / 1_000_000
                    if (deltaMs > FRAME_SKIP_THRESHOLD_MS) {
                        mark("⚠ SKIPPED_FRAME (${deltaMs}ms)")
                    }
                }
                lastFrameNanos = frameTimeNanos
                if (SystemClock.elapsedRealtime() < endAt) {
                    Choreographer.getInstance().postFrameCallback(this)
                } else {
                    frameWatcherRunning = false
                    mark("── frame_watch_end ──")
                }
            }
        }
        Choreographer.getInstance().postFrameCallback(callback)
    }

    fun showLog(context: Context) {
        val snapshot = synchronized(this) { entries.toList() }
        val builder = StringBuilder()
        var previousTime: Long? = null
        for ((tag, time) in snapshot) {
            val delta = previousTime
            if (delta != null) {
                builder.append("+%4dms   %s\n".format(time - delta, tag))
            } else {
                builder.append("   0ms   %s\n".format(tag))
            }
            previousTime = time
        }
        if (snapshot.isEmpty()) {
            builder.append("مفيش بيانات لسه — كرر حركة الدخول/الخروج الأول ثم اعمل Long-press تاني")
        }

        val textView = TextView(context).apply {
            text = builder.toString()
            setTextIsSelectable(true)
            textSize = 11f
            typeface = android.graphics.Typeface.MONOSPACE
            setPadding(32, 24, 32, 24)
        }

        AlertDialog.Builder(context)
            .setTitle("Jank Log — ${snapshot.size} نقطة")
            .setView(ScrollView(context).apply { addView(textView) })
            .setPositiveButton("تم", null)
            .setNegativeButton("مسح السجل") { _, _ -> clear() }
            .show()
    }

    @Synchronized
    fun clear() {
        entries.clear()
        frameWatcherRunning = false
    }
}
