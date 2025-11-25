cmake_minimum_required(VERSION 3.4.1)

project("image_processing")

# Force 16KB Alignment
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384")

add_library(
        image_processing
        SHARED
        image_processing.cpp
)

# Specify include directories
include_directories(${ANDROID_NDK}/sources/android/cpufeatures)

# Search for prebuilt libraries
find_library(
        log-lib
        log
)

# Link the native library with log
target_link_libraries(
        image_processing
        ${log-lib}
)
