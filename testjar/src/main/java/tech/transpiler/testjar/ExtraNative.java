package tech.transpiler.testjar;

import ru.nexusguard.protection.annotations.Native;

public final class ExtraNative {
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
}
