import React, { useRef } from 'react';
import {
  SafeAreaView,
  StyleSheet,
  Text,
  View,
  Button,
  ScrollView,
} from 'react-native';
import RichTextEditor, {
  RichTextEditorRef,
  Block,
  ContentChangeEvent,
} from '@chaitrabhairappa/react-native-rich-text-editor';

function App(): React.JSX.Element {
  const editorRef = useRef<RichTextEditorRef>(null);
  const [content, setContent] = React.useState<Block[]>([]);

  const handleContentChange = (event: ContentChangeEvent) => {
    console.log('Content changed:', event.nativeEvent.text);
    setContent(event.nativeEvent.blocks);
  };

  const handleBold = () => {
    editorRef.current?.toggleBold();
  };

  const handleItalic = () => {
    editorRef.current?.toggleItalic();
  };

  const handleClear = () => {
    editorRef.current?.clear();
  };

  const sampleContent: Block[] = [
    {
      type: 'paragraph',
      text: 'This is a rich text editor demo with multiple lines of content that should be truncate when displayed in read-only mode with numberOfLines set.',
      styles: [{ style: 'bold', start: 0, end: 4 }],
    },
    {
      type: 'paragraph',
      text: 'This second paragraph adds more content to demonstrate the ellipsis truncation behavior.',
      styles: [{ style: 'italic', start: 5, end: 11 }],
    },
    {
      type: 'paragraph',
      text: 'This third paragraph should not be visible at all when numberOfLines is 2 fhfhfh fhfhf fhhfhf fhfhfh fhfhfhhdhdh dhhdhdhdhshs.',
      styles: [],
    },
  ];

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>Rich Text Editor Demo</Text>
      </View>

      <Text style={styles.sectionLabel}>Read-Only with numberOfLines=2:</Text>
      <View style={styles.editorContainer}>
        <RichTextEditor
          style={styles.editor}
          initialContent={sampleContent}
          readOnly
          numberOfLines={2}
          variant="flat"
        />
      </View>

      <Text style={styles.sectionLabel}>Editable Editor:</Text>
      <View style={styles.editorContainer}>
        <RichTextEditor
          ref={editorRef}
          style={styles.editor}
          placeholder="Start typing..."
          onContentChange={handleContentChange}
          variant="outlined"
          maxHeight={200}
        />
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  header: {
    padding: 16,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold',
    textAlign: 'center',
  },
  sectionLabel: {
    fontSize: 14,
    fontWeight: '600',
    color: '#666',
    paddingHorizontal: 16,
    paddingTop: 16,
    paddingBottom: 4,
  },
  editorContainer: {
    paddingHorizontal: 16,
    paddingVertical: 8,
  },
  editor: {
    backgroundColor: '#fff',
    borderRadius: 8,
  },
  buttonRow: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    padding: 16,
  },
  debugContainer: {
    flex: 1,
    padding: 16,
    backgroundColor: '#fff',
    margin: 16,
    borderRadius: 8,
  },
  debugTitle: {
    fontSize: 14,
    fontWeight: 'bold',
    marginBottom: 8,
  },
  debugText: {
    fontSize: 12,
    fontFamily: 'monospace',
    color: '#666',
  },
});

export default App;
