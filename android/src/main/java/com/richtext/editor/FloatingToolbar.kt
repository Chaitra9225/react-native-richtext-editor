package com.richtext.editor

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.UnderlineSpan
import android.text.style.BackgroundColorSpan
import androidx.core.content.ContextCompat

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
        fun onMediaAttachmentClick()
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

    private val buttons = mutableMapOf<String, ImageView>()
    private val buttonContainer: LinearLayout
    private val scrollView: HorizontalScrollView

    private var enabledOptions: List<String> = listOf(
        "bold", "italic", "underline", "strikethrough", "code", "highlight",
        "heading", "bullet", "numbered", "quote", "checklist",
        "mediaAttachment",
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
            textSize = 20f
            setTextColor(Color.parseColor("#FFFFFF"))
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            val params = LayoutParams((16 * density).toInt(), ViewGroup.LayoutParams.MATCH_PARENT)
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
            "mediaAttachment",
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

    private fun getDrawableResId(option: String): Int {
        return when (option) {
            "bold" -> R.drawable.ic_format_bold
            "italic" -> R.drawable.ic_format_italic
            "underline" -> R.drawable.ic_format_underline
            "strikethrough" -> R.drawable.ic_format_strikethrough
            "code" -> R.drawable.ic_format_code
            "highlight" -> R.drawable.ic_format_highlight
            "heading" -> R.drawable.ic_format_heading
            "bullet" -> R.drawable.ic_format_list_bulleted
            "numbered" -> R.drawable.ic_format_list_numbered
            "quote" -> R.drawable.ic_format_quote
            "checklist" -> R.drawable.ic_format_checklist
            "mediaAttachment" -> R.drawable.ic_format_media_attachment
            "link" -> R.drawable.ic_format_link
            "undo" -> R.drawable.ic_format_undo
            "redo" -> R.drawable.ic_format_redo
            "clearFormatting" -> R.drawable.ic_format_clear
            "indent" -> R.drawable.ic_format_indent
            "outdent" -> R.drawable.ic_format_outdent
            "alignLeft" -> R.drawable.ic_format_align_left
            "alignCenter" -> R.drawable.ic_format_align_center
            "alignRight" -> R.drawable.ic_format_align_right
            else -> 0
        }
    }

    private val iconSize = (20 * density).toInt()
    private val iconPadding = (10 * density).toInt()

    private fun createButton(option: String): ImageView? {
        val drawableResId = getDrawableResId(option)
        if (drawableResId == 0) return null

        val button = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(iconPadding, iconPadding, iconPadding, iconPadding)

            val bg = GradientDrawable().apply {
                cornerRadius = 6 * density
                setColor(Color.TRANSPARENT)
            }
            background = bg

            val params = LayoutParams(buttonSize, buttonSize)
            params.marginEnd = buttonSpacing
            layoutParams = params

            // Set the icon - drawable will scale to fit within padding
            setImageResource(drawableResId)
            colorFilter = PorterDuffColorFilter(inactiveColor, PorterDuff.Mode.SRC_IN)
        }

        when (option) {
            "bold" -> button.setOnClickListener { listener?.onBoldClick() }
            "italic" -> button.setOnClickListener { listener?.onItalicClick() }
            "underline" -> button.setOnClickListener { listener?.onUnderlineClick() }
            "strikethrough" -> button.setOnClickListener { listener?.onStrikethroughClick() }
            "code" -> button.setOnClickListener { listener?.onCodeClick() }
            "highlight" -> button.setOnClickListener { listener?.onHighlightClick() }
            "heading" -> button.setOnClickListener { listener?.onHeadingClick() }
            "bullet" -> button.setOnClickListener { listener?.onBulletListClick() }
            "numbered" -> button.setOnClickListener { listener?.onNumberedListClick() }
            "quote" -> button.setOnClickListener { listener?.onQuoteClick() }
            "checklist" -> button.setOnClickListener { listener?.onChecklistClick() }
            "mediaAttachment" -> button.setOnClickListener { listener?.onMediaAttachmentClick() }
            "link" -> button.setOnClickListener { listener?.onLinkClick() }
            "undo" -> button.setOnClickListener { listener?.onUndoClick() }
            "redo" -> button.setOnClickListener { listener?.onRedoClick() }
            "clearFormatting" -> button.setOnClickListener { listener?.onClearFormattingClick() }
            "indent" -> button.setOnClickListener { listener?.onIndentClick() }
            "outdent" -> button.setOnClickListener { listener?.onOutdentClick() }
            "alignLeft" -> button.setOnClickListener { listener?.onAlignLeftClick() }
            "alignCenter" -> button.setOnClickListener { listener?.onAlignCenterClick() }
            "alignRight" -> button.setOnClickListener { listener?.onAlignRightClick() }
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
        mediaAttachment: Boolean = false,
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
            "mediaAttachment" to mediaAttachment,
            "alignLeft" to alignLeft,
            "alignCenter" to alignCenter,
            "alignRight" to alignRight
        )

        for ((option, button) in buttons) {
            val isActive = states[option] ?: false
            button.colorFilter = PorterDuffColorFilter(
                if (isActive) activeColor else inactiveColor,
                PorterDuff.Mode.SRC_IN
            )
            (button.background as? GradientDrawable)?.setColor(
                if (isActive) Color.parseColor("#40FFFFFF") else Color.TRANSPARENT
            )
        }
    }

    fun getToolbarWidth(): Int {
        val screenWidth = context.resources.displayMetrics.widthPixels
        val maxWidth = (screenWidth * 0.9).toInt()
        val buttonCount = enabledOptions.size
        val calculatedWidth = (buttonCount * buttonSize) + ((buttonCount - 1) * buttonSpacing) + (48 * density).toInt()
        return minOf(calculatedWidth, maxWidth)
    }

    fun getToolbarHeight(): Int = toolbarHeight
}
