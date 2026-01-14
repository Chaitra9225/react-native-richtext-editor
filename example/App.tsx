import React, {useRef} from 'react';
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

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>Rich Text Editor Demo</Text>
      </View>

      <View style={styles.editorContainer}>
        <RichTextEditor
          ref={editorRef}
          style={styles.editor}
          placeholder="Start typing..."
          onContentChange={handleContentChange}
          variant="outlined"
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
  editorContainer: {
    padding: 16,
  },
  editor: {
    minHeight: 200,
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
