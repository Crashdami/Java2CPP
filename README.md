# NexusGuard J2C

Java-to-C++ transpiler and obfuscator that turns `@Native` methods into JNI
implementations and ships a native DLL alongside the output jar.

## Highlights

- Marks Java methods with `ru.nexusguard.protection.annotations.Native`.
- Generates C++ sources and a CMake project.
- Rewrites annotated methods to `native` and injects a runtime loader.
- Packs `ru/nexusguard/protection/native/library.dll` into the output jar.
- Uses a lightweight native VM stack for translated bytecode.

## Project layout

- `obfusactor/` - transpiler and jar rewriter (Gradle app).
- `testjar/` - sample jar for quick validation.

## Requirements

- Java 23
- Gradle (via wrapper)
- CMake 3.16+
- C++ toolchain (MSVC, clang, or gcc with JNI support)

## Quick start

Build the transpiler:

```powershell
.\gradlew.bat :obfusactor:jar
```

Transpile a jar:

```powershell
java -jar obfusactor/build/libs/obfuscator.jar input.jar outDirectory -a
```

Build native DLL:

```powershell
cmake -S outDirectory/cpp/output -B outDirectory/cpp/output/build
cmake --build outDirectory/cpp/output/build --config Release
```

If you want the DLL embedded into the jar, run the transpiler again after the
native build so the new `library.dll` is packaged.

## Output layout

- `outDirectory/` - rewritten jar and runtime classes
- `outDirectory/cpp/output/` - generated C++ sources + CMake project
- `outDirectory/cpp/output/library.dll` - native library (after build)

## Notes

- Only methods annotated with `@Native` are translated.
- The runtime loader lives in `ru/nexusguard/protection/native/NativeRunner`.
- JNI registration happens in `JNI_OnLoad`.

## Warning

Some opcodes may not be supported yet. This project will be updated soon.
