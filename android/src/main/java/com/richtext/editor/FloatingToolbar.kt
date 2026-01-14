package com.richtext.editor

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.UnderlineSpan
import android.text.style.BackgroundColorSpan

class FloatingToolbar(context: Context) : LinearLayout(context) {

    interface ToolbarActionListener {
        fun onBoldClick()
        fun onItalicClick()
        fun onUnderlineClick()
        fun onStrikethroughClick()
        fun onCodeClick()
        fun onHighlightClick()
        fun onHeadingClick()
        fun onBulletListClick()
        fun onNumberedListClick()
        fun onQuoteClick()
        fun onChecklistClick()
        fun onLinkClick()
        fun onUndoClick()
        fun onRedoClick()
        fun onClearFormattingClick()
        fun onIndentClick()
        fun onOutdentClick()
        fun onAlignLeftClick()
        fun onAlignCenterClick()
        fun onAlignRightClick()
    }

    var listener: ToolbarActionListener? = null

    private val density = context.resources.displayMetrics.density
    private val buttonSize = (36 * density).toInt()
    private val toolbarHeight = (52 * density).toInt()
    private val buttonSpacing = (8 * density).toInt()

    private val toolbarBackgroundColor = Color.parseColor("#2D2D2D")
    private val activeColor = Color.parseColor("#5082C8")
    private val inactiveColor = Color.WHITE

    private val buttons = mutableMapOf<String, TextView>()
    private val buttonContainer: LinearLayout
    private val scrollView: HorizontalScrollView

    private var enabledOptions: List<String> = listOf(
        "bold", "italic", "underline", "strikethrough", "code", "highlight",
        "heading", "bullet", "numbered", "quote", "checklist",
        "link", "undo", "redo", "clearFormatting",
        "indent", "outdent",
        "alignLeft", "alignCenter", "alignRight"
    )

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding((8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt())

        // Set background with rounded corners and shadow
        val bg = GradientDrawable().apply {
            setColor(toolbarBackgroundColor)
            cornerRadius = 10 * density
        }
        background = bg
        elevation = 6 * density

        // Left arrow
        val leftArrow = createArrowButton("‹").apply {
            setOnClickListener { scrollLeft() }
        }
        addView(leftArrow)

        // Scroll view for buttons
        scrollView = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
        }

        buttonContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        scrollView.addView(buttonContainer)
        addView(scrollView)

        // Right arrow
        val rightArrow = createArrowButton("›").apply {
            setOnClickListener { scrollRight() }
        }
        addView(rightArrow)

        buildButtons()
    }

    private fun createArrowButton(text: String): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 24f
            setTextColor(Color.parseColor("#FFFFFF"))
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            val params = LayoutParams((32 * density).toInt(), ViewGroup.LayoutParams.MATCH_PARENT)
            layoutParams = params
            // Add touch feedback
            val bg = GradientDrawable().apply {
                cornerRadius = 4 * density
                setColor(Color.TRANSPARENT)
            }
            background = bg
        }
    }

    private fun scrollLeft() {
        val scrollAmount = (150 * density).toInt()
        scrollView.smoothScrollBy(-scrollAmount, 0)
    }

    private fun scrollRight() {
        val scrollAmount = (150 * density).toInt()
        scrollView.smoothScrollBy(scrollAmount, 0)
    }

    fun setToolbarOptions(options: List<String>?) {
        enabledOptions = options ?: listOf(
            "bold", "italic", "underline", "strikethrough", "code", "highlight",
            "heading", "bullet", "numbered", "quote", "checklist",
            "link", "undo", "redo", "clearFormatting",
            "indent", "outdent",
            "alignLeft", "alignCenter", "alignRight"
        )
        buildButtons()
    }

    private fun buildButtons() {
        buttonContainer.removeAllViews()
        buttons.clear()

        for (option in enabledOptions) {
            val button = createButton(option)
            if (button != null) {
                buttons[option] = button
                buttonContainer.addView(button)
            }
        }
    }

    private fun createButton(option: String): TextView? {
        val button = TextView(context).apply {
            gravity = Gravity.CENTER
            setTextColor(inactiveColor)

            val bg = GradientDrawable().apply {
                cornerRadius = 6 * density
                setColor(Color.TRANSPARENT)
            }
            background = bg

            val params = LayoutParams(buttonSize, buttonSize)
            params.marginEnd = buttonSpacing
            layoutParams = params
        }

        when (option) {
            "bold" -> {
                button.text = "B"
                button.textSize = 18f
                button.setTypeface(null, Typeface.BOLD)
                button.setOnClickListener { listener?.onBoldClick() }
            }
            "italic" -> {
                button.text = "I"
                button.textSize = 18f
                button.setTypeface(null, Typeface.ITALIC)
                button.setOnClickListener { listener?.onItalicClick() }
            }
            "underline" -> {
                val spannable = SpannableString("U")
                spannable.setSpan(UnderlineSpan(), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                button.text = spannable
                button.textSize = 18f
                button.setOnClickListener { listener?.onUnderlineClick() }
            }
            "strikethrough" -> {
                val spannable = SpannableString("S")
                spannable.setSpan(StrikethroughSpan(), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                button.text = spannable
                button.textSize = 18f
                button.setOnClickListener { listener?.onStrikethroughClick() }
            }
            "code" -> {
                button.text = "</>"
                button.textSize = 12f
                button.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL)
                button.setOnClickListener { listener?.onCodeClick() }
            }
            "highlight" -> {
                // Use marker/highlighter icon representation
                button.text = "✎"
                button.textSize = 18f
                button.setOnClickListener { listener?.onHighlightClick() }
            }
            "heading" -> {
                button.text = "H1"
                button.textSize = 14f
                button.setTypeface(null, Typeface.BOLD)
                button.setOnClickListener { listener?.onHeadingClick() }
            }
            "bullet" -> {
                button.text = "•≡"
                button.textSize = 14f
                button.setOnClickListener { listener?.onBulletListClick() }
            }
            "numbered" -> {
                button.text = "1."
                button.textSize = 14f
                button.setOnClickListener { listener?.onNumberedListClick() }
            }
            "quote" -> {
                button.text = "❞"
                button.textSize = 18f
                button.setOnClickListener { listener?.onQuoteClick() }
            }
            "checklist" -> {
                button.text = "☑"
                button.textSize = 18f
                button.setOnClickListener { listener?.onChecklistClick() }
            }
            "link" -> {
                button.text = "🔗"
                button.textSize = 14f
                button.setOnClickListener { listener?.onLinkClick() }
            }
            "undo" -> {
                button.text = "↩"
                button.textSize = 18f
                button.setOnClickListener { listener?.onUndoClick() }
            }
            "redo" -> {
                button.text = "↪"
                button.textSize = 18f
                button.setOnClickListener { listener?.onRedoClick() }
            }
            "clearFormatting" -> {
                val spannable = SpannableString("Tx")
                spannable.setSpan(StrikethroughSpan(), 0, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                button.text = spannable
                button.textSize = 14f
                button.setOnClickListener { listener?.onClearFormattingClick() }
            }
            "indent" -> {
                button.text = "→⊢"
                button.textSize = 12f
                button.setOnClickListener { listener?.onIndentClick() }
            }
            "outdent" -> {
                button.text = "⊣←"
                button.textSize = 12f
                button.setOnClickListener { listener?.onOutdentClick() }
            }
            "alignLeft" -> {
                button.text = "≡"
                button.textSize = 18f
                button.gravity = Gravity.START or Gravity.CENTER_VERTICAL
                button.setOnClickListener { listener?.onAlignLeftClick() }
            }
            "alignCenter" -> {
                button.text = "≡"
                button.textSize = 18f
                button.gravity = Gravity.CENTER
                button.setOnClickListener { listener?.onAlignCenterClick() }
            }
            "alignRight" -> {
                button.text = "≡"
                button.textSize = 18f
                button.gravity = Gravity.END or Gravity.CENTER_VERTICAL
                button.setOnClickListener { listener?.onAlignRightClick() }
            }
            else -> return null
        }

        return button
    }

    fun updateButtonStates(
        bold: Boolean = false,
        italic: Boolean = false,
        underline: Boolean = false,
        strikethrough: Boolean = false,
        code: Boolean = false,
        highlight: Boolean = false,
        heading: Boolean = false,
        bullet: Boolean = false,
        numbered: Boolean = false,
        quote: Boolean = false,
        checklist: Boolean = false,
        alignLeft: Boolean = true,
        alignCenter: Boolean = false,
        alignRight: Boolean = false
    ) {
        val states = mapOf(
            "bold" to bold,
            "italic" to italic,
            "underline" to underline,
            "strikethrough" to strikethrough,
            "code" to code,
            "highlight" to highlight,
            "heading" to heading,
            "bullet" to bullet,
            "numbered" to numbered,
            "quote" to quote,
            "checklist" to checklist,
            "alignLeft" to alignLeft,
            "alignCenter" to alignCenter,
            "alignRight" to alignRight
        )

        for ((option, button) in buttons) {
            val isActive = states[option] ?: false
            button.setTextColor(if (isActive) activeColor else inactiveColor)
            (button.background as? GradientDrawable)?.setColor(
                if (isActive) Color.parseColor("#40FFFFFF") else Color.TRANSPARENT
            )
        }
    }

    fun getToolbarWidth(): Int {
        val screenWidth = context.resources.displayMetrics.widthPixels
        val maxWidth = (screenWidth * 0.9).toInt()
        val buttonCount = enabledOptions.size
        val calculatedWidth = (buttonCount * buttonSize) + ((buttonCount - 1) * buttonSpacing) + (56 * density).toInt()
        return minOf(calculatedWidth, maxWidth)
    }

    fun getToolbarHeight(): Int = toolbarHeight
}
