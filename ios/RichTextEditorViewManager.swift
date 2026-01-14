import React

@objc(RichTextEditorViewManager)
class RichTextEditorViewManager: RCTViewManager {

    override func view() -> UIView! {
        return RichTextEditorView()
    }

    override static func requiresMainQueueSetup() -> Bool {
        return true
    }

    @objc func setContent(_ node: NSNumber, blocks: NSArray) {
        DispatchQueue.main.async {
            if let view = self.bridge?.uiManager.view(forReactTag: node) as? RichTextEditorView,
               let blocksArray = blocks as? [[String: Any]] {
                view.setContent(blocks: blocksArray)
            }
        }
    }

    @objc func getText(_ node: NSNumber, resolver resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
        DispatchQueue.main.async {
            if let view = self.bridge?.uiManager.view(forReactTag: node) as? RichTextEditorView {
                resolve(view.getText())
            } else {
                reject("ERROR", "View not found", nil)
            }
        }
    }

    @objc func getBlocks(_ node: NSNumber, resolver resolve: @escaping RCTPromiseResolveBlock, rejecter reject: @escaping RCTPromiseRejectBlock) {
        DispatchQueue.main.async {
            if let view = self.bridge?.uiManager.view(forReactTag: node) as? RichTextEditorView {
                resolve(view.getBlocksArray())
            } else {
                reject("ERROR", "View not found", nil)
            }
        }
    }

    @objc func clear(_ node: NSNumber) {
        DispatchQueue.main.async {
            if let view = self.bridge?.uiManager.view(forReactTag: node) as? RichTextEditorView {
                view.clear()
            }
        }
    }

    @objc func focus(_ node: NSNumber) {
        DispatchQueue.main.async {
            if let view = self.bridge?.uiManager.view(forReactTag: node) as? RichTextEditorView {
                view.focus()
            }
        }
    }

    @objc func blur(_ node: NSNumber) {
        DispatchQueue.main.async {
            if let view = self.bridge?.uiManager.view(forReactTag: node) as? RichTextEditorView {
                view.blur()
            }
        }
    }

    @objc func insertLink(_ node: NSNumber, url: NSString, text: NSString) {
        DispatchQueue.main.async {
            if let view = self.bridge?.uiManager.view(forReactTag: node) as? RichTextEditorView {
                view.insertLink(url: url as String, text: text as String)
            }
        }
    }

    @objc func undo(_ node: NSNumber) {
        DispatchQueue.main.async {
            if let view = self.bridge?.uiManager.view(forReactTag: node) as? RichTextEditorView {
                view.undo()
            }
        }
    }

    @objc func redo(_ node: NSNumber) {
        DispatchQueue.main.async {
            if let view = self.bridge?.uiManager.view(forReactTag: node) as? RichTextEditorView {
                view.redo()
            }
        }
    }

    @objc func toggleBold(_ node: NSNumber) {
        DispatchQueue.main.async {
            if let view = self.bridge?.uiManager.view(forReactTag: node) as? RichTextEditorView {
                view.toggleBold()
            }
        }
    }

    @objc func toggleItalic(_ node: NSNumber) {
        DispatchQueue.main.async {
            if let view = self.bridge?.uiManager.view(forReactTag: node) as? RichTextEditorView {
                view.toggleItalic()
            }
        }
    }

    @objc func toggleUnderline(_ node: NSNumber) {
        DispatchQueue.main.async {
            if let view = self.bridge?.uiManager.view(forReactTag: node) as? RichTextEditorView {
                view.toggleUnderline()
            }
        }
    }

    @objc func toggleStrikethrough(_ node: NSNumber) {
        DispatchQueue.main.async {
            if let view = self.bridge?.uiManager.view(forReactTag: node) as? RichTextEditorView {
                view.toggleStrikethrough()
            }
        }
    }

    @objc func toggleCode(_ node: NSNumber) {
        DispatchQueue.main.async {
            if let view = self.bridge?.uiManager.view(forReactTag: node) as? RichTextEditorView {
                view.toggleCode()
            }
        }
    }

    @objc func toggleHighlight(_ node: NSNumber, color: NSString?) {
        DispatchQueue.main.async {
            if let view = self.bridge?.uiManager.view(forReactTag: node) as? RichTextEditorView {
                view.toggleHighlight(color: color as String?)
            }
        }
    }

    @objc func setHeading(_ node: NSNumber) {
        DispatchQueue.main.async {
            if let view = self.bridge?.uiManager.view(forReactTag: node) as? RichTextEditorView {
                view.setHeading()
            }
        }
    }

    @objc func setBulletList(_ node: NSNumber) {
        DispatchQueue.main.async {
            if let view = self.bridge?.uiManager.view(forReactTag: node) as? RichTextEditorView {
                view.toggleBulletList()
            }
        }
    }

    @objc func setNumberedList(_ node: NSNumber) {
        DispatchQueue.main.async {
            if let view = self.bridge?.uiManager.view(forReactTag: node) as? RichTextEditorView {
                view.toggleNumberedList()
            }
        }
    }

    @objc func setQuote(_ node: NSNumber) {
        DispatchQueue.main.async {
            if let view = self.bridge?.uiManager.view(forReactTag: node) as? RichTextEditorView {
                view.setQuote()
            }
        }
    }

    @objc func setChecklist(_ node: NSNumber) {
        DispatchQueue.main.async {
            if let view = self.bridge?.uiManager.view(forReactTag: node) as? RichTextEditorView {
                view.setChecklist()
            }
        }
    }

    @objc func setParagraph(_ node: NSNumber) {
        DispatchQueue.main.async {
            if let view = self.bridge?.uiManager.view(forReactTag: node) as? RichTextEditorView {
                view.setParagraph()
            }
        }
    }

    @objc func clearFormatting(_ node: NSNumber) {
        DispatchQueue.main.async {
            if let view = self.bridge?.uiManager.view(forReactTag: node) as? RichTextEditorView {
                view.clearFormatting()
            }
        }
    }

    @objc func indent(_ node: NSNumber) {
        DispatchQueue.main.async {
            if let view = self.bridge?.uiManager.view(forReactTag: node) as? RichTextEditorView {
                view.indent()
            }
        }
    }

    @objc func outdent(_ node: NSNumber) {
        DispatchQueue.main.async {
            if let view = self.bridge?.uiManager.view(forReactTag: node) as? RichTextEditorView {
                view.outdent()
            }
        }
    }

    @objc func setAlignment(_ node: NSNumber, alignment: NSString) {
        DispatchQueue.main.async {
            if let view = self.bridge?.uiManager.view(forReactTag: node) as? RichTextEditorView {
                let alignmentValue: NSTextAlignment
                switch alignment as String {
                case "center":
                    alignmentValue = .center
                case "right":
                    alignmentValue = .right
                default:
                    alignmentValue = .left
                }
                view.setAlignment(alignmentValue)
            }
        }
    }

    @objc func toggleChecklistItem(_ node: NSNumber) {
        DispatchQueue.main.async {
            if let view = self.bridge?.uiManager.view(forReactTag: node) as? RichTextEditorView {
                view.toggleChecklistItem()
            }
        }
    }
}
