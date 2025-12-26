package tech.transpiler.testjar;

import ru.nexusguard.protection.annotations.Native;

public final class ExtraNative {
    private static int touchCount;

    public static final class Box {
        int value;
        String text;
    }

    private ExtraNative() {
    }

    public static void main(String[] args) {
        System.out.println("-------------Test #4: Mini Perf-------------");
        int result = smallPerf(500_000);
        System.out.println("Mini perf result: " + result);
        int objResult = objectOpsTest();
        System.out.println("Object ops result: " + objResult);
        int arrayResult = arrayOpsTest();
        System.out.println("Array ops result: " + arrayResult);
        int lambdaResult = lambdaFieldTest();
        System.out.println("Lambda/field result: " + lambdaResult);
        int opcodeResult = opcodeExtrasTest();
        System.out.println("Opcode extras result: " + opcodeResult);
        int multiResult = multiArrayTypeTest();
        System.out.println("Multi-array/type result: " + multiResult);
        int monitorResult = monitorTest();
        System.out.println("Monitor test result: " + monitorResult);
        int ldcResult = ldcClassTest();
        System.out.println("LDC class test result: " + ldcResult);
        try {
            throwTest();
        } catch (RuntimeException ex) {
            System.out.println("Athrow result: " + ex.getMessage());
        }
    }

    @Native
    public static int smallPerf(int iterations) {
        int acc = 0;
        int x = 5;
        int y = 11;
        for (int i = 0; i < iterations; i++) {
            acc += (x * y) + (i % 5);
            x += 2;
            y -= 1;
        }
        return acc;
    }

    @Native
    public static int objectOpsTest() {
        Object obj = new StringBuilder("x");
        if (!(obj instanceof StringBuilder)) {
            return -1;
        }
        StringBuilder sb = (StringBuilder) obj;
        Object[] arr = new Object[2];
        return sb.length();
    }

    @Native
    public static int arrayOpsTest() {
        int[] ints = new int[4];
        ints[0] = 7;
        long[] longs = new long[1];
        longs[0] = 9L;
        byte[] bytes = new byte[2];
        bytes[1] = 3;
        short[] shorts = new short[1];
        shorts[0] = 5;
        char[] chars = new char[1];
        chars[0] = 'A';
        boolean[] bools = new boolean[1];
        bools[0] = true;
        float[] floats = new float[1];
        floats[0] = 1.5f;
        double[] doubles = new double[1];
        doubles[0] = 2.5;
        Object[] arr = new Object[2];
        arr[0] = new StringBuilder("xy");
        Object obj = arr[0];
        if (!(obj instanceof StringBuilder)) {
            return -2;
        }
        StringBuilder sb = (StringBuilder) obj;
        float f = floats[0];
        double d = doubles[0];
        long l = longs[0];
        int sum = ints.length + sb.length();
        sum += ints[0];
        sum += bytes[1];
        sum += shorts[0];
        sum += chars[0];
        sum += bools[0] ? 1 : 0;
        return sum;
    }

    @Native
    public static int lambdaFieldTest() {
        Box box = new Box();
        box.value = 10;
        box.text = "ok";
        int value = box.value;
        String text = box.text;
        Runnable r = ExtraNative::touch;
        r.run();
        return value + text.length() + touchCount;
    }

    @Native
    public static int opcodeExtrasTest() {
        int a = -123;
        long l = 1234567890123L;
        int neg = -a;
        long lneg = -l;
        int and = (a & 0xFF) ^ 0x5A;
        int shl = a << 2;
        int ushr = a >>> 3;
        long lshr = l >> 4;
        long lushr = l >>> 5;
        long i2l = (long) a;
        int l2i = (int) l;
        double i2d = (double) a;
        float l2f = (float) l;
        int f2i = (int) (l2f + 1.25f);
        long d2l = (long) (i2d + 1000.0);
        float d2f = (float) i2d;
        int cmp = l > lneg ? 1 : (l < lneg ? -1 : 0);
        return neg + (int) lneg + and + shl + ushr + (int) lshr + (int) lushr
                + (int) i2l + l2i + (int) i2d + (int) l2f + f2i + (int) d2l + (int) d2f + cmp;
    }

    @Native
    public static int multiArrayTypeTest() {
        int[][] ints = new int[2][3];
        ints[1][2] = 7;
        String[][] strs = new String[1][2];
        strs[0][1] = "x";
        Class<?> c1 = int.class;
        Class<?> c2 = String.class;
        Class<?> c3 = int[][].class;
        return ints.length + ints[0].length + ints[1][2]
                + strs[0].length
                + c1.getName().length() + c2.getName().length() + c3.getName().length();
    }

    @Native
    public static int monitorTest() {
        Object lock = new Object();
        int[] data = new int[1];
        synchronized (lock) {
            data[0] = 42;
        }
        return data[0];
    }

    @Native
    public static int ldcClassTest() {
        Class<?> cObj = Object.class;
        Class<?> cBool = boolean.class;
        Class<?> cByte = byte.class;
        Class<?> cChar = char.class;
        Class<?> cShort = short.class;
        Class<?> cInt = int.class;
        Class<?> cLong = long.class;
        Class<?> cFloat = float.class;
        Class<?> cDouble = double.class;
        Class<?> cVoid = void.class;
        Class<?> cBoolObj = Boolean.class;
        Class<?> cByteObj = Byte.class;
        Class<?> cCharObj = Character.class;
        Class<?> cShortObj = Short.class;
        Class<?> cIntObj = Integer.class;
        Class<?> cLongObj = Long.class;
        Class<?> cFloatObj = Float.class;
        Class<?> cDoubleObj = Double.class;
        Class<?> cStrObj = String.class;
        Class<?> cObjArr = Object[].class;
        Class<?> cBoolArr = boolean[][].class;
        int sum = cObj.getName().length()
                + cBool.getName().length()
                + cByte.getName().length()
                + cChar.getName().length()
                + cShort.getName().length()
                + cInt.getName().length()
                + cLong.getName().length()
                + cFloat.getName().length()
                + cDouble.getName().length()
                + cVoid.getName().length()
                + cBoolObj.getName().length()
                + cByteObj.getName().length()
                + cCharObj.getName().length()
                + cShortObj.getName().length()
                + cIntObj.getName().length()
                + cLongObj.getName().length()
                + cFloatObj.getName().length()
                + cDoubleObj.getName().length()
                + cStrObj.getName().length()
                + cObjArr.getName().length()
                + cBoolArr.getName().length();
        return sum;
    }

    private static void touch() {
        touchCount++;
    }

    @Native
    public static void throwTest() {
        throw new RuntimeException("boom");
    }
}
