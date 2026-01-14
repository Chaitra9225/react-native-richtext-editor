const path = require('path');
const {getDefaultConfig, mergeConfig} = require('@react-native/metro-config');
const exclusionList = require('metro-config/src/defaults/exclusionList');

const root = path.resolve(__dirname, '..');
const libraryPackageJson = require(path.resolve(root, 'package.json'));

// Get peer dependencies that should come from the example app
const peerDeps = Object.keys(libraryPackageJson.peerDependencies || {});

/**
 * Metro configuration
 * https://facebook.github.io/metro/docs/configuration
 *
 * @type {import('metro-config').MetroConfig}
 */
const config = {
  watchFolders: [root],
  resolver: {
    // Only block peer dependencies (react, react-native) from library's node_modules
    blockList: exclusionList(
      peerDeps.map(
        dep => new RegExp(`${root.replace(/[/\\]/g, '[/\\\\]')}/node_modules/${dep}/.*`)
      )
    ),
    extraNodeModules: peerDeps.reduce((acc, dep) => {
      acc[dep] = path.resolve(__dirname, 'node_modules', dep);
      return acc;
    }, {}),
  },
};

module.exports = mergeConfig(getDefaultConfig(__dirname), config);
