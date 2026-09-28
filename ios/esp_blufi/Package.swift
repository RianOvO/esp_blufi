// swift-tools-version: 5.9
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "esp_blufi",
    platforms: [
        .iOS("13.0"),
    ],
    products: [
        .library(name: "esp-blufi", targets: ["esp_blufi"]),
    ],
    dependencies: [
        .package(name: "FlutterFramework", path: "../FlutterFramework"),
    ],
    targets: [
        .target(
            name: "esp_blufi",
            dependencies: [
                .product(name: "FlutterFramework", package: "FlutterFramework"),
            ],
            resources: [
                .process("PrivacyInfo.xcprivacy"),
            ],
            cSettings: [
                // Private headers live next to their sources and are imported by file name.
                .headerSearchPath("BlufiLibrary"),
                .headerSearchPath("BlufiLibrary/Data"),
                .headerSearchPath("BlufiLibrary/Response"),
                .headerSearchPath("BlufiLibrary/Security"),
                .headerSearchPath("ESPAPPResources"),
            ],
            linkerSettings: [
                .linkedFramework("CoreBluetooth"),
                .linkedFramework("Security"),
            ]
        ),
    ]
)
