package tech.transpiler.testjar;

import ru.nexusguard.protection.annotations.Native;

public final class HelloNative {
    public static int count = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("Obfuscator Test Program");
        System.out.println("-------------Test #3: Efficiency-------------");
        int arith = arithmeticTest(1_000_000);
        System.out.println("Arithmetic test result: " + arith);
        String result = a();
        System.out.println("Native returned: " + result);
        System.out.println("count=" + count);
        ExtraNative.main(new String[0]);
        System.out.println("-------------Tests r Finished-------------");
    }

    @Native
    public static String a() {
        System.out.println("Dont remove this SOUT in String a(). This is test.");
        count += 1;
        return "Privet)";
    }

    @Native
    public static int arithmeticTest(int iterations) {
        int acc = 0;
        int a = 17;
        int b = 31;
        for (int i = 0; i < iterations; i++) {
            acc += (a * b) + (i % 7);
            a += 3;
            b -= 2;
        }
        return acc;
    }
}
