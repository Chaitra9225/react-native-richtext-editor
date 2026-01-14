import React, { forwardRef, useImperativeHandle, useRef, useCallback, useState } from 'react';
import { requireNativeComponent, UIManager, findNodeHandle, StyleSheet } from 'react-native';
import type {
  Block,
  TextAlignment,
  ContentChangeEvent,
  SelectionChangeEvent,
  RichTextEditorProps,
  RichTextEditorRef,
} from './types';

const COMPONENT_NAME = 'RichTextEditorView';

interface SizeChangeEvent {
  nativeEvent: {
    height: number;
  };
}

interface NativeComponentProps extends Omit<RichTextEditorProps, 'onFocus' | 'onBlur' | 'readOnly' | 'initialContent' | 'toolbarOptions'> {
  editable?: boolean;
  initialContent?: Block[];
  toolbarOptions?: string[];
  onEditorFocus?: () => void;
  onEditorBlur?: () => void;
  onSizeChange?: (event: SizeChangeEvent) => void;
}

const RichTextEditorViewNative = requireNativeComponent<NativeComponentProps>(COMPONENT_NAME);

const RichTextEditor = forwardRef<RichTextEditorRef, RichTextEditorProps>(
  (props, ref) => {
    const nativeRef = useRef(null);
    const [height, setHeight] = useState<number>(44);

    const handleSizeChange = useCallback((event: SizeChangeEvent) => {
      const newHeight = event.nativeEvent?.height;
      if (newHeight && newHeight > 0) {
        setHeight(newHeight);
      }
    }, []);

    const dispatchCommand = useCallback((command: string, args: unknown[] = []) => {
      const handle = findNodeHandle(nativeRef.current);
      if (handle) {
        const commands = UIManager.getViewManagerConfig(COMPONENT_NAME)?.Commands;
        const commandId = commands?.[command];
        if (commandId !== undefined) {
          UIManager.dispatchViewManagerCommand(handle, commandId, args);
        }
      }
    }, []);

    useImperativeHandle(ref, () => ({
      setContent: (blocks: Block[]) => {
        dispatchCommand('setContent', [blocks]);
      },
      getText: async (): Promise<string> => {
        return new Promise((resolve) => {
          resolve('');
        });
      },
      getBlocks: async (): Promise<Block[]> => {
        return new Promise((resolve) => {
          resolve([]);
        });
      },
      clear: () => {
        dispatchCommand('clear');
      },
      focus: () => {
        dispatchCommand('focus');
      },
      blur: () => {
        dispatchCommand('blur');
      },
      toggleBold: () => {
        dispatchCommand('toggleBold');
      },
      toggleItalic: () => {
        dispatchCommand('toggleItalic');
      },
      toggleUnderline: () => {
        dispatchCommand('toggleUnderline');
      },
      toggleStrikethrough: () => {
        dispatchCommand('toggleStrikethrough');
      },
      toggleCode: () => {
        dispatchCommand('toggleCode');
      },
      toggleHighlight: (color?: string) => {
        dispatchCommand('toggleHighlight', color ? [color] : []);
      },
      setHeading: () => {
        dispatchCommand('setHeading');
      },
      setBulletList: () => {
        dispatchCommand('setBulletList');
      },
      setNumberedList: () => {
        dispatchCommand('setNumberedList');
      },
      setQuote: () => {
        dispatchCommand('setQuote');
      },
      setChecklist: () => {
        dispatchCommand('setChecklist');
      },
      setParagraph: () => {
        dispatchCommand('setParagraph');
      },
      insertLink: (url: string, text: string) => {
        dispatchCommand('insertLink', [url, text]);
      },
      undo: () => {
        dispatchCommand('undo');
      },
      redo: () => {
        dispatchCommand('redo');
      },
      clearFormatting: () => {
        dispatchCommand('clearFormatting');
      },
      indent: () => {
        dispatchCommand('indent');
      },
      outdent: () => {
        dispatchCommand('outdent');
      },
      setAlignment: (alignment: TextAlignment) => {
        dispatchCommand('setAlignment', [alignment]);
      },
      toggleChecklistItem: () => {
        dispatchCommand('toggleChecklistItem');
      },
    }));

    const handleContentChange = useCallback(
      (event: ContentChangeEvent) => {
        props.onContentChange?.(event);
      },
      [props.onContentChange]
    );

    const handleSelectionChange = useCallback(
      (event: SelectionChangeEvent) => {
        props.onSelectionChange?.(event);
      },
      [props.onSelectionChange]
    );

    const handleFocus = useCallback(() => {
      props.onFocus?.();
    }, [props.onFocus]);

    const handleBlur = useCallback(() => {
      props.onBlur?.();
    }, [props.onBlur]);

    const combinedStyle = StyleSheet.flatten([props.style, { height }]);

    return (
      <RichTextEditorViewNative
        ref={nativeRef}
        style={combinedStyle}
        placeholder={props.placeholder}
        initialContent={props.initialContent}
        editable={props.readOnly !== undefined ? !props.readOnly : true}
        maxHeight={props.maxHeight}
        showToolbar={props.readOnly ? false : (props.showToolbar ?? true)}
        toolbarOptions={props.toolbarOptions}
        variant={props.variant ?? 'outlined'}
        onContentChange={handleContentChange}
        onSelectionChange={handleSelectionChange}
        onEditorFocus={handleFocus}
        onEditorBlur={handleBlur}
        onSizeChange={handleSizeChange}
      />
    );
  }
);

RichTextEditor.displayName = 'RichTextEditor';

export default RichTextEditor;
export { DEFAULT_TOOLBAR_OPTIONS } from './types';
export type {
  Block,
  BlockType,
  StyleRange,
  TextAlignment,
  EditorVariant,
  ContentChangeEvent,
  SelectionChangeEvent,
  RichTextEditorRef,
  RichTextEditorProps,
  ToolbarOption,
} from './types';
