package com.richtext.editor

import android.content.Context
import android.content.ClipboardManager
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
import android.view.inputmethod.InputConnection
import android.widget.PopupWindow
import android.widget.FrameLayout
import android.app.AlertDialog
import android.net.Uri
import android.widget.EditText
import android.widget.LinearLayout
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ContentInfoCompat
import androidx.core.view.ViewCompat
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.events.RCTEventEmitter

class RichTextEditorView(context: Context) : androidx.appcompat.widget.AppCompatEditText(context),
    FloatingToolbar.ToolbarActionListener {

    companion object {
        private const val MEDIA_PLACEHOLDER_CHAR = '\uFFFC'
    }

    private var placeholder: String = ""
    private var maxHeightValue: Int = 0
    private var numberOfLinesValue: Int = 0
    private var showToolbar: Boolean = true
    private var variant: String = "outlined"
    private var customFontFamily: String? = null
    private var customFontSize: Float = 16f
    private var customTypeface: Typeface? = null
    private var density: Float = 1f
    private var isInternalChange = false
    private var lastReportedHeight: Float = 0f
    private var calculatedHeight: Float = 0f
    private var minHeightPx: Float = 0f
    private var isInitialized = false

    private var previousText: String = ""
    private var pendingDelta: Map<String, Any>? = null
    private var pendingPrefixDeletion: Pair<Int, Int>? = null // (lineStart, prefixLength) for backspace-in-prefix
    private var imagePickerLauncher: ActivityResultLauncher<String>? = null
    private val imagePickerLauncherKey = "richtext_image_picker_${hashCode()}"
    private val mediaAttachmentSupport by lazy {
        MediaAttachmentSupport(
            context = context,
            density = density,
            placeholderChar = MEDIA_PLACEHOLDER_CHAR,
            getLineSpacingMultiplier = { lineSpacingMultiplier },
            getTargetWidthPx = {
                val contentWidth = width - totalPaddingLeft - totalPaddingRight
                if (contentWidth > 0) {
                    contentWidth
                } else {
                    (context.resources.displayMetrics.widthPixels - totalPaddingLeft - totalPaddingRight)
                        .coerceAtLeast((120 * density).toInt())
                }
            },
            editableProvider = { text },
            runOnUiThread = { action -> post(action) },
            onMediaSpansUpdated = {
                invalidate()
                requestLayout()
                post { updateContentSize() }
            }
        )
    }

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
        calculatedHeight = 0f
        bottomBorderPaint.strokeWidth = density

        val paddingHorizontal = (12 * density).toInt()
        val paddingVertical = (10 * density).toInt()

        setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
        textSize = 16f
        setLineSpacing(0f, 1.3f)  // Consistent line height multiplier
        setTextColor(Color.BLACK)
        setHintTextColor(Color.parseColor("#9E9E9E"))
        gravity = Gravity.TOP or Gravity.START
        isFocusable = true
        isFocusableInTouchMode = true
        inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE
        setupImageReceiveContentHandler()

        // Disable vertical scrolling by default
        isVerticalScrollBarEnabled = false

        // Transparent background so it inherits parent's background
        setBackgroundColor(Color.TRANSPARENT)

        // Default outlined style
        applyVariantStyle()

        // Setup toolbar
        setupToolbar()

        addTextChangedListener(object : TextWatcher {
            private var changeStart = 0
            private var removedCount = 0
            private var addedCount = 0

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                changeStart = start
                removedCount = count
                addedCount = after
                detectBackspaceInListPrefix(s, start, count, after)
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Capture delta information
                if (!isInternalChange && s != null) {
                    val newText = s.toString()
                    val deltaMap = mutableMapOf<String, Any>()

                    when {
                        removedCount == 0 && addedCount > 0 -> {
                            // Insert
                            deltaMap["type"] = "insert"
                            deltaMap["position"] = changeStart
                            deltaMap["text"] = newText.substring(start, start + count)
                        }
                        removedCount > 0 && addedCount == 0 -> {
                            // Delete
                            deltaMap["type"] = "delete"
                            deltaMap["position"] = changeStart
                            deltaMap["length"] = removedCount
                        }
                        removedCount > 0 && addedCount > 0 -> {
                            // Replace
                            deltaMap["type"] = "replace"
                            deltaMap["position"] = changeStart
                            deltaMap["length"] = removedCount
                            deltaMap["text"] = newText.substring(start, start + count)
                        }
                    }

                    if (deltaMap.isNotEmpty()) {
                        pendingDelta = deltaMap
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {
                if (!isInternalChange) {
                    var handled = handleBackspaceInListPrefix(s)
                    if (!handled) {
                        handled = autoContinueListOnEnter(s)
                    }
                    if (!handled) {
                        handled = applyInlineStyleShortcut(s)
                    }
                    if (!handled) {
                        isInternalChange = true
                        renumberNumberedLists()
                        isInternalChange = false
                    }
                    sendContentChangeWithDelta()
                    saveToUndoStack()
                    pendingDelta = null
                }
                previousText = s?.toString() ?: ""
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

    // Handle image pasting from clipboard and input method
    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val inputConnection = super.onCreateInputConnection(outAttrs) ?: return null
        EditorInfoCompat.setContentMimeTypes(outAttrs, arrayOf("image/*"))

        return InputConnectionCompat.createWrapper(
            inputConnection,
            outAttrs
        ) { inputContentInfo: InputContentInfoCompat, flags: Int, _ ->
            if ((flags and InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0) {
                runCatching { inputContentInfo.requestPermission() }
            }

            val uri = inputContentInfo.contentUri
            if (isImageUri(uri)) {
                post { insertMediaAttachmentBlock(uri.toString()) }
                true
            } else {
                false
            }
        }
    }

    // Handle receiving images via drag-and-drop or clipboard paste (Android 13+)
    private fun setupImageReceiveContentHandler() {
        ViewCompat.setOnReceiveContentListener(this, arrayOf("image/*")) { _, payload: ContentInfoCompat ->
            val clip = payload.clip
            if (clip == null || clip.itemCount == 0) {
                return@setOnReceiveContentListener payload
            }

            var handled = false
            for (index in 0 until clip.itemCount) {
                val item = clip.getItemAt(index)
                val uri = item.uri
                if (uri != null && isImageUri(uri)) {
                    insertMediaAttachmentBlock(uri.toString())
                    handled = true
                }
            }

            if (handled) null else payload
        }
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

        // Emit active styles synchronously for instant toolbar updates
        emitActiveStyles()

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

    // Synchronous style detection - emits current active styles to JS
    private fun emitActiveStyles() {
        val start = selectionStart
        val end = selectionEnd
        val spannable = text as? Spanned

        var hasBold = false
        var hasItalic = false
        var hasUnderline = false
        var hasStrikethrough = false
        var hasCode = false
        var hasHighlight = false
        var blockType = "paragraph"
        var alignment = "left"

        if (spannable != null && start <= end) {
            // Check for style spans
            spannable.getSpans(start, end.coerceAtLeast(start + 1), StyleSpan::class.java).forEach { span ->
                when (span.style) {
                    Typeface.BOLD -> hasBold = true
                    Typeface.ITALIC -> hasItalic = true
                    Typeface.BOLD_ITALIC -> {
                        hasBold = true
                        hasItalic = true
                    }
                }
            }

            hasUnderline = spannable.getSpans(start, end.coerceAtLeast(start + 1), UnderlineSpan::class.java).isNotEmpty()
            hasStrikethrough = spannable.getSpans(start, end.coerceAtLeast(start + 1), StrikethroughSpan::class.java).isNotEmpty()
            hasCode = spannable.getSpans(start, end.coerceAtLeast(start + 1), TypefaceSpan::class.java).any { it.family == "monospace" }

            // Check highlight (but not code background)
            val bgSpans = spannable.getSpans(start, end.coerceAtLeast(start + 1), BackgroundColorSpan::class.java)
            hasHighlight = bgSpans.any {
                val color = it.backgroundColor
                color == Color.parseColor("#80FFFF00") || color == Color.YELLOW
            }

            // Check block type from line content
            val lineText = getCurrentLineText()
            blockType = when {
                lineText.startsWith("• ") -> "bullet"
                lineText.matches(Regex("^\\d+\\.\\s.*")) -> "numbered"
                lineText.startsWith("☐ ") || lineText.startsWith("☑ ") -> "checklist"
                lineText.startsWith("\"") && lineText.endsWith("\"") -> "quote"
                spannable.getSpans(start, end.coerceAtLeast(start + 1), RelativeSizeSpan::class.java).any { it.sizeChange > 1.2f } -> "heading"
                else -> "paragraph"
            }

            // Check alignment
            spannable.getSpans(start, end.coerceAtLeast(start + 1), AlignmentSpan.Standard::class.java).firstOrNull()?.let { span ->
                alignment = when (span.alignment) {
                    Layout.Alignment.ALIGN_CENTER -> "center"
                    Layout.Alignment.ALIGN_OPPOSITE -> "right"
                    else -> "left"
                }
            }
        }

        val map = Arguments.createMap()
        map.putBoolean("bold", hasBold)
        map.putBoolean("italic", hasItalic)
        map.putBoolean("underline", hasUnderline)
        map.putBoolean("strikethrough", hasStrikethrough)
        map.putBoolean("code", hasCode)
        map.putBoolean("highlight", hasHighlight)
        map.putString("blockType", blockType)
        map.putString("alignment", alignment)
        sendEvent("onActiveStylesChange", map)
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

    // Handle image pasting from clipboard (Android 13+ also sends via onReceiveContent)
    override fun onTextContextMenuItem(id: Int): Boolean {
        val isPasteAction = id == android.R.id.paste || id == android.R.id.pasteAsPlainText
        if (!isPasteAction) {
            return super.onTextContextMenuItem(id)
        }

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return super.onTextContextMenuItem(id)
        val primaryClip = clipboard.primaryClip ?: return super.onTextContextMenuItem(id)

        val imageUris = mutableListOf<Uri>()
        for (index in 0 until primaryClip.itemCount) {
            val item = primaryClip.getItemAt(index)
            item.uri?.let { uri ->
                if (isImageUri(uri)) {
                    imageUris.add(uri)
                }
            }

            val maybeUriText = item.text?.toString()?.trim().orEmpty()
            if (maybeUriText.isNotEmpty()) {
                runCatching { Uri.parse(maybeUriText) }
                    .getOrNull()
                    ?.let { parsed ->
                        if (parsed.scheme != null && isImageUri(parsed)) {
                            imageUris.add(parsed)
                        }
                    }
            }
        }

        if (imageUris.isEmpty()) {
            return super.onTextContextMenuItem(id)
        }

        imageUris
            .distinctBy { it.toString() }
            .forEach { insertMediaAttachmentBlock(it.toString()) }

        return true
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

    private fun isImageUri(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase()

        if (scheme == "http" || scheme == "https") {
            val path = uri.path.orEmpty()
            return path.endsWith(".png", true) ||
                path.endsWith(".jpg", true) ||
                path.endsWith(".jpeg", true) ||
                path.endsWith(".webp", true) ||
                path.endsWith(".gif", true) ||
                path.endsWith(".bmp", true)
        }

        val mimeType = runCatching {
            context.contentResolver.getType(uri)
        }.getOrNull()

        if (mimeType?.startsWith("image/") == true) {
            return true
        }

        val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        if (!extension.isNullOrEmpty()) {
            val guessedMime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
            if (guessedMime?.startsWith("image/") == true) {
                return true
            }
        }

        return false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (drawBottomBorder) {
            val y = scrollY + height.toFloat() - bottomBorderPaint.strokeWidth / 2
            canvas.drawLine(0f, y, width.toFloat(), y, bottomBorderPaint)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val textLayout = layout
        if (textLayout != null) {
            val lineCount = textLayout.lineCount
            if (lineCount > 0) {
                val effectiveLineCount = if (numberOfLinesValue > 0 && !isEnabled) {
                    minOf(numberOfLinesValue, lineCount)
                } else {
                    lineCount
                }

                var desiredHeight = textLayout.getLineBottom(effectiveLineCount - 1).toFloat() + paddingTop + paddingBottom

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

        val effectiveLineCount = if (numberOfLinesValue > 0 && !isEnabled) {
            minOf(numberOfLinesValue, lineCount)
        } else {
            lineCount
        }

        var newHeightPx = textLayout.getLineBottom(effectiveLineCount - 1).toFloat() + paddingTop + paddingBottom

        if (newHeightPx < minHeightPx) {
            newHeightPx = minHeightPx
        }

        if (maxHeightValue > 0) {
            val maxHeightPx = maxHeightValue * density
            if (newHeightPx > maxHeightPx) {
                isVerticalScrollBarEnabled = true
                newHeightPx = maxHeightPx
            } else {
                isVerticalScrollBarEnabled = false
            }
        }

        val previousHeight = calculatedHeight
        calculatedHeight = newHeightPx

        val newHeightDp = newHeightPx / density

        if (kotlin.math.abs(newHeightDp - lastReportedHeight) > 0.5f) {
            lastReportedHeight = newHeightDp
            val map = Arguments.createMap()
            map.putInt("height", newHeightDp.toInt())
            sendEvent("onSizeChange", map)

            if (kotlin.math.abs(previousHeight - calculatedHeight) > 1f) {
                requestLayout()
            }
        }

        if (maxHeightValue > 0 && textLayout.lineCount > 0 && !(numberOfLinesValue > 0 && !isEnabled)) {
            val cursorPos = selectionEnd.coerceAtLeast(0)
            val cursorLine = textLayout.getLineForOffset(cursorPos)
            val cursorBottom = textLayout.getLineBottom(cursorLine)
            val visibleBottom = scrollY + height - paddingBottom
            if (cursorBottom > visibleBottom) {
                scrollTo(0, cursorBottom - height + paddingBottom)
            } else if (textLayout.getLineTop(cursorLine) < scrollY + paddingTop) {
                scrollTo(0, textLayout.getLineTop(cursorLine) - paddingTop)
            }
        }
    }

    private fun applyVariantStyle() {
        if (variant == "flat") {
            background = null
            setBackgroundColor(Color.TRANSPARENT)
            drawBottomBorder = true
        } else if (variant == "plain") {
            background = null
            setBackgroundColor(Color.TRANSPARENT)
            drawBottomBorder = false
        } else {
            drawBottomBorder = false
            val bg = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
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
        sendContentChangeWithDelta(null)
    }

    private fun sendContentChangeWithDelta(delta: Map<String, Any>? = pendingDelta) {
        try {
            val map = Arguments.createMap()
            map.putString("text", text?.toString() ?: "")
            // Serialize blocks to JSON string (codegen doesn't support nested arrays)
            map.putString("blocksJson", getBlocksJsonString())

            // Include delta information if available
            if (delta != null) {
                val deltaMap = Arguments.createMap()
                delta["type"]?.let { deltaMap.putString("type", it as String) }
                delta["position"]?.let { deltaMap.putInt("position", it as Int) }
                delta["length"]?.let { deltaMap.putInt("length", it as Int) }
                delta["text"]?.let { deltaMap.putString("text", it as String) }
                delta["style"]?.let { deltaMap.putString("style", it as String) }
                map.putMap("delta", deltaMap)
            }

            sendEvent("onContentChange", map)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Send format delta when applying styles
    private fun sendFormatDelta(style: String, start: Int, end: Int) {
        val delta = mapOf(
            "type" to "format",
            "position" to start,
            "length" to (end - start),
            "style" to style
        )
        sendContentChangeWithDelta(delta)
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

    fun setFontFamily(value: String?) {
        customFontFamily = value
        customTypeface = if (value != null) {
            try {
                val assetPath = "fonts/$value.ttf"
                Typeface.createFromAsset(context.assets, assetPath)
            } catch (_: Exception) {
                try {
                    val assetPath = "fonts/$value.otf"
                    Typeface.createFromAsset(context.assets, assetPath)
                } catch (_: Exception) {
                    Typeface.create(value, Typeface.NORMAL)
                }
            }
        } else {
            null
        }
        applyFont()
    }

    fun setFontSizeValue(value: Float) {
        customFontSize = if (value > 0) value else 16f
        applyFont()
    }

    private fun applyFont() {
        textSize = customFontSize
        typeface = customTypeface
    }

    fun setVariant(value: String) {
        variant = value
        applyVariantStyle()
    }

    fun setEditable(value: Boolean) {
        isEnabled = value
        if (!value) {
            movementMethod = android.text.method.LinkMovementMethod.getInstance()
        }
        if (numberOfLinesValue > 0) {
            setNumberOfLinesValue(numberOfLinesValue)
        }
    }

    fun setSelectableValue(value: Boolean) {
        setTextIsSelectable(value)
    }

    fun setMaxHeightValue(value: Int) {
        maxHeightValue = value
        post { updateContentSize() }
    }

    fun setNumberOfLinesValue(value: Int) {
        numberOfLinesValue = if (value == 0) 0 else value
        if (numberOfLinesValue > 0 && !isEnabled) {
            maxLines = numberOfLinesValue
            ellipsize = android.text.TextUtils.TruncateAt.END
            isVerticalScrollBarEnabled = false
            scrollTo(0, 0)
        } else {
            maxLines = Integer.MAX_VALUE
            ellipsize = null
        }
        requestLayout()
    }

    private fun applyEllipsisIfNeeded() {
        if (numberOfLinesValue <= 0 || isEnabled) return
        val textLayout = layout ?: return
        if (textLayout.lineCount <= numberOfLinesValue) return

        val content = text ?: return
        val availableWidth = textLayout.width

        val staticLayout = android.text.StaticLayout.Builder
            .obtain(content, 0, content.length, paint, availableWidth)
            .setMaxLines(numberOfLinesValue)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)
            .setIncludePad(includeFontPadding)
            .build()

        val lastLine = numberOfLinesValue - 1
        val ellipsisStart = staticLayout.getEllipsisStart(lastLine)
        val ellipsisCount = staticLayout.getEllipsisCount(lastLine)

        if (ellipsisCount > 0) {
            val lineStart = staticLayout.getLineStart(lastLine)
            val truncPoint = lineStart + ellipsisStart

            isInternalChange = true
            val editable = content as? android.text.Editable ?: return
            editable.delete(truncPoint, editable.length)
            editable.append("\u2026")
            setSelection(0)
            isInternalChange = false
        }
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

            if (blockType == "mediaAttachment") {
                numberedListCounter = 1
                val mediaData = mediaAttachmentSupport.parseMediaData(block) ?: return@forEachIndexed
                currentOffset = mediaAttachmentSupport.appendMediaBlock(
                    spannable = spannable,
                    currentOffset = currentOffset,
                    mediaData = mediaData,
                    appendTrailingNewline = index < blocks.size - 1
                )
                return@forEachIndexed
            }

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

                if (start >= end || end > textContent.length) continue

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
                    "link" -> {
                        val url = styleInfo["url"] as? String ?: ""
                        spannable.setSpan(android.text.style.URLSpan(url), absoluteStart, absoluteEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
            }

            if (index < blocks.size - 1) {
                spannable.append("\n")
                currentOffset += 1
            }
        }

        isInternalChange = true
        setText(spannable)
        if (numberOfLinesValue > 0 && !isEnabled) {
            setSelection(0)
        } else {
            setSelection(spannable.length)
        }
        isInternalChange = false
        if (numberOfLinesValue > 0 && !isEnabled) {
            scrollTo(0, 0)
            applyEllipsisIfNeeded()
        }
        updateContentSize()

        post {
            requestLayout()
            post { updateContentSize() }
        }
    }

    fun getTextContent(): String = text.toString()

    // Fabric command response methods
    fun emitGetTextResponse() {
        val map = Arguments.createMap()
        map.putString("text", text?.toString() ?: "")
        sendEvent("onGetTextResponse", map)
    }

    fun emitGetBlocksResponse() {
        val map = Arguments.createMap()
        map.putArray("blocks", getBlocksArray())
        sendEvent("onGetBlocksResponse", map)
    }

    private fun extractStylesForRange(spannable: Spanned, lineStart: Int, lineEnd: Int): List<Map<String, Any>> {
        val styles = mutableListOf<Map<String, Any>>()

        spannable.getSpans(lineStart, lineEnd, StyleSpan::class.java).forEach { span ->
            val start = (spannable.getSpanStart(span) - lineStart).coerceAtLeast(0)
            val end = (spannable.getSpanEnd(span) - lineStart).coerceAtMost(lineEnd - lineStart)
            when (span.style) {
                Typeface.BOLD -> styles.add(mapOf("style" to "bold", "start" to start, "end" to end))
                Typeface.ITALIC -> styles.add(mapOf("style" to "italic", "start" to start, "end" to end))
                Typeface.BOLD_ITALIC -> {
                    styles.add(mapOf("style" to "bold", "start" to start, "end" to end))
                    styles.add(mapOf("style" to "italic", "start" to start, "end" to end))
                }
            }
        }

        spannable.getSpans(lineStart, lineEnd, UnderlineSpan::class.java).forEach { span ->
            if (spannable.getSpans(spannable.getSpanStart(span), spannable.getSpanEnd(span), URLSpan::class.java).isEmpty()) {
                val start = (spannable.getSpanStart(span) - lineStart).coerceAtLeast(0)
                val end = (spannable.getSpanEnd(span) - lineStart).coerceAtMost(lineEnd - lineStart)
                styles.add(mapOf("style" to "underline", "start" to start, "end" to end))
            }
        }

        spannable.getSpans(lineStart, lineEnd, StrikethroughSpan::class.java).forEach { span ->
            val start = (spannable.getSpanStart(span) - lineStart).coerceAtLeast(0)
            val end = (spannable.getSpanEnd(span) - lineStart).coerceAtMost(lineEnd - lineStart)
            styles.add(mapOf("style" to "strikethrough", "start" to start, "end" to end))
        }

        spannable.getSpans(lineStart, lineEnd, TypefaceSpan::class.java).filter { it.family == "monospace" }.forEach { span ->
            val start = (spannable.getSpanStart(span) - lineStart).coerceAtLeast(0)
            val end = (spannable.getSpanEnd(span) - lineStart).coerceAtMost(lineEnd - lineStart)
            styles.add(mapOf("style" to "code", "start" to start, "end" to end))
        }

        spannable.getSpans(lineStart, lineEnd, BackgroundColorSpan::class.java).filter {
            val color = it.backgroundColor
            color == Color.parseColor("#80FFFF00") || color == Color.YELLOW
        }.forEach { span ->
            val start = (spannable.getSpanStart(span) - lineStart).coerceAtLeast(0)
            val end = (spannable.getSpanEnd(span) - lineStart).coerceAtMost(lineEnd - lineStart)
            styles.add(mapOf("style" to "highlight", "start" to start, "end" to end))
        }

        return styles
    }

    private fun detectBlockType(lineText: String): Pair<String, String> {
        return when {
            lineText.startsWith("• ") -> "bullet" to lineText.substring(2)
            lineText.matches(Regex("^\\d+\\.\\s.*")) -> "numbered" to lineText.replace(Regex("^\\d+\\.\\s"), "")
            lineText.startsWith("☐ ") || lineText.startsWith("☑ ") -> "checklist" to lineText.substring(2)
            lineText.startsWith("\"") && lineText.endsWith("\"") && lineText.length >= 2 -> "quote" to lineText.substring(1, lineText.length - 1)
            else -> "paragraph" to lineText
        }
    }

    fun getBlocksArray(): WritableArray {
        val blocks = Arguments.createArray()
        val spannable = text as? Spanned ?: return blocks
        val textContent = spannable.toString()
        if (textContent.isEmpty()) return blocks

        val lines = textContent.split("\n")
        var currentIndex = 0
        lines.forEach { line ->
            val lineStart = currentIndex
            val lineEnd = currentIndex + line.length

            val mediaSpan = mediaAttachmentSupport.findMediaAttachmentSpan(spannable, lineStart, lineEnd)
            if (mediaSpan != null && mediaAttachmentSupport.isMediaLine(line)) {
                val mediaData = mediaSpan.toMediaAttachmentData()
                val block = mediaAttachmentSupport.createWritableMediaBlock(mediaData)

                blocks.pushMap(block)
                currentIndex += line.length + 1
                return@forEach
            }

            val (blockType, displayText) = detectBlockType(line)
            val prefixLen = line.length - displayText.length

            val block = Arguments.createMap()
            block.putString("type", blockType)
            block.putString("text", displayText)

            val stylesArray = Arguments.createArray()
            val styleStart = currentIndex + prefixLen
            extractStylesForRange(spannable, styleStart, lineEnd).forEach { style ->
                val styleMap = Arguments.createMap()
                styleMap.putString("style", style["style"] as String)
                styleMap.putInt("start", style["start"] as Int)
                styleMap.putInt("end", style["end"] as Int)
                stylesArray.pushMap(styleMap)
            }
            block.putArray("styles", stylesArray)
            blocks.pushMap(block)

            currentIndex += line.length + 1
        }
        return blocks
    }

    fun getBlocksJsonString(): String {
        val spannable = text as? Spanned ?: return "[]"
        val textContent = spannable.toString()
        if (textContent.isEmpty()) return "[]"

        val jsonArray = org.json.JSONArray()
        val lines = textContent.split("\n")
        var currentIndex = 0
        lines.forEach { line ->
            val lineStart = currentIndex
            val lineEnd = currentIndex + line.length

            val mediaSpan = mediaAttachmentSupport.findMediaAttachmentSpan(spannable, lineStart, lineEnd)
            if (mediaSpan != null && mediaAttachmentSupport.isMediaLine(line)) {
                val mediaData = mediaSpan.toMediaAttachmentData()
                val block = mediaAttachmentSupport.createJsonMediaBlock(mediaData)

                jsonArray.put(block)
                currentIndex += line.length + 1
                return@forEach
            }

            val (blockType, displayText) = detectBlockType(line)
            val prefixLen = line.length - displayText.length

            val block = org.json.JSONObject()
            block.put("type", blockType)
            block.put("text", displayText)

            val stylesJson = org.json.JSONArray()
            val styleStart = currentIndex + prefixLen
            extractStylesForRange(spannable, styleStart, lineEnd).forEach { style ->
                val styleObj = org.json.JSONObject()
                styleObj.put("style", style["style"])
                styleObj.put("start", style["start"])
                styleObj.put("end", style["end"])
                stylesJson.put(styleObj)
            }
            block.put("styles", stylesJson)
            jsonArray.put(block)

            currentIndex += line.length + 1
        }
        return jsonArray.toString()
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

    override fun onMediaAttachmentClick() {
        openImagePicker()
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
        val selectedText = currentText.substring(lineStart, lineEnd)
        val lines = selectedText.split("\n")

        val numberedRegex = Regex("^\\d+\\.\\s")

        // Check if all non-empty lines already have this prefix
        val allHavePrefix = lines.all { it.trimEnd().isEmpty() || it.startsWith(prefix) }

        val newLines = lines.map { line ->
            if (line.trimEnd().isEmpty()) return@map line
            if (allHavePrefix) {
                // Remove prefix
                if (line.startsWith(prefix)) line.substring(prefix.length) else line
            } else {
                // Remove any existing prefix, then add the new one
                val cleanLine = removeExistingPrefix(line)
                prefix + cleanLine
            }
        }

        val newText = newLines.joinToString("\n")

        isInternalChange = true
        val editable = text ?: return
        editable.replace(lineStart, lineEnd, newText)
        setSelection(lineStart, lineStart + newText.length)
        isInternalChange = false
        sendContentChange()
        updateToolbarButtonStates()
    }

    private fun applyNumberedList() {
        val (lineStart, lineEnd) = getLineRange()
        val currentText = text?.toString() ?: return
        val selectedText = currentText.substring(lineStart, lineEnd)
        val lines = selectedText.split("\n")

        val numberedRegex = Regex("^\\d+\\.\\s")

        // Check if all non-empty lines already have numbered prefix
        val allHaveNumbered = lines.all { it.trimEnd().isEmpty() || numberedRegex.containsMatchIn(it) }

        val newLines = lines.mapIndexed { index, line ->
            if (line.trimEnd().isEmpty()) return@mapIndexed line
            if (allHaveNumbered) {
                // Remove numbered prefix
                val match = numberedRegex.find(line)
                if (match != null) line.substring(match.value.length) else line
            } else {
                // Remove any existing prefix, then add numbered prefix
                val cleanLine = removeExistingPrefix(line)
                "${index + 1}. $cleanLine"
            }
        }

        val newText = newLines.joinToString("\n")

        isInternalChange = true
        val editable = text ?: return
        editable.replace(lineStart, lineEnd, newText)
        setSelection(lineStart, lineStart + newText.length)
        renumberNumberedLists()
        isInternalChange = false
        sendContentChange()
        updateToolbarButtonStates()
    }

    private fun detectBackspaceInListPrefix(s: CharSequence?, start: Int, count: Int, after: Int) {
        pendingPrefixDeletion = null
        if (isInternalChange || s == null || count != 1 || after != 0) return

        val text = s.toString()
        var lineStart = start
        while (lineStart > 0 && text[lineStart - 1] != '\n') lineStart--

        val lineEnd = text.indexOf('\n', lineStart).let { if (it == -1) text.length else it }
        val line = text.substring(lineStart, lineEnd)

        val numberedMatch = Regex("^(\\d+)\\.\\s").find(line)
        if (numberedMatch != null && start < lineStart + numberedMatch.value.length) {
            pendingPrefixDeletion = Pair(lineStart, numberedMatch.value.length)
            return
        }
        if ((line.startsWith("• ") || line.startsWith("☐ ") || line.startsWith("☑ ")) && start < lineStart + 2) {
            pendingPrefixDeletion = Pair(lineStart, 2)
            return
        }
    }

    private fun handleBackspaceInListPrefix(s: Editable?): Boolean {
        val deletion = pendingPrefixDeletion ?: return false
        pendingPrefixDeletion = null
        if (s == null) return false

        val (origLineStart, origPrefixLen) = deletion
        // After the single-char delete, the remaining prefix starts at the same lineStart
        // but is now origPrefixLen - 1 chars long
        val remainingLen = origPrefixLen - 1
        if (remainingLen <= 0) return false
        if (origLineStart + remainingLen > s.length) return false

        isInternalChange = true
        s.delete(origLineStart, origLineStart + remainingLen)
        setSelection(origLineStart)
        renumberNumberedLists()
        isInternalChange = false
        return true
    }

    private fun autoContinueListOnEnter(s: Editable?): Boolean {
        if (s == null) return false
        val cursorPos = selectionStart
        if (cursorPos <= 0 || cursorPos > s.length) return false
        if (s[cursorPos - 1] != '\n') return false

        if (cursorPos >= 2 && s[cursorPos - 2] == '\n') return false

        val text = s.toString()
        var prevLineStart = cursorPos - 2
        while (prevLineStart > 0 && text[prevLineStart - 1] != '\n') prevLineStart--
        if (prevLineStart < 0) prevLineStart = 0

        val prevLine = text.substring(prevLineStart, cursorPos - 1)

        val numberedMatch = Regex("^(\\d+)\\.\\s").find(prevLine)
        if (numberedMatch != null) {
            val content = prevLine.substring(numberedMatch.value.length)
            if (content.isBlank()) {
                isInternalChange = true
                s.delete(prevLineStart, cursorPos)
                setSelection(prevLineStart.coerceAtMost(s.length))
                isInternalChange = false
                return true
            }
            val nextNum = (numberedMatch.groupValues[1].toIntOrNull() ?: 0) + 1
            val prefix = "$nextNum. "
            isInternalChange = true
            s.insert(cursorPos, prefix)
            setSelection(cursorPos + prefix.length)
            renumberNumberedLists()
            isInternalChange = false
            return true
        }

        if (prevLine.startsWith("• ")) {
            val content = prevLine.substring(2)
            if (content.isBlank()) {
                isInternalChange = true
                s.delete(prevLineStart, cursorPos)
                setSelection(prevLineStart.coerceAtMost(s.length))
                isInternalChange = false
                return true
            }
            isInternalChange = true
            s.insert(cursorPos, "• ")
            setSelection(cursorPos + 2)
            isInternalChange = false
            return true
        }

        if (prevLine.startsWith("☐ ") || prevLine.startsWith("☑ ")) {
            val content = prevLine.substring(2)
            if (content.isBlank()) {
                isInternalChange = true
                s.delete(prevLineStart, cursorPos)
                setSelection(prevLineStart.coerceAtMost(s.length))
                isInternalChange = false
                return true
            }
            isInternalChange = true
            s.insert(cursorPos, "☐ ")
            setSelection(cursorPos + 2)
            isInternalChange = false
            return true
        }

        return false
    }

    private fun applyInlineStyleShortcut(s: Editable?): Boolean {
        if (s == null) return false

        val start = selectionStart
        val end = selectionEnd
        if (start != end || start <= 0 || start > s.length) return false

        var lineStart = start - 1
        while (lineStart > 0 && s[lineStart - 1] != '\n') {
            lineStart--
        }

        val textBeforeCursor = s.subSequence(lineStart, start).toString()
        if (textBeforeCursor.length < 3) return false

        data class ShortcutPattern(
            val regex: Regex,
            val apply: (Editable, Int, Int) -> Unit,
        )

        val patterns = listOf(
            ShortcutPattern(Regex("(^|\\s)\\*([^*\\n]+)\\*$")) { editable, spanStart, spanEnd ->
                editable.setSpan(StyleSpan(Typeface.BOLD), spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            },
            ShortcutPattern(Regex("(^|\\s)_([^_\\n]+)_$")) { editable, spanStart, spanEnd ->
                editable.setSpan(StyleSpan(Typeface.ITALIC), spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            },
            ShortcutPattern(Regex("(^|\\s)~([^~\\n]+)~$")) { editable, spanStart, spanEnd ->
                editable.setSpan(StrikethroughSpan(), spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        )

        for (pattern in patterns) {
            val match = pattern.regex.find(textBeforeCursor) ?: continue
            val prefixWhitespaceLen = match.groupValues[1].length
            val styledText = match.groupValues[2]
            if (styledText.isEmpty()) continue

            val markerStartInLine = match.range.first + prefixWhitespaceLen
            val markerStart = lineStart + markerStartInLine
            if (markerStart < 0 || markerStart > start) continue

            isInternalChange = true
            s.replace(markerStart, start, styledText)

            val styleStart = markerStart
            val styleEnd = markerStart + styledText.length
            if (styleStart < styleEnd && styleEnd <= s.length) {
                pattern.apply(s, styleStart, styleEnd)
                setSelection(styleEnd)
            }
            isInternalChange = false
            return true
        }

        return false
    }

    private fun renumberNumberedLists() {
        val editable = text ?: return
        val fullText = editable.toString()
        val lines = fullText.split("\n")
        val numberedRegex = Regex("^(\\d+)\\.\\s")

        var counter = 0
        var offset = 0

        for (line in lines) {
            val match = numberedRegex.find(line)
            if (match != null) {
                counter++
                val oldPrefix = match.value
                val newPrefix = "$counter. "
                if (oldPrefix != newPrefix) {
                    editable.replace(offset, offset + oldPrefix.length, newPrefix)
                    offset += line.length - oldPrefix.length + newPrefix.length + 1
                } else {
                    offset += line.length + 1
                }
            } else {
                counter = 0
                offset += line.length + 1
            }
        }
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

    private fun openImagePicker() {
        val reactContext = context as? ReactContext ?: return
        val activity = reactContext.currentActivity ?: return

        if (activity !is ComponentActivity) {
            return
        }

        if (imagePickerLauncher == null) {
            imagePickerLauncher = activity.activityResultRegistry.register(
                imagePickerLauncherKey,
                ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                if (uri == null) return@register
                insertMediaAttachmentBlock(uri.toString())
            }
        }

        imagePickerLauncher?.launch("image/*")
    }

    private fun insertMediaAttachmentBlock(mediaData: MediaAttachmentData) {
        val editable = text ?: return
        var insertPos = selectionStart.coerceIn(0, editable.length)

        isInternalChange = true
        val nextPos = mediaAttachmentSupport.insertMediaAttachmentBlock(editable, insertPos, mediaData)
        setSelection(nextPos.coerceAtMost(editable.length))

        isInternalChange = false
        sendContentChange()
        saveToUndoStack()
        post { updateContentSize() }
    }

    private fun insertMediaAttachmentBlock(uri: String) {
        val safeUri = uri.trim()
        if (safeUri.isEmpty()) return
        val mediaData = mediaAttachmentSupport.createMediaDataForUri(safeUri)
        insertMediaAttachmentBlock(mediaData)
    }

    private fun parseMediaAttachmentPayload(payload: String): MediaAttachmentData? {
        val trimmed = payload.trim()
        if (trimmed.isEmpty()) return null

        return try {
            if (trimmed.startsWith("{")) {
                val obj = org.json.JSONObject(trimmed)
                val uri = obj.optString("uri", "").trim()
                if (uri.isEmpty()) {
                    null
                } else {
                    val sourceUri = obj.optString("sourceUri", uri).ifBlank { uri }
                    val kind = obj.optString("kind", "image")
                    val fileName = obj.optString("fileName", "").ifBlank { null }
                    val extension = obj.optString("extension", "").ifBlank { null }
                    val contentType = obj.optString("contentType", "").ifBlank { null }
                    val fileSize = if (obj.has("fileSize")) obj.optLong("fileSize", -1L) else -1L
                    val width = obj.optInt("width", 100).coerceAtLeast(1)
                    val height = obj.optInt("height", 100).coerceAtLeast(1)
                    val alt = obj.optString("alt", "Selected image")

                    MediaAttachmentData(
                        kind = kind,
                        uri = uri,
                        sourceUri = sourceUri,
                        fileName = fileName,
                        extension = extension,
                        contentType = contentType,
                        fileSize = fileSize.takeIf { it >= 0L },
                        widthDp = width,
                        heightDp = height,
                        alt = alt
                    )
                }
            } else {
                mediaAttachmentSupport.createMediaDataForUri(trimmed)
            }
        } catch (_: Exception) {
            mediaAttachmentSupport.createMediaDataForUri(trimmed)
        }
    }

    fun insertMediaAttachment(payload: String?) {
        val safePayload = payload?.trim().orEmpty()
        if (safePayload.isEmpty()) return
        val mediaData = parseMediaAttachmentPayload(safePayload) ?: return
        insertMediaAttachmentBlock(mediaData)
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
        imagePickerLauncher?.unregister()
        imagePickerLauncher = null
        hideToolbar()
        toolbarPopup = null
        floatingToolbar = null
    }
}
