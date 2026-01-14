package com.richtext.editor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.Layout
import android.text.method.ScrollingMovementMethod
import android.text.style.*
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.PopupWindow
import android.widget.FrameLayout
import android.app.AlertDialog
import android.widget.EditText
import android.widget.LinearLayout
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.events.RCTEventEmitter

class RichTextEditorView(context: Context) : androidx.appcompat.widget.AppCompatEditText(context),
    FloatingToolbar.ToolbarActionListener {

    private var placeholder: String = ""
    private var maxHeightValue: Int = 0
    private var showToolbar: Boolean = true
    private var variant: String = "outlined"
    private var density: Float = 1f
    private var isInternalChange = false
    private var lastReportedHeight: Float = 0f
    private var calculatedHeight: Float = 0f
    private var minHeightPx: Float = 0f
    private var isInitialized = false

    // For flat variant bottom border
    private val bottomBorderPaint = Paint().apply {
        color = Color.parseColor("#E0E0E0")
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }
    private var drawBottomBorder = false

    // Undo/Redo stacks
    private val undoStack = mutableListOf<CharSequence>()
    private val redoStack = mutableListOf<CharSequence>()
    private var lastSavedText: CharSequence = ""

    // Floating toolbar
    private var floatingToolbar: FloatingToolbar? = null
    private var toolbarPopup: PopupWindow? = null
    private var toolbarOptions: List<String>? = null

    // Store selection for toolbar actions (selection might be lost when clicking toolbar)
    private var savedSelectionStart: Int = 0
    private var savedSelectionEnd: Int = 0

    // Gesture detector for double-tap word selection
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            selectWordAtPosition(e.x, e.y)
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            // Hide toolbar on single tap (deselect)
            return false
        }
    })

    init {
        density = context.resources.displayMetrics.density
        minHeightPx = 44 * density
        calculatedHeight = minHeightPx
        bottomBorderPaint.strokeWidth = density

        val paddingHorizontal = (12 * density).toInt()
        val paddingVertical = (10 * density).toInt()

        setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
        textSize = 16f
        setTextColor(Color.BLACK)
        setHintTextColor(Color.parseColor("#9E9E9E"))
        gravity = Gravity.TOP or Gravity.START
        isFocusable = true
        isFocusableInTouchMode = true
        inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE

        // Disable vertical scrolling by default
        isVerticalScrollBarEnabled = false

        // Set white background by default
        setBackgroundColor(Color.WHITE)

        // Default outlined style
        applyVariantStyle()

        // Setup toolbar
        setupToolbar()

        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isInternalChange) {
                    sendContentChange()
                    saveToUndoStack()
                }
                post { updateContentSize() }
            }
        })

        setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                sendEvent("onEditorFocus", Arguments.createMap())
            } else {
                hideToolbar()
                sendEvent("onEditorBlur", Arguments.createMap())
            }
        }

        isInitialized = true
    }

    private fun setupToolbar() {
        floatingToolbar = FloatingToolbar(context).apply {
            listener = this@RichTextEditorView
        }

        toolbarPopup = PopupWindow(context).apply {
            contentView = floatingToolbar
            width = floatingToolbar?.getToolbarWidth() ?: WindowManager.LayoutParams.WRAP_CONTENT
            height = floatingToolbar?.getToolbarHeight() ?: WindowManager.LayoutParams.WRAP_CONTENT
            isOutsideTouchable = true
            isFocusable = false  // Don't take focus away from EditText
            isTouchable = true
            elevation = 10 * density

            // Don't dim the background
            setBackgroundDrawable(null)
        }

        // Disable the default text selection action mode (cut/copy/paste bar)
        customSelectionActionModeCallback = object : android.view.ActionMode.Callback {
            override fun onCreateActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?): Boolean {
                // Return true to create the action mode, but we'll clear the menu
                return true
            }

            override fun onPrepareActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?): Boolean {
                // Clear the default menu items
                menu?.clear()
                return true
            }

            override fun onActionItemClicked(mode: android.view.ActionMode?, item: android.view.MenuItem?): Boolean {
                return false
            }

            override fun onDestroyActionMode(mode: android.view.ActionMode?) {
                // Do nothing
            }
        }

        customInsertionActionModeCallback = object : android.view.ActionMode.Callback {
            override fun onCreateActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?): Boolean {
                return true
            }

            override fun onPrepareActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?): Boolean {
                menu?.clear()
                return true
            }

            override fun onActionItemClicked(mode: android.view.ActionMode?, item: android.view.MenuItem?): Boolean {
                return false
            }

            override fun onDestroyActionMode(mode: android.view.ActionMode?) {}
        }
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)

        // Skip if not initialized yet (during construction)
        if (!isInitialized) return

        android.util.Log.d("RichTextEditor", "onSelectionChanged: start=$selStart, end=$selEnd, showToolbar=$showToolbar, hasFocus=${hasFocus()}")

        // Save selection for toolbar actions
        if (selStart != selEnd) {
            savedSelectionStart = selStart
            savedSelectionEnd = selEnd
        }

        // Send selection change event
        val map = Arguments.createMap()
        map.putInt("start", selStart)
        map.putInt("end", selEnd)
        sendEvent("onSelectionChange", map)

        // Show/hide toolbar based on selection
        if (selStart != selEnd && showToolbar && hasFocus()) {
            android.util.Log.d("RichTextEditor", "Should show toolbar - selection exists")
            removeCallbacks(hideToolbarRunnable)
            // Use postDelayed to ensure layout is complete
            postDelayed({ showToolbarAtSelection() }, 50)
        } else {
            // Delay hiding to prevent flicker during selection changes
            android.util.Log.d("RichTextEditor", "Scheduling hide toolbar")
            postDelayed(hideToolbarRunnable, 200)
        }

        // Update toolbar button states
        if (selStart != selEnd) {
            updateToolbarButtonStates()
        }
    }

    private val hideToolbarRunnable = Runnable {
        android.util.Log.d("RichTextEditor", "hideToolbarRunnable: selectionStart=$selectionStart, selectionEnd=$selectionEnd")
        if (selectionStart == selectionEnd) {
            hideToolbar()
        }
    }

    private fun showToolbarAtSelection() {
        if (!showToolbar || toolbarPopup == null || floatingToolbar == null) return
        if (!isAttachedToWindow) return

        val selStart = selectionStart
        val selEnd = selectionEnd
        if (selStart == selEnd) return

        try {
            val textLayout = layout ?: return

            // Get the line of the end of selection
            val endLine = textLayout.getLineForOffset(selEnd)
            val lineBottom = textLayout.getLineBottom(endLine)

            val location = IntArray(2)
            getLocationOnScreen(location)

            val toolbarWidth = floatingToolbar?.getToolbarWidth() ?: (300 * density).toInt()
            val toolbarHeight = floatingToolbar?.getToolbarHeight() ?: (52 * density).toInt()

            // Center horizontally
            val screenWidth = context.resources.displayMetrics.widthPixels
            var x = (screenWidth - toolbarWidth) / 2

            // Ensure x is not negative
            if (x < 0) x = 0

            // Position BELOW the selection (like iOS: convertedRect.maxY + 8)
            var y = location[1] + lineBottom + paddingTop + (8 * density).toInt()

            // If toolbar would go off screen at bottom, show above selection
            val screenHeight = context.resources.displayMetrics.heightPixels
            if (y + toolbarHeight > screenHeight - (100 * density).toInt()) {
                val startLine = textLayout.getLineForOffset(selStart)
                val lineTop = textLayout.getLineTop(startLine)
                y = location[1] + lineTop + paddingTop - toolbarHeight - (8 * density).toInt()
            }

            // Ensure y is not negative
            if (y < 0) y = (8 * density).toInt()

            android.util.Log.d("RichTextEditor", "Showing toolbar at x=$x, y=$y, width=$toolbarWidth, height=$toolbarHeight")

            toolbarPopup?.width = toolbarWidth
            toolbarPopup?.height = toolbarHeight

            // Use windowToken to get the activity's window
            val token = windowToken
            if (token == null) {
                android.util.Log.e("RichTextEditor", "Window token is null, cannot show popup")
                return
            }

            if (toolbarPopup?.isShowing == true) {
                toolbarPopup?.update(x, y, toolbarWidth, toolbarHeight)
            } else {
                // Show at the root window using absolute coordinates
                val decorView = (context as? android.app.Activity)?.window?.decorView
                    ?: rootView
                toolbarPopup?.showAtLocation(decorView, Gravity.NO_GRAVITY, x, y)
                android.util.Log.d("RichTextEditor", "Toolbar popup shown: ${toolbarPopup?.isShowing}")
            }
        } catch (e: Exception) {
            android.util.Log.e("RichTextEditor", "Error showing toolbar", e)
            e.printStackTrace()
        }
    }

    private fun hideToolbar() {
        try {
            if (toolbarPopup?.isShowing == true) {
                android.util.Log.d("RichTextEditor", "Hiding toolbar")
                toolbarPopup?.dismiss()
            }
        } catch (e: Exception) {
            android.util.Log.e("RichTextEditor", "Error hiding toolbar", e)
            e.printStackTrace()
        }
    }

    private fun updateToolbarButtonStates() {
        val start = selectionStart
        val end = selectionEnd
        if (start == end || text == null) {
            floatingToolbar?.updateButtonStates()
            return
        }

        val spannable = text as? Spanned ?: return

        var hasBold = false
        var hasItalic = false
        var hasUnderline = false
        var hasStrikethrough = false
        var hasCode = false
        var hasHighlight = false

        // Check for style spans in selection
        spannable.getSpans(start, end, StyleSpan::class.java).forEach { span ->
            when (span.style) {
                Typeface.BOLD -> hasBold = true
                Typeface.ITALIC -> hasItalic = true
                Typeface.BOLD_ITALIC -> {
                    hasBold = true
                    hasItalic = true
                }
            }
        }

        hasUnderline = spannable.getSpans(start, end, UnderlineSpan::class.java).isNotEmpty()
        hasStrikethrough = spannable.getSpans(start, end, StrikethroughSpan::class.java).isNotEmpty()
        hasCode = spannable.getSpans(start, end, TypefaceSpan::class.java).any { it.family == "monospace" }
        hasHighlight = spannable.getSpans(start, end, BackgroundColorSpan::class.java).isNotEmpty()

        // Check for list prefixes
        val lineText = getCurrentLineText()
        val hasBullet = lineText.startsWith("• ")
        val hasNumbered = lineText.matches(Regex("^\\d+\\.\\s.*"))
        val hasQuote = lineText.startsWith("\"") && lineText.endsWith("\"")
        val hasChecklist = lineText.startsWith("☐ ") || lineText.startsWith("☑ ")

        // Check alignment
        var alignLeft = true
        var alignCenter = false
        var alignRight = false

        spannable.getSpans(start, end, AlignmentSpan.Standard::class.java).firstOrNull()?.let { span ->
            when (span.alignment) {
                Layout.Alignment.ALIGN_CENTER -> {
                    alignLeft = false
                    alignCenter = true
                }
                Layout.Alignment.ALIGN_OPPOSITE -> {
                    alignLeft = false
                    alignRight = true
                }
                else -> {}
            }
        }

        floatingToolbar?.updateButtonStates(
            bold = hasBold,
            italic = hasItalic,
            underline = hasUnderline,
            strikethrough = hasStrikethrough,
            code = hasCode,
            highlight = hasHighlight,
            bullet = hasBullet,
            numbered = hasNumbered,
            quote = hasQuote,
            checklist = hasChecklist,
            alignLeft = alignLeft,
            alignCenter = alignCenter,
            alignRight = alignRight
        )
    }

    private fun getCurrentLineText(): String {
        val text = text?.toString() ?: return ""
        val cursorPos = selectionStart
        if (cursorPos < 0) return ""

        var lineStart = cursorPos
        while (lineStart > 0 && text[lineStart - 1] != '\n') {
            lineStart--
        }

        var lineEnd = cursorPos
        while (lineEnd < text.length && text[lineEnd] != '\n') {
            lineEnd++
        }

        return text.substring(lineStart, lineEnd)
    }

    private fun getLineRange(): Pair<Int, Int> {
        val text = text?.toString() ?: return Pair(0, 0)
        val start = selectionStart
        val end = selectionEnd

        var lineStart = start
        while (lineStart > 0 && text[lineStart - 1] != '\n') {
            lineStart--
        }

        var lineEnd = end
        while (lineEnd < text.length && text[lineEnd] != '\n') {
            lineEnd++
        }

        return Pair(lineStart, lineEnd)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    private fun selectWordAtPosition(x: Float, y: Float) {
        val layout = layout ?: return
        val textContent = text?.toString() ?: return

        // Convert touch position to text offset
        val line = layout.getLineForVertical(y.toInt() - paddingTop)
        val offset = layout.getOffsetForHorizontal(line, x - paddingLeft)

        if (offset < 0 || offset > textContent.length) return

        // Find word boundaries
        var start = offset
        var end = offset

        // Move start to beginning of word
        while (start > 0 && !Character.isWhitespace(textContent[start - 1])) {
            start--
        }

        // Move end to end of word
        while (end < textContent.length && !Character.isWhitespace(textContent[end])) {
            end++
        }

        // Select the word if valid
        if (start < end) {
            setSelection(start, end)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (drawBottomBorder) {
            val y = height.toFloat() - bottomBorderPaint.strokeWidth / 2
            canvas.drawLine(0f, y, width.toFloat(), y, bottomBorderPaint)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        // After super.onMeasure, we have layout info
        val textLayout = layout
        if (textLayout != null) {
            val lineCount = textLayout.lineCount
            if (lineCount > 0) {
                val textHeight = textLayout.getLineTop(lineCount).toFloat()
                var desiredHeight = textHeight + paddingTop + paddingBottom

                if (desiredHeight < minHeightPx) {
                    desiredHeight = minHeightPx
                }

                if (maxHeightValue > 0) {
                    val maxHeightPx = maxHeightValue * density
                    if (desiredHeight > maxHeightPx) {
                        desiredHeight = maxHeightPx
                    }
                }

                calculatedHeight = desiredHeight

                // Apply the calculated height
                val measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
                setMeasuredDimension(measuredWidth, desiredHeight.toInt())
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        updateContentSize()
    }

    private fun updateContentSize() {
        val textLayout = layout ?: return

        val lineCount = textLayout.lineCount
        if (lineCount == 0) return

        val textHeight = textLayout.getLineTop(lineCount).toFloat()
        var newHeightPx = textHeight + paddingTop + paddingBottom

        if (newHeightPx < minHeightPx) {
            newHeightPx = minHeightPx
        }

        if (maxHeightValue > 0) {
            val maxHeightPx = maxHeightValue * density
            if (newHeightPx > maxHeightPx) {
                isVerticalScrollBarEnabled = true
                movementMethod = ScrollingMovementMethod.getInstance()
                newHeightPx = maxHeightPx
            } else {
                isVerticalScrollBarEnabled = false
            }
        }

        val previousHeight = calculatedHeight
        calculatedHeight = newHeightPx

        // Convert pixels to dp for React Native
        val newHeightDp = newHeightPx / density

        if (kotlin.math.abs(newHeightDp - lastReportedHeight) > 0.5f) {
            lastReportedHeight = newHeightDp
            val map = Arguments.createMap()
            map.putInt("height", newHeightDp.toInt())
            sendEvent("onSizeChange", map)

            // Request re-layout if height changed
            if (kotlin.math.abs(previousHeight - calculatedHeight) > 1f) {
                requestLayout()
            }
        }
    }

    private fun applyVariantStyle() {
        if (variant == "flat") {
            background = null
            setBackgroundColor(Color.WHITE)
            drawBottomBorder = true
        } else {
            drawBottomBorder = false
            val bg = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 8 * density
                setStroke((1 * density).toInt(), Color.parseColor("#E0E0E0"))
            }
            background = bg
        }
        invalidate()
    }

    private fun sendEvent(eventName: String, params: WritableMap) {
        try {
            val reactContext = context as? ReactContext ?: return
            reactContext.getJSModule(RCTEventEmitter::class.java)
                ?.receiveEvent(id, eventName, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendContentChange() {
        try {
            val map = Arguments.createMap()
            map.putString("text", text?.toString() ?: "")
            map.putArray("blocks", getBlocksArray())
            sendEvent("onContentChange", map)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveToUndoStack() {
        val currentText = text?.toString() ?: ""
        if (currentText != lastSavedText) {
            undoStack.add(SpannableStringBuilder(text))
            if (undoStack.size > 50) {
                undoStack.removeAt(0)
            }
            redoStack.clear()
            lastSavedText = currentText
        }
    }

    // ==================== Public API ====================

    fun setPlaceholderText(value: String) {
        placeholder = value
        hint = value
    }

    fun setVariant(value: String) {
        variant = value
        applyVariantStyle()
    }

    fun setEditable(value: Boolean) {
        isEnabled = value
    }

    fun setMaxHeightValue(value: Int) {
        maxHeightValue = value
        post { updateContentSize() }
    }

    fun setShowToolbar(value: Boolean) {
        showToolbar = value
        if (!value) hideToolbar()
    }

    fun setToolbarOptions(options: List<String>?) {
        toolbarOptions = options
        floatingToolbar?.setToolbarOptions(options)
    }

    fun setContent(blocks: List<Map<String, Any>>) {
        val spannable = SpannableStringBuilder()
        var currentOffset = 0
        var numberedListCounter = 1

        blocks.forEachIndexed { index, block ->
            val textContent = block["text"] as? String ?: ""
            val blockType = block["type"] as? String ?: "paragraph"

            // Add list prefix based on block type
            val prefix = when (blockType) {
                "bullet", "bulletList" -> "• "
                "numbered", "numberedList" -> "${numberedListCounter++}. "
                "checklist" -> "☐ "
                "quote" -> "\""
                else -> {
                    // Reset numbered list counter when not in numbered list
                    if (blockType != "numbered" && blockType != "numberedList") numberedListCounter = 1
                    ""
                }
            }

            // Add suffix for quotes
            val suffix = if (blockType == "quote") "\"" else ""

            val blockStart = currentOffset
            spannable.append(prefix)
            currentOffset += prefix.length

            val textStart = currentOffset
            spannable.append(textContent)
            currentOffset += textContent.length

            spannable.append(suffix)
            currentOffset += suffix.length

            // Apply heading style
            if (blockType == "heading") {
                spannable.setSpan(RelativeSizeSpan(1.5f), blockStart, currentOffset, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(StyleSpan(Typeface.BOLD), blockStart, currentOffset, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            // Apply styles from the block
            @Suppress("UNCHECKED_CAST")
            val styles = block["styles"] as? List<Map<String, Any>> ?: emptyList()
            for (styleInfo in styles) {
                val styleName = styleInfo["style"] as? String ?: continue
                val start = (styleInfo["start"] as? Number)?.toInt() ?: 0
                val end = (styleInfo["end"] as? Number)?.toInt() ?: textContent.length

                val absoluteStart = textStart + start
                val absoluteEnd = textStart + end

                when (styleName) {
                    "bold" -> spannable.setSpan(StyleSpan(Typeface.BOLD), absoluteStart, absoluteEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    "italic" -> spannable.setSpan(StyleSpan(Typeface.ITALIC), absoluteStart, absoluteEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    "underline" -> spannable.setSpan(UnderlineSpan(), absoluteStart, absoluteEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    "strikethrough" -> spannable.setSpan(StrikethroughSpan(), absoluteStart, absoluteEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    "code" -> {
                        spannable.setSpan(TypefaceSpan("monospace"), absoluteStart, absoluteEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        spannable.setSpan(BackgroundColorSpan(Color.parseColor("#F5F5F5")), absoluteStart, absoluteEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                    "highlight" -> spannable.setSpan(BackgroundColorSpan(Color.parseColor("#80FFFF00")), absoluteStart, absoluteEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }

            if (index < blocks.size - 1) {
                spannable.append("\n")
                currentOffset += 1
            }
        }

        isInternalChange = true
        setText(spannable)
        setSelection(spannable.length)
        isInternalChange = false
        post { updateContentSize() }
    }

    fun getTextContent(): String = text.toString()

    fun getBlocksArray(): WritableArray {
        val blocks = Arguments.createArray()
        val textContent = text?.toString() ?: ""
        if (textContent.isEmpty()) {
            return blocks
        }
        val lines = textContent.split("\n")
        lines.forEach { line ->
            val block = Arguments.createMap()
            block.putString("type", "paragraph")
            block.putString("text", line)
            block.putArray("styles", Arguments.createArray())
            blocks.pushMap(block)
        }
        return blocks
    }

    fun clearContent() {
        isInternalChange = true
        text?.clear()
        isInternalChange = false
    }

    fun focusEditor() {
        requestFocus()
    }

    fun blurEditor() {
        clearFocus()
    }

    // ==================== Text Styling (ToolbarActionListener) ====================

    override fun onBoldClick() {
        android.util.Log.d("RichTextEditor", "onBoldClick called")
        toggleStyle(Typeface.BOLD)
    }

    override fun onItalicClick() {
        android.util.Log.d("RichTextEditor", "onItalicClick called")
        toggleStyle(Typeface.ITALIC)
    }

    override fun onUnderlineClick() {
        android.util.Log.d("RichTextEditor", "onUnderlineClick called")
        toggleSpan(UnderlineSpan::class.java) { UnderlineSpan() }
    }

    override fun onStrikethroughClick() {
        android.util.Log.d("RichTextEditor", "onStrikethroughClick called")
        toggleSpan(StrikethroughSpan::class.java) { StrikethroughSpan() }
    }

    override fun onCodeClick() {
        // Use saved selection if current selection is empty
        var start = selectionStart
        var end = selectionEnd

        if (start >= end && savedSelectionStart < savedSelectionEnd) {
            start = savedSelectionStart
            end = savedSelectionEnd
        }

        if (start >= end) return

        val spannable = text as? Editable ?: return
        val existingSpans = spannable.getSpans(start, end, TypefaceSpan::class.java)
            .filter { it.family == "monospace" }

        isInternalChange = true
        if (existingSpans.isNotEmpty()) {
            existingSpans.forEach { spannable.removeSpan(it) }
            // Remove background
            spannable.getSpans(start, end, BackgroundColorSpan::class.java)
                .filter { spannable.getSpanStart(it) >= start && spannable.getSpanEnd(it) <= end }
                .forEach { spannable.removeSpan(it) }
        } else {
            spannable.setSpan(TypefaceSpan("monospace"), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(BackgroundColorSpan(Color.parseColor("#F5F5F5")), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        setSelection(start, end)
        isInternalChange = false
        invalidate()
        sendContentChange()
        updateToolbarButtonStates()
    }

    override fun onHighlightClick() {
        // Use saved selection if current selection is empty
        var start = selectionStart
        var end = selectionEnd

        if (start >= end && savedSelectionStart < savedSelectionEnd) {
            start = savedSelectionStart
            end = savedSelectionEnd
        }

        if (start >= end) return

        val spannable = text as? Editable ?: return
        val existingSpans = spannable.getSpans(start, end, BackgroundColorSpan::class.java)
            .filter {
                val color = it.backgroundColor
                color == Color.parseColor("#80FFFF00") || color == Color.YELLOW
            }

        isInternalChange = true
        if (existingSpans.isNotEmpty()) {
            existingSpans.forEach { spannable.removeSpan(it) }
        } else {
            spannable.setSpan(BackgroundColorSpan(Color.parseColor("#80FFFF00")), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        setSelection(start, end)
        isInternalChange = false
        invalidate()
        sendContentChange()
        updateToolbarButtonStates()
    }

    override fun onHeadingClick() {
        val (lineStart, lineEnd) = getLineRange()
        if (lineStart >= lineEnd) return

        val spannable = text as? Editable ?: return

        // Check if already a heading
        val existingSpans = spannable.getSpans(lineStart, lineEnd, RelativeSizeSpan::class.java)
        val isHeading = existingSpans.any { it.sizeChange > 1.2f }

        isInternalChange = true
        existingSpans.forEach { spannable.removeSpan(it) }
        spannable.getSpans(lineStart, lineEnd, StyleSpan::class.java)
            .filter { it.style == Typeface.BOLD }
            .forEach { spannable.removeSpan(it) }

        if (!isHeading) {
            spannable.setSpan(RelativeSizeSpan(1.5f), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(StyleSpan(Typeface.BOLD), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        isInternalChange = false
        sendContentChange()
        updateToolbarButtonStates()
    }

    override fun onBulletListClick() {
        toggleListPrefix("• ")
    }

    override fun onNumberedListClick() {
        applyNumberedList()
    }

    override fun onQuoteClick() {
        toggleQuote()
    }

    override fun onChecklistClick() {
        toggleChecklistPrefix()
    }

    override fun onLinkClick() {
        promptInsertLink()
    }

    override fun onUndoClick() {
        undo()
    }

    override fun onRedoClick() {
        redo()
    }

    override fun onClearFormattingClick() {
        clearFormatting()
    }

    override fun onIndentClick() {
        indent()
    }

    override fun onOutdentClick() {
        outdent()
    }

    override fun onAlignLeftClick() {
        setAlignment(Layout.Alignment.ALIGN_NORMAL)
    }

    override fun onAlignCenterClick() {
        setAlignment(Layout.Alignment.ALIGN_CENTER)
    }

    override fun onAlignRightClick() {
        setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
    }

    // ==================== Helper Methods ====================

    private fun toggleStyle(style: Int) {
        // Use saved selection if current selection is empty (can happen when clicking toolbar)
        var start = selectionStart
        var end = selectionEnd

        if (start >= end && savedSelectionStart < savedSelectionEnd) {
            start = savedSelectionStart
            end = savedSelectionEnd
            android.util.Log.d("RichTextEditor", "Using saved selection: start=$start, end=$end")
        }

        android.util.Log.d("RichTextEditor", "toggleStyle called: style=$style, start=$start, end=$end")

        if (start >= end) {
            android.util.Log.d("RichTextEditor", "No selection, skipping style toggle")
            return
        }

        val spannable = text as? Editable ?: return
        val existingSpans = spannable.getSpans(start, end, StyleSpan::class.java)
            .filter { it.style == style || it.style == Typeface.BOLD_ITALIC }

        val hasStyle = existingSpans.any { it.style == style }

        isInternalChange = true
        if (hasStyle) {
            android.util.Log.d("RichTextEditor", "Removing style $style")
            existingSpans.filter { it.style == style }.forEach { spannable.removeSpan(it) }
        } else {
            android.util.Log.d("RichTextEditor", "Applying style $style")
            spannable.setSpan(StyleSpan(style), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        // Restore and maintain selection after applying style
        setSelection(start, end)
        isInternalChange = false
        invalidate()
        sendContentChange()
        updateToolbarButtonStates()
    }

    private fun <T : Any> toggleSpan(spanClass: Class<T>, createSpan: () -> Any) {
        // Use saved selection if current selection is empty
        var start = selectionStart
        var end = selectionEnd

        if (start >= end && savedSelectionStart < savedSelectionEnd) {
            start = savedSelectionStart
            end = savedSelectionEnd
        }

        if (start >= end) return

        val spannable = text as? Editable ?: return
        val existingSpans = spannable.getSpans(start, end, spanClass)

        isInternalChange = true
        if (existingSpans.isNotEmpty()) {
            existingSpans.forEach { spannable.removeSpan(it) }
        } else {
            spannable.setSpan(createSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        // Restore and maintain selection after applying style
        setSelection(start, end)
        isInternalChange = false
        invalidate()
        sendContentChange()
        updateToolbarButtonStates()
    }

    private fun toggleListPrefix(prefix: String) {
        val (lineStart, lineEnd) = getLineRange()
        val currentText = text?.toString() ?: return
        val lineText = currentText.substring(lineStart, lineEnd)

        isInternalChange = true
        val editable = text ?: return

        if (lineText.startsWith(prefix)) {
            // Remove prefix
            editable.delete(lineStart, lineStart + prefix.length)
        } else {
            // Remove other prefixes first
            val cleanLine = removeExistingPrefix(lineText)
            editable.replace(lineStart, lineEnd, prefix + cleanLine)
        }
        isInternalChange = false
        sendContentChange()
        updateToolbarButtonStates()
    }

    private fun applyNumberedList() {
        val (lineStart, lineEnd) = getLineRange()
        val currentText = text?.toString() ?: return
        val lineText = currentText.substring(lineStart, lineEnd)

        isInternalChange = true
        val editable = text ?: return

        val numberedRegex = Regex("^(\\d+)\\.\\s")
        val match = numberedRegex.find(lineText)

        if (match != null) {
            // Remove numbered prefix
            editable.delete(lineStart, lineStart + match.value.length)
        } else {
            // Add numbered prefix
            val cleanLine = removeExistingPrefix(lineText)
            editable.replace(lineStart, lineEnd, "1. $cleanLine")
        }
        isInternalChange = false
        sendContentChange()
        updateToolbarButtonStates()
    }

    private fun toggleQuote() {
        val (lineStart, lineEnd) = getLineRange()
        val currentText = text?.toString() ?: return
        val lineText = currentText.substring(lineStart, lineEnd)

        isInternalChange = true
        val editable = text ?: return

        if (lineText.startsWith("\"") && lineText.endsWith("\"") && lineText.length >= 2) {
            // Remove quotes
            val unquoted = lineText.substring(1, lineText.length - 1)
            editable.replace(lineStart, lineEnd, unquoted)
        } else {
            // Add quotes
            val cleanLine = removeExistingPrefix(lineText)
            editable.replace(lineStart, lineEnd, "\"$cleanLine\"")
        }
        isInternalChange = false
        sendContentChange()
        updateToolbarButtonStates()
    }

    private fun toggleChecklistPrefix() {
        val (lineStart, lineEnd) = getLineRange()
        val currentText = text?.toString() ?: return
        val lineText = currentText.substring(lineStart, lineEnd)

        isInternalChange = true
        val editable = text ?: return

        when {
            lineText.startsWith("☐ ") -> {
                // Remove checklist
                editable.delete(lineStart, lineStart + 2)
            }
            lineText.startsWith("☑ ") -> {
                // Remove checklist
                editable.delete(lineStart, lineStart + 2)
            }
            else -> {
                // Add checklist
                val cleanLine = removeExistingPrefix(lineText)
                editable.replace(lineStart, lineEnd, "☐ $cleanLine")
            }
        }
        isInternalChange = false
        sendContentChange()
        updateToolbarButtonStates()
    }

    private fun removeExistingPrefix(line: String): String {
        return when {
            line.startsWith("• ") -> line.substring(2)
            line.startsWith("☐ ") || line.startsWith("☑ ") -> line.substring(2)
            line.matches(Regex("^\\d+\\.\\s.*")) -> line.replace(Regex("^\\d+\\.\\s"), "")
            line.startsWith("\"") && line.endsWith("\"") && line.length >= 2 -> line.substring(1, line.length - 1)
            else -> line
        }
    }

    private fun promptInsertLink() {
        val context = context
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Insert Link")

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val textInput = EditText(context).apply {
            hint = "Link text"
            // Pre-fill with selected text
            val selectedText = text?.subSequence(selectionStart, selectionEnd)?.toString() ?: ""
            setText(selectedText)
        }

        val urlInput = EditText(context).apply {
            hint = "URL"
        }

        layout.addView(textInput)
        layout.addView(urlInput)
        builder.setView(layout)

        builder.setPositiveButton("Insert") { _, _ ->
            val linkText = textInput.text.toString()
            val url = urlInput.text.toString()
            if (linkText.isNotEmpty() && url.isNotEmpty()) {
                insertLink(url, linkText)
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    fun insertLink(url: String, linkText: String) {
        val start = selectionStart
        val end = selectionEnd
        val editable = text ?: return

        isInternalChange = true
        if (start != end) {
            editable.delete(start, end)
        }

        val linkSpannable = SpannableStringBuilder(linkText)
        linkSpannable.setSpan(URLSpan(url), 0, linkText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        linkSpannable.setSpan(ForegroundColorSpan(Color.parseColor("#2196F3")), 0, linkText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        linkSpannable.setSpan(UnderlineSpan(), 0, linkText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        editable.insert(start, linkSpannable)
        isInternalChange = false
        sendContentChange()
    }

    fun undo() {
        if (undoStack.size > 1) {
            val current = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(current)
            val previous = undoStack.last()

            isInternalChange = true
            setText(previous)
            setSelection(previous.length)
            lastSavedText = previous.toString()
            isInternalChange = false
            sendContentChange()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(next)

            isInternalChange = true
            setText(next)
            setSelection(next.length)
            lastSavedText = next.toString()
            isInternalChange = false
            sendContentChange()
        }
    }

    fun clearFormatting() {
        // Use saved selection if current selection is empty
        var start = selectionStart
        var end = selectionEnd

        if (start >= end && savedSelectionStart < savedSelectionEnd) {
            start = savedSelectionStart
            end = savedSelectionEnd
        }

        if (start >= end) return

        val spannable = text as? Editable ?: return

        isInternalChange = true
        // Remove all spans in selection
        spannable.getSpans(start, end, Any::class.java).forEach { span ->
            if (span is StyleSpan || span is UnderlineSpan || span is StrikethroughSpan ||
                span is BackgroundColorSpan || span is ForegroundColorSpan || span is TypefaceSpan ||
                span is RelativeSizeSpan || span is URLSpan) {
                spannable.removeSpan(span)
            }
        }
        setSelection(start, end)
        isInternalChange = false
        sendContentChange()
        updateToolbarButtonStates()
    }

    fun indent() {
        val (lineStart, _) = getLineRange()
        val editable = text ?: return

        isInternalChange = true
        editable.insert(lineStart, "    ")
        isInternalChange = false
        sendContentChange()
    }

    fun outdent() {
        val (lineStart, lineEnd) = getLineRange()
        val currentText = text?.toString() ?: return
        val lineText = currentText.substring(lineStart, lineEnd)
        val editable = text ?: return

        isInternalChange = true
        when {
            lineText.startsWith("    ") -> editable.delete(lineStart, lineStart + 4)
            lineText.startsWith("\t") -> editable.delete(lineStart, lineStart + 1)
            lineText.startsWith(" ") -> {
                var spaces = 0
                for (c in lineText) {
                    if (c == ' ' && spaces < 4) spaces++ else break
                }
                if (spaces > 0) editable.delete(lineStart, lineStart + spaces)
            }
        }
        isInternalChange = false
        sendContentChange()
    }

    fun setAlignment(alignment: Layout.Alignment) {
        val (lineStart, lineEnd) = getLineRange()
        val spannable = text as? Editable ?: return

        isInternalChange = true
        // Remove existing alignment spans
        spannable.getSpans(lineStart, lineEnd, AlignmentSpan.Standard::class.java)
            .forEach { spannable.removeSpan(it) }

        spannable.setSpan(AlignmentSpan.Standard(alignment), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        isInternalChange = false
        sendContentChange()
        updateToolbarButtonStates()
    }

    // Legacy method names for ViewManager commands
    fun toggleBold() = onBoldClick()
    fun toggleItalic() = onItalicClick()
    fun toggleUnderline() = onUnderlineClick()
    fun toggleStrikethrough() = onStrikethroughClick()
    fun toggleCode() = onCodeClick()
    fun toggleHighlight(color: String?) = onHighlightClick()
    fun setHeading() = onHeadingClick()
    fun toggleBulletList() = onBulletListClick()
    fun toggleNumberedList() = applyNumberedList()
    fun setQuote() = onQuoteClick()
    fun setChecklist() = onChecklistClick()
    fun setParagraph() {
        // Remove all block-level formatting from current line
        val (lineStart, lineEnd) = getLineRange()
        val currentText = text?.toString() ?: return
        val lineText = currentText.substring(lineStart, lineEnd)
        val cleanLine = removeExistingPrefix(lineText)

        isInternalChange = true
        text?.replace(lineStart, lineEnd, cleanLine)
        isInternalChange = false
        sendContentChange()
    }

    fun toggleChecklistItem() {
        val (lineStart, lineEnd) = getLineRange()
        val currentText = text?.toString() ?: return
        val lineText = currentText.substring(lineStart, lineEnd)
        val editable = text ?: return

        isInternalChange = true
        when {
            lineText.startsWith("☐ ") -> {
                editable.replace(lineStart, lineStart + 1, "☑")
            }
            lineText.startsWith("☑ ") -> {
                editable.replace(lineStart, lineStart + 1, "☐")
            }
        }
        isInternalChange = false
        sendContentChange()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        hideToolbar()
        toolbarPopup = null
        floatingToolbar = null
    }
}
