#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html.
# Run `pod lib lint flutter_blufi.podspec' to validate before publishing.
#
Pod::Spec.new do |s|
  s.name             = 'esp_blufi'
  s.version          = '0.0.1'
  s.summary          = 'A new Flutter plugin.'
  s.description      = <<-DESC
A new Flutter plugin.
                       DESC
  s.homepage         = 'https://github.com/RianOvO/esp_blufi'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Rian' => 'yu.legend@outlook.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.public_header_files = 'Classes/**/*.h'
  s.private_header_files = 'Classes/BlufiLibrary/Security/openssl/include/openssl/*{.h,.cpp,.a}', 'Classes/BlufiLibrary/Security/openssl/include/*{.h,.cpp,.a}', 'Classes/BlufiLibrary/Security/openssl/*{.h,.cpp,.a}',
  'Classes/BlufiLibrary/**/*{.h,.cpp,.a}'
  s.dependency 'Flutter'
  s.platform = :ios, '8.0'
  s.ios.xcconfig = { "HEADER_SEARCH_PATHS" => "$(SRCROOT)/../.symlinks/plugins/flutter_blufi/ios/Classes/BlufiLibrary/Security/openssl/include" }
  s.ios.vendored_libraries = 'Classes/BlufiLibrary/Security/openssl/*{.a}'

  # Flutter.framework does not contain a i386 slice.
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386',
   'ENABLE_BITCODE' => 'NO'}
end

