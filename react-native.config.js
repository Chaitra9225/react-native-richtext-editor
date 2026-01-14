module.exports = {
  dependency: {
    platforms: {
      android: {
        sourceDir: './android',
        packageImportPath: 'import com.richtext.editor.RichTextEditorPackage;',
        packageInstance: 'new RichTextEditorPackage()',
      },
      ios: {},
    },
  },
};
