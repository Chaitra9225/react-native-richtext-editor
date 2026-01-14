package com.richtext.editor

import com.facebook.react.bridge.ReadableArray
import com.facebook.react.common.MapBuilder
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp

class RichTextEditorViewManager : SimpleViewManager<RichTextEditorView>() {

    override fun getName(): String = "RichTextEditorView"

    override fun createViewInstance(reactContext: ThemedReactContext): RichTextEditorView {
        return RichTextEditorView(reactContext)
    }

    @ReactProp(name = "placeholder")
    fun setPlaceholder(view: RichTextEditorView, placeholder: String?) {
        try {
            view.setPlaceholderText(placeholder ?: "")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @ReactProp(name = "editable")
    fun setEditable(view: RichTextEditorView, editable: Boolean) {
        try {
            view.setEditable(editable)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @ReactProp(name = "maxHeight")
    fun setMaxHeight(view: RichTextEditorView, maxHeight: Int) {
        try {
            view.setMaxHeightValue(maxHeight)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @ReactProp(name = "showToolbar")
    fun setShowToolbar(view: RichTextEditorView, showToolbar: Boolean) {
        try {
            view.setShowToolbar(showToolbar)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @ReactProp(name = "toolbarOptions")
    fun setToolbarOptions(view: RichTextEditorView, toolbarOptions: ReadableArray?) {
        try {
            if (toolbarOptions != null) {
                val options = mutableListOf<String>()
                for (i in 0 until toolbarOptions.size()) {
                    toolbarOptions.getString(i)?.let { options.add(it) }
                }
                view.setToolbarOptions(options)
            } else {
                view.setToolbarOptions(null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @ReactProp(name = "variant")
    fun setVariant(view: RichTextEditorView, variant: String?) {
        try {
            view.setVariant(variant ?: "outlined")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @ReactProp(name = "initialContent")
    fun setInitialContent(view: RichTextEditorView, initialContent: ReadableArray?) {
        if (initialContent == null || initialContent.size() == 0) return

        try {
            val blocksList = mutableListOf<Map<String, Any>>()
            for (i in 0 until initialContent.size()) {
                val block = initialContent.getMap(i) ?: continue
                val blockMap = mutableMapOf<String, Any>()
                blockMap["text"] = block.getString("text") ?: ""
                blockMap["type"] = block.getString("type") ?: "paragraph"

                val stylesList = mutableListOf<Map<String, Any>>()
                if (block.hasKey("styles")) {
                    val styles = block.getArray("styles")
                    if (styles != null) {
                        for (j in 0 until styles.size()) {
                            val style = styles.getMap(j) ?: continue
                            val styleMap = mutableMapOf<String, Any>()
                            styleMap["style"] = style.getString("style") ?: ""
                            styleMap["start"] = if (style.hasKey("start")) style.getInt("start") else 0
                            styleMap["end"] = if (style.hasKey("end")) style.getInt("end") else 0
                            stylesList.add(styleMap)
                        }
                    }
                }
                blockMap["styles"] = stylesList
                blocksList.add(blockMap)
            }
            // Delay setting content until view is ready
            view.post {
                view.setContent(blocksList)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getExportedCustomDirectEventTypeConstants(): Map<String, Any>? {
        return MapBuilder.builder<String, Any>()
            .put("onContentChange", MapBuilder.of("registrationName", "onContentChange"))
            .put("onSelectionChange", MapBuilder.of("registrationName", "onSelectionChange"))
            .put("onEditorFocus", MapBuilder.of("registrationName", "onEditorFocus"))
            .put("onEditorBlur", MapBuilder.of("registrationName", "onEditorBlur"))
            .put("onSizeChange", MapBuilder.of("registrationName", "onSizeChange"))
            .build()
    }

    override fun getCommandsMap(): Map<String, Int>? {
        return MapBuilder.builder<String, Int>()
            // Content management
            .put("setContent", 1)
            .put("getText", 2)
            .put("getBlocks", 3)
            .put("clear", 4)
            // Focus management
            .put("focus", 5)
            .put("blur", 6)
            // Text styles
            .put("toggleBold", 10)
            .put("toggleItalic", 11)
            .put("toggleUnderline", 12)
            .put("toggleStrikethrough", 13)
            .put("toggleCode", 14)
            .put("toggleHighlight", 15)
            // Block types
            .put("setHeading", 16)
            .put("setBulletList", 17)
            .put("setNumberedList", 18)
            .put("setQuote", 19)
            .put("setChecklist", 20)
            .put("setParagraph", 21)
            // Actions
            .put("insertLink", 7)
            .put("undo", 8)
            .put("redo", 9)
            .put("clearFormatting", 22)
            // Indentation
            .put("indent", 23)
            .put("outdent", 24)
            // Alignment
            .put("setAlignment", 25)
            // Checklist
            .put("toggleChecklistItem", 26)
            .build()
    }

    override fun receiveCommand(view: RichTextEditorView, commandId: Int, args: ReadableArray?) {
        when (commandId) {
            1 -> {
                val blocks = args?.getArray(0)
                if (blocks != null) {
                    val blocksList = mutableListOf<Map<String, Any>>()
                    for (i in 0 until blocks.size()) {
                        val block = blocks.getMap(i)
                        val blockMap = mutableMapOf<String, Any>()
                        blockMap["text"] = block?.getString("text") ?: ""
                        blockMap["type"] = block?.getString("type") ?: "paragraph"

                        val styles = block?.getArray("styles")
                        if (styles != null) {
                            val stylesList = mutableListOf<Map<String, Any>>()
                            for (j in 0 until styles.size()) {
                                val style = styles.getMap(j)
                                val styleMap = mutableMapOf<String, Any>()
                                styleMap["style"] = style?.getString("style") ?: ""
                                styleMap["start"] = style?.getInt("start") ?: 0
                                styleMap["end"] = style?.getInt("end") ?: 0
                                stylesList.add(styleMap)
                            }
                            blockMap["styles"] = stylesList
                        }
                        blocksList.add(blockMap)
                    }
                    view.setContent(blocksList)
                }
            }
            4 -> view.clearContent()
            5 -> view.focusEditor()
            6 -> view.blurEditor()
            7 -> {
                val url = args?.getString(0) ?: ""
                val text = args?.getString(1) ?: ""
                view.insertLink(url, text)
            }
            8 -> view.undo()
            9 -> view.redo()
            10 -> view.toggleBold()
            11 -> view.toggleItalic()
            12 -> view.toggleUnderline()
            13 -> view.toggleStrikethrough()
            14 -> view.toggleCode()
            15 -> {
                val color = args?.getString(0)
                view.toggleHighlight(color)
            }
            16 -> view.setHeading()
            17 -> view.toggleBulletList()
            18 -> view.toggleNumberedList()
            19 -> view.setQuote()
            20 -> view.setChecklist()
            21 -> view.setParagraph()
            22 -> view.clearFormatting()
            23 -> view.indent()
            24 -> view.outdent()
            25 -> {
                val alignment = args?.getString(0) ?: "left"
                val layoutAlignment = when (alignment) {
                    "center" -> android.text.Layout.Alignment.ALIGN_CENTER
                    "right" -> android.text.Layout.Alignment.ALIGN_OPPOSITE
                    else -> android.text.Layout.Alignment.ALIGN_NORMAL
                }
                view.setAlignment(layoutAlignment)
            }
            26 -> view.toggleChecklistItem()
        }
    }
}
