import React, {
  forwardRef,
  useImperativeHandle,
  useRef,
  useCallback,
  useState,
} from "react";
import { findNodeHandle, Platform, StyleSheet, UIManager } from "react-native";
import type {
  Block,
  MediaAttachment,
  TextAlignment,
  ContentChangeEvent,
  SelectionChangeEvent,
  RichTextEditorProps,
  RichTextEditorRef,
} from "./types";
import RichTextEditorViewNative from "./RichTextEditorViewNativeComponent";

interface SizeChangeEvent {
  nativeEvent: {
    height: number;
  };
}

export interface ActiveStylesState {
  bold: boolean;
  italic: boolean;
  underline: boolean;
  strikethrough: boolean;
  code: boolean;
  highlight: boolean;
  blockType: string;
  alignment: string;
}

interface ActiveStylesChangeEvent {
  nativeEvent: ActiveStylesState;
}

export interface RichTextEditorPropsExtended extends RichTextEditorProps {
  onActiveStylesChange?: (styles: ActiveStylesState) => void;
}

const RichTextEditor = forwardRef<
  RichTextEditorRef,
  RichTextEditorPropsExtended
>((props, ref) => {
  const nativeRef =
    useRef<React.ElementRef<typeof RichTextEditorViewNative>>(null);
  const [height, setHeight] = useState<number>(44);

  const handleSizeChange = useCallback((event: SizeChangeEvent) => {
    const newHeight = event.nativeEvent?.height;
    if (newHeight && newHeight > 0) {
      setHeight(newHeight);
    }
  }, []);

  const dispatchAndroidCommand = useCallback(
    (commandName: string, args: (string | number | boolean)[] = []) => {
      if (Platform.OS !== "android") return;

      const nativeTag = findNodeHandle(nativeRef.current);
      if (nativeTag == null) return;

      const commandConfig =
        UIManager.getViewManagerConfig("RichTextEditorView")?.Commands;
      const commandId = commandConfig?.[commandName];

      if (commandId == null) return;

      UIManager.dispatchViewManagerCommand(nativeTag, commandId, args);
    },
    [],
  );

  // These are placeholder methods for potential future UIManager.dispatchViewManagerCommand usage
  useImperativeHandle(
    ref,
    () => ({
      setContent: (_blocks: Block[]) => {
        /* Native toolbar handles this */
      },
      getText: async (): Promise<string> => "",
      getBlocks: async (): Promise<Block[]> => [],
      clear: () => {
        /* Native toolbar handles this */
      },
      focus: () => {
        /* Native toolbar handles this */
      },
      blur: () => {
        /* Native toolbar handles this */
      },
      toggleBold: () => {
        /* Native toolbar handles this */
      },
      toggleItalic: () => {
        /* Native toolbar handles this */
      },
      toggleUnderline: () => {
        /* Native toolbar handles this */
      },
      toggleStrikethrough: () => {
        /* Native toolbar handles this */
      },
      toggleCode: () => {
        /* Native toolbar handles this */
      },
      toggleHighlight: (_color?: string) => {
        /* Native toolbar handles this */
      },
      setHeading: () => {
        /* Native toolbar handles this */
      },
      setBulletList: () => {
        /* Native toolbar handles this */
      },
      setNumberedList: () => {
        /* Native toolbar handles this */
      },
      setQuote: () => {
        /* Native toolbar handles this */
      },
      setChecklist: () => {
        /* Native toolbar handles this */
      },
      setParagraph: () => {
        /* Native toolbar handles this */
      },
      insertLink: (_url: string, _text: string) => {
        /* Native toolbar handles this */
      },
      insertMediaAttachment: (mediaAttachment: MediaAttachment) => {
        dispatchAndroidCommand("insertMediaAttachment", [mediaAttachment.uri]);
      },
      undo: () => {
        /* Native toolbar handles this */
      },
      redo: () => {
        /* Native toolbar handles this */
      },
      clearFormatting: () => {
        /* Native toolbar handles this */
      },
      indent: () => {
        /* Native toolbar handles this */
      },
      outdent: () => {
        /* Native toolbar handles this */
      },
      setAlignment: (_alignment: TextAlignment) => {
        /* Native toolbar handles this */
      },
      toggleChecklistItem: () => {
        /* Native toolbar handles this */
      },
    }),
    [dispatchAndroidCommand],
  );

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const handleContentChange = useCallback(
    (event: any) => {
      // Parse blocksJson string (codegen doesn't support nested ReadonlyArray<Object>)
      let blocks: Block[] = [];
      try {
        if (event.nativeEvent.blocksJson) {
          blocks = JSON.parse(event.nativeEvent.blocksJson);
        } else if (event.nativeEvent.blocks) {
          // Fallback for backward compatibility
          blocks = [...event.nativeEvent.blocks];
        }
      } catch {
        blocks = [];
      }

      // Convert native event to our API format
      const contentEvent: ContentChangeEvent = {
        nativeEvent: {
          text: event.nativeEvent.text,
          blocks,
          delta: event.nativeEvent.delta,
        },
      };
      props.onContentChange?.(contentEvent);
    },
    [props.onContentChange],
  );

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const handleSelectionChange = useCallback(
    (event: any) => {
      const selectionEvent: SelectionChangeEvent = {
        nativeEvent: {
          start: event.nativeEvent.start,
          end: event.nativeEvent.end,
        },
      };
      props.onSelectionChange?.(selectionEvent);
    },
    [props.onSelectionChange],
  );

  const handleFocus = useCallback(() => {
    props.onFocus?.();
  }, [props.onFocus]);

  const handleBlur = useCallback(() => {
    props.onBlur?.();
  }, [props.onBlur]);

  const handleActiveStylesChange = useCallback(
    (event: ActiveStylesChangeEvent) => {
      props.onActiveStylesChange?.(event.nativeEvent);
    },
    [props.onActiveStylesChange],
  );

  const combinedStyle = StyleSheet.flatten([props.style, { height }]);

  return (
    <RichTextEditorViewNative
      ref={nativeRef}
      style={combinedStyle}
      placeholder={props.placeholder}
      initialContentJson={
        props.initialContent ? JSON.stringify(props.initialContent) : undefined
      }
      editable={props.readOnly !== undefined ? !props.readOnly : true}
      maxHeight={props.maxHeight}
      numberOfLines={props.numberOfLines}
      showToolbar={props.readOnly ? false : (props.showToolbar ?? true)}
      toolbarOptions={props.toolbarOptions}
      variant={props.variant ?? "outlined"}
      onContentChange={handleContentChange}
      onSelectionChange={handleSelectionChange}
      onEditorFocus={handleFocus}
      onEditorBlur={handleBlur}
      onSizeChange={handleSizeChange}
      onActiveStylesChange={handleActiveStylesChange}
    />
  );
});

RichTextEditor.displayName = "RichTextEditor";

export default RichTextEditor;
export { DEFAULT_TOOLBAR_OPTIONS } from "./types";
export type {
  Block,
  BlockType,
  StyleRange,
  MediaAttachment,
  TextAlignment,
  EditorVariant,
  ContentChangeEvent,
  SelectionChangeEvent,
  RichTextEditorRef,
  RichTextEditorProps,
  ToolbarOption,
  ContentDelta,
  DeltaType,
} from "./types";
