import type { ViewProps, HostComponent } from 'react-native';
import { requireNativeComponent, UIManager } from 'react-native';

type Double = number;
type Int32 = number;
type DirectEventHandler<T> = (event: { nativeEvent: T }) => void;

export type StyleRange = Readonly<{
  style: string;
  start: Int32;
  end: Int32;
  url?: string;
  highlightColor?: string;
}>;

export type Block = Readonly<{
  type: string;
  text: string;
  styles: ReadonlyArray<StyleRange>;
  alignment?: string;
  checked?: boolean;
  indentLevel?: Int32;
}>;

type ContentChangeEventData = Readonly<{
  text: string;
  blocksJson: string;
}>;

type SelectionChangeEventData = Readonly<{
  start: Int32;
  end: Int32;
}>;

type SizeChangeEventData = Readonly<{
  height: Double;
}>;

type ActiveStylesEventData = Readonly<{
  bold: boolean;
  italic: boolean;
  underline: boolean;
  strikethrough: boolean;
  code: boolean;
  highlight: boolean;
  blockType: string;
  alignment: string;
}>;

export interface NativeProps extends ViewProps {
  placeholder?: string;
  editable?: boolean;
  selectable?: boolean;
  maxHeight?: Double;
  numberOfLines?: Int32;
  showToolbar?: boolean;
  toolbarOptions?: ReadonlyArray<string>;
  variant?: string;
  fontFamily?: string;
  fontSize?: Double;
  maxImageWidth?: Double;
  initialContentJson?: string;
  editorConfigJson?: string;

  onContentChange?: DirectEventHandler<ContentChangeEventData>;
  onSelectionChange?: DirectEventHandler<SelectionChangeEventData>;
  onEditorFocus?: DirectEventHandler<Readonly<{}>>;
  onEditorBlur?: DirectEventHandler<Readonly<{}>>;
  onSizeChange?: DirectEventHandler<SizeChangeEventData>;
  onActiveStylesChange?: DirectEventHandler<ActiveStylesEventData>;
}

const COMPONENT_NAME = 'RichTextEditorView';

// Check if native component exists
const hasNativeComponent = UIManager.getViewManagerConfig(COMPONENT_NAME) != null;

let RichTextEditorViewNative: HostComponent<NativeProps>;

if (hasNativeComponent) {
  RichTextEditorViewNative = requireNativeComponent<NativeProps>(
    COMPONENT_NAME,
  ) as HostComponent<NativeProps>;
} else {
  RichTextEditorViewNative = (() => null) as any;
}

export default RichTextEditorViewNative;
