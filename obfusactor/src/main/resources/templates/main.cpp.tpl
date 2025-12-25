#include <jni.h>

{{DECLARATIONS}}

struct NgenNativeEntry {
    int id;
    const char* className;
    JNINativeMethod* methods;
    jint count;
};

static NgenNativeEntry __ngen_natives[] = {
{{ENTRIES}}
};

static const jint __ngen_natives_count = static_cast<jint>(sizeof(__ngen_natives) / sizeof(__ngen_natives[0]));

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    (void)reserved;
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_8) != JNI_OK) {
        return JNI_ERR;
    }
    for (jint i = 0; i < __ngen_natives_count; ++i) {
        const NgenNativeEntry& entry = __ngen_natives[i];
        jclass cls = env->FindClass(entry.className);
        if (!cls) {
            continue;
        }
        env->RegisterNatives(cls, entry.methods, entry.count);
        env->DeleteLocalRef(cls);
    }
    return JNI_VERSION_1_8;
}
