# react-native-richtext-editor

A powerful native rich text editor for React Native with support for text formatting, lists, and more. Works on both iOS and Android.

Unlike other rich text editor packages that rely on HTML and WebView, this library is built with **pure native components** — using native iOS and Android text editing APIs directly. This provides better performance, smoother animations, and a more seamless integration with your React Native app.

> **Note:** This library requires React Native's **New Architecture** to be enabled. It will not work with the old architecture.

## Demo

<p align="center">
  <img src="https://raw.githubusercontent.com/Chaitra9225/Assets/main/demo-1.jpeg" width="300" alt="Demo 1" />
  <img src="https://raw.githubusercontent.com/Chaitra9225/Assets/main/demo-2.jpeg" width="300" alt="Demo 2" />
</p>

## Features

- Bold, Italic, Underline, Strikethrough
- Code and Highlight formatting
- Bullet lists and Numbered lists
- Headings
- Quotes and Checklists
- Media attachments (images)
- Link insertion with clickable hyperlinks in readOnly mode
- Auto-detection of URLs (typed or pasted)
- Undo/Redo
- Text alignment (left, center, right)
- Indent/Outdent
- Floating toolbar with customizable options
- Three variants: outlined, flat, and plain
- Auto-growing height
- **Delta-based content updates** for optimized performance
- **Synchronous style detection** via `onActiveStylesChange`

## Why Delta-Based Updates?

Unlike other editors that send the **entire document** on every keystroke, this library includes **delta information** — only what changed.

```typescript
onContentChange={(event) => {
  // Full content (for saving)
  console.log(event.nativeEvent.text);
  console.log(event.nativeEvent.blocks);

  // Delta (for optimized processing)
  console.log(event.nativeEvent.delta);
  // { type: "insert", position: 50, text: "a" }
}}
```

| Delta Type | When               | Data                          |
| ---------- | ------------------ | ----------------------------- |
| `insert`   | User types         | `position`, `text`            |
| `delete`   | User deletes       | `position`, `length`          |
| `replace`  | Selection replaced | `position`, `length`, `text`  |
| `format`   | Style applied      | `position`, `length`, `style` |

**Benefits:**

- **Server sync** — Send only deltas instead of full document
- **Collaborative editing** — Apply remote changes efficiently
- **Analytics** — Track exactly what users type/delete
- **Performance** — Process small changes without parsing entire content

## Installation

```bash
npm install @chaitrabhairappa/react-native-rich-text-editor
# or
yarn add @chaitrabhairappa/react-native-rich-text-editor
```

### iOS

```bash
cd ios && bundle install && bundle exec pod install && cd ..
```

### Android

No additional setup required.

## Usage

```tsx
import React, { useRef } from 'react';
import { View } from 'react-native';
import RichTextEditor, {
  RichTextEditorRef,
  Block,
  ContentChangeEvent,
} from '@chaitrabhairappa/react-native-rich-text-editor';

const App = () => {
  const editorRef = useRef<RichTextEditorRef>(null);

  const handleContentChange = (event: ContentChangeEvent) => {
    console.log('Content changed:', event.nativeEvent.blocks);
  };

  const initialContent: Block[] = [
    {
      type: 'paragraph',
      text: 'Hello World',
      styles: [{ style: 'bold', start: 0, end: 5 }],
    },
    {
      type: 'bullet',
      text: 'First item',
      styles: [],
    },
    {
      type: 'bullet',
      text: 'Second item',
      styles: [{ style: 'italic', start: 0, end: 6 }],
    },
  ];

  return (
    <View style={{ flex: 1, padding: 16 }}>
      <RichTextEditor
        ref={editorRef}
        placeholder="Enter text..."
        initialContent={initialContent}
        onContentChange={handleContentChange}
        maxHeight={300}
        variant="outlined"
      />
    </View>
  );
};

export default App;
```

## Props

| Prop                | Type                                    | Default      | Description                                  |
| ------------------- | --------------------------------------- | ------------ | -------------------------------------------- |
| `placeholder`       | `string`                                | `""`         | Placeholder text                             |
| `initialContent`    | `Block[]`                               | `[]`         | Initial content blocks                       |
| `readOnly`          | `boolean`                               | `false`      | Make editor read-only                        |
| `selectable`        | `boolean`                               | `true`       | Enable/disable text selection                |
| `maxHeight`         | `number`                                | `undefined`  | Maximum height before scrolling              |
| `showToolbar`       | `boolean`                               | `true`       | Show/hide floating toolbar                   |
| `toolbarOptions`    | `ToolbarOption[]`                       | All options  | Customize toolbar buttons                    |
| `variant`           | `'outlined' \| 'flat' \| 'plain'`       | `'outlined'` | Editor style variant                         |
| `onContentChange`   | `(event: ContentChangeEvent) => void`   | `undefined`  | Called when content changes                  |
| `onSelectionChange` | `(event: SelectionChangeEvent) => void` | `undefined`  | Called when selection changes                |
| `onFocus`           | `() => void`                            | `undefined`  | Called when editor gains focus               |
| `onBlur`            | `() => void`                            | `undefined`  | Called when editor loses focus               |

## Ref Methods

```tsx
const editorRef = useRef<RichTextEditorRef>(null);

// Content management
editorRef.current?.setContent(blocks);
editorRef.current?.clear();
const text = await editorRef.current?.getText();
const blocks = await editorRef.current?.getBlocks();

// Focus management
editorRef.current?.focus();
editorRef.current?.blur();

// Text styles
editorRef.current?.toggleBold();
editorRef.current?.toggleItalic();
editorRef.current?.toggleUnderline();
editorRef.current?.toggleStrikethrough();
editorRef.current?.toggleCode();
editorRef.current?.toggleHighlight();

// Block types
editorRef.current?.setHeading();
editorRef.current?.setBulletList();
editorRef.current?.setNumberedList();
editorRef.current?.setQuote();
editorRef.current?.setChecklist();
editorRef.current?.setParagraph();

// Media
editorRef.current?.insertMediaAttachment({ kind: 'image', uri: 'https://example.com/image.png' });

// Actions
editorRef.current?.insertLink(url, text);
editorRef.current?.undo();
editorRef.current?.redo();
editorRef.current?.clearFormatting();

// Indentation
editorRef.current?.indent();
editorRef.current?.outdent();

// Alignment
editorRef.current?.setAlignment('left' | 'center' | 'right');
```

## Types

```typescript
interface Block {
  type: BlockType;
  text: string;
  styles: StyleRange[];
  alignment?: TextAlignment;
  checked?: boolean;
  indentLevel?: number;
  mediaAttachment?: MediaAttachment;
}

interface MediaAttachment {
  kind: 'image';
  uri: string;
  width?: number;
  height?: number;
  alt?: string;
}

type BlockType = 'paragraph' | 'bullet' | 'numbered' | 'heading' | 'quote' | 'checklist' | 'mediaAttachment';
type TextAlignment = 'left' | 'center' | 'right';

interface StyleRange {
  style: 'bold' | 'italic' | 'underline' | 'strikethrough' | 'link' | 'code' | 'highlight';
  start: number;
  end: number;
  url?: string;
  highlightColor?: string;
}

type ToolbarOption =
  | 'bold'
  | 'italic'
  | 'strikethrough'
  | 'underline'
  | 'code'
  | 'highlight'
  | 'heading'
  | 'bullet'
  | 'numbered'
  | 'quote'
  | 'checklist'
  | 'mediaAttachment'
  | 'link'
  | 'undo'
  | 'redo'
  | 'clearFormatting'
  | 'indent'
  | 'outdent'
  | 'alignLeft'
  | 'alignCenter'
  | 'alignRight';
```

## Media Attachments

You can insert images into the editor using the `insertMediaAttachment` ref method. The toolbar also includes a `mediaAttachment` button that triggers the native media attachment flow.

```tsx
import RichTextEditor, {
  RichTextEditorRef,
  MediaAttachment,
} from '@chaitrabhairappa/react-native-rich-text-editor';

const editorRef = useRef<RichTextEditorRef>(null);

// Insert an image programmatically
const insertImage = () => {
  editorRef.current?.insertMediaAttachment({
    kind: 'image',
    uri: 'https://example.com/photo.png',
  });
};
```

Media attachment blocks are included in the content output:

```typescript
{
  type: 'mediaAttachment',
  text: '',
  styles: [],
  mediaAttachment: {
    kind: 'image',
    uri: 'https://example.com/photo.png',
  }
}
```

## Customizing Toolbar

```tsx
import RichTextEditor, { ToolbarOption } from '@chaitrabhairappa/react-native-rich-text-editor';

const toolbarOptions: ToolbarOption[] = ['bold', 'italic', 'underline', 'bullet', 'numbered'];

<RichTextEditor
  toolbarOptions={toolbarOptions}
  // ...
/>;
```

## Changelog

### 3.5.0

- Clickable hyperlinks in readOnly mode — links are highlighted in blue with underline and open in the browser on tap (iOS & Android)
- Auto-detection of URLs — typed or pasted URLs are automatically converted to clickable links after a space or newline
- Fix Android link URL not being parsed from initialContent

### 3.3.0

- Add `selectable` prop to enable/disable text selection (iOS & Android)

### 3.0.0

- Add media attachment support — insert images into the editor (iOS & Android)
- New `insertMediaAttachment` ref method for programmatic image insertion
- New `mediaAttachment` toolbar option
- New `MediaAttachment` type and `mediaAttachment` block type

### 2.1.2

- Fix Android `onContentChange` to correctly extract text styles (bold, italic, underline, strikethrough, code, highlight)
- Fix Android `onContentChange` to correctly detect block types (bullet, numbered, checklist, quote)
- Android: auto-scroll to cursor when content exceeds maxHeight
- Android: fix flat variant bottom border position during scroll

## License

MIT
