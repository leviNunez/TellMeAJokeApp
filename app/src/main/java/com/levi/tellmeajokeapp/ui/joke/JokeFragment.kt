package com.levi.tellmeajokeapp.ui.joke

import android.animation.*
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import androidx.core.animation.doOnRepeat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.levi.tellmeajokeapp.R
import com.levi.tellmeajokeapp.databinding.FragmentJokeBinding
import com.levi.tellmeajokeapp.util.*
import kotlinx.coroutines.*


class JokeFragment : Fragment() {

    companion object {
        private const val IS_PUNCHLINE_VISIBLE_KEY = "isPunchlineVisible"
        fun newInstance() = JokeFragment()
    }

    private lateinit var binding: FragmentJokeBinding
    private val viewModel: JokeViewModel by viewModels { JokeViewModel.Factory }
    private var isPunchlineVisible = false
    private var animationJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentJokeBinding.inflate(layoutInflater, container, false)

        isPunchlineVisible = savedInstanceState?.getBoolean(IS_PUNCHLINE_VISIBLE_KEY) ?: false

        if (isPunchlineVisible) hideSetupAndShowPunchline()

        bindClickListeners()

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(IS_PUNCHLINE_VISIBLE_KEY, isPunchlineVisible)
    }

    private fun hideSetupAndShowPunchline() {
        binding.apply {
            setupText.visibility = View.GONE
            questionMarkButton.visibility = View.GONE
            punchlineText.visibility = View.VISIBLE
            controlButtonsConstainer.visibility = View.VISIBLE
        }
    }

    private fun bindClickListeners() {
        binding.apply {
            questionMarkButton.setOnClickListener {
                viewModel?.revealPunchline()
            }

            nextButton.setOnClickListener {
                viewModel?.next()
            }

            backButton.setOnClickListener {
                viewModel?.back()
            }

            errorLayoutContainer.retryButton.setOnClickListener {
                viewModel?.next()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { uiState ->
                        when {
                            uiState.shouldPlayAnimationLoop -> startAnimationLoop()
                            else -> cancelAnimationLoop()
                        }
                    }
                }
                launch {
                    viewModel.uiAction.collect { uiAction ->
                        when (uiAction) {
                            is UiAction.Next -> {
                                isPunchlineVisible = false
                                hidePunchlineAndShowSetup()
                                startSetupAnimation()
                            }
                            is UiAction.RevealPunchline -> {
                                isPunchlineVisible = true
                                startPunchlineAnimation()
                            }
                            is UiAction.Back -> {
                                isPunchlineVisible = false
                                hidePunchlineAndShowSetup()
                            }
                            is UiAction.ShowSnackBar -> {
                                uiAction.message?.let { message ->
                                    Snackbar.make(
                                        binding.mainLayout,
                                        getString(R.string.snackbar_error_text, message),
                                        Snackbar.LENGTH_LONG
                                    ).setTextMaxLines(1)
                                        .show()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startAnimationLoop() {
        cancelAnimationLoop()
        animationJob = viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                delay(5000)
                binding.apply {
                    val containerH = mainLayout.height
                    val translateValue = (containerH / 10).toFloat()
                    val translator =
                        ObjectAnimator.ofFloat(
                            buttonsContainer,
                            View.TRANSLATION_Y,
                            -translateValue
                        ).apply {
                            duration = 400
                            repeatMode = ObjectAnimator.REVERSE
                            repeatCount = 1
                            interpolator = AccelerateInterpolator(1f)
                        }

                    val rotator =
                        ObjectAnimator.ofFloat(buttonsContainer, View.ROTATION, -360f, 0f)
                            .apply {
                                startDelay = 100
                                duration = 400
                                interpolator = AccelerateInterpolator(1f)
                            }

                    val set = AnimatorSet().apply {
                        playTogether(translator, rotator)
                        disableViewDuringAnimation(questionMarkButton)
                    }

                    set.start()
                }
            }
        }
    }

    private fun cancelAnimationLoop() {
        animationJob?.cancel()
    }

    private fun hidePunchlineAndShowSetup() {
        binding.apply {
            punchlineText.visibility = View.GONE
            controlButtonsConstainer.visibility = View.GONE
            setupText.visibility = View.VISIBLE
            questionMarkButton.visibility = View.VISIBLE
        }
    }

    private fun startSetupAnimation() {
        binding.apply {
            val fader = ObjectAnimator.ofFloat(mainLayout, View.ALPHA, 0f, 1f).apply {
                duration = 1000
                interpolator = AccelerateDecelerateInterpolator()
                disableViewDuringAnimation(questionMarkButton)
            }

            fader.start()
        }
    }

    private fun startPunchlineAnimation() {
        binding.apply {

            val mainLayoutFader = ObjectAnimator.ofFloat(mainLayout, View.ALPHA, 0f).apply {
                duration = 600L
                repeatCount = 1
                repeatMode = ObjectAnimator.REVERSE
                interpolator = AccelerateDecelerateInterpolator()

                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationRepeat(animation: Animator) {
                        setupText.visibility = View.GONE
                        questionMarkButton.visibility = View.GONE
                        punchlineText.visibility = View.VISIBLE
                        laughImage.visibility = View.VISIBLE
                    }

                    // Needed for testing when animations are disabled
                    override fun onAnimationEnd(animation: Animator) {
                        setupText.visibility = View.GONE
                        questionMarkButton.visibility = View.GONE
                        punchlineText.visibility = View.VISIBLE
                        laughImage.visibility = View.VISIBLE
                    }
                })
            }

            val containerH = mainLayout.height
            val scaleValue = (containerH / 300).toFloat()
            val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, scaleValue)
            val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, scaleValue)

            val jokeContainerScaler =
                ObjectAnimator.ofPropertyValuesHolder(jokeContainer, scaleX, scaleY).apply {
                    duration = 600L
                    repeatCount = 1
                    repeatMode = ObjectAnimator.REVERSE
                    interpolator = AccelerateDecelerateInterpolator()
                }

            val laughImageScaler = ObjectAnimator.ofPropertyValuesHolder(laughImage, scaleX, scaleY)
                .apply {
                    startDelay = jokeContainerScaler.duration
                    duration = 500L
                    repeatCount = 3
                    repeatMode = ObjectAnimator.REVERSE
                    interpolator = AccelerateDecelerateInterpolator()
                }

            val laughImageFader = ObjectAnimator.ofFloat(laughImage, View.ALPHA, 0f).apply {
                startDelay = laughImageScaler.totalDuration - 300
                duration = 500L
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        laughImage.visibility = View.GONE
                        laughImage.alpha = 1f
                    }
                })
            }

            val controlButtonsFader =
                ObjectAnimator.ofFloat(controlButtonsConstainer, View.ALPHA, 0f, 1f).apply {
                    startDelay = laughImageFader.totalDuration - 200
                    duration = 600L
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator) {
                            controlButtonsConstainer.visibility = View.VISIBLE
                        }
                    })
                }

            val set = AnimatorSet().apply {
                playTogether(
                    mainLayoutFader,
                    jokeContainerScaler,
                    laughImageScaler,
                    laughImageFader,
                    controlButtonsFader
                )
                disableViewDuringAnimation(questionMarkButton)
            }

            set.start()
        }
    }
}