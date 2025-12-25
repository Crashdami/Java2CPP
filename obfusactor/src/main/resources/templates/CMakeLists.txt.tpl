cmake_minimum_required(VERSION 3.16)
project(obfuscator_native LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Java + JNI
find_package(Java REQUIRED COMPONENTS Development)
if(NOT DEFINED ENV{JAVA_HOME} AND Java_JAVA_EXECUTABLE)
    get_filename_component(_java_bin "${Java_JAVA_EXECUTABLE}" DIRECTORY)
    get_filename_component(_java_home "${_java_bin}" DIRECTORY)
    set(ENV{JAVA_HOME} "${_java_home}")
endif()
if(NOT DEFINED JAVA_HOME AND DEFINED ENV{JAVA_HOME})
    set(JAVA_HOME "$ENV{JAVA_HOME}")
endif()
find_package(JNI REQUIRED)

message(STATUS "Found Java: ${Java_JAVA_EXECUTABLE}")
message(STATUS "JNI include dirs: ${JNI_INCLUDE_DIRS}")
message(STATUS "JNI libraries: ${JNI_LIBRARIES}")

add_library(obfuscator_native SHARED
    {{SOURCES}}
)

set_target_properties(obfuscator_native PROPERTIES
    OUTPUT_NAME "library"
    RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}"
)

target_include_directories(obfuscator_native PRIVATE
    ${JNI_INCLUDE_DIRS}
)

target_link_libraries(obfuscator_native PRIVATE
    JNI::JNI
)
