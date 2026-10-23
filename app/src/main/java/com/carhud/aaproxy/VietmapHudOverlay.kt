package com.carhud.aaproxy

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Modern floating HUD overlay for Android Auto and Mobile displays.
 * Renders European/Vietnamese-style Speed Limit Badge, Dynamic Speedometer,
 * and Camera/Road Alert Pills with zero-latency event updates.
 */
class VietmapHudOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var stateJob: Job? = null

    // UI references
    private var speedLimitBadge: FrameLayout? = null
    private var speedLimitText: TextView? = null
    private var secondaryLimitBadge: FrameLayout? = null
    private var secondaryLimitText: TextView? = null
    private var currentSpeedContainer: LinearLayout? = null
    private var currentSpeedText: TextView? = null
    private var currentSpeedUnit: TextView? = null
    private var turnIcon: TextView? = null
    private var turnDistanceText: TextView? = null
    private var turnContainer: View? = null
    private var roadNameText: TextView? = null
    private var etaText: TextView? = null
    private var wazeDot: View? = null
    private var wazePill: LinearLayout? = null
    private var alertContainer: View? = null
    private var alertIconView: WazeAlertIconView? = null
    private var alertTitleText: TextView? = null
    private var alertDistanceText: TextView? = null
    private var alertUpcomingText: TextView? = null
    private var laneGuidanceText: TextView? = null
    private val referenceAlertContainers = mutableListOf<LinearLayout>()
    private val referenceAlertIcons = mutableListOf<WazeAlertIconView>()
    private val referenceAlertDistances = mutableListOf<TextView>()
    private var referenceReportButton: TextView? = null

    private var activeStyleId = 1
    private var isPreviewMode = false
    private var isPulsingOverSpeed = false
    private val pulseHandler = Handler(Looper.getMainLooper())
    private var pulseState = false

    private var lastData: VietmapAlertData? = null
    private var projectionViewportWidthPx: Int = 0
    private var projectionViewportHeightPx: Int = 0

    // Dragging & Locking
    private var isLocked = false
    private var lockButton: TextView? = null
    private var dX = 0f
    private var dY = 0f
    private var isDragging = false
    private var startRawX = 0f
    private var startRawY = 0f

    private val pulseRunnable = object : Runnable {
        override fun run() {
            if (isPulsingOverSpeed) {
                pulseState = !pulseState
                if (pulseState) {
                    applyStyleBackground(overSpeed = true)
                    currentSpeedText?.setTextColor(Color.parseColor("#FF4D4F"))
                } else {
                    applyStyleBackground(overSpeed = false)
                    currentSpeedText?.setTextColor(Color.WHITE)
                }
                pulseHandler.postDelayed(this, 650)
            }
        }
    }

    init {
        isLocked = WazeHudManager.isLocked(context)
        rebuildViewsForStyle(WazeHudManager.getActiveStyleId(context))
        applyHudConfig()
    }

    fun setPreviewMode(preview: Boolean) {
        this.isPreviewMode = preview
    }

    fun applyProjectionViewport(widthPx: Int, heightPx: Int) {
        projectionViewportWidthPx = widthPx.coerceAtLeast(0)
        projectionViewportHeightPx = heightPx.coerceAtLeast(0)
        applyHudConfig()
    }

    fun applyHudConfig(styleIdOverride: Int? = null) {
        try {
            val newStyleId = styleIdOverride ?: WazeHudManager.getActiveStyleId(context)
            if (newStyleId != activeStyleId || childCount == 0) {
                activeStyleId = newStyleId
                rebuildViewsForStyle(activeStyleId)
            }

            if (isPreviewMode) {
                scaleX = 1.0f
                scaleY = 1.0f
                alpha = 1.0f
            } else {
                val configuredScale = WazeHudManager.getScale(context)
                val density = context.resources.displayMetrics.density.coerceAtLeast(1f)
                val scale = when (activeStyleId) {
                    6 -> {
                        // Keep the large panel adaptive to the car viewport.
                        val designWidthPx = 215f * density
                        val viewportScale = if (projectionViewportWidthPx > 0) {
                            (projectionViewportWidthPx * 0.255f / designWidthPx).coerceIn(0.42f, 0.82f)
                        } else 0.68f
                        viewportScale * (configuredScale / 120f)
                    }
                    7 -> {
                        // Style #7 uses the user's percentage literally:
                        // 100% = the full reference strip size, never an extra auto-shrink.
                        configuredScale / 100f
                    }
                    else -> configuredScale / 100f
                }
                pivotX = 0f
                pivotY = 0f
                scaleX = scale
                scaleY = scale
                val userOpacity = WazeHudManager.getOpacity(context) / 100f
                alpha = if (userOpacity < 0.85f) 0.95f else userOpacity
            }

            lastData?.let { updateUi(it) }
        } catch (e: Exception) {
            // fallback safe
        }
    }

    private fun rebuildViewsForStyle(styleId: Int) {
        removeAllViews()
        speedLimitBadge = null
        speedLimitText = null
        secondaryLimitBadge = null
        secondaryLimitText = null
        currentSpeedContainer = null
        currentSpeedText = null
        currentSpeedUnit = null
        turnIcon = null
        turnDistanceText = null
        turnContainer = null
        roadNameText = null
        etaText = null
        wazeDot = null
        wazePill = null
        alertContainer = null
        alertIconView = null
        alertTitleText = null
        alertDistanceText = null
        alertUpcomingText = null
        laneGuidanceText = null
        referenceAlertContainers.clear()
        referenceAlertIcons.clear()
        referenceAlertDistances.clear()
        referenceReportButton = null
        lockButton = null

        val defaultBorder = Color.parseColor("#00E5FF")
        background = rounded(Color.parseColor("#F50B132B"), 18f, defaultBorder, 2)

        when (styleId) {
            1 -> buildStyle1BubbleNgangTieuChuan()
            2 -> buildStyle2BubbleNgangDayDu()
            3 -> buildStyle3DocCotTrai()
            4 -> buildStyle4DocCotPhai()
            5 -> buildStyle5SieuTinhGon()
            6 -> buildStyle6WazePanelLon()
            7 -> buildStyle7WazeThanhNgang()
            else -> buildStyle1BubbleNgangTieuChuan()
        }
    }

    // -------------------------------------------------------------
    // STYLE 1: BUBBLE NGANG TIÊU CHUẨN
    // -------------------------------------------------------------
    private fun buildStyle1BubbleNgangTieuChuan() {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(12), dp(8), dp(12), dp(8))
        layoutParams?.let {
            it.width = ViewGroup.LayoutParams.WRAP_CONTENT
            it.height = ViewGroup.LayoutParams.WRAP_CONTENT
            layoutParams = it
        }
        minimumWidth = 0

        // 1. Red circle speed limit sign
        speedLimitBadge = FrameLayout(context).apply {
            val size = dp(44)
            layoutParams = LayoutParams(size, size).apply { marginEnd = dp(5) }
            background = circle(Color.WHITE, Color.parseColor("#DC2626"), 3f)

            speedLimitText = TextView(context).apply {
                text = "0"
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
            }
            addView(speedLimitText, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER))
        }
        addView(speedLimitBadge)

        // 2. Vertical Divider
        addView(createVerticalDivider())

        // 3. Current Speed
        currentSpeedContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(6), 0, dp(6), 0)

            currentSpeedText = TextView(context).apply {
                text = "0"
                textSize = 32f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            }
            addView(currentSpeedText)

            currentSpeedUnit = TextView(context).apply {
                text = "KM/H"
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#E2E8F0"))
                gravity = Gravity.CENTER
            }
            addView(currentSpeedUnit)
        }
        addView(currentSpeedContainer)

        // 4. Turn Cyan Box + Distance
        val turnRow = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginStart = dp(8)
            }
        }
        turnContainer = turnRow
        turnRow.apply {

            val turnBox = FrameLayout(context).apply {
                val size = dp(38)
                layoutParams = LayoutParams(size, size).apply { bottomMargin = dp(1) }
                background = rounded(Color.parseColor("#3306B6D4"), 10f, Color.parseColor("#00E5FF"), 2)

                turnIcon = TextView(context).apply {
                    text = "↑"
                    textSize = 22f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.parseColor("#38BDF8"))
                    gravity = Gravity.CENTER
                }
                addView(turnIcon, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER))
            }
            addView(turnBox)

            turnDistanceText = TextView(context).apply {
                text = "0m"
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#38BDF8"))
                gravity = Gravity.CENTER
            }
            addView(turnDistanceText)
        }
        addView(turnRow)

        // 4.5 Active Alert Badge
        addView(createAlertBadge(isVertical = false))

        // 5. Waze Pill
        wazePill = createWazePill().apply {
            (layoutParams as LayoutParams).marginStart = dp(8)
        }
        addView(wazePill)

        // 6. Lock / Unlock Button
        addView(createLockButton())
    }

    // -------------------------------------------------------------
    // STYLE 2: BUBBLE NGANG ĐẦY ĐỦ (FULL INFO)
    // -------------------------------------------------------------
    private fun buildStyle2BubbleNgangDayDu() {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(12), dp(8), dp(12), dp(8))
        layoutParams?.let {
            it.width = ViewGroup.LayoutParams.WRAP_CONTENT
            it.height = ViewGroup.LayoutParams.WRAP_CONTENT
            layoutParams = it
        }
        minimumWidth = 0

        // 1. Red speed limit circle + Blue secondary limit circle
        val signsRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = dp(6)
            }

            speedLimitBadge = FrameLayout(context).apply {
                val size = dp(38)
                layoutParams = LayoutParams(size, size)
                background = circle(Color.WHITE, Color.parseColor("#E74C3C"), 2.2f)

                speedLimitText = TextView(context).apply {
                    text = "0"
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.BLACK)
                    gravity = Gravity.CENTER
                }
                addView(speedLimitText, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER))
            }
            addView(speedLimitBadge)

            secondaryLimitBadge = FrameLayout(context).apply {
                val size = dp(28)
                layoutParams = LayoutParams(size, size).apply { marginStart = dp(4) }
                background = circle(Color.parseColor("#0284C7"), Color.parseColor("#38BDF8"), 1.2f)

                secondaryLimitText = TextView(context).apply {
                    text = "--"
                    textSize = 9f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                }
                addView(secondaryLimitText, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER))
            }
            addView(secondaryLimitBadge)
        }
        addView(signsRow)

        // 2. Vertical Divider
        addView(createVerticalDivider())

        // 3. Current Speed
        currentSpeedContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(4), 0, dp(4), 0)

            currentSpeedText = TextView(context).apply {
                text = "0"
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            }
            addView(currentSpeedText)

            currentSpeedUnit = TextView(context).apply {
                text = "KM/H"
                textSize = 7f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#8097AC"))
                gravity = Gravity.CENTER
            }
            addView(currentSpeedUnit)
        }
        addView(currentSpeedContainer)

        // 4. Vertical Divider
        addView(createVerticalDivider())

        // 5. Nav Info: Turn arrow + Distance, ETA + Distance
        val navCol = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), 0, dp(2), 0)

            val turnLine = LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER
            }
            turnContainer = turnLine
            turnLine.apply {

                turnIcon = TextView(context).apply {
                    text = "↑"
                    textSize = 16f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.parseColor("#38BDF8"))
                    gravity = Gravity.CENTER
                    setPadding(0, 0, 0, dp(1))
                }
                addView(turnIcon)

                turnDistanceText = TextView(context).apply {
                    text = "0m"
                    textSize = 9f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.parseColor("#38BDF8"))
                    gravity = Gravity.CENTER
                }
                addView(turnDistanceText)
            }
            addView(turnLine)

            etaText = TextView(context).apply {
                text = "ETA • 0km"
                textSize = 9.5f
                setTextColor(Color.parseColor("#94A3B8"))
                setPadding(0, dp(1), 0, 0)
            }
            addView(etaText)
        }
        addView(navCol)

        // 5.5 Active Alert Badge
        addView(createAlertBadge(isVertical = false))

        // 6. Waze Pill
        wazePill = createWazePill().apply {
            (layoutParams as LayoutParams).marginStart = dp(8)
        }
        addView(wazePill)

        // 7. Lock / Unlock Button
        addView(createLockButton())
    }

    // -------------------------------------------------------------
    // STYLE 3: DỌC CỘT TRÁI (LEFT DOCK)
    // -------------------------------------------------------------
    private fun buildStyle3DocCotTrai() {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(dp(8), dp(10), dp(8), dp(10))
        layoutParams?.let {
            it.width = dp(78)
            it.height = ViewGroup.LayoutParams.WRAP_CONTENT
            layoutParams = it
        } ?: run {
            minimumWidth = dp(78)
        }

        // 1. Waze pill at top
        wazePill = createWazePill().apply {
            (layoutParams as LinearLayout.LayoutParams).bottomMargin = dp(8)
        }
        addView(wazePill)

        // 2. Speed Limit Sign
        speedLimitBadge = FrameLayout(context).apply {
            val size = dp(38)
            layoutParams = LayoutParams(size, size).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(4)
            }
            background = circle(Color.WHITE, Color.parseColor("#E74C3C"), 2.2f)

            speedLimitText = TextView(context).apply {
                text = "0"
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
            }
            addView(speedLimitText, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER))
        }
        addView(speedLimitBadge)

        // 2.5 Secondary Limit Sign (Blue circle)
        secondaryLimitBadge = FrameLayout(context).apply {
            val size = dp(26)
            layoutParams = LayoutParams(size, size).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(4)
            }
            background = circle(Color.parseColor("#0284C7"), Color.parseColor("#38BDF8"), 1.2f)

            secondaryLimitText = TextView(context).apply {
                text = "60"
                textSize = 8.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            }
            addView(secondaryLimitText, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER))
        }
        addView(secondaryLimitBadge)

        // 3. Horizontal Divider
        addView(createHorizontalDivider())

        // 4. Current Speed
        currentSpeedContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, dp(2), 0, dp(2))

            currentSpeedText = TextView(context).apply {
                text = "0"
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            }
            addView(currentSpeedText)

            currentSpeedUnit = TextView(context).apply {
                text = "KM/H"
                textSize = 8.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#8097AC"))
                gravity = Gravity.CENTER
            }
            addView(currentSpeedUnit)
        }
        addView(currentSpeedContainer)

        // 5. Horizontal Divider
        addView(createHorizontalDivider())

        // 6. Turn Box + Distance
        val turnBox = FrameLayout(context).apply {
            val size = dp(32)
            layoutParams = LayoutParams(size, size).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(2)
            }
            background = rounded(Color.parseColor("#2606B6D4"), 10f, Color.parseColor("#06B6D4"), 1)
        }
        turnContainer = turnBox
        turnBox.apply {

            turnIcon = TextView(context).apply {
                text = "↑"
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#38BDF8"))
                gravity = Gravity.CENTER
            }
            addView(turnIcon, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER))
        }
        addView(turnBox)

        turnDistanceText = TextView(context).apply {
            text = "0m"
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#38BDF8"))
            gravity = Gravity.CENTER
        }
        addView(turnDistanceText)

        // 6.5 Active Alert Badge
        addView(createAlertBadge(isVertical = true))

        // Lock / Unlock Button
        addView(createLockButton().apply {
            (layoutParams as LinearLayout.LayoutParams).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(6)
                marginStart = 0
            }
        })
    }

    // -------------------------------------------------------------
    // STYLE 4: DỌC CỘT PHẢI (RIGHT DOCK)
    // -------------------------------------------------------------
    private fun buildStyle4DocCotPhai() {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(dp(8), dp(10), dp(8), dp(10))
        layoutParams?.let {
            it.width = dp(82)
            it.height = ViewGroup.LayoutParams.WRAP_CONTENT
            layoutParams = it
        } ?: run {
            minimumWidth = dp(82)
        }

        // 1. Top row: Waze pill and Blue secondary limit
        val topRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(6)
            }

            wazePill = createWazePill()
            addView(wazePill)

            secondaryLimitBadge = FrameLayout(context).apply {
                val size = dp(24)
                layoutParams = LayoutParams(size, size).apply { marginStart = dp(3) }
                background = circle(Color.parseColor("#0284C7"), Color.parseColor("#38BDF8"), 1.2f)

                secondaryLimitText = TextView(context).apply {
                    text = "--"
                    textSize = 10.5f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                }
                addView(secondaryLimitText, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER))
            }
            addView(secondaryLimitBadge)
        }
        addView(topRow)

        // 2. Current Speed
        currentSpeedContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, dp(2), 0, dp(4))

            currentSpeedText = TextView(context).apply {
                text = "0"
                textSize = 28f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            }
            addView(currentSpeedText)

            currentSpeedUnit = TextView(context).apply {
                text = "KM/H"
                textSize = 8.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#8097AC"))
                gravity = Gravity.CENTER
            }
            addView(currentSpeedUnit)
        }
        addView(currentSpeedContainer)

        // 3. Speed Limit Sign
        speedLimitBadge = FrameLayout(context).apply {
            val size = dp(38)
            layoutParams = LayoutParams(size, size).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(4)
            }
            background = circle(Color.WHITE, Color.parseColor("#E74C3C"), 2.2f)

            speedLimitText = TextView(context).apply {
                text = "0"
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
            }
            addView(speedLimitText, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER))
        }
        addView(speedLimitBadge)

        // 4. Horizontal Divider
        addView(createHorizontalDivider())

        // 5. Turn Icon + Distance
        turnIcon = TextView(context).apply {
            text = "↑"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#38BDF8"))
            gravity = Gravity.CENTER
        }
        addView(turnIcon)

        turnDistanceText = TextView(context).apply {
            text = "0m"
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#38BDF8"))
            gravity = Gravity.CENTER
        }
        addView(turnDistanceText)

        // 5.5 Active Alert Badge
        addView(createAlertBadge(isVertical = true))

        // Lock / Unlock Button
        addView(createLockButton().apply {
            (layoutParams as LinearLayout.LayoutParams).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(6)
                marginStart = 0
            }
        })
    }

    // -------------------------------------------------------------
    // STYLE 5: SIÊU TINH GỌN (CHỈ TỐC ĐỘ GIỚI HẠN, HIỆN TẠI & NÚT KHÓA)
    // -------------------------------------------------------------
    private fun buildStyle5SieuTinhGon() {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(10), dp(6), dp(10), dp(6))
        layoutParams?.let {
            it.width = ViewGroup.LayoutParams.WRAP_CONTENT
            it.height = ViewGroup.LayoutParams.WRAP_CONTENT
            layoutParams = it
        }
        minimumWidth = 0

        // 1. Red circle speed limit sign (BGTVT / QCVN standard)
        speedLimitBadge = FrameLayout(context).apply {
            val size = dp(38)
            layoutParams = LayoutParams(size, size).apply { marginEnd = dp(6) }
            background = circle(Color.WHITE, Color.parseColor("#E74C3C"), 2.2f)

            speedLimitText = TextView(context).apply {
                text = "--"
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
            }
            addView(speedLimitText, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER))
        }
        addView(speedLimitBadge)

        // 2. Vertical Divider
        addView(createVerticalDivider())

        // 3. Current Speed Container
        currentSpeedContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(4), 0, dp(4), 0)

            currentSpeedText = TextView(context).apply {
                text = "0"
                textSize = 26f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            }
            addView(currentSpeedText)

            currentSpeedUnit = TextView(context).apply {
                text = "KM/H"
                textSize = 9f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#8097AC"))
                gravity = Gravity.CENTER
            }
            addView(currentSpeedUnit)
        }
        addView(currentSpeedContainer)

        // 3.5 Active Alert Badge
        addView(createAlertBadge(isVertical = false))

        // 4. Vertical Divider
        addView(createVerticalDivider())

        // 5. Waze maneuver: arrow with distance directly underneath
        val compactTurn = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(5), 0, dp(5), 0)

            turnIcon = TextView(context).apply {
                text = "↑"
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#38BDF8"))
                gravity = Gravity.CENTER
            }
            addView(turnIcon)

            turnDistanceText = TextView(context).apply {
                text = ""
                textSize = 10.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#38BDF8"))
                gravity = Gravity.CENTER
            }
            addView(turnDistanceText)
        }
        turnContainer = compactTurn
        addView(compactTurn)

        addView(createVerticalDivider())

        // 6. Lock / Unlock Button
        addView(createLockButton().apply {
            (layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = dp(4)
                marginEnd = dp(2)
            }
        })
    }


    // -------------------------------------------------------------
    // STYLE 6: WAZE PANEL LỚN
    // Closely follows the supplied reference: large limit/speed block,
    // navigation/lane card, stacked road alerts, and a blue Report action.
    // -------------------------------------------------------------
    private fun buildStyle6WazePanelLon() {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(5), dp(4), dp(4), dp(4))
        minimumWidth = 0
        applyStyleBackground(overSpeed = false)

        speedLimitBadge = FrameLayout(context).apply {
            val size = dp(44)
            layoutParams = LayoutParams(size, size).apply { marginEnd = dp(4) }
            background = circle(Color.WHITE, Color.parseColor("#E31B23"), 5f)
            speedLimitText = TextView(context).apply {
                text = "--"
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
                includeFontPadding = false
            }
            addView(speedLimitText, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
        }
        addView(speedLimitBadge)

        currentSpeedContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(dp(46), LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(4) }
            currentSpeedText = TextView(context).apply {
                text = "0"
                textSize = 34f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                includeFontPadding = false
            }
            addView(currentSpeedText)
            currentSpeedUnit = TextView(context).apply {
                text = "km/h"
                textSize = 8f
                setTextColor(Color.parseColor("#C6CBD2"))
                gravity = Gravity.CENTER
                includeFontPadding = false
            }
            addView(currentSpeedUnit)
        }
        addView(currentSpeedContainer)

        val navColumn = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(dp(54), LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(4) }
        }

        val turnBox = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            background = rounded(Color.parseColor("#176AC8"), 10f, Color.parseColor("#4DFFFFFF"), 1)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(50))
            val top = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER
                turnIcon = TextView(context).apply {
                    text = "↱"
                    textSize = 20f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                    includeFontPadding = false
                }
                addView(turnIcon, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
                turnDistanceText = TextView(context).apply {
                    text = ""
                    textSize = 9f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                    includeFontPadding = false
                }
                addView(turnDistanceText, LayoutParams(0, LayoutParams.MATCH_PARENT, 1.35f))
            }
            addView(top, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
            roadNameText = TextView(context).apply {
                text = ""
                textSize = 7f
                setTextColor(Color.parseColor("#D4D8DE"))
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                includeFontPadding = false
            }
            addView(roadNameText, LayoutParams(LayoutParams.MATCH_PARENT, dp(14)))
        }
        turnContainer = turnBox
        navColumn.addView(turnBox)

        laneGuidanceText = TextView(context).apply {
            text = ""
            textSize = 9f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#D6D9DE"))
            gravity = Gravity.CENTER
            background = rounded(Color.parseColor("#5A22282F"), 7f, Color.parseColor("#24FFFFFF"), 1)
            setPadding(dp(2), dp(1), dp(2), dp(1))
            visibility = GONE
        }
        navColumn.addView(
            laneGuidanceText,
            LayoutParams(LayoutParams.MATCH_PARENT, dp(18)).apply { topMargin = dp(2) }
        )
        addView(navColumn)

        val alertsColumn = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LayoutParams(dp(50), LayoutParams.WRAP_CONTENT)
        }

        alertsColumn.addView(
            createAlertBadge(isVertical = false).apply {
                minimumWidth = 0
                setPadding(dp(2), dp(1), dp(2), dp(1))
                background = rounded(Color.parseColor("#E6161B22"), 9f, Color.parseColor("#38536A7A"), 1)
                alertIconView?.layoutParams = LayoutParams(dp(24), dp(24)).apply { gravity = Gravity.CENTER_HORIZONTAL }
                alertDistanceText?.textSize = 8f
            },
            LayoutParams(LayoutParams.MATCH_PARENT, dp(40))
        )

        alertsColumn.addView(createReferenceAlertCell(dp(50), dp(38), compact = false))
        alertsColumn.addView(createReferenceAlertCell(dp(50), dp(38), compact = false))

        referenceReportButton = TextView(context).apply {
            text = "⚠  Báo cáo"
            textSize = 9f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = rounded(Color.parseColor("#176AC8"), 8f, Color.parseColor("#4DFFFFFF"), 1)
            contentDescription = "Báo cáo Waze"
        }
        alertsColumn.addView(
            referenceReportButton,
            LayoutParams(LayoutParams.MATCH_PARENT, dp(28)).apply { topMargin = dp(2) }
        )

        addView(alertsColumn)
    }

    // -------------------------------------------------------------
    // STYLE 7: WAZE THANH NGANG
    // Low continuous strip matching the second supplied reference.
    // -------------------------------------------------------------
    private fun buildStyle7WazeThanhNgang() {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(6), dp(3), dp(6), dp(3))
        minimumWidth = dp(430)
        applyStyleBackground(overSpeed = false)

        speedLimitBadge = FrameLayout(context).apply {
            val size = dp(36)
            layoutParams = LayoutParams(size, size).apply { marginEnd = dp(4) }
            background = circle(Color.WHITE, Color.parseColor("#E31B23"), 3.5f)
            speedLimitText = TextView(context).apply {
                text = "--"
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
                includeFontPadding = false
            }
            addView(speedLimitText, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
        }
        addView(speedLimitBadge)

        currentSpeedContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(dp(72), dp(40)).apply { marginEnd = dp(5) }
            currentSpeedText = TextView(context).apply {
                text = "0"
                textSize = 29f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                includeFontPadding = false
            }
            addView(currentSpeedText)
            currentSpeedUnit = TextView(context).apply {
                text = " km/h"
                textSize = 6.5f
                setTextColor(Color.parseColor("#B9BEC5"))
                setPadding(dp(1), dp(12), 0, 0)
                includeFontPadding = false
            }
            addView(currentSpeedUnit)
        }
        addView(currentSpeedContainer)

        val turnBox = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(dp(88), dp(38)).apply { marginEnd = dp(5) }
            background = rounded(Color.parseColor("#176AC8"), 8f, Color.parseColor("#4DFFFFFF"), 1)
            turnIcon = TextView(context).apply {
                text = "↱"
                textSize = 23f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                includeFontPadding = false
            }
            addView(turnIcon, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
            turnDistanceText = TextView(context).apply {
                text = ""
                textSize = 7.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                includeFontPadding = false
            }
            addView(turnDistanceText, LayoutParams(0, LayoutParams.MATCH_PARENT, 1.25f))
        }
        turnContainer = turnBox
        addView(turnBox)

        roadNameText = TextView(context).apply {
            visibility = GONE
            text = ""
        }
        addView(roadNameText, LayoutParams(0, 0))

        laneGuidanceText = TextView(context).apply {
            text = ""
            textSize = 8.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#D4D8DE"))
            gravity = Gravity.CENTER
            background = rounded(Color.parseColor("#4D252A30"), 7f, Color.parseColor("#22FFFFFF"), 1)
            setPadding(dp(2), 0, dp(2), 0)
            visibility = GONE
        }
        addView(
            laneGuidanceText,
            LayoutParams(dp(64), dp(38)).apply { marginEnd = dp(5) }
        )

        addView(
            createAlertBadge(isVertical = false).apply {
                minimumWidth = 0
                setPadding(dp(2), dp(1), dp(2), dp(1))
                alertIconView?.layoutParams = LayoutParams(dp(29), dp(29)).apply { gravity = Gravity.CENTER_HORIZONTAL }
                alertDistanceText?.textSize = 9f
            },
            LayoutParams(dp(54), dp(38)).apply { marginEnd = dp(5) }
        )

        addView(createReferenceAlertCell(dp(54), dp(38), compact = true))
        addView(createReferenceAlertCell(dp(54), dp(38), compact = true))

        referenceReportButton = TextView(context).apply {
            text = "⚠"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = rounded(Color.parseColor("#176AC8"), 8f, Color.parseColor("#4DFFFFFF"), 1)
            contentDescription = "Báo cáo Waze"
        }
        addView(
            referenceReportButton,
            LayoutParams(dp(42), dp(38)).apply { marginStart = dp(3) }
        )
    }

    private fun createReferenceAlertCell(widthPx: Int, heightPx: Int, compact: Boolean): LinearLayout {
        val cell = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            minimumWidth = 0
            setPadding(dp(2), dp(1), dp(2), dp(1))
            background = rounded(Color.parseColor("#D9161B22"), if (compact) 8f else 9f, Color.parseColor("#38536A7A"), 1)
            visibility = GONE
            layoutParams = LayoutParams(widthPx, heightPx).apply {
                if (!compact) topMargin = dp(2) else marginEnd = dp(4)
            }
        }
        val icon = WazeAlertIconView(context).apply {
            layoutParams = LayoutParams(dp(if (compact) 29 else 24), dp(if (compact) 29 else 24)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            setAlert(4, VietmapWarningType.HAZARD)
        }
        val distance = TextView(context).apply {
            text = ""
            textSize = if (compact) 6.8f else 8f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            includeFontPadding = false
            maxLines = 1
        }
        cell.addView(icon)
        cell.addView(distance)
        referenceAlertContainers += cell
        referenceAlertIcons += icon
        referenceAlertDistances += distance
        return cell
    }

    private fun applyStyleBackground(overSpeed: Boolean) {
        when (activeStyleId) {
            6 -> background = rounded(
                Color.parseColor(if (overSpeed) "#F2380C0C" else "#F20B0D10"),
                13f,
                Color.parseColor(if (overSpeed) "#EF4444" else "#3AFFFFFF"),
                if (overSpeed) 2 else 1
            )
            7 -> background = rounded(
                Color.parseColor(if (overSpeed) "#F2380C0C" else "#F20B0D10"),
                11f,
                Color.parseColor(if (overSpeed) "#EF4444" else "#3AFFFFFF"),
                if (overSpeed) 2 else 1
            )
            else -> {
                val defaultBorder = if (activeStyleId == 2 || activeStyleId == 4) {
                    Color.parseColor("#0284C7")
                } else {
                    Color.parseColor("#1F374E")
                }
                background = rounded(
                    Color.parseColor(if (overSpeed) "#E62A0808" else "#E6090E17"),
                    18f,
                    if (overSpeed) Color.parseColor("#EF4444") else defaultBorder,
                    if (overSpeed) 2 else 1
                )
            }
        }
    }

    private fun createAlertBadge(isVertical: Boolean = false): LinearLayout {
        // Waze-like compact alert cell used by every HUD style:
        // big icon on top, distance below. The long title/upcoming list stays
        // hidden so the floating bubble remains readable on Android Auto.
        return LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            minimumWidth = if (isVertical) 0 else dp(62)
            setPadding(
                if (isVertical) dp(4) else dp(8),
                dp(5),
                if (isVertical) dp(4) else dp(8),
                dp(5)
            )
            background = rounded(
                Color.parseColor("#D9161B22"),
                12f,
                Color.parseColor("#38536A7A"),
                1
            )
            layoutParams = LayoutParams(
                if (isVertical) LayoutParams.MATCH_PARENT else LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                if (isVertical) {
                    topMargin = dp(5)
                    bottomMargin = dp(4)
                } else {
                    marginStart = dp(7)
                    marginEnd = dp(4)
                }
            }

            alertIconView = WazeAlertIconView(context).apply {
                layoutParams = LayoutParams(
                    dp(if (isVertical) 38 else 42),
                    dp(if (isVertical) 38 else 42)
                ).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                }
                setAlert(2, VietmapWarningType.SPEED_CAMERA)
            }
            addView(alertIconView)

            alertDistanceText = TextView(context).apply {
                text = "430 m"
                textSize = if (isVertical) 10.5f else 11.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                includeFontPadding = false
                setPadding(0, dp(2), 0, 0)
                maxLines = 1
            }
            addView(alertDistanceText)

            // Kept as hidden views so the same update code/state model can still
            // expose a semantic title without adding visual clutter.
            alertTitleText = TextView(context).apply {
                visibility = GONE
                maxLines = 1
            }
            addView(alertTitleText)

            alertUpcomingText = TextView(context).apply {
                visibility = GONE
                maxLines = 1
            }
            addView(alertUpcomingText)

            alertContainer = this
            visibility = GONE
        }
    }

    private fun createVerticalDivider(): View {
        return View(context).apply {
            val p = LayoutParams(dp(1), dp(32)).apply {
                marginStart = dp(8)
                marginEnd = dp(8)
            }
            layoutParams = p
            setBackgroundColor(Color.parseColor("#26FFFFFF"))
        }
    }

    private fun createHorizontalDivider(): View {
        return View(context).apply {
            val p = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)).apply {
                topMargin = dp(6)
                bottomMargin = dp(6)
            }
            layoutParams = p
            setBackgroundColor(Color.parseColor("#26FFFFFF"))
        }
    }

    private fun createWazePill(): LinearLayout {
        return LinearLayout(context).apply {
            visibility = View.GONE
            layoutParams = LayoutParams(0, 0)
        }
    }

    fun isHudLocked(): Boolean = isLocked

    fun getLockButton(): TextView? = lockButton

    fun isTouchOnLockLocal(localX: Float, localY: Float): Boolean {
        val btn = lockButton ?: return false
        if (btn.visibility != View.VISIBLE) return false

        var btnL = btn.left.toFloat()
        var btnT = btn.top.toFloat()
        var p = btn.parent
        while (p is View && p != this) {
            btnL += p.left - p.scrollX
            btnT += p.top - p.scrollY
            p = p.parent
        }
        val btnW = if (btn.width > 0) btn.width.toFloat() else dp(36).toFloat()
        val btnH = if (btn.height > 0) btn.height.toFloat() else dp(36).toFloat()
        val pad = dp(24).toFloat()
        return localX >= btnL - pad && localX <= btnL + btnW + pad &&
               localY >= btnT - pad && localY <= btnT + btnH + pad
    }

    fun hitTestLock(rootX: Float, rootY: Float): Boolean {
        val btn = lockButton ?: return false
        if (btn.visibility != View.VISIBLE || visibility != View.VISIBLE) return false

        val lp = layoutParams as? FrameLayout.LayoutParams
        val overlayLeft = (lp?.leftMargin ?: this.left).toFloat()
        val overlayTop = (lp?.topMargin ?: this.top).toFloat()

        val pts = floatArrayOf(rootX - overlayLeft, rootY - overlayTop)
        val invMatrix = android.graphics.Matrix()
        if (this.matrix.invert(invMatrix)) {
            invMatrix.mapPoints(pts)
        }
        return isTouchOnLockLocal(pts[0], pts[1])
    }

    fun isTouchOnOverlay(rootX: Float, rootY: Float): Boolean {
        if (visibility != View.VISIBLE) return false
        val lp = layoutParams as? FrameLayout.LayoutParams
        val overlayLeft = (lp?.leftMargin ?: this.left).toFloat()
        val overlayTop = (lp?.topMargin ?: this.top).toFloat()

        val pts = floatArrayOf(rootX - overlayLeft, rootY - overlayTop)
        val invMatrix = android.graphics.Matrix()
        if (this.matrix.invert(invMatrix)) {
            invMatrix.mapPoints(pts)
        }
        val w = if (width > 0) width.toFloat() else dp(160).toFloat()
        val h = if (height > 0) height.toFloat() else dp(48).toFloat()
        val pad = dp(14).toFloat()
        return pts[0] >= -pad && pts[0] <= w + pad &&
               pts[1] >= -pad && pts[1] <= h + pad
    }

    fun moveTo(posX: Float, posY: Float, parentW: Int = 0, parentH: Int = 0) {
        if (isLocked) return
        val lp = layoutParams as? FrameLayout.LayoutParams ?: return
        val pView = parent as? View
        val pW = if (parentW > 0) parentW else pView?.width?.takeIf { it > 0 } ?: 800
        val pH = if (parentH > 0) parentH else pView?.height?.takeIf { it > 0 } ?: 480
        val hudW = (width * scaleX).toInt().coerceAtLeast(dp(80))
        val hudH = (height * scaleY).toInt().coerceAtLeast(dp(36))

        var newLeft = (posX - hudW / 2).toInt()
        var newTop = (posY - hudH / 2).toInt()

        newLeft = newLeft.coerceIn(0, (pW - hudW).coerceAtLeast(0))
        newTop = newTop.coerceIn(0, (pH - hudH).coerceAtLeast(0))

        lp.gravity = Gravity.TOP or Gravity.START
        lp.leftMargin = newLeft
        lp.topMargin = newTop
        layoutParams = lp
        layout(newLeft, newTop, newLeft + (width.takeIf { it > 0 } ?: hudW), newTop + (height.takeIf { it > 0 } ?: hudH))
        requestLayout()

        val density = resources.displayMetrics.density
        val posXdp = (newLeft / density).roundToInt()
        val posYdp = (newTop / density).roundToInt()
        WazeHudManager.setPosition(context, posXdp, posYdp)
    }

    fun moveBy(deltaX: Float, deltaY: Float, parentW: Int = 0, parentH: Int = 0) {
        if (isLocked) return
        val lp = layoutParams as? FrameLayout.LayoutParams ?: return
        val pView = parent as? View
        val pW = if (parentW > 0) parentW else pView?.width?.takeIf { it > 0 } ?: 800
        val pH = if (parentH > 0) parentH else pView?.height?.takeIf { it > 0 } ?: 480
        val hudW = (width * scaleX).toInt().coerceAtLeast(dp(80))
        val hudH = (height * scaleY).toInt().coerceAtLeast(dp(36))

        val currentLeft = if (lp.leftMargin > 0) lp.leftMargin else this.left
        val currentTop = if (lp.topMargin > 0) lp.topMargin else this.top
        var newLeft = (currentLeft + deltaX).toInt()
        var newTop = (currentTop + deltaY).toInt()

        newLeft = newLeft.coerceIn(0, (pW - hudW).coerceAtLeast(0))
        newTop = newTop.coerceIn(0, (pH - hudH).coerceAtLeast(0))

        lp.gravity = Gravity.TOP or Gravity.START
        lp.leftMargin = newLeft
        lp.topMargin = newTop
        layoutParams = lp
        layout(newLeft, newTop, newLeft + (width.takeIf { it > 0 } ?: hudW), newTop + (height.takeIf { it > 0 } ?: hudH))
        requestLayout()

        val density = resources.displayMetrics.density
        val posXdp = (newLeft / density).roundToInt()
        val posYdp = (newTop / density).roundToInt()
        WazeHudManager.setPosition(context, posXdp, posYdp)
    }

    private fun createLockButton(): TextView {
        isLocked = WazeHudManager.isLocked(context)
        val btn = TextView(context).apply {
            text = if (isLocked) "🔒" else "🔓"
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(4), dp(8), dp(4))
            background = rounded(
                if (isLocked) Color.parseColor("#40EF4444") else Color.parseColor("#400284C7"),
                8f,
                if (isLocked) Color.parseColor("#EF4444") else Color.parseColor("#0284C7"),
                1
            )
            layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginStart = dp(6)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                toggleLock()
            }
        }
        lockButton = btn
        return btn
    }

    fun toggleLock() {
        isLocked = !isLocked
        WazeHudManager.setLocked(context, isLocked)
        updateLockButtonUi()
        val toastText = if (isLocked) {
            val lp = layoutParams as? FrameLayout.LayoutParams
            if (lp != null) {
                val density = resources.displayMetrics.density
                val posXdp = (lp.leftMargin / density).roundToInt()
                val posYdp = (lp.topMargin / density).roundToInt()
                WazeHudManager.setPosition(context, posXdp, posYdp)
                "🔒 Đã khóa vị trí HUD (${posXdp}dp, ${posYdp}dp)"
            } else {
                "🔒 Đã khóa vị trí HUD"
            }
        } else {
            "🔓 Đã mở khóa - Chạm hoặc kéo để đặt vị trí HUD"
        }
        Handler(Looper.getMainLooper()).post {
            try {
                android.widget.Toast.makeText(context.applicationContext, toastText, android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {}
        }
    }

    private fun updateLockButtonUi() {
        lockButton?.apply {
            text = if (isLocked) "🔒" else "🔓"
            background = rounded(
                if (isLocked) Color.parseColor("#40EF4444") else Color.parseColor("#400284C7"),
                8f,
                if (isLocked) Color.parseColor("#EF4444") else Color.parseColor("#0284C7"),
                1
            )
        }
    }

    override fun onInterceptTouchEvent(ev: android.view.MotionEvent): Boolean {
        if (isPreviewMode) return super.onInterceptTouchEvent(ev)
        if (isTouchOnLockLocal(ev.x, ev.y)) {
            return false
        }
        if (isLocked) return super.onInterceptTouchEvent(ev)
        when (ev.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> {
                startRawX = ev.rawX
                startRawY = ev.rawY
                isDragging = false
                dX = this.left - ev.rawX
                dY = this.top - ev.rawY
                return false
            }
            android.view.MotionEvent.ACTION_MOVE -> {
                val dist = Math.hypot((ev.rawX - startRawX).toDouble(), (ev.rawY - startRawY).toDouble())
                if (dist > dp(6)) {
                    isDragging = true
                    return true
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        if (isPreviewMode) return super.onTouchEvent(event)

        if (isTouchOnLockLocal(event.x, event.y)) {
            if (event.actionMasked == android.view.MotionEvent.ACTION_UP) {
                toggleLock()
            }
            return true
        }

        if (isLocked) return super.onTouchEvent(event)
        val parentView = parent as? View ?: return super.onTouchEvent(event)
        val lp = layoutParams as? FrameLayout.LayoutParams ?: return super.onTouchEvent(event)

        when (event.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> {
                startRawX = event.rawX
                startRawY = event.rawY
                dX = this.left - event.rawX
                dY = this.top - event.rawY
                isDragging = false
                return true
            }
            android.view.MotionEvent.ACTION_MOVE -> {
                val dist = Math.hypot((event.rawX - startRawX).toDouble(), (event.rawY - startRawY).toDouble())
                if (dist > dp(4) || isDragging) {
                    isDragging = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    val parentW = parentView.width.takeIf { it > 0 } ?: 800
                    val parentH = parentView.height.takeIf { it > 0 } ?: 480
                    val hudW = (width * scaleX).toInt().coerceAtLeast(dp(80))
                    val hudH = (height * scaleY).toInt().coerceAtLeast(dp(36))

                    var newLeft = (event.rawX + dX).toInt()
                    var newTop = (event.rawY + dY).toInt()

                    newLeft = newLeft.coerceIn(0, (parentW - hudW).coerceAtLeast(0))
                    newTop = newTop.coerceIn(0, (parentH - hudH).coerceAtLeast(0))

                    lp.gravity = Gravity.TOP or Gravity.START
                    lp.leftMargin = newLeft
                    lp.topMargin = newTop
                    layoutParams = lp
                    requestLayout()
                    return true
                }
            }
            android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    val density = resources.displayMetrics.density
                    val posXdp = (lp.leftMargin / density).roundToInt()
                    val posYdp = (lp.topMargin / density).roundToInt()
                    WazeHudManager.setPosition(context, posXdp, posYdp)
                    return true
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isPreviewMode) {
            startObservingState()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopObservingState()
        pulseHandler.removeCallbacks(pulseRunnable)
        // The view can be detached/re-attached when Android Auto recreates a surface.
        // Reset pulse state so a later attach can start the animation again.
        isPulsingOverSpeed = false
        pulseState = false
    }

    var onAlertUpdate: ((VietmapAlertData) -> Unit)? = null

    fun startObservingState() {
        stateJob?.cancel()
        stateJob = mainScope.launch {
            VietmapStateRepository.alertState.collectLatest { data ->
                updateUi(data)
                onAlertUpdate?.invoke(data)
            }
        }
    }

    fun stopObservingState() {
        stateJob?.cancel()
        stateJob = null
    }

    fun updateUi(data: VietmapAlertData) {
        lastData = data

        // 1. Speed Limit
        val limit = data.speedLimit
        if (limit != null && limit > 0) {
            speedLimitText?.text = "$limit"
            speedLimitBadge?.visibility = VISIBLE
        } else {
            speedLimitText?.text = "--"
            speedLimitBadge?.visibility = VISIBLE
        }

        // 1.5 Secondary Speed Limit
        val secLimit = data.secondarySpeedLimit
        if (secLimit != null && secLimit > 0) {
            secondaryLimitText?.text = "$secLimit"
            secondaryLimitBadge?.visibility = VISIBLE
        } else {
            secondaryLimitBadge?.visibility = GONE
        }

        // 2. Current Speed
        val vSpeed = data.currentSpeed
        val displaySpeed = if (vSpeed > 0) vSpeed else GpsSpeedManager.rawGpsSpeed.value
        currentSpeedText?.text = "$displaySpeed"

        val masterEnabled = isPreviewMode || WazeHudManager.isMasterAlertsEnabled(context)
        val overspeedAlertEnabled = isPreviewMode || WazeHudManager.isOverspeedAlertEnabled(context)
        val turnManeuverEnabled = isPreviewMode || WazeHudManager.isTurnManeuverEnabled(context)

        // Over speed handling
        val shouldPulse = data.isOverSpeed && masterEnabled && overspeedAlertEnabled
        if (shouldPulse) {
            if (!isPulsingOverSpeed) {
                isPulsingOverSpeed = true
                pulseHandler.post(pulseRunnable)
            }
        } else {
            if (isPulsingOverSpeed) {
                isPulsingOverSpeed = false
                pulseHandler.removeCallbacks(pulseRunnable)
            }
            applyStyleBackground(overSpeed = false)

            val spd = displaySpeed
            val lim = data.speedLimit ?: 999
            if (spd >= lim - 5 && lim < 999 && overspeedAlertEnabled) {
                currentSpeedText?.setTextColor(Color.parseColor("#F1C40F"))
            } else {
                currentSpeedText?.setTextColor(Color.WHITE)
            }
        }

        // 3. Turn Arrow & Distance. Waze maneuver text is primary because it
        // distinguishes keep/exit/sharp turns better than a coarse numeric code.
        val wazeManeuver = listOfNotNull(data.turnAction, data.turnDescription)
            .joinToString(" ")
            .lowercase(Locale.ROOT)
            .replace('_', '-')
        val arrowStr = when (data.turnCode) {
            1 -> "↑"
            2 -> "↰"
            3 -> "↱"
            4, 13, 15 -> "↖"
            5, 14, 16 -> "↗"
            6 -> "↶"
            7 -> "↷"
            8, 9 -> "↩"
            10 -> "⟳"
            11 -> "↶"
            12 -> "↷"
            17 -> "🏁"
            19 -> "↑"
            20 -> "↩"
            else -> when {
                wazeManeuver.contains("sharp-left") || wazeManeuver.contains("rẽ gắt sang trái") -> "↶"
                wazeManeuver.contains("sharp-right") || wazeManeuver.contains("rẽ gắt sang phải") -> "↷"
                wazeManeuver.contains("exit-left") || wazeManeuver.contains("slight-left") ||
                    wazeManeuver.contains("keep-left") || wazeManeuver.contains("chếch sang trái") -> "↖"
                wazeManeuver.contains("exit-right") || wazeManeuver.contains("slight-right") ||
                    wazeManeuver.contains("keep-right") || wazeManeuver.contains("chếch sang phải") -> "↗"
                wazeManeuver.contains("u-turn") || wazeManeuver.contains("uturn") ||
                    wazeManeuver.contains("quay đầu") -> "↩"
                wazeManeuver.contains("roundabout") || wazeManeuver.contains("traffic-circle") ||
                    wazeManeuver.contains("vòng xuyến") || wazeManeuver.contains("bùng binh") -> "⟳"
                wazeManeuver.contains("destination") || wazeManeuver.contains("arrive") ||
                    wazeManeuver.contains("đến nơi") -> "🏁"
                wazeManeuver.contains("turn-left") || wazeManeuver.contains("rẽ trái") -> "↰"
                wazeManeuver.contains("turn-right") || wazeManeuver.contains("rẽ phải") -> "↱"
                wazeManeuver.contains("straight") || wazeManeuver.contains("continue") -> "↑"
                else -> ""
            }
        }
        turnIcon?.text = arrowStr

        val distM = data.distanceToTurnMeters
        val dist = when {
            distM >= 1000 -> String.format(Locale.US, "%.1f km", distM / 1000f)
            distM > 0 -> "${distM}m"
            else -> ""
        }
        turnDistanceText?.text = dist

        // Alert distance and maneuver distance are different pieces of data. The
        // previous fallback used alert.distanceText as a turn distance, which could
        // display a camera's "250m" next to a straight-ahead arrow. Only show the
        // maneuver block when real navigation data is present.
        val hasManeuver = arrowStr.isNotBlank() && (
            data.turnCode > 0 ||
                data.distanceToTurnMeters > 0 ||
                (!data.turnAction.isBlank() && data.turnAction != "straight") ||
                !data.turnDescription.isNullOrBlank()
            )
        val turnVis = if (turnManeuverEnabled && hasManeuver) VISIBLE else GONE
        turnIcon?.visibility = turnVis
        turnDistanceText?.visibility = if (turnVis == VISIBLE && dist.isNotBlank()) VISIBLE else GONE
        turnContainer?.visibility = when {
            turnVis == VISIBLE -> VISIBLE
            activeStyleId == 6 -> INVISIBLE
            else -> GONE
        }

        // 4. Road Name & Lane Guidance
        val road = when {
            !data.roadName.isNullOrBlank() -> data.roadName
            !data.nextRoadName.isNullOrBlank() -> data.nextRoadName
            !data.turnDescription.isNullOrBlank() -> data.turnDescription
            else -> ""
        }
        val laneGuidance = if (isPreviewMode || WazeHudManager.isLaneGuidanceEnabled(context)) {
            data.laneGuidance
                ?.replace("[", "")
                ?.replace("]", "")
                ?.replace(34.toChar().toString(), "")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        } else null

        val roadAndLane = buildList {
            if (road.isNotBlank()) add(road)
            if (!laneGuidance.isNullOrBlank()) add("Làn: $laneGuidance")
        }.joinToString(" • ")

        roadNameText?.text = road
        roadNameText?.visibility = if (road.isBlank() || activeStyleId == 7) GONE else VISIBLE

        val compactLane = laneGuidance
            ?.replace(",", "  ")
            ?.replace("|", "  ")
            ?.replace(";", "  ")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        laneGuidanceText?.text = compactLane.orEmpty()
        laneGuidanceText?.visibility = when {
            !compactLane.isNullOrBlank() -> VISIBLE
            activeStyleId == 6 -> INVISIBLE
            else -> GONE
        }

        // 5. ETA
        val etaStr = data.etaTime ?: "--:--"
        val remKm = data.remainingDistanceKm?.let { " • ${it}km" } ?: ""
        etaText?.text = "ETA $etaStr$remKm"

        // 6. Waze Connection Dot
        val isConn = data.isConnected || isPreviewMode
        wazeDot?.background = circle(if (isConn) Color.parseColor("#10B981") else Color.parseColor("#64748B"), 0, 0f)

        // 7. Compact Waze-style road alert cell.
        // The closest HLP alert is the only one rendered in the floating HUD:
        // icon + distance, matching the small bubble shown over other apps.
        val rawAlertTitle = data.alertTitle?.replace("VML-TPMS", "")?.replace("TPMS", "")?.trim()
        val cleanAlertTitle = if (rawAlertTitle != null && !WazeHlpWebSocketManager.isIgnoredText(rawAlertTitle)) rawAlertTitle else null

        val primaryAlert = data.upcomingAlerts.firstOrNull()
        val effWarning = when {
            primaryAlert != null && primaryAlert.warningType != VietmapWarningType.NONE -> primaryAlert.warningType
            data.warningType != VietmapWarningType.NONE -> data.warningType
            !cleanAlertTitle.isNullOrBlank() -> VietmapIconClassifier.classifyFromText(cleanAlertTitle)
                .takeIf { it != VietmapWarningType.NONE }
                ?: WazeHlpWebSocketManager.mapWarningType(cleanAlertTitle)
            else -> VietmapWarningType.NONE
        }

        val isAlertFresh = isPreviewMode || WazeAlertPolicy.isAlertFresh(data)
        val categoryEnabled = isPreviewMode ||
            !WazeAlertPolicy.isCameraCategory(effWarning) ||
            WazeHudManager.isSpeedCameraEnabled(context)
        val hasAlertData = primaryAlert != null ||
            effWarning != VietmapWarningType.NONE ||
            !cleanAlertTitle.isNullOrBlank()
        val hasActiveAlertBadge = hasAlertData && masterEnabled && categoryEnabled && isAlertFresh

        if (hasActiveAlertBadge) {
            val displayWarning = if (effWarning != VietmapWarningType.NONE) {
                effWarning
            } else {
                VietmapWarningType.HAZARD
            }

            val code = primaryAlert?.code ?: 0
            val value = primaryAlert?.value

            val alertDistanceMeters = primaryAlert?.distanceMeters
                ?: WazeAlertPolicy.effectiveAlertDistanceMeters(data)
            val distance = WazeAlertPolicy.formatDistance(alertDistanceMeters)

            val semanticTitle = primaryAlert?.title?.takeIf { it.isNotBlank() }
                ?: WazeAlertPolicy.shortTitle(displayWarning, cleanAlertTitle)

            val accent = WazeAlertPolicy.accentColor(displayWarning, alertDistanceMeters)
            val urgent = alertDistanceMeters != null &&
                alertDistanceMeters <= 150 &&
                WazeAlertPolicy.isRoadHazard(displayWarning)

            alertContainer?.background = rounded(
                Color.parseColor("#E6161B22"),
                12f,
                accent,
                if (urgent) 2 else 1
            )
            alertIconView?.setAlert(code, displayWarning, value)
            alertDistanceText?.text = if (distance.isNotBlank()) distance else "•"
            alertDistanceText?.setTextColor(Color.WHITE)
            alertDistanceText?.visibility = VISIBLE

            // Hidden visually, but useful for accessibility/debugging.
            alertTitleText?.text = semanticTitle
            alertTitleText?.contentDescription = buildString {
                append(semanticTitle)
                if (distance.isNotBlank()) append(", ").append(distance)
            }
            alertTitleText?.visibility = GONE
            alertUpcomingText?.visibility = GONE

            alertContainer?.contentDescription = alertTitleText?.contentDescription
            alertContainer?.visibility = VISIBLE
        } else {
            alertUpcomingText?.visibility = GONE
            alertTitleText?.visibility = GONE
            alertContainer?.visibility = if (activeStyleId == 6 || activeStyleId == 7) INVISIBLE else GONE
        }

        if (activeStyleId == 6 || activeStyleId == 7) {
            val extras = if (primaryAlert != null) data.upcomingAlerts.drop(1) else data.upcomingAlerts
            referenceAlertContainers.forEachIndexed { index, cell ->
                val item = extras.getOrNull(index)
                if (item != null && item.warningType != VietmapWarningType.NONE) {
                    referenceAlertIcons.getOrNull(index)?.setAlert(item.code, item.warningType, item.value)
                    referenceAlertDistances.getOrNull(index)?.text =
                        WazeAlertPolicy.formatDistance(item.distanceMeters).ifBlank { "•" }
                    cell.visibility = VISIBLE
                } else {
                    cell.visibility = INVISIBLE
                }
            }
            referenceReportButton?.visibility = if (data.isConnected || isPreviewMode) VISIBLE else GONE
            referenceReportButton?.alpha = 1f
        } else {
            referenceAlertContainers.forEach { it.visibility = GONE }
        }
    }

    fun setSamplePreview(styleId: Int) {
        setPreviewMode(true)
        applyHudConfig(styleId)
        val live = VietmapStateRepository.alertState.value
        val testData = VietmapAlertData(
            isConnected = live.isConnected,
            currentSpeed = live.currentSpeed,
            speedLimit = live.speedLimit,
            secondarySpeedLimit = live.secondarySpeedLimit,
            turnCode = live.turnCode,
            turnAction = live.turnAction,
            turnDescription = live.turnDescription,
            distanceToTurnMeters = live.distanceToTurnMeters,
            distanceText = "250m",
            distanceMeters = 250,
            warningType = VietmapWarningType.RED_LIGHT_CAMERA,
            alertTitle = "Camera đèn đỏ",
            upcomingAlerts = listOf(
                WazeAlertItem(
                    code = 2,
                    warningType = VietmapWarningType.SPEED_CAMERA,
                    title = "Camera tốc độ",
                    distanceMeters = 400
                ),
                WazeAlertItem(
                    code = 8,
                    warningType = VietmapWarningType.SPEED_LIMIT_ZONE,
                    title = "Giới hạn tốc độ",
                    distanceMeters = 700,
                    value = 80
                ),
                WazeAlertItem(
                    code = 4,
                    warningType = VietmapWarningType.HAZARD,
                    title = "Nguy hiểm",
                    distanceMeters = 950
                )
            ),
            roadName = live.roadName,
            nextRoadName = live.nextRoadName ?: "Nguyễn Văn Linh",
            laneGuidance = live.laneGuidance ?: "↑  ↑  ↗",
            etaTime = live.etaTime,
            remainingDistanceKm = live.remainingDistanceKm,
            isOverSpeed = live.speedLimit?.let { live.currentSpeed > it } ?: false
        )
        updateUi(testData)
    }

    private fun rounded(fill: Int, radiusDp: Float, stroke: Int = 0, strokeDp: Int = 0): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fill)
            cornerRadius = dp(radiusDp).toFloat()
            if (strokeDp > 0) {
                setStroke(dp(strokeDp), stroke)
            }
        }
    }

    private fun circle(fill: Int, stroke: Int = 0, strokeDp: Float = 0f): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fill)
            if (strokeDp > 0f && stroke != 0) {
                setStroke(dp(strokeDp), stroke)
            }
        }
    }

    private fun dp(v: Int): Int = (v * context.resources.displayMetrics.density).roundToInt()
    private fun dp(v: Float): Int = (v * context.resources.displayMetrics.density).roundToInt()
}
