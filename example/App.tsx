import React, { useRef } from 'react';
import {
  SafeAreaView,
  StyleSheet,
  Text,
  View,
  Button,
  ActivityIndicator,
  TouchableOpacity,
  KeyboardAvoidingView,
  ScrollView,
  Platform,
} from 'react-native';
import RichTextEditor, {
  RichTextEditorRef,
  Block,
  ContentChangeEvent,
} from '@chaitrabhairappa/react-native-rich-text-editor';

const sampleContent: Block[] = [
  {
    type: 'paragraph',
    text: 'This is a rich text editor demo with multiple lines of content.',
    styles: [{ style: 'bold', start: 0, end: 4 }],
  },
  {
    type: 'numbered',
    text: 'First bullet item with bold text',
    styles: [{ style: 'bold', start: 23, end: 32 }],
  },
  {
    type: 'bullet',
    text: 'Second bullet item with italic',
    styles: [{ style: 'italic', start: 24, end: 30 }],
  },
  {
    type: 'numbered',
    text: 'First numbered item',
    styles: [{ style: 'underline', start: 0, end: 5 }],
  },
  {
    type: 'paragraph',
    text: 'Visit Google for more info.',
    styles: [
      { style: 'link', start: 6, end: 12, url: 'https://www.google.com' },
    ],
  },
];

function App(): React.JSX.Element {
  const editorRef = useRef<RichTextEditorRef>(null);
  const [mode, setMode] = React.useState<'view' | 'edit'>('view');
  const [isLoading, setIsLoading] = React.useState(true);

  // Simulate data fetch delay (like detailQuery.isLoading)
  React.useEffect(() => {
    const timer = setTimeout(() => setIsLoading(false), 1000);
    return () => clearTimeout(timer);
  }, []);

  const handleSwitchToEdit = () => setMode('edit');
  const handleBackToView = () => setMode('view');

  // ─── Exact same layout as ManageKBRegistry view mode ───
  // ContainerView = KeyboardAvoidingView > View > ScrollView
  if (mode === 'view') {
    return (
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        style={styles.flex1}
        enabled={false}
      >
        <SafeAreaView style={styles.flex1}>
          <ScrollView
            showsHorizontalScrollIndicator={false}
            showsVerticalScrollIndicator={false}
            contentContainerStyle={styles.viewScrollContent}
          >
            {/* Header with back + actions (like Animated.View viewHeader) */}
            <View style={styles.viewHeader}>
              <Text style={styles.title}>RichText example</Text>
              <TouchableOpacity onPress={handleSwitchToEdit}>
                <Text style={styles.actionButton}>Edit</Text>
              </TouchableOpacity>
            </View>

            {/* Body container (like viewBodyContainer) */}
            <View style={styles.viewBodyContainer}>
              <View style={styles.viewTitleContainer}>
                <Text style={styles.titleText}>Title</Text>
              </View>
              <View style={styles.shortDescriptionContainer}>
                <Text style={styles.shortDescriptionText}>
                  This is a short description
                </Text>
              </View>

              {/* Conditional render - exact same pattern */}
              <View style={styles.viewDescriptionContainer}>
                {isLoading && <ActivityIndicator />}
                {!isLoading && (
                  <RichTextEditor
                    readOnly
                    initialContent={sampleContent}
                    variant="plain"
                    style={styles.richTextEditorReadOnly}
                  />
                )}
              </View>
            </View>
          </ScrollView>

          <View style={styles.reloadRow}>
            <Button
              title="Simulate Reload"
              onPress={() => {
                setIsLoading(true);
                setTimeout(() => setIsLoading(false), 500);
              }}
            />
          </View>
        </SafeAreaView>
      </KeyboardAvoidingView>
    );
  }

  // ─── Edit mode (like ManageKBRegistry edit mode) ───
  return (
    <SafeAreaView style={styles.flex1}>
      <KeyboardAvoidingView style={styles.flex1} behavior="padding">
        <View style={styles.viewHeader}>
          <TouchableOpacity onPress={handleBackToView}>
            <Text style={styles.actionButton}>← Back</Text>
          </TouchableOpacity>
          <Text style={styles.title}>Edit Mode</Text>
        </View>

        <ScrollView
          style={styles.flex1}
          contentContainerStyle={styles.formContentContainer}
          keyboardShouldPersistTaps="handled"
        >
          <RichTextEditor
            ref={editorRef}
            initialContent={sampleContent}
            variant="flat"
            style={{}}
          />
        </ScrollView>

        <View style={styles.buttonContainer}>
          <TouchableOpacity
            style={styles.saveButton}
            onPress={handleBackToView}
          >
            <Text style={styles.saveButtonText}>Cancel</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.saveButton}>
            <Text style={styles.saveButtonText}>Save</Text>
          </TouchableOpacity>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  flex1: {
    flex: 1,
    paddingTop: 40,
  },
  viewHeader: {
    zIndex: 998,
    flexDirection: 'row',
    justifyContent: 'space-between',
    height: 64,
    alignItems: 'center',
    paddingHorizontal: 16,
    backgroundColor: '#fff',
  },
  title: {
    fontSize: 17,
    fontWeight: '500',
  },
  actionButton: {
    fontSize: 16,
    color: '#007AFF',
  },
  viewScrollContent: {
    paddingTop: 20,
    paddingBottom: 24,
  },
  viewBodyContainer: {
    marginHorizontal: 20,
  },
  viewTitleContainer: {
    marginBottom: 8,
  },
  titleText: {
    fontSize: 15,
    fontWeight: '500',
  },
  shortDescriptionText: {
    color: '#666',
    fontSize: 13,
    marginTop: 4,
  },
  shortDescriptionContainer: {
    marginBottom: 12,
  },
  viewDescriptionContainer: {},
  richTextEditorReadOnly: {
    padding: 0,
    margin: 0,
  },
  formContentContainer: {
    paddingHorizontal: 20,
    paddingTop: 20,
    paddingBottom: 32,
    gap: 8,
  },
  buttonContainer: {
    flexDirection: 'row',
    gap: 12,
    paddingHorizontal: 20,
    paddingBottom: 10,
  },
  saveButton: {
    flex: 1,
    padding: 14,
    backgroundColor: '#007AFF',
    borderRadius: 8,
    alignItems: 'center',
  },
  saveButtonText: {
    color: '#fff',
    fontWeight: '600',
  },
  reloadRow: {
    padding: 12,
  },
});

export default App;
