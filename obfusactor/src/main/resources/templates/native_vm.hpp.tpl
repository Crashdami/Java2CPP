#pragma once
#include <jni.h>
#include <cstdint>
#include <cstring>
#include <vector>

namespace ng {
namespace runtime {

enum class ValueKind { I64, REF };

struct Value {
    ValueKind kind;
    int64_t i64;
    jobject ref;

    static Value FromI64(int64_t v) {
        Value value;
        value.kind = ValueKind::I64;
        value.i64 = v;
        value.ref = nullptr;
        return value;
    }

    static Value FromRef(jobject v) {
        Value value;
        value.kind = ValueKind::REF;
        value.i64 = 0;
        value.ref = v;
        return value;
    }
};

inline int64_t packFloat(float v) {
    uint32_t bits = 0;
    std::memcpy(&bits, &v, sizeof(bits));
    return static_cast<int64_t>(bits);
}

inline float unpackFloat(int64_t v) {
    uint32_t bits = static_cast<uint32_t>(v);
    float out = 0.0f;
    std::memcpy(&out, &bits, sizeof(out));
    return out;
}

inline int64_t packDouble(double v) {
    int64_t bits = 0;
    std::memcpy(&bits, &v, sizeof(bits));
    return bits;
}

inline double unpackDouble(int64_t v) {
    double out = 0.0;
    std::memcpy(&out, &v, sizeof(out));
    return out;
}

class OperandStack {
public:
    explicit OperandStack(int max) {
        values_.reserve(static_cast<size_t>(max));
    }

    void pushI64(int64_t v) {
        values_.push_back(Value::FromI64(v));
    }

    int64_t popI64() {
        return pop().i64;
    }

    void pushRef(jobject v) {
        values_.push_back(Value::FromRef(v));
    }

    jobject popRef() {
        return pop().ref;
    }

    void dup() {
        if (values_.empty()) {
            return;
        }
        values_.push_back(values_.back());
    }

    void dup(JNIEnv* env) {
        if (values_.empty()) {
            return;
        }
        Value value = values_.back();
        if (value.kind == ValueKind::REF && value.ref != nullptr) {
            value.ref = env->NewLocalRef(value.ref);
        }
        values_.push_back(value);
    }

    void popDiscard() {
        if (!values_.empty()) {
            values_.pop_back();
        }
    }

    void popDiscard(JNIEnv* env) {
        if (values_.empty()) {
            return;
        }
        Value value = values_.back();
        values_.pop_back();
        if (value.kind == ValueKind::REF && value.ref != nullptr) {
            env->DeleteLocalRef(value.ref);
        }
    }

private:
    Value pop() {
        Value value = values_.back();
        values_.pop_back();
        return value;
    }

    std::vector<Value> values_;
};

class LocalVars {
public:
    explicit LocalVars(int max) : values_(static_cast<size_t>(max)) {
    }

    void setI64(int index, int64_t v) {
        values_.at(static_cast<size_t>(index)) = Value::FromI64(v);
    }

    int64_t getI64(int index) const {
        return values_.at(static_cast<size_t>(index)).i64;
    }

    void setRef(int index, jobject v) {
        values_.at(static_cast<size_t>(index)) = Value::FromRef(v);
    }

    jobject getRef(int index) const {
        return values_.at(static_cast<size_t>(index)).ref;
    }

private:
    std::vector<Value> values_;
};

struct Frame {
    OperandStack stack;
    LocalVars locals;

    Frame(int maxStack, int maxLocals) : stack(maxStack), locals(maxLocals) {
    }
};

} // namespace runtime
} // namespace ng
