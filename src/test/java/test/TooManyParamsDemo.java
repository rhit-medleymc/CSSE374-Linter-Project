package test;

public class TooManyParamsDemo {
    public TooManyParamsDemo(int a, int b, int c, int d, int e, int f) {}

    public void okMethod(int a, int b, int c, int d, int e) {}

    public void badMethod(int a, int b, int c, int d, int e, int f) {}

    private static String badStatic(int a, int b, int c, int d, int e, int f, int g) {
        return "x";
    }
}
