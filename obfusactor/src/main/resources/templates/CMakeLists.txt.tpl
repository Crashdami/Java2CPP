cmake_minimum_required(VERSION 3.16)
project(obfuscator_native LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Автопоиск Java и JNI
find_package(Java REQUIRED COMPONENTS Development)  # при желании можно убрать REQUIRED
find_package(JNI  REQUIRED)

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

# Используем найденные JNI include‑директории вместо JAVA_HOME/include
target_include_directories(obfuscator_native PRIVATE
    ${JNI_INCLUDE_DIRS}
)

# Если нужно, линкуем JVM (обычно достаточно JNI::JNI)
target_link_libraries(obfuscator_native PRIVATE
    JNI::JNI
)
