cmake_minimum_required(VERSION 3.21)
project(ble_linux LANGUAGES C)

# ---------------------------------------------------------------------------
# Require C11 — GLib/GDBus headers require at least C99; C11 is preferred.
# ---------------------------------------------------------------------------
set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

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
    "${JAVA_HOME}/include/linux"
)

# ---------------------------------------------------------------------------
# GLib / GIO (GDBus) — located via pkg-config
# ---------------------------------------------------------------------------
find_package(PkgConfig REQUIRED)
pkg_check_modules(GLIB REQUIRED glib-2.0)
pkg_check_modules(GIO  REQUIRED gio-2.0)

# ---------------------------------------------------------------------------
# Shared library target
# ---------------------------------------------------------------------------
add_library(ble-linux SHARED
    BleBridge.c
)

target_include_directories(ble-linux PRIVATE
    ${JNI_INCLUDE_DIRS}
    ${GLIB_INCLUDE_DIRS}
    ${GIO_INCLUDE_DIRS}
    ${CMAKE_CURRENT_SOURCE_DIR}
)

target_compile_options(ble-linux PRIVATE
    -Wall
    -Wextra
    -Werror
    -fvisibility=hidden
    ${GLIB_CFLAGS_OTHER}
    ${GIO_CFLAGS_OTHER}
)

target_link_libraries(ble-linux PRIVATE
    ${GLIB_LIBRARIES}
    ${GIO_LIBRARIES}
)

target_link_directories(ble-linux PRIVATE
    ${GLIB_LIBRARY_DIRS}
    ${GIO_LIBRARY_DIRS}
)

# Output name — Java side calls System.mapLibraryName("ble-linux") which
# returns "libble-linux.so" on Linux.
set_target_properties(ble-linux PROPERTIES
    OUTPUT_NAME "ble-linux"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/lib"
)

# ---------------------------------------------------------------------------
# Install rule — the Maven build copies the .so into the JAR resources
# ---------------------------------------------------------------------------
install(TARGETS ble-linux
    LIBRARY DESTINATION lib
)
