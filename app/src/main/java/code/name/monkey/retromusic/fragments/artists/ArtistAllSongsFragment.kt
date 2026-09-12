package code.name.monkey.retromusic.fragments.artists

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.adapter.song.SimpleSongAdapter
import code.name.monkey.retromusic.databinding.FragmentArtistAllSongsBinding
import code.name.monkey.retromusic.fragments.base.AbsMainActivityFragment
import code.name.monkey.retromusic.glide.RetroGlideExtension
import code.name.monkey.retromusic.glide.RetroGlideExtension.artistImageOptions
import code.name.monkey.retromusic.model.Artist
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.ArtistPaletteEngine
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ArtistAllSongsFragment : AbsMainActivityFragment(R.layout.fragment_artist_all_songs) {

    private var _binding: FragmentArtistAllSongsBinding? = null
    private val binding get() = _binding!!

    private lateinit var songAdapter: SimpleSongAdapter
    private var dominantBackgroundColor: Int = Color.TRANSPARENT
    private var navDestinationListener: NavController.OnDestinationChangedListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ملحوظة: الأنيميشن بتاعة الدخول والخروج للصفحة دي بقت متحكم فيها بالكامل
        // من Animation resources (nav_slide_in_right / nav_slide_out_left / ...)
        // اللي بتتحدد وقت الـ navigate() في AbsArtistDetailsFragment، مش من هنا.
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 1. تأجيل الأنيميشن لحد ما القائمة والعناصر تترسم بالكامل لمنع التقطيع
        postponeEnterTransition()

        _binding = FragmentArtistAllSongsBinding.bind(view)

        dominantBackgroundColor = arguments?.getInt(EXTRA_DOMINANT_COLOR, Color.TRANSPARENT) ?: Color.TRANSPARENT

        binding.rootLayout.setBackgroundColor(
            if (dominantBackgroundColor != Color.TRANSPARENT) dominantBackgroundColor else neutralFallbackColor()
        )

        val artist: Artist? = arguments?.let {
            BundleCompat.getParcelable(it, EXTRA_ARTIST, Artist::class.java)
        }

        @Suppress("DEPRECATION")
        val songs: ArrayList<Song> =
            arguments?.getParcelableArrayList(EXTRA_SONGS) ?: arrayListOf()

        songAdapter = SimpleSongAdapter(requireActivity(), songs, R.layout.item_song)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = DefaultItemAnimator()
            adapter = songAdapter
        }

        WindowCompat.setDecorFitsSystemWindows(requireActivity().window, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout) { _, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            binding.appBarLayout.updatePadding(top = statusBarInsets.top)
            binding.recyclerView.updatePadding(bottom = navBarInsets.bottom)

            insets
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        if (artist != null) {
            binding.toolbar.title = artist.name
            
            if (dominantBackgroundColor != Color.TRANSPARENT) {
                applyDynamicColor(dominantBackgroundColor)
            } else {
                extractArtistColor(artist)
            }
        }

        // 2. إعطاء أمر ببدء الأنيميشن فوراً بمجرد ما الشاشة تجهز للعرض
        view.doOnPreDraw {
            startPostponedEnterTransition()
        }
    }

    private fun extractArtistColor(artist: Artist) {
        Glide.with(requireContext())
            .asBitmap()
            .artistImageOptions(artist)
            .load(RetroGlideExtension.getArtistModel(artist))
            .dontAnimate()
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    extractAndApplyDominantColor(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    private fun extractAndApplyDominantColor(bitmap: Bitmap) {
        lifecycleScope.launch(Dispatchers.Default) {
            val dominantColor = ArtistPaletteEngine.findDominantColorAtSubtitleRegion(
                bitmap = bitmap,
                startRatio = 0.68f,
                endRatio = 0.78f
            )
            withContext(Dispatchers.Main) {
                if (_binding != null) {
                    animateToRealColor(dominantColor)
                }
            }
        }
    }

    private fun animateToRealColor(newColor: Int) {
        val currentColor = dominantBackgroundColor
        android.animation.ValueAnimator.ofArgb(currentColor, newColor).apply {
            duration = 350L
            addUpdateListener { applyDynamicColor(it.animatedValue as Int) }
            start()
        }
    }

    private fun applyDynamicColor(backgroundColor: Int) {
        dominantBackgroundColor = backgroundColor
        binding.rootLayout.setBackgroundColor(backgroundColor)

        val isLightBackground = ColorUtils.calculateLuminance(backgroundColor) > 0.45f
        val foregroundColor = if (isLightBackground) Color.BLACK else Color.WHITE

        binding.toolbar.setTitleTextColor(foregroundColor)
        binding.toolbar.navigationIcon?.let { DrawableCompat.setTint(it, foregroundColor) }

        applyStatusBarAppearance(isLightBackground)

        if (::songAdapter.isInitialized) {
            songAdapter.setDynamicTextColors(
                foregroundColor,
                ColorUtils.setAlphaComponent(foregroundColor, 0xCC)
            )
        }
    }

    // شريط الحالة (الساعة/البطارية فوق) يتحول لأيقونات غامقة لما الخلفية فاتحة،
    // وأيقونات بيضة لما الخلفية غامقة. بنفرضها من جديد في onResume (مش بس أول مرة)
    // عشان لما نرجع من صفحة تانية، الصفحة دي تضمن إنها هي اللي فارضة القيمة الصح
    // بغض النظر عن توقيت خروج أي صفحة تانية كانت بتتحكم في نفس الخاصية.
    private fun applyStatusBarAppearance(isLightBackground: Boolean) {
        activity?.window?.let { window ->
            WindowInsetsControllerCompat(window, window.decorView)
                .isAppearanceLightStatusBars = isLightBackground
        }
    }

    override fun onResume() {
        super.onResume()
        if (dominantBackgroundColor != Color.TRANSPARENT) {
            applyStatusBarAppearance(ColorUtils.calculateLuminance(dominantBackgroundColor) > 0.45f)
        }

        // زي ما هو في صفحة تفاصيل الفنان: لما نخرج خالص من السلسلة (لا احنا ولا
        // صفحة تفاصيل الفنان اللي جاي منها)، نرجّع شريط الحالة لطبيعته الافتراضية.
        val ownDestinationId = findNavController().currentDestination?.id
        val previousDestinationId = findNavController().previousBackStackEntry?.destination?.id
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            if (destination.id != ownDestinationId && destination.id != previousDestinationId) {
                applyStatusBarAppearance(false)
            }
        }
        navDestinationListener = listener
        findNavController().addOnDestinationChangedListener(listener)
    }

    override fun onPause() {
        navDestinationListener?.let { findNavController().removeOnDestinationChangedListener(it) }
        navDestinationListener = null
        super.onPause()
    }

    private fun neutralFallbackColor(): Int {
        val typedValue = android.util.TypedValue()
        val resolved = requireContext().theme.resolveAttribute(
            com.google.android.material.R.attr.colorSurface, typedValue, true
        )
        return if (resolved) typedValue.data else Color.TRANSPARENT
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val EXTRA_ARTIST = "extra_artist"
        const val EXTRA_SONGS = "extra_songs"
        const val EXTRA_TRANSITION_NAME = "extra_transition_name"
        const val EXTRA_DOMINANT_COLOR = "extra_dominant_color"

        fun createBundle(
            artist: Artist, 
            songs: List<Song>, 
            transitionName: String,
            dominantColor: Int = Color.TRANSPARENT
        ): Bundle =
            bundleOf(
                EXTRA_ARTIST to artist,
                EXTRA_SONGS to ArrayList(songs),
                EXTRA_TRANSITION_NAME to transitionName,
                EXTRA_DOMINANT_COLOR to dominantColor
            )
    }
}
