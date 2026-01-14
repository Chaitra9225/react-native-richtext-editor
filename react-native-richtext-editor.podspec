require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "react-native-richtext-editor"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = package["homepage"]
  s.license      = package["license"]
  s.authors      = package["author"]

  s.platforms    = { :ios => "13.0" }
  s.source       = { :git => "https://github.com/Chaitra9225/react-native-richtext-editor.git", :tag => "#{s.version}" }

  # Only include Swift and ObjC files for Paper ViewManager (no C++ files)
  s.source_files = "ios/**/*.{swift,m}"

  # Explicitly disable module creation to avoid C++ header conflicts
  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'NO',
    'CLANG_ENABLE_MODULES' => 'NO'
  }

  s.dependency "React-Core"

  s.swift_version = "5.0"
end
