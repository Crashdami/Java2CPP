#include <jni.h>
#include <cstdio>
#include <cstdint>

extern "C" {

JNIEXPORT jstring JNICALL Java_tech_transpiler_testjar_HelloNative_a(JNIEnv* env, jclass cls) {
    std::printf("Called runAll()");
    std::fflush(stdout);

    jfieldID countField = env->GetStaticFieldID(cls, "count", "I");
    jint count = env->GetStaticIntField(cls, countField);
    env->SetStaticIntField(cls, countField, count + 1);

    return env->NewStringUTF("Privet)");
}

JNIEXPORT jlong JNICALL Java_tech_transpiler_testjar_HelloNative_arithmeticTest(
        JNIEnv* env, jclass cls, jint iterations) {
    (void)env;
    (void)cls;

    int64_t acc = 0;
    int64_t a = 17;
    int64_t b = 31;

    for (jint i = 0; i < iterations; ++i) {
        acc += (a * b) + (i % 7);
        a += 3;
        b ^= a;
    }

    return static_cast<jlong>(acc);
}

} // extern "C"
