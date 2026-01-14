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
- Link insertion
- Undo/Redo
- Text alignment (left, center, right)
- Indent/Outdent
- Floating toolbar with customizable options
- Two variants: outlined and flat
- Auto-growing height

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

| Prop                | Type                                    | Default      | Description                     |
| ------------------- | --------------------------------------- | ------------ | ------------------------------- |
| `placeholder`       | `string`                                | `""`         | Placeholder text                |
| `initialContent`    | `Block[]`                               | `[]`         | Initial content blocks          |
| `readOnly`          | `boolean`                               | `false`      | Make editor read-only           |
| `maxHeight`         | `number`                                | `undefined`  | Maximum height before scrolling |
| `showToolbar`       | `boolean`                               | `true`       | Show/hide floating toolbar      |
| `toolbarOptions`    | `ToolbarOption[]`                       | All options  | Customize toolbar buttons       |
| `variant`           | `'outlined' \| 'flat'`                  | `'outlined'` | Editor style variant            |
| `onContentChange`   | `(event: ContentChangeEvent) => void`   | `undefined`  | Called when content changes     |
| `onSelectionChange` | `(event: SelectionChangeEvent) => void` | `undefined`  | Called when selection changes   |
| `onFocus`           | `() => void`                            | `undefined`  | Called when editor gains focus  |
| `onBlur`            | `() => void`                            | `undefined`  | Called when editor loses focus  |

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
}

type BlockType = 'paragraph' | 'bullet' | 'numbered' | 'heading' | 'quote' | 'checklist';
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

## Customizing Toolbar

```tsx
import RichTextEditor, { ToolbarOption } from '@chaitrabhairappa/react-native-rich-text-editor';

const toolbarOptions: ToolbarOption[] = ['bold', 'italic', 'underline', 'bullet', 'numbered'];

<RichTextEditor
  toolbarOptions={toolbarOptions}
  // ...
/>;
```

## License

MIT
