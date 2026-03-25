cmake_minimum_required(VERSION 3.21)
project(ble_macos LANGUAGES C OBJC)

# ---------------------------------------------------------------------------
# Universal binary: build for both Apple Silicon (arm64) and Intel (x86_64).
# The resulting .dylib can be loaded on either architecture without any
# runtime branching on the Java side.
# ---------------------------------------------------------------------------
set(CMAKE_OSX_ARCHITECTURES "arm64;x86_64" CACHE STRING "macOS target architectures" FORCE)
set(CMAKE_OSX_DEPLOYMENT_TARGET "13.0" CACHE STRING "Minimum macOS deployment target" FORCE)

# ---------------------------------------------------------------------------
# JNI headers — locate the JDK that Maven is using.
# JAVA_HOME must be set in the environment (or passed via -DJAVA_HOME=...).
# ---------------------------------------------------------------------------
if(NOT DEFINED ENV{JAVA_HOME} AND NOT DEFINED JAVA_HOME)
    message(FATAL_ERROR "JAVA_HOME must be set to the JDK root directory.")
endif()

if(NOT DEFINED JAVA_HOME)
    set(JAVA_HOME "$ENV{JAVA_HOME}")
endif()

set(JNI_INCLUDE_DIRS
    "${JAVA_HOME}/include"
    "${JAVA_HOME}/include/darwin"
)

# ---------------------------------------------------------------------------
# Shared library target
# ---------------------------------------------------------------------------
add_library(ble-macos SHARED
    BleBridge.m
)

target_include_directories(ble-macos PRIVATE
    ${JNI_INCLUDE_DIRS}
    ${CMAKE_CURRENT_SOURCE_DIR}
)

target_compile_options(ble-macos PRIVATE
    -Wall
    -Wextra
    -Werror
    -fobjc-arc           # Automatic Reference Counting — mandatory for CoreBluetooth
    -fmodules            # Enable Clang modules (@import CoreBluetooth)
)

# Link against CoreBluetooth and Foundation
target_link_libraries(ble-macos PRIVATE
    "-framework CoreBluetooth"
    "-framework Foundation"
)

# Output name — Java side looks for "libble-macos.dylib"
set_target_properties(ble-macos PROPERTIES
    OUTPUT_NAME "ble-macos"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/lib"
)

# ---------------------------------------------------------------------------
# Install rule — the Maven build copies the .dylib into the JAR resources
# ---------------------------------------------------------------------------
install(TARGETS ble-macos
    LIBRARY DESTINATION lib
)
