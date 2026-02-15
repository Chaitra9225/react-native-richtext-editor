package com.richtext.editor

import com.facebook.react.bridge.ReadableArray
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp

@ReactModule(name = RichTextEditorViewManager.NAME)
class RichTextEditorViewManager : SimpleViewManager<RichTextEditorView>() {

    companion object {
        const val NAME = "RichTextEditorView"
    }

    override fun getName(): String = NAME

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
    fun setMaxHeight(view: RichTextEditorView, maxHeight: Double) {
        try {
            view.setMaxHeightValue(maxHeight.toInt())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @ReactProp(name = "numberOfLines")
    fun setNumberOfLines(view: RichTextEditorView, numberOfLines: Int) {
        try {
            view.setNumberOfLinesValue(numberOfLines)
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

    @ReactProp(name = "initialContentJson")
    fun setInitialContentJson(view: RichTextEditorView, initialContentJson: String?) {
        if (initialContentJson.isNullOrEmpty()) return

        try {
            val json = org.json.JSONArray(initialContentJson)
            val blocksList = mutableListOf<Map<String, Any>>()
            for (i in 0 until json.length()) {
                val block = json.getJSONObject(i)
                val blockMap = mutableMapOf<String, Any>()
                blockMap["text"] = block.optString("text", "")
                blockMap["type"] = block.optString("type", "paragraph")

                val stylesList = mutableListOf<Map<String, Any>>()
                val styles = block.optJSONArray("styles")
                if (styles != null) {
                    for (j in 0 until styles.length()) {
                        val style = styles.getJSONObject(j)
                        val styleMap = mutableMapOf<String, Any>()
                        styleMap["style"] = style.optString("style", "")
                        styleMap["start"] = style.optInt("start", 0)
                        styleMap["end"] = style.optInt("end", 0)
                        stylesList.add(styleMap)
                    }
                }
                blockMap["styles"] = stylesList
                blocksList.add(blockMap)
            }
            view.post {
                view.setContent(blocksList)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any> {
        val events = mutableMapOf<String, Any>()
        events["onContentChange"] = mapOf("registrationName" to "onContentChange")
        events["onSelectionChange"] = mapOf("registrationName" to "onSelectionChange")
        events["onEditorFocus"] = mapOf("registrationName" to "onEditorFocus")
        events["onEditorBlur"] = mapOf("registrationName" to "onEditorBlur")
        events["onSizeChange"] = mapOf("registrationName" to "onSizeChange")
        events["onActiveStylesChange"] = mapOf("registrationName" to "onActiveStylesChange")
        return events
    }
}
