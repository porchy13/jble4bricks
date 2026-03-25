cmake_minimum_required(VERSION 3.21)
project(ble_windows LANGUAGES CXX)

# ---------------------------------------------------------------------------
# Require C++20 — WinRT C++/WinRT headers need at least C++17; C++20 is
# recommended for coroutine-style async helpers.
# ---------------------------------------------------------------------------
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

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
    "${JAVA_HOME}/include/win32"
)

# ---------------------------------------------------------------------------
# C++/WinRT — the WinRT projection headers are consumed directly from the
# Windows SDK.  No separate nuget package is required when targeting the
# system-installed Windows SDK (10.0.22000.0 or later).
# ---------------------------------------------------------------------------

# ---------------------------------------------------------------------------
# Shared library target
# ---------------------------------------------------------------------------
add_library(ble-windows SHARED
    BleBridge.cpp
)

target_include_directories(ble-windows PRIVATE
    ${JNI_INCLUDE_DIRS}
    ${CMAKE_CURRENT_SOURCE_DIR}
)

target_compile_options(ble-windows PRIVATE
    /W4           # Warning level 4
    /WX           # Treat warnings as errors
    /EHsc         # C++ exception handling — required for WinRT
    /await        # Enable coroutine support (MSVC)
    /std:c++20
)

target_compile_definitions(ble-windows PRIVATE
    WINRT_LEAN_AND_MEAN
    NOMINMAX
    WIN32_LEAN_AND_MEAN
    _UNICODE
    UNICODE
)

# Link against WindowsApp.lib, which provides access to all WinRT types and
# the RuntimeObject infrastructure.
target_link_libraries(ble-windows PRIVATE
    WindowsApp
)

# Output name — Java side looks for "ble-windows.dll" (no "lib" prefix on
# Windows; System.mapLibraryName("ble-windows") returns "ble-windows.dll").
set_target_properties(ble-windows PROPERTIES
    OUTPUT_NAME "ble-windows"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/lib"
    RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/lib"
)

# ---------------------------------------------------------------------------
# Install rule — the Maven build copies the .dll into the JAR resources
# ---------------------------------------------------------------------------
install(TARGETS ble-windows
    RUNTIME DESTINATION lib
)
