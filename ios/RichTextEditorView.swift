import UIKit
import React

class FloatingToolbar: UIView, UIScrollViewDelegate {
    weak var editorView: RichTextEditorView?

    private var buttons: [UIButton] = []
    private var scrollView: UIScrollView!
    private var stackView: UIStackView!
    private var leftArrow: UILabel!
    private var rightArrow: UILabel!
    private var enabledOptions: [String] = [
        "bold", "italic", "underline", "strikethrough", "code", "highlight",
        "heading", "bullet", "numbered", "quote", "checklist",
        "link", "undo", "redo", "clearFormatting",
        "indent", "outdent",
        "alignLeft", "alignCenter", "alignRight"
    ]

    private let optionToIndex: [String: Int] = [
        "bold": 0, "italic": 1, "strikethrough": 2, "underline": 3, "code": 4, "highlight": 5,
        "heading": 6, "bullet": 7, "numbered": 8, "quote": 9, "checklist": 10,
        "link": 11, "undo": 12, "redo": 13, "clearFormatting": 14,
        "indent": 15, "outdent": 16,
        "alignLeft": 17, "alignCenter": 18, "alignRight": 19
    ]

    private let toolbarBackgroundColor = UIColor(red: 45/255, green: 45/255, blue: 45/255, alpha: 1.0)
    private let activeColor = UIColor(red: 80/255, green: 130/255, blue: 200/255, alpha: 1.0)
    private let inactiveColor = UIColor.white
    private let arrowColor = UIColor(white: 1.0, alpha: 0.7)

    override init(frame: CGRect) {
        super.init(frame: frame)
        setupToolbar()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupToolbar()
    }

    func setToolbarOptions(_ options: [String]?) {
        if let options = options, !options.isEmpty {
            enabledOptions = options
        } else {
            enabledOptions = [
                "bold", "italic", "underline", "strikethrough", "code", "highlight",
                "heading", "bullet", "numbered", "quote", "checklist",
                "link", "undo", "redo", "clearFormatting",
                "indent", "outdent",
                "alignLeft", "alignCenter", "alignRight"
            ]
        }
        rebuildButtons()
        updateScrollIndicators()
    }

    private func setupToolbar() {
        backgroundColor = toolbarBackgroundColor
        layer.cornerRadius = 10
        layer.shadowColor = UIColor.black.cgColor
        layer.shadowOffset = CGSize(width: 0, height: 2)
        layer.shadowOpacity = 0.3
        layer.shadowRadius = 6

        leftArrow = UILabel()
        leftArrow.text = "‹"
        leftArrow.font = UIFont.systemFont(ofSize: 20, weight: .bold)
        leftArrow.textColor = arrowColor
        leftArrow.textAlignment = .center
        leftArrow.translatesAutoresizingMaskIntoConstraints = false
        leftArrow.isHidden = true
        leftArrow.isUserInteractionEnabled = true
        let leftTap = UITapGestureRecognizer(target: self, action: #selector(leftArrowTapped))
        leftArrow.addGestureRecognizer(leftTap)
        addSubview(leftArrow)

        rightArrow = UILabel()
        rightArrow.text = "›"
        rightArrow.font = UIFont.systemFont(ofSize: 20, weight: .bold)
        rightArrow.textColor = arrowColor
        rightArrow.textAlignment = .center
        rightArrow.translatesAutoresizingMaskIntoConstraints = false
        rightArrow.isHidden = false
        rightArrow.isUserInteractionEnabled = true
        let rightTap = UITapGestureRecognizer(target: self, action: #selector(rightArrowTapped))
        rightArrow.addGestureRecognizer(rightTap)
        addSubview(rightArrow)

        scrollView = UIScrollView()
        scrollView.showsHorizontalScrollIndicator = false
        scrollView.showsVerticalScrollIndicator = false
        scrollView.translatesAutoresizingMaskIntoConstraints = false
        scrollView.delegate = self
        addSubview(scrollView)

        stackView = UIStackView()
        stackView.axis = .horizontal
        stackView.spacing = 8
        stackView.distribution = .fill
        stackView.translatesAutoresizingMaskIntoConstraints = false
        scrollView.addSubview(stackView)

        NSLayoutConstraint.activate([
            leftArrow.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 4),
            leftArrow.centerYAnchor.constraint(equalTo: centerYAnchor),
            leftArrow.widthAnchor.constraint(equalToConstant: 16),

            rightArrow.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -4),
            rightArrow.centerYAnchor.constraint(equalTo: centerYAnchor),
            rightArrow.widthAnchor.constraint(equalToConstant: 16),

            scrollView.leadingAnchor.constraint(equalTo: leftArrow.trailingAnchor, constant: 2),
            scrollView.trailingAnchor.constraint(equalTo: rightArrow.leadingAnchor, constant: -2),
            scrollView.topAnchor.constraint(equalTo: topAnchor, constant: 8),
            scrollView.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -8),

            stackView.leadingAnchor.constraint(equalTo: scrollView.leadingAnchor),
            stackView.trailingAnchor.constraint(equalTo: scrollView.trailingAnchor),
            stackView.topAnchor.constraint(equalTo: scrollView.topAnchor),
            stackView.bottomAnchor.constraint(equalTo: scrollView.bottomAnchor),
            stackView.heightAnchor.constraint(equalTo: scrollView.heightAnchor)
        ])

        rebuildButtons()
    }

    func scrollViewDidScroll(_ scrollView: UIScrollView) {
        updateScrollIndicators()
    }

    private func updateScrollIndicators() {
        guard let scrollView = scrollView else { return }

        let contentWidth = scrollView.contentSize.width
        let scrollViewWidth = scrollView.bounds.width
        let offsetX = scrollView.contentOffset.x

        leftArrow.isHidden = offsetX <= 5
        rightArrow.isHidden = offsetX >= (contentWidth - scrollViewWidth - 5)
    }

    @objc private func leftArrowTapped() {
        guard let scrollView = scrollView else { return }
        let scrollAmount: CGFloat = 120
        let newOffsetX = max(0, scrollView.contentOffset.x - scrollAmount)
        scrollView.setContentOffset(CGPoint(x: newOffsetX, y: 0), animated: true)
    }

    @objc private func rightArrowTapped() {
        guard let scrollView = scrollView else { return }
        let scrollAmount: CGFloat = 120
        let maxOffsetX = scrollView.contentSize.width - scrollView.bounds.width
        let newOffsetX = min(maxOffsetX, scrollView.contentOffset.x + scrollAmount)
        scrollView.setContentOffset(CGPoint(x: newOffsetX, y: 0), animated: true)
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        DispatchQueue.main.async {
            self.updateScrollIndicators()
        }
    }

    private let iconSize: CGFloat = 20
    private let buttonSize: CGFloat = 36
    private let iconPadding: CGFloat = 8

    private func rebuildButtons() {
        buttons.forEach { $0.removeFromSuperview() }
        buttons.removeAll()

        for option in enabledOptions {
            guard let index = optionToIndex[option] else { continue }

            let button = UIButton(type: .custom)
            button.tag = index
            button.layer.cornerRadius = 6
            button.addTarget(self, action: #selector(buttonTapped(_:)), for: .touchUpInside)
            button.widthAnchor.constraint(equalToConstant: buttonSize).isActive = true
            button.heightAnchor.constraint(equalToConstant: buttonSize).isActive = true

            // Use vector icons with consistent sizing
            if let icon = ToolbarIcons.getIcon(for: option, color: inactiveColor, size: CGSize(width: iconSize, height: iconSize)) {
                button.setImage(icon.withRenderingMode(.alwaysOriginal), for: .normal)
                button.imageView?.contentMode = .scaleAspectFit
                button.contentEdgeInsets = UIEdgeInsets(top: iconPadding, left: iconPadding, bottom: iconPadding, right: iconPadding)
            }

            buttons.append(button)
            stackView.addArrangedSubview(button)
        }
    }

    func getToolbarWidth() -> CGFloat {
        let screenWidth = UIScreen.main.bounds.width
        let maxWidth = screenWidth * 0.9
        let buttonCount = CGFloat(enabledOptions.count)
        let calculatedWidth = (buttonCount * 36) + ((buttonCount - 1) * 8) + 48
        return min(calculatedWidth, maxWidth)
    }

    private func createButtonAttributedString(for index: Int, active: Bool) -> NSAttributedString {
        let color = active ? activeColor : inactiveColor
        let fontSize: CGFloat = 18

        switch index {
        case 0:
            return NSAttributedString(string: "B", attributes: [
                .font: UIFont.boldSystemFont(ofSize: fontSize),
                .foregroundColor: color
            ])
        case 1:
            return NSAttributedString(string: "I", attributes: [
                .font: UIFont.italicSystemFont(ofSize: fontSize),
                .foregroundColor: color
            ])
        case 2:
            return NSAttributedString(string: "S", attributes: [
                .font: UIFont.systemFont(ofSize: fontSize, weight: .medium),
                .foregroundColor: color,
                .strikethroughStyle: NSUnderlineStyle.single.rawValue,
                .strikethroughColor: color
            ])
        case 3:
            return NSAttributedString(string: "U", attributes: [
                .font: UIFont.systemFont(ofSize: fontSize, weight: .medium),
                .foregroundColor: color,
                .underlineStyle: NSUnderlineStyle.single.rawValue,
                .underlineColor: color
            ])
        case 4:
            return NSAttributedString(string: "</>", attributes: [
                .font: UIFont(name: "Menlo", size: fontSize - 4) ?? UIFont.monospacedSystemFont(ofSize: fontSize - 4, weight: .medium),
                .foregroundColor: color
            ])
        case 5:
            return NSAttributedString(string: "H", attributes: [
                .font: UIFont.systemFont(ofSize: fontSize, weight: .medium),
                .foregroundColor: color,
                .backgroundColor: active ? UIColor.yellow.withAlphaComponent(0.5) : UIColor.yellow.withAlphaComponent(0.3)
            ])
        case 6:
            return NSAttributedString(string: "H1", attributes: [
                .font: UIFont.boldSystemFont(ofSize: fontSize - 2),
                .foregroundColor: color
            ])
        case 7:
            return createListIcon(type: .bullet, color: color)
        case 8:
            return createListIcon(type: .numbered, color: color)
        case 9:
            return NSAttributedString(string: "❞", attributes: [
                .font: UIFont.systemFont(ofSize: fontSize + 2),
                .foregroundColor: color
            ])
        case 10:
            return NSAttributedString(string: "☑", attributes: [
                .font: UIFont.systemFont(ofSize: fontSize),
                .foregroundColor: color
            ])
        case 11:
            return NSAttributedString(string: "🔗", attributes: [
                .font: UIFont.systemFont(ofSize: fontSize - 2),
                .foregroundColor: color
            ])
        case 12:
            return NSAttributedString(string: "↩", attributes: [
                .font: UIFont.systemFont(ofSize: fontSize),
                .foregroundColor: color
            ])
        case 13:
            return NSAttributedString(string: "↪", attributes: [
                .font: UIFont.systemFont(ofSize: fontSize),
                .foregroundColor: color
            ])
        case 14:
            return NSAttributedString(string: "Tx", attributes: [
                .font: UIFont.systemFont(ofSize: fontSize - 2, weight: .medium),
                .foregroundColor: color,
                .strikethroughStyle: NSUnderlineStyle.single.rawValue,
                .strikethroughColor: color
            ])
        case 15:
            return NSAttributedString(string: "→⊢", attributes: [
                .font: UIFont.systemFont(ofSize: fontSize - 4),
                .foregroundColor: color
            ])
        case 16:
            return NSAttributedString(string: "⊣←", attributes: [
                .font: UIFont.systemFont(ofSize: fontSize - 4),
                .foregroundColor: color
            ])
        case 17:
            return createAlignmentIcon(alignment: .left, color: color)
        case 18:
            return createAlignmentIcon(alignment: .center, color: color)
        case 19:
            return createAlignmentIcon(alignment: .right, color: color)
        default:
            return NSAttributedString(string: "")
        }
    }

    private func createAlignmentIcon(alignment: NSTextAlignment, color: UIColor) -> NSAttributedString {
        let result = NSMutableAttributedString()
        let paragraphStyle = NSMutableParagraphStyle()
        paragraphStyle.lineSpacing = 1
        paragraphStyle.alignment = alignment
        paragraphStyle.lineHeightMultiple = 0.9

        let attrs: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 8, weight: .medium),
            .foregroundColor: color,
            .paragraphStyle: paragraphStyle
        ]

        let lines = ["────", "──────", "────"]
        for (i, line) in lines.enumerated() {
            result.append(NSAttributedString(string: line, attributes: attrs))
            if i < lines.count - 1 {
                result.append(NSAttributedString(string: "\n", attributes: attrs))
            }
        }

        return result
    }

    private enum ListIconType {
        case bullet
        case numbered
    }

    private func createListIcon(type: ListIconType, color: UIColor) -> NSAttributedString {
        let result = NSMutableAttributedString()

        let paragraphStyle = NSMutableParagraphStyle()
        paragraphStyle.lineSpacing = 1
        paragraphStyle.alignment = .left
        paragraphStyle.lineHeightMultiple = 0.9

        let attrs: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 9, weight: .medium),
            .foregroundColor: color,
            .paragraphStyle: paragraphStyle
        ]

        for i in 0..<3 {
            let marker = type == .bullet ? "•" : "\(i + 1)"
            let line = "\(marker) ──"
            result.append(NSAttributedString(string: line, attributes: attrs))
            if i < 2 {
                result.append(NSAttributedString(string: "\n", attributes: attrs))
            }
        }

        return result
    }

    @objc private func buttonTapped(_ sender: UIButton) {
        switch sender.tag {
        case 0: editorView?.toggleBold()
        case 1: editorView?.toggleItalic()
        case 2: editorView?.toggleStrikethrough()
        case 3: editorView?.toggleUnderline()
        case 4: editorView?.toggleCode()
        case 5: editorView?.toggleHighlight(color: nil)
        case 6: editorView?.setHeading()
        case 7: editorView?.toggleBulletList()
        case 8: editorView?.toggleNumberedList()
        case 9: editorView?.setQuote()
        case 10: editorView?.setChecklist()
        case 11: editorView?.promptInsertLink()
        case 12: editorView?.undo()
        case 13: editorView?.redo()
        case 14: editorView?.clearFormatting()
        case 15: editorView?.indent()
        case 16: editorView?.outdent()
        case 17: editorView?.setAlignment(.left)
        case 18: editorView?.setAlignment(.center)
        case 19: editorView?.setAlignment(.right)
        default: break
        }
        editorView?.updateToolbarButtonStates()
    }

    private let indexToOption: [Int: String] = [
        0: "bold", 1: "italic", 2: "strikethrough", 3: "underline", 4: "code", 5: "highlight",
        6: "heading", 7: "bullet", 8: "numbered", 9: "quote", 10: "checklist",
        11: "link", 12: "undo", 13: "redo", 14: "clearFormatting",
        15: "indent", 16: "outdent",
        17: "alignLeft", 18: "alignCenter", 19: "alignRight"
    ]

    func updateButtonStates(bold: Bool, italic: Bool, underline: Bool, strikethrough: Bool, code: Bool = false, highlight: Bool = false, heading: Bool = false, bullet: Bool, numbered: Bool, quote: Bool = false, checklist: Bool = false, alignLeft: Bool = true, alignCenter: Bool = false, alignRight: Bool = false) {
        let styleStates: [Int: Bool] = [
            0: bold, 1: italic, 2: strikethrough, 3: underline, 4: code, 5: highlight,
            6: heading, 7: bullet, 8: numbered, 9: quote, 10: checklist,
            11: false, 12: false, 13: false, 14: false,
            15: false, 16: false,
            17: alignLeft, 18: alignCenter, 19: alignRight
        ]

        for button in buttons {
            let tag = button.tag
            let isActive = styleStates[tag] ?? false
            let color = isActive ? activeColor : inactiveColor

            // Update icon with new color
            if let option = indexToOption[tag],
               let icon = ToolbarIcons.getIcon(for: option, color: color, size: CGSize(width: iconSize, height: iconSize)) {
                button.setImage(icon.withRenderingMode(.alwaysOriginal), for: .normal)
            }

            button.backgroundColor = isActive ? UIColor.white.withAlphaComponent(0.15) : .clear
        }
    }
}

class RichTextView: UITextView {
    override init(frame: CGRect, textContainer: NSTextContainer?) {
        super.init(frame: frame, textContainer: textContainer)
        disableAutofill()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        disableAutofill()
    }

    private func disableAutofill() {
        autocorrectionType = .no
        autocapitalizationType = .none
        spellCheckingType = .no
        smartQuotesType = .no
        smartDashesType = .no
        smartInsertDeleteType = .no

        if #available(iOS 10.0, *) {
            textContentType = UITextContentType(rawValue: "")
        }

        if #available(iOS 9.0, *) {
            inputAssistantItem.leadingBarButtonGroups = []
            inputAssistantItem.trailingBarButtonGroups = []
        }

        inputAccessoryView = UIView(frame: .zero)

        if #available(iOS 16.0, *) {
            isFindInteractionEnabled = false
        }

        isSecureTextEntry = true
        isSecureTextEntry = false
    }

    override func canPerformAction(_ action: Selector, withSender sender: Any?) -> Bool {
        return false
    }
}

@objcMembers
class RichTextEditorView: UIView, UITextViewDelegate {
    private static let defaultLineHeightMultiple: CGFloat = 1.3

    private let textView: RichTextView = {
        let tv = RichTextView()
        tv.font = UIFont.systemFont(ofSize: 16)
        tv.textContainerInset = UIEdgeInsets(top: 12, left: 8, bottom: 12, right: 8)
        tv.translatesAutoresizingMaskIntoConstraints = false
        tv.backgroundColor = .clear

        // Set default paragraph style with consistent line height
        let paragraphStyle = NSMutableParagraphStyle()
        paragraphStyle.lineHeightMultiple = defaultLineHeightMultiple
        tv.typingAttributes = [
            .font: UIFont.systemFont(ofSize: 16),
            .paragraphStyle: paragraphStyle
        ]

        return tv
    }()

    private let placeholderLabel: UILabel = {
        let label = UILabel()
        label.textColor = UIColor.placeholderText
        label.font = UIFont.systemFont(ofSize: 16)
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()

    private let floatingToolbar: FloatingToolbar = {
        let toolbar = FloatingToolbar()
        toolbar.translatesAutoresizingMaskIntoConstraints = true
        toolbar.isHidden = true
        return toolbar
    }()

    private lazy var toolbarBackdrop: UIView = {
        let view = UIView()
        view.backgroundColor = .clear
        view.isUserInteractionEnabled = false
        view.isHidden = true
        return view
    }()

    private var maxHeightConstraint: NSLayoutConstraint?
    private var undoStack: [NSAttributedString] = []
    private var redoStack: [NSAttributedString] = []
    private var isInternalChange = false
    private var currentKeyboardHeight: CGFloat = 0
    private var savedSelectionRange: NSRange = NSRange(location: 0, length: 0)

    @objc var placeholder: String = "" {
        didSet { placeholderLabel.text = placeholder }
    }

    @objc var editable: Bool = true {
        didSet {
            textView.isEditable = editable
            applyNumberOfLines()
        }
    }

    @objc var maxHeight: CGFloat = 0 {
        didSet {
            if maxHeight > 0 {
                maxHeightConstraint?.isActive = false
                maxHeightConstraint = textView.heightAnchor.constraint(lessThanOrEqualToConstant: maxHeight)
                maxHeightConstraint?.isActive = true
            }
        }
    }

    @objc var numberOfLines: Int = 0 {
        didSet {
            applyNumberOfLines()
        }
    }

    @objc var showToolbar: Bool = true

    @objc var toolbarOptions: [String]? {
        didSet {
            floatingToolbar.setToolbarOptions(toolbarOptions)
        }
    }

    @objc var initialContentJson: String? {
        didSet {
            if let jsonString = initialContentJson,
               let data = jsonString.data(using: .utf8),
               let blocks = try? JSONSerialization.jsonObject(with: data, options: []) as? [[String: Any]] {
                setContent(blocks: blocks)
            }
        }
    }

    @objc var variant: String = "outlined" {
        didSet {
            applyVariantStyle()
        }
    }

    @objc var onContentChange: RCTDirectEventBlock?
    @objc var onSelectionChange: RCTDirectEventBlock?
    @objc var onEditorFocus: RCTDirectEventBlock?
    @objc var onEditorBlur: RCTDirectEventBlock?
    @objc var onSizeChange: RCTDirectEventBlock?
    @objc var onActiveStylesChange: RCTDirectEventBlock?

    private var lastReportedHeight: CGFloat = 0
    private var calculatedHeight: CGFloat = 44

    override init(frame: CGRect) {
        super.init(frame: frame)
        setupView()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupView()
    }

    private lazy var bottomBorder: UIView = {
        let view = UIView()
        view.backgroundColor = UIColor.separator
        view.translatesAutoresizingMaskIntoConstraints = false
        view.isHidden = true
        return view
    }()

    private func setupView() {
        backgroundColor = .systemBackground

        addSubview(textView)
        addSubview(placeholderLabel)
        addSubview(bottomBorder)

        NSLayoutConstraint.activate([
            bottomBorder.leadingAnchor.constraint(equalTo: leadingAnchor),
            bottomBorder.trailingAnchor.constraint(equalTo: trailingAnchor),
            bottomBorder.bottomAnchor.constraint(equalTo: bottomAnchor),
            bottomBorder.heightAnchor.constraint(equalToConstant: 1)
        ])

        applyVariantStyle()

        floatingToolbar.editorView = self

        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(backdropTapped))
        toolbarBackdrop.addGestureRecognizer(tapGesture)

        DispatchQueue.main.async { [weak self] in
            if let window = self?.window {
                window.addSubview(self!.toolbarBackdrop)
                window.addSubview(self!.floatingToolbar)
            }
        }

        NSLayoutConstraint.activate([
            textView.topAnchor.constraint(equalTo: topAnchor),
            textView.leadingAnchor.constraint(equalTo: leadingAnchor),
            textView.trailingAnchor.constraint(equalTo: trailingAnchor),
            textView.bottomAnchor.constraint(equalTo: bottomAnchor),

            placeholderLabel.topAnchor.constraint(equalTo: textView.topAnchor, constant: 12),
            placeholderLabel.leadingAnchor.constraint(equalTo: textView.leadingAnchor, constant: 13)
        ])

        textView.delegate = self
        textView.isScrollEnabled = false

        NotificationCenter.default.addObserver(self, selector: #selector(textDidChange), name: UITextView.textDidChangeNotification, object: textView)
        NotificationCenter.default.addObserver(self, selector: #selector(keyboardWillShow(_:)), name: UIResponder.keyboardWillShowNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(keyboardWillHide(_:)), name: UIResponder.keyboardWillHideNotification, object: nil)
    }

    @objc private func keyboardWillShow(_ notification: Notification) {
        if let keyboardFrame = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect {
            currentKeyboardHeight = keyboardFrame.height
        }
    }

    @objc private func keyboardWillHide(_ notification: Notification) {
        currentKeyboardHeight = 0
    }

    @objc private func backdropTapped() {
        hideToolbar()
        textView.selectedRange = NSRange(location: textView.selectedRange.location, length: 0)
    }

    private func hideToolbar() {
        floatingToolbar.isHidden = true
        toolbarBackdrop.isHidden = true
    }

    override func didMoveToWindow() {
        super.didMoveToWindow()
        if let window = window {
            toolbarBackdrop.removeFromSuperview()
            floatingToolbar.removeFromSuperview()
            window.addSubview(toolbarBackdrop)
            window.addSubview(floatingToolbar)
        }
    }

    override func removeFromSuperview() {
        toolbarBackdrop.removeFromSuperview()
        floatingToolbar.removeFromSuperview()
        super.removeFromSuperview()
    }

    deinit {
        toolbarBackdrop.removeFromSuperview()
        floatingToolbar.removeFromSuperview()
        NotificationCenter.default.removeObserver(self)
    }

    private func updateToolbarPosition() {
        guard let selectedRange = textView.selectedTextRange,
              !selectedRange.isEmpty,
              showToolbar,
              let window = window else {
            hideToolbar()
            return
        }

        let selectionRect = textView.firstRect(for: selectedRange)

        guard !selectionRect.isNull && !selectionRect.isInfinite && selectionRect.width > 0 else {
            hideToolbar()
            return
        }

        let convertedRect = textView.convert(selectionRect, to: window)

        guard convertedRect.minY > 0 && convertedRect.maxY < window.bounds.height &&
              convertedRect.minX >= 0 && convertedRect.maxX <= window.bounds.width else {
            hideToolbar()
            return
        }

        let toolbarWidth: CGFloat = floatingToolbar.getToolbarWidth()
        let toolbarHeight: CGFloat = 52

        let safeAreaTop = window.safeAreaInsets.top
        let safeAreaBottom = window.safeAreaInsets.bottom

        var toolbarX = (window.bounds.width - toolbarWidth) / 2
        var toolbarY = convertedRect.maxY + 8

        toolbarX = max(8, min(toolbarX, window.bounds.width - toolbarWidth - 8))

        let maxY = window.bounds.height - safeAreaBottom - currentKeyboardHeight - toolbarHeight - 8
        if toolbarY > maxY {
            toolbarY = convertedRect.minY - toolbarHeight - 8
            if toolbarY < safeAreaTop + 8 {
                toolbarY = safeAreaTop + 8
            }
        }

        toolbarBackdrop.frame = window.bounds
        toolbarBackdrop.isHidden = false
        window.bringSubviewToFront(toolbarBackdrop)

        floatingToolbar.frame = CGRect(x: toolbarX, y: toolbarY, width: toolbarWidth, height: toolbarHeight)
        floatingToolbar.isHidden = false
        window.bringSubviewToFront(floatingToolbar)
    }

    func textViewDidBeginEditing(_ textView: UITextView) {
        onEditorFocus?([:])
    }

    func textViewDidEndEditing(_ textView: UITextView) {
        hideToolbar()
        onEditorBlur?([:])
    }

    func textViewDidChangeSelection(_ textView: UITextView) {
        // Save selection when there's a valid selection (for toolbar actions)
        let range = textView.selectedRange
        if range.length > 0 {
            savedSelectionRange = range
        }

        updateToolbarPosition()
        updateToolbarButtonStates()

        emitActiveStyles()

        onSelectionChange?([
            "start": range.location,
            "end": range.location + range.length
        ])
    }

    private func emitActiveStyles() {
        guard let attributedText = textView.attributedText else { return }

        let range = textView.selectedRange
        let checkRange = range.length > 0 ? range : NSRange(location: max(0, range.location - 1), length: 1)

        guard checkRange.location >= 0, checkRange.location < attributedText.length else {
            onActiveStylesChange?([
                "bold": false,
                "italic": false,
                "underline": false,
                "strikethrough": false,
                "code": false,
                "highlight": false,
                "blockType": "paragraph",
                "alignment": "left"
            ])
            return
        }

        var hasBold = false
        var hasItalic = false
        var hasUnderline = false
        var hasStrikethrough = false
        var hasCode = false
        var hasHighlight = false
        var blockType = "paragraph"
        var alignment = "left"

        if let font = attributedText.attribute(.font, at: checkRange.location, effectiveRange: nil) as? UIFont {
            let traits = font.fontDescriptor.symbolicTraits
            hasBold = traits.contains(.traitBold)
            hasItalic = traits.contains(.traitItalic)

            if font.fontDescriptor.symbolicTraits.contains(.traitMonoSpace) ||
               font.fontName.lowercased().contains("mono") ||
               font.fontName.lowercased().contains("courier") {
                hasCode = true
            }
        }

        if let underlineStyle = attributedText.attribute(.underlineStyle, at: checkRange.location, effectiveRange: nil) as? Int,
           underlineStyle != 0 {
            hasUnderline = true
        }

        if let strikeStyle = attributedText.attribute(.strikethroughStyle, at: checkRange.location, effectiveRange: nil) as? Int,
           strikeStyle != 0 {
            hasStrikethrough = true
        }

        if let bgColor = attributedText.attribute(.backgroundColor, at: checkRange.location, effectiveRange: nil) as? UIColor {
            var red: CGFloat = 0, green: CGFloat = 0, blue: CGFloat = 0, alpha: CGFloat = 0
            bgColor.getRed(&red, green: &green, blue: &blue, alpha: &alpha)
            if red > 0.8 && green > 0.8 && blue < 0.5 {
                hasHighlight = true
            }
        }

        let text = textView.text ?? ""
        let lineRange = (text as NSString).lineRange(for: NSRange(location: range.location, length: 0))
        let lineText = (text as NSString).substring(with: lineRange)

        if lineText.hasPrefix("• ") {
            blockType = "bullet"
        } else if lineText.range(of: "^\\d+\\.\\s", options: .regularExpression) != nil {
            blockType = "numbered"
        } else if lineText.hasPrefix("☐ ") || lineText.hasPrefix("☑ ") {
            blockType = "checklist"
        } else if lineText.hasPrefix("\"") && lineText.hasSuffix("\"") {
            blockType = "quote"
        }

        if let font = attributedText.attribute(.font, at: checkRange.location, effectiveRange: nil) as? UIFont,
           font.pointSize > 20 {
            blockType = "heading"
        }

        if let paragraphStyle = attributedText.attribute(.paragraphStyle, at: checkRange.location, effectiveRange: nil) as? NSParagraphStyle {
            switch paragraphStyle.alignment {
            case .center:
                alignment = "center"
            case .right:
                alignment = "right"
            default:
                alignment = "left"
            }
        }

        onActiveStylesChange?([
            "bold": hasBold,
            "italic": hasItalic,
            "underline": hasUnderline,
            "strikethrough": hasStrikethrough,
            "code": hasCode,
            "highlight": hasHighlight,
            "blockType": blockType,
            "alignment": alignment
        ])
    }

    func textView(_ textView: UITextView, shouldChangeTextIn range: NSRange, replacementText text: String) -> Bool {
        guard text == "\n" else { return true }

        let currentText = textView.text ?? ""
        let nsText = currentText as NSString

        var lineStart = range.location
        while lineStart > 0 && nsText.character(at: lineStart - 1) != 10 {
            lineStart -= 1
        }

        let lineLength = range.location - lineStart
        let currentLine = nsText.substring(with: NSRange(location: lineStart, length: lineLength))

        if currentLine.hasPrefix("• ") {
            let lineContent = String(currentLine.dropFirst(2))
            if lineContent.trimmingCharacters(in: .whitespaces).isEmpty {
                let deleteRange = NSRange(location: lineStart, length: lineLength)
                textView.selectedRange = deleteRange
                textView.insertText("")
                return false
            }
            let plainAttributes: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: 16),
                .foregroundColor: UIColor.label
            ]
            textView.typingAttributes = plainAttributes
            textView.insertText("\n• ")
            return false
        }

        let numberedPattern = "^(\\d+)\\.\\s"
        if let regex = try? NSRegularExpression(pattern: numberedPattern),
           let match = regex.firstMatch(in: currentLine, range: NSRange(location: 0, length: currentLine.count)),
           let numberRange = Range(match.range(at: 1), in: currentLine) {

            let currentNumber = Int(currentLine[numberRange]) ?? 1
            let lineContent = String(currentLine.dropFirst(match.range.length))

            if lineContent.trimmingCharacters(in: .whitespaces).isEmpty {
                let deleteRange = NSRange(location: lineStart, length: lineLength)
                textView.selectedRange = deleteRange
                textView.insertText("")
                return false
            }

            let nextNumber = currentNumber + 1
            let plainAttributes: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: 16),
                .foregroundColor: UIColor.label
            ]
            textView.typingAttributes = plainAttributes
            textView.insertText("\n\(nextNumber). ")
            return false
        }

        return true
    }

    func updateToolbarButtonStates() {
        let range = textView.selectedRange

        let text = textView.text ?? ""
        let nsText = text as NSString
        var lineStart = range.location
        while lineStart > 0 && nsText.character(at: lineStart - 1) != 10 {
            lineStart -= 1
        }

        var lineEnd = range.location
        while lineEnd < text.count && nsText.character(at: lineEnd) != 10 {
            lineEnd += 1
        }

        let lineContent = lineStart < lineEnd ? nsText.substring(with: NSRange(location: lineStart, length: lineEnd - lineStart)) : ""

        let hasBullet = lineContent.hasPrefix("• ")
        let hasNumbered = lineContent.range(of: "^\\d+\\.\\s", options: .regularExpression) != nil
        let hasQuote = lineContent.hasPrefix("\"") && lineContent.hasSuffix("\"")
        let hasChecklist = lineContent.hasPrefix("☐ ") || lineContent.hasPrefix("☑ ")

        var currentAlignment: NSTextAlignment = .left
        if let attrText = textView.attributedText, attrText.length > 0 {
            let checkIndex = min(max(0, range.location - 1), attrText.length - 1)
            if let paragraphStyle = attrText.attribute(.paragraphStyle, at: checkIndex, effectiveRange: nil) as? NSParagraphStyle {
                currentAlignment = paragraphStyle.alignment
            }
        }

        guard range.length > 0 else {
            floatingToolbar.updateButtonStates(
                bold: false, italic: false, underline: false, strikethrough: false,
                code: false, highlight: false, heading: false,
                bullet: hasBullet, numbered: hasNumbered, quote: hasQuote, checklist: hasChecklist,
                alignLeft: currentAlignment == .left, alignCenter: currentAlignment == .center, alignRight: currentAlignment == .right
            )
            return
        }

        let attributedText = textView.attributedText ?? NSAttributedString()
        var hasBold = false
        var hasItalic = false
        var hasUnderline = false
        var hasStrikethrough = false
        var hasCode = false
        var hasHighlight = false
        var hasHeading = false

        // Ensure range is valid for the attributed text
        let safeRange = NSRange(
            location: min(range.location, attributedText.length),
            length: min(range.length, max(0, attributedText.length - range.location))
        )
        guard safeRange.length > 0 else {
            floatingToolbar.updateButtonStates(
                bold: false, italic: false, underline: false, strikethrough: false,
                code: false, highlight: false, heading: false,
                bullet: hasBullet, numbered: hasNumbered, quote: hasQuote, checklist: hasChecklist,
                alignLeft: currentAlignment == .left, alignCenter: currentAlignment == .center, alignRight: currentAlignment == .right
            )
            return
        }

        attributedText.enumerateAttributes(in: safeRange, options: []) { attrs, _, _ in
            if let font = attrs[.font] as? UIFont {
                let traits = font.fontDescriptor.symbolicTraits
                if traits.contains(.traitBold) { hasBold = true }
                if traits.contains(.traitItalic) { hasItalic = true }
                if traits.contains(.traitMonoSpace) { hasCode = true }
                if font.pointSize > 18 { hasHeading = true }
            }
            if attrs[.underlineStyle] != nil { hasUnderline = true }
            if attrs[.strikethroughStyle] != nil { hasStrikethrough = true }
            if let bgColor = attrs[.backgroundColor] as? UIColor, bgColor != UIColor.systemGray5 {
                hasHighlight = true
            }
        }

        floatingToolbar.updateButtonStates(
            bold: hasBold, italic: hasItalic, underline: hasUnderline, strikethrough: hasStrikethrough,
            code: hasCode, highlight: hasHighlight, heading: hasHeading,
            bullet: hasBullet, numbered: hasNumbered, quote: hasQuote, checklist: hasChecklist,
            alignLeft: currentAlignment == .left, alignCenter: currentAlignment == .center, alignRight: currentAlignment == .right
        )
    }

    @objc private func textDidChange() {
        placeholderLabel.isHidden = !textView.text.isEmpty

        if !isInternalChange {
            autoContinueListOnEnter()
            saveToUndoStack()
        }

        applyListIndentation()
        updateContentSize()
        sendContentChange()
    }

    private func autoContinueListOnEnter() {
        guard let text = textView.text, !text.isEmpty else { return }
        let cursorPos = textView.selectedRange.location
        guard cursorPos > 0, cursorPos <= text.count else { return }

        let nsText = text as NSString
        guard nsText.character(at: cursorPos - 1) == 10 else { return } // '\n'

        var prevLineStart = cursorPos - 2
        while prevLineStart > 0 && nsText.character(at: prevLineStart - 1) != 10 {
            prevLineStart -= 1
        }
        if prevLineStart < 0 { prevLineStart = 0 }

        let prevLine = nsText.substring(with: NSRange(location: prevLineStart, length: cursorPos - 1 - prevLineStart))

        let numberedRegex = try? NSRegularExpression(pattern: "^(\\d+)\\.\\s")
        if let match = numberedRegex?.firstMatch(in: prevLine, range: NSRange(location: 0, length: prevLine.count)) {
            let prefixStr = (prevLine as NSString).substring(with: match.range)
            let content = String(prevLine.dropFirst(prefixStr.count))

            if content.trimmingCharacters(in: .whitespaces).isEmpty {
                isInternalChange = true
                let mutable = NSMutableAttributedString(attributedString: textView.attributedText)
                mutable.deleteCharacters(in: NSRange(location: prevLineStart, length: cursorPos - prevLineStart))
                textView.attributedText = mutable
                textView.selectedRange = NSRange(location: prevLineStart, length: 0)
                isInternalChange = false
                return
            }

            let numStr = (prevLine as NSString).substring(with: match.range(at: 1))
            let nextNum = (Int(numStr) ?? 0) + 1
            let prefix = "\(nextNum). "

            isInternalChange = true
            let mutable = NSMutableAttributedString(attributedString: textView.attributedText)
            let attrs = mutable.attributes(at: max(0, cursorPos - 2), effectiveRange: nil)
            mutable.insert(NSAttributedString(string: prefix, attributes: attrs), at: cursorPos)
            textView.attributedText = mutable
            textView.selectedRange = NSRange(location: cursorPos + prefix.count, length: 0)
            renumberNumberedLists()
            isInternalChange = false
            return
        }

        if prevLine.hasPrefix("• ") {
            let content = String(prevLine.dropFirst(2))
            if content.trimmingCharacters(in: .whitespaces).isEmpty {
                isInternalChange = true
                let mutable = NSMutableAttributedString(attributedString: textView.attributedText)
                mutable.deleteCharacters(in: NSRange(location: prevLineStart, length: cursorPos - prevLineStart))
                textView.attributedText = mutable
                textView.selectedRange = NSRange(location: prevLineStart, length: 0)
                isInternalChange = false
                return
            }
            isInternalChange = true
            let mutable = NSMutableAttributedString(attributedString: textView.attributedText)
            let attrs = mutable.attributes(at: max(0, cursorPos - 2), effectiveRange: nil)
            mutable.insert(NSAttributedString(string: "• ", attributes: attrs), at: cursorPos)
            textView.attributedText = mutable
            textView.selectedRange = NSRange(location: cursorPos + 2, length: 0)
            isInternalChange = false
            return
        }

        if prevLine.hasPrefix("☐ ") || prevLine.hasPrefix("☑ ") {
            let content = String(prevLine.dropFirst(2))
            if content.trimmingCharacters(in: .whitespaces).isEmpty {
                isInternalChange = true
                let mutable = NSMutableAttributedString(attributedString: textView.attributedText)
                mutable.deleteCharacters(in: NSRange(location: prevLineStart, length: cursorPos - prevLineStart))
                textView.attributedText = mutable
                textView.selectedRange = NSRange(location: prevLineStart, length: 0)
                isInternalChange = false
                return
            }
            isInternalChange = true
            let mutable = NSMutableAttributedString(attributedString: textView.attributedText)
            let attrs = mutable.attributes(at: max(0, cursorPos - 2), effectiveRange: nil)
            mutable.insert(NSAttributedString(string: "☐ ", attributes: attrs), at: cursorPos)
            textView.attributedText = mutable
            textView.selectedRange = NSRange(location: cursorPos + 2, length: 0)
            isInternalChange = false
            return
        }
    }

    private func renumberNumberedLists() {
        guard let text = textView.text, !text.isEmpty else { return }
        let lines = text.components(separatedBy: "\n")
        let numberedRegex = try? NSRegularExpression(pattern: "^(\\d+)\\.\\s")

        var counter = 0
        var replacements: [(range: NSRange, newPrefix: String)] = []
        var offset = 0

        for line in lines {
            if let match = numberedRegex?.firstMatch(in: line, range: NSRange(location: 0, length: line.count)) {
                counter += 1
                let oldPrefix = (line as NSString).substring(with: match.range)
                let newPrefix = "\(counter). "
                if oldPrefix != newPrefix {
                    replacements.append((NSRange(location: offset, length: oldPrefix.count), newPrefix))
                }
            } else {
                counter = 0
            }
            offset += line.count + 1
        }

        if !replacements.isEmpty {
            let cursorPos = textView.selectedRange
            let mutableAttrString = NSMutableAttributedString(attributedString: textView.attributedText)
            for replacement in replacements.reversed() {
                let attrs = mutableAttrString.attributes(at: replacement.range.location, effectiveRange: nil)
                mutableAttrString.replaceCharacters(in: replacement.range, with: NSAttributedString(string: replacement.newPrefix, attributes: attrs))
            }
            textView.attributedText = mutableAttrString
            textView.selectedRange = cursorPos
        }
    }

    private func applyNumberOfLines() {
        if numberOfLines > 0 && !editable {
            textView.textContainer.maximumNumberOfLines = numberOfLines
            textView.textContainer.lineBreakMode = .byTruncatingTail
            textView.isScrollEnabled = false
        } else {
            textView.textContainer.maximumNumberOfLines = 0
            textView.textContainer.lineBreakMode = .byWordWrapping
        }
        updateContentSize()
    }

    private func updateContentSize() {
        let width = bounds.width > 0 ? bounds.width : UIScreen.main.bounds.width - 32
        let fittingSize = textView.sizeThatFits(CGSize(width: width, height: CGFloat.greatestFiniteMagnitude))

        let minHeight: CGFloat = 44
        var newHeight = max(fittingSize.height, minHeight)

        if maxHeight > 0 {
            textView.isScrollEnabled = newHeight > maxHeight
            newHeight = min(newHeight, maxHeight)
        } else {
            textView.isScrollEnabled = false
        }

        calculatedHeight = newHeight

        if abs(newHeight - lastReportedHeight) > 0.5 {
            lastReportedHeight = newHeight
            onSizeChange?(["height": newHeight])
        }

        invalidateIntrinsicContentSize()
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        updateContentSize()
    }

    override var intrinsicContentSize: CGSize {
        return CGSize(width: UIView.noIntrinsicMetric, height: calculatedHeight)
    }

    override func sizeThatFits(_ size: CGSize) -> CGSize {
        let width = size.width > 0 ? size.width : bounds.width > 0 ? bounds.width : UIScreen.main.bounds.width - 32
        let fittingSize = textView.sizeThatFits(CGSize(width: width, height: CGFloat.greatestFiniteMagnitude))
        var height = max(fittingSize.height, 44)

        if maxHeight > 0 {
            height = min(height, maxHeight)
        }

        return CGSize(width: size.width, height: height)
    }

    private func applyVariantStyle() {
        if variant == "flat" {
            layer.borderWidth = 0
            layer.cornerRadius = 0
            bottomBorder.isHidden = false
        } else {
            layer.borderColor = UIColor.separator.cgColor
            layer.borderWidth = 1
            layer.cornerRadius = 8
            bottomBorder.isHidden = true
        }
    }

    private func applyListIndentation() {
        guard let text = textView.text, !text.isEmpty else { return }

        let mutableAttrString = NSMutableAttributedString(attributedString: textView.attributedText)
        let nsText = text as NSString

        let font = UIFont.systemFont(ofSize: 16)
        let bulletPrefix = "• "
        let bulletWidth = (bulletPrefix as NSString).size(withAttributes: [.font: font]).width

        let numberedPattern = "^(\\d+)\\.\\s"
        let regex = try? NSRegularExpression(pattern: numberedPattern, options: [])

        var lineStart = 0
        while lineStart < text.count {
            var lineEnd = lineStart
            while lineEnd < text.count && nsText.character(at: lineEnd) != 10 {
                lineEnd += 1
            }

            let lineRange = NSRange(location: lineStart, length: lineEnd - lineStart)
            let lineText = nsText.substring(with: lineRange)

            let paragraphStyle = NSMutableParagraphStyle()
            paragraphStyle.alignment = .left
            paragraphStyle.lineHeightMultiple = RichTextEditorView.defaultLineHeightMultiple

            if lineText.hasPrefix("• ") {
                paragraphStyle.firstLineHeadIndent = 0
                paragraphStyle.headIndent = bulletWidth
            } else if let match = regex?.firstMatch(in: lineText, range: NSRange(location: 0, length: lineText.count)) {
                let matchedPrefix = (lineText as NSString).substring(with: match.range)
                let prefixWidth = (matchedPrefix as NSString).size(withAttributes: [.font: font]).width
                paragraphStyle.firstLineHeadIndent = 0
                paragraphStyle.headIndent = prefixWidth
            } else {
                paragraphStyle.firstLineHeadIndent = 0
                paragraphStyle.headIndent = 0
            }

            mutableAttrString.addAttribute(.paragraphStyle, value: paragraphStyle, range: lineRange)

            lineStart = lineEnd + 1
        }

        let selectedRange = textView.selectedRange

        isInternalChange = true
        textView.attributedText = mutableAttrString
        textView.selectedRange = selectedRange
        isInternalChange = false
    }

    private func saveToUndoStack() {
        undoStack.append(textView.attributedText)
        if undoStack.count > 50 {
            undoStack.removeFirst()
        }
        redoStack.removeAll()
    }

    private func sendContentChange() {
        let blocks = getBlocksArray()
        var blocksJson = "[]"
        if let jsonData = try? JSONSerialization.data(withJSONObject: blocks, options: []),
           let jsonString = String(data: jsonData, encoding: .utf8) {
            blocksJson = jsonString
        }
        onContentChange?([
            "text": textView.text ?? "",
            "blocksJson": blocksJson
        ])
    }

    func toggleBold() {
        toggleStyle(key: .font, trait: .traitBold)
    }

    func toggleItalic() {
        toggleStyle(key: .font, trait: .traitItalic)
    }

    func toggleUnderline() {
        toggleAttribute(key: .underlineStyle, value: NSUnderlineStyle.single.rawValue)
    }

    func toggleStrikethrough() {
        toggleAttribute(key: .strikethroughStyle, value: NSUnderlineStyle.single.rawValue)
    }

    func toggleCode() {
        let range = textView.selectedRange
        guard range.length > 0 else { return }

        let mutableAttrString = NSMutableAttributedString(attributedString: textView.attributedText)
        var hasMonospace = false

        mutableAttrString.enumerateAttribute(.font, in: range, options: []) { value, _, _ in
            if let font = value as? UIFont {
                hasMonospace = font.fontDescriptor.symbolicTraits.contains(.traitMonoSpace)
            }
        }

        let monoFont = UIFont.monospacedSystemFont(ofSize: 15, weight: .regular)
        let regularFont = UIFont.systemFont(ofSize: 16)

        mutableAttrString.enumerateAttribute(.font, in: range, options: []) { value, attrRange, _ in
            let newFont = hasMonospace ? regularFont : monoFont
            mutableAttrString.addAttribute(.font, value: newFont, range: attrRange)
        }

        if !hasMonospace {
            let codeBackground = UIColor(red: 0.95, green: 0.93, blue: 0.93, alpha: 1.0)
            mutableAttrString.addAttribute(.backgroundColor, value: codeBackground, range: range)
            mutableAttrString.addAttribute(.foregroundColor, value: UIColor(red: 0.8, green: 0.2, blue: 0.2, alpha: 1.0), range: range)
        } else {
            mutableAttrString.removeAttribute(.backgroundColor, range: range)
            mutableAttrString.addAttribute(.foregroundColor, value: UIColor.label, range: range)
        }

        isInternalChange = true
        textView.attributedText = mutableAttrString
        textView.selectedRange = range
        isInternalChange = false
        saveToUndoStack()
        sendContentChange()
    }

    func toggleHighlight(color: String?) {
        let range = textView.selectedRange
        guard range.length > 0 else { return }

        let mutableAttrString = NSMutableAttributedString(attributedString: textView.attributedText)
        var hasHighlight = false

        mutableAttrString.enumerateAttribute(.backgroundColor, in: range, options: []) { value, _, _ in
            if let bgColor = value as? UIColor, bgColor != UIColor.systemGray5 {
                hasHighlight = true
            }
        }

        if hasHighlight {
            mutableAttrString.removeAttribute(.backgroundColor, range: range)
        } else {
            let highlightColor = UIColor.yellow.withAlphaComponent(0.5)
            mutableAttrString.addAttribute(.backgroundColor, value: highlightColor, range: range)
        }

        isInternalChange = true
        textView.attributedText = mutableAttrString
        textView.selectedRange = range
        isInternalChange = false
        saveToUndoStack()
        sendContentChange()
    }

    func setHeading() {
        let range = textView.selectedRange
        let text = textView.text ?? ""
        let nsText = text as NSString

        var lineStart = range.location
        while lineStart > 0 && nsText.character(at: lineStart - 1) != 10 {
            lineStart -= 1
        }

        var lineEnd = range.location
        while lineEnd < text.count && nsText.character(at: lineEnd) != 10 {
            lineEnd += 1
        }

        let lineRange = NSRange(location: lineStart, length: lineEnd - lineStart)

        let mutableAttrString = NSMutableAttributedString(attributedString: textView.attributedText)

        var isHeading = false
        if lineRange.length > 0 {
            mutableAttrString.enumerateAttribute(.font, in: lineRange, options: []) { value, _, _ in
                if let font = value as? UIFont, font.pointSize > 18 {
                    isHeading = true
                }
            }
        }

        let headingFont = UIFont.boldSystemFont(ofSize: 24)
        let regularFont = UIFont.systemFont(ofSize: 16)

        mutableAttrString.addAttribute(.font, value: isHeading ? regularFont : headingFont, range: lineRange)

        isInternalChange = true
        textView.attributedText = mutableAttrString
        textView.selectedRange = range
        isInternalChange = false
        saveToUndoStack()
        sendContentChange()
    }

    func setQuote() {
        let range = textView.selectedRange
        let text = textView.text ?? ""
        let nsText = text as NSString

        var lineStart = range.location
        while lineStart > 0 && nsText.character(at: lineStart - 1) != 10 {
            lineStart -= 1
        }

        var lineEnd = range.location + range.length
        while lineEnd < text.count && nsText.character(at: lineEnd) != 10 {
            lineEnd += 1
        }

        let lineRange = NSRange(location: lineStart, length: lineEnd - lineStart)
        let lineText = nsText.substring(with: lineRange)

        var leadingWhitespace = ""
        var contentStart = 0
        for char in lineText {
            if char == " " || char == "\t" {
                leadingWhitespace.append(char)
                contentStart += 1
            } else {
                break
            }
        }

        let contentText = String(lineText.dropFirst(contentStart))
        var newText: String

        // Check if content (without indentation) is already quoted
        if contentText.hasPrefix("\"") && contentText.hasSuffix("\"") && contentText.count >= 2 {
            // Remove quotes, keep indentation
            let unquoted = String(contentText.dropFirst().dropLast())
            newText = leadingWhitespace + unquoted
        } else {
            // Add quotes around content, keep indentation
            newText = leadingWhitespace + "\"" + contentText + "\""
        }

        let mutableText = NSMutableString(string: text)
        mutableText.replaceCharacters(in: lineRange, with: newText)

        isInternalChange = true
        textView.text = mutableText as String
        textView.selectedRange = NSRange(location: lineStart + newText.count, length: 0)
        isInternalChange = false

        saveToUndoStack()
        sendContentChange()
    }

    func setChecklist() {
        let range = textView.selectedRange
        let text = textView.text ?? ""
        let nsText = text as NSString

        var lineStart = range.location
        while lineStart > 0 && nsText.character(at: lineStart - 1) != 10 {
            lineStart -= 1
        }

        var lineEnd = range.location + range.length
        while lineEnd < text.count && nsText.character(at: lineEnd) != 10 {
            lineEnd += 1
        }

        let lineRange = NSRange(location: lineStart, length: lineEnd - lineStart)
        let lineText = nsText.substring(with: lineRange)

        let uncheckedPrefix = "☐ "
        let checkedPrefix = "☑ "
        var newText: String

        if lineText.hasPrefix(uncheckedPrefix) || lineText.hasPrefix(checkedPrefix) {
            // Remove checklist
            newText = String(lineText.dropFirst(2))
        } else {
            // Add unchecked checkbox
            newText = uncheckedPrefix + lineText
        }

        let mutableText = NSMutableString(string: text)
        mutableText.replaceCharacters(in: lineRange, with: newText)

        isInternalChange = true
        textView.text = mutableText as String
        textView.selectedRange = NSRange(location: lineStart + newText.count, length: 0)
        isInternalChange = false

        saveToUndoStack()
        sendContentChange()
    }

    func toggleChecklistItem() {
        let range = textView.selectedRange
        let text = textView.text ?? ""
        let nsText = text as NSString

        var lineStart = range.location
        while lineStart > 0 && nsText.character(at: lineStart - 1) != 10 {
            lineStart -= 1
        }

        var lineEnd = range.location
        while lineEnd < text.count && nsText.character(at: lineEnd) != 10 {
            lineEnd += 1
        }

        let lineRange = NSRange(location: lineStart, length: lineEnd - lineStart)
        let lineText = nsText.substring(with: lineRange)

        let uncheckedPrefix = "☐ "
        let checkedPrefix = "☑ "
        var newText: String

        if lineText.hasPrefix(uncheckedPrefix) {
            // Check the item
            newText = checkedPrefix + String(lineText.dropFirst(2))
        } else if lineText.hasPrefix(checkedPrefix) {
            // Uncheck the item
            newText = uncheckedPrefix + String(lineText.dropFirst(2))
        } else {
            return
        }

        let mutableText = NSMutableString(string: text)
        mutableText.replaceCharacters(in: lineRange, with: newText)

        isInternalChange = true
        textView.text = mutableText as String
        textView.selectedRange = range
        isInternalChange = false

        saveToUndoStack()
        sendContentChange()
    }

    func setParagraph() {
        // Reset to normal paragraph style
        let range = textView.selectedRange
        guard range.length > 0 else { return }

        let mutableAttrString = NSMutableAttributedString(attributedString: textView.attributedText)
        let plainAttributes: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 16),
            .foregroundColor: UIColor.label
        ]

        mutableAttrString.setAttributes(plainAttributes, range: range)

        isInternalChange = true
        textView.attributedText = mutableAttrString
        textView.selectedRange = range
        isInternalChange = false
        saveToUndoStack()
        sendContentChange()
    }

    func clearFormatting() {
        // Use saved selection if current selection is empty
        var range = textView.selectedRange
        if range.length == 0 && savedSelectionRange.length > 0 {
            range = savedSelectionRange
        }
        guard range.length > 0 else { return }

        let mutableAttrString = NSMutableAttributedString(attributedString: textView.attributedText)
        let plainText = (textView.text as NSString?)?.substring(with: range) ?? ""

        let plainAttributes: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 16),
            .foregroundColor: UIColor.label
        ]

        mutableAttrString.replaceCharacters(in: range, with: NSAttributedString(string: plainText, attributes: plainAttributes))

        isInternalChange = true
        textView.attributedText = mutableAttrString
        textView.selectedRange = range
        isInternalChange = false
        saveToUndoStack()
        sendContentChange()
    }

    func indent() {
        let range = textView.selectedRange
        let text = textView.text ?? ""
        let nsText = text as NSString

        var lineStart = range.location
        while lineStart > 0 && nsText.character(at: lineStart - 1) != 10 {
            lineStart -= 1
        }

        let indentString = "    " // 4 spaces
        let mutableText = NSMutableString(string: text)
        mutableText.insert(indentString, at: lineStart)

        isInternalChange = true
        textView.text = mutableText as String
        textView.selectedRange = NSRange(location: range.location + indentString.count, length: range.length)
        isInternalChange = false

        saveToUndoStack()
        sendContentChange()
    }

    func outdent() {
        let range = textView.selectedRange
        let text = textView.text ?? ""
        let nsText = text as NSString

        var lineStart = range.location
        while lineStart > 0 && nsText.character(at: lineStart - 1) != 10 {
            lineStart -= 1
        }

        var lineEnd = range.location
        while lineEnd < text.count && nsText.character(at: lineEnd) != 10 {
            lineEnd += 1
        }

        let lineText = nsText.substring(with: NSRange(location: lineStart, length: lineEnd - lineStart))
        var charsToRemove = 0

        if lineText.hasPrefix("    ") {
            charsToRemove = 4
        } else if lineText.hasPrefix("\t") {
            charsToRemove = 1
        } else {
            // Remove up to 4 leading spaces
            for char in lineText {
                if char == " " && charsToRemove < 4 {
                    charsToRemove += 1
                } else {
                    break
                }
            }
        }

        guard charsToRemove > 0 else { return }

        let mutableText = NSMutableString(string: text)
        mutableText.deleteCharacters(in: NSRange(location: lineStart, length: charsToRemove))

        isInternalChange = true
        textView.text = mutableText as String
        let newLocation = max(lineStart, range.location - charsToRemove)
        textView.selectedRange = NSRange(location: newLocation, length: range.length)
        isInternalChange = false

        saveToUndoStack()
        sendContentChange()
    }

    func setAlignment(_ alignment: NSTextAlignment) {
        let range = textView.selectedRange
        let text = textView.text ?? ""
        let nsText = text as NSString

        var lineStart = range.location
        while lineStart > 0 && nsText.character(at: lineStart - 1) != 10 {
            lineStart -= 1
        }

        var lineEnd = range.location + range.length
        while lineEnd < text.count && nsText.character(at: lineEnd) != 10 {
            lineEnd += 1
        }

        let lineRange = NSRange(location: lineStart, length: lineEnd - lineStart)

        let mutableAttrString = NSMutableAttributedString(attributedString: textView.attributedText)
        let paragraphStyle = NSMutableParagraphStyle()
        paragraphStyle.alignment = alignment
        paragraphStyle.lineHeightMultiple = RichTextEditorView.defaultLineHeightMultiple

        mutableAttrString.addAttribute(.paragraphStyle, value: paragraphStyle, range: lineRange)

        isInternalChange = true
        textView.attributedText = mutableAttrString
        textView.selectedRange = range
        isInternalChange = false
        saveToUndoStack()
        sendContentChange()
    }

    func promptInsertLink() {
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let viewController = windowScene.windows.first?.rootViewController else {
            return
        }

        let alert = UIAlertController(title: "Insert Link", message: nil, preferredStyle: .alert)
        alert.addTextField { textField in
            textField.placeholder = "Link text"
            if let selectedText = self.textView.text(in: self.textView.selectedTextRange ?? UITextRange()) {
                textField.text = selectedText
            }
        }
        alert.addTextField { textField in
            textField.placeholder = "URL"
            textField.keyboardType = .URL
        }

        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        alert.addAction(UIAlertAction(title: "Insert", style: .default) { [weak self] _ in
            guard let text = alert.textFields?[0].text,
                  let url = alert.textFields?[1].text,
                  !text.isEmpty, !url.isEmpty else { return }
            self?.insertLink(url: url, text: text)
        })

        viewController.present(alert, animated: true)
    }

    private func toggleStyle(key: NSAttributedString.Key, trait: UIFontDescriptor.SymbolicTraits) {
        let range = textView.selectedRange
        guard range.length > 0 else { return }

        let mutableAttrString = NSMutableAttributedString(attributedString: textView.attributedText)
        var hasTrait = false

        mutableAttrString.enumerateAttribute(.font, in: range, options: []) { value, _, _ in
            if let font = value as? UIFont {
                hasTrait = font.fontDescriptor.symbolicTraits.contains(trait)
            }
        }

        mutableAttrString.enumerateAttribute(.font, in: range, options: []) { value, attrRange, _ in
            if let font = value as? UIFont {
                var newTraits = font.fontDescriptor.symbolicTraits
                if hasTrait {
                    newTraits.remove(trait)
                } else {
                    newTraits.insert(trait)
                }
                if let descriptor = font.fontDescriptor.withSymbolicTraits(newTraits) {
                    let newFont = UIFont(descriptor: descriptor, size: font.pointSize)
                    mutableAttrString.addAttribute(.font, value: newFont, range: attrRange)
                }
            }
        }

        isInternalChange = true
        textView.attributedText = mutableAttrString
        textView.selectedRange = range
        isInternalChange = false
        saveToUndoStack()
        sendContentChange()
    }

    private func toggleAttribute(key: NSAttributedString.Key, value: Int) {
        let range = textView.selectedRange
        guard range.length > 0 else { return }

        let mutableAttrString = NSMutableAttributedString(attributedString: textView.attributedText)
        var hasAttribute = false

        mutableAttrString.enumerateAttribute(key, in: range, options: []) { attrValue, _, _ in
            if attrValue != nil {
                hasAttribute = true
            }
        }

        if hasAttribute {
            mutableAttrString.removeAttribute(key, range: range)
        } else {
            mutableAttrString.addAttribute(key, value: value, range: range)
        }

        isInternalChange = true
        textView.attributedText = mutableAttrString
        textView.selectedRange = range
        isInternalChange = false
        saveToUndoStack()
        sendContentChange()
    }

    func toggleBulletList() {
        toggleListStyle(bullet: true)
    }

    func toggleNumberedList() {
        toggleListStyle(bullet: false)
    }

    private func toggleListStyle(bullet: Bool) {
        let range = textView.selectedRange
        let text = textView.text ?? ""
        let nsText = text as NSString

        var selectionStart = range.location
        var selectionEnd = range.location + range.length

        while selectionStart > 0 && nsText.character(at: selectionStart - 1) != 10 {
            selectionStart -= 1
        }
        while selectionEnd < text.count && nsText.character(at: selectionEnd) != 10 {
            selectionEnd += 1
        }

        let selectedText = nsText.substring(with: NSRange(location: selectionStart, length: selectionEnd - selectionStart))
        let lines = selectedText.components(separatedBy: "\n")

        let bulletPrefix = "• "
        let numberedPattern = "^\\d+\\.\\s"
        let regex = try? NSRegularExpression(pattern: numberedPattern)

        let allHavePrefix: Bool
        if bullet {
            allHavePrefix = lines.allSatisfy { $0.hasPrefix(bulletPrefix) || $0.trimmingCharacters(in: .whitespaces).isEmpty }
        } else {
            allHavePrefix = lines.allSatisfy { line in
                if line.trimmingCharacters(in: .whitespaces).isEmpty { return true }
                return regex?.firstMatch(in: line, range: NSRange(location: 0, length: line.count)) != nil
            }
        }

        var newLines: [String] = []
        for (index, line) in lines.enumerated() {
            if line.trimmingCharacters(in: .whitespaces).isEmpty {
                newLines.append(line)
                continue
            }

            if allHavePrefix {
                if bullet && line.hasPrefix(bulletPrefix) {
                    newLines.append(String(line.dropFirst(bulletPrefix.count)))
                } else if !bullet, let match = regex?.firstMatch(in: line, range: NSRange(location: 0, length: line.count)) {
                    newLines.append(String(line.dropFirst(match.range.length)))
                } else {
                    newLines.append(line)
                }
            } else {
                var cleanLine = line
                if line.hasPrefix(bulletPrefix) {
                    cleanLine = String(line.dropFirst(bulletPrefix.count))
                } else if let match = regex?.firstMatch(in: line, range: NSRange(location: 0, length: line.count)) {
                    cleanLine = String(line.dropFirst(match.range.length))
                }

                if bullet {
                    newLines.append(bulletPrefix + cleanLine)
                } else {
                    newLines.append("\(index + 1). " + cleanLine)
                }
            }
        }

        let newText = newLines.joined(separator: "\n")
        let mutableText = NSMutableString(string: text)
        mutableText.replaceCharacters(in: NSRange(location: selectionStart, length: selectionEnd - selectionStart), with: newText)

        isInternalChange = true
        textView.text = mutableText as String
        textView.selectedRange = NSRange(location: selectionStart, length: newText.count)
        isInternalChange = false

        applyListIndentation()
        saveToUndoStack()
        sendContentChange()
    }

    func setContent(blocks: [[String: Any]]) {
        let attributedString = NSMutableAttributedString()
        let font = UIFont.systemFont(ofSize: 16)

        var numberedIndex = 1
        for (blockIndex, block) in blocks.enumerated() {
            guard let text = block["text"] as? String else { continue }
            let blockType = block["type"] as? String ?? "paragraph"

            var displayText = text
            var prefixLength = 0
            let paragraphStyle = NSMutableParagraphStyle()
            paragraphStyle.alignment = .left
            paragraphStyle.lineHeightMultiple = RichTextEditorView.defaultLineHeightMultiple

            switch blockType {
            case "bullet":
                let bulletPrefix = "• "
                displayText = bulletPrefix + text
                prefixLength = 2
                let bulletWidth = (bulletPrefix as NSString).size(withAttributes: [.font: font]).width
                paragraphStyle.firstLineHeadIndent = 0
                paragraphStyle.headIndent = bulletWidth
            case "numbered":
                let prefix = "\(numberedIndex). "
                displayText = prefix + text
                prefixLength = prefix.count
                let prefixWidth = (prefix as NSString).size(withAttributes: [.font: font]).width
                paragraphStyle.firstLineHeadIndent = 0
                paragraphStyle.headIndent = prefixWidth
                numberedIndex += 1
            default:
                paragraphStyle.firstLineHeadIndent = 0
                paragraphStyle.headIndent = 0
                numberedIndex = 1
            }

            let blockAttrString = NSMutableAttributedString(string: displayText, attributes: [
                .font: font,
                .foregroundColor: UIColor.label,
                .paragraphStyle: paragraphStyle
            ])

            if let styles = block["styles"] as? [[String: Any]] {
                for style in styles {
                    guard let start = style["start"] as? Int,
                          let end = style["end"] as? Int,
                          let styleType = style["style"] as? String,
                          start < end && end <= text.count else { continue }

                    let range = NSRange(location: start + prefixLength, length: end - start)

                    switch styleType {
                    case "bold":
                        let boldFont = UIFont.boldSystemFont(ofSize: font.pointSize)
                        blockAttrString.addAttribute(.font, value: boldFont, range: range)
                    case "italic":
                        let italicFont = UIFont.italicSystemFont(ofSize: font.pointSize)
                        blockAttrString.addAttribute(.font, value: italicFont, range: range)
                    case "underline":
                        blockAttrString.addAttribute(.underlineStyle, value: NSUnderlineStyle.single.rawValue, range: range)
                    case "strikethrough":
                        blockAttrString.addAttribute(.strikethroughStyle, value: NSUnderlineStyle.single.rawValue, range: range)
                    default:
                        break
                    }
                }
            }

            if blockIndex < blocks.count - 1 {
                blockAttrString.append(NSAttributedString(string: "\n", attributes: [
                    .font: font,
                    .foregroundColor: UIColor.label,
                    .paragraphStyle: paragraphStyle
                ]))
            }
            attributedString.append(blockAttrString)
        }

        isInternalChange = true
        textView.attributedText = attributedString
        placeholderLabel.isHidden = !textView.text.isEmpty
        isInternalChange = false
        applyListIndentation()

        let endPosition = textView.text?.count ?? 0
        textView.selectedRange = NSRange(location: endPosition, length: 0)

        DispatchQueue.main.async { [weak self] in
            self?.textView.scrollRangeToVisible(NSRange(location: endPosition, length: 0))
            self?.updateContentSize()
        }
    }

    func getText() -> String {
        return textView.text ?? ""
    }

    func getBlocksArray() -> [[String: Any]] {
        let text = textView.text ?? ""
        let attributedText = textView.attributedText ?? NSAttributedString()

        let lines = text.components(separatedBy: "\n")
        var blocks: [[String: Any]] = []
        var currentIndex = 0
        let numberedPattern = "^(\\d+)\\.\\s"
        let regex = try? NSRegularExpression(pattern: numberedPattern, options: [])

        for line in lines {
            var blockType = "paragraph"
            var displayText = line

            if line.hasPrefix("• ") {
                blockType = "bullet"
                displayText = String(line.dropFirst(2))
            } else if let match = regex?.firstMatch(in: line, range: NSRange(location: 0, length: line.count)) {
                blockType = "numbered"
                displayText = String(line.dropFirst(match.range.length))
            }

            var styles: [[String: Any]] = []
            let lineRange = NSRange(location: currentIndex, length: line.count)

            if lineRange.location + lineRange.length <= attributedText.length {
                attributedText.enumerateAttributes(in: lineRange, options: []) { attrs, range, _ in
                    let relativeStart = range.location - currentIndex
                    let relativeEnd = relativeStart + range.length

                    if let font = attrs[.font] as? UIFont {
                        let traits = font.fontDescriptor.symbolicTraits
                        if traits.contains(.traitBold) {
                            styles.append(["style": "bold", "start": relativeStart, "end": relativeEnd])
                        }
                        if traits.contains(.traitItalic) {
                            styles.append(["style": "italic", "start": relativeStart, "end": relativeEnd])
                        }
                    }
                    if attrs[.underlineStyle] != nil {
                        styles.append(["style": "underline", "start": relativeStart, "end": relativeEnd])
                    }
                    if attrs[.strikethroughStyle] != nil {
                        styles.append(["style": "strikethrough", "start": relativeStart, "end": relativeEnd])
                    }
                }
            }

            blocks.append([
                "type": blockType,
                "text": displayText,
                "styles": styles
            ])

            currentIndex += line.count + 1
        }

        return blocks
    }

    func clear() {
        isInternalChange = true
        textView.text = ""
        textView.attributedText = NSAttributedString()
        placeholderLabel.isHidden = false
        isInternalChange = false
        sendContentChange()
    }

    func focus() {
        textView.becomeFirstResponder()
    }

    func blur() {
        textView.resignFirstResponder()
    }

    func insertLink(url: String, text: String) {
        let range = textView.selectedRange
        let mutableAttrString = NSMutableAttributedString(attributedString: textView.attributedText)

        let linkAttrString = NSAttributedString(string: text, attributes: [
            .link: url,
            .foregroundColor: UIColor.systemBlue,
            .underlineStyle: NSUnderlineStyle.single.rawValue
        ])

        if range.length > 0 {
            mutableAttrString.replaceCharacters(in: range, with: linkAttrString)
        } else {
            mutableAttrString.insert(linkAttrString, at: range.location)
        }

        isInternalChange = true
        textView.attributedText = mutableAttrString
        isInternalChange = false
        saveToUndoStack()
        sendContentChange()
    }

    func undo() {
        guard undoStack.count > 1 else { return }

        let current = undoStack.removeLast()
        redoStack.append(current)

        if let previous = undoStack.last {
            isInternalChange = true
            textView.attributedText = previous
            placeholderLabel.isHidden = !textView.text.isEmpty
            isInternalChange = false
            sendContentChange()
        }
    }

    func redo() {
        guard let next = redoStack.popLast() else { return }

        undoStack.append(next)
        isInternalChange = true
        textView.attributedText = next
        placeholderLabel.isHidden = !textView.text.isEmpty
        isInternalChange = false
        sendContentChange()
    }
}
