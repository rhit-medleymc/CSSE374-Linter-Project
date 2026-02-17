package example;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;

/**
 * A class designed to trigger all Design Risk Linter checks.
 */
public class RiskExample {

    // 1. Large Class (Fields > 8)
    private int field1;
    private int field2;
    private int field3;
    private int field4;
    private int field5;
    private int field6;
    private int field7;
    private int field8;
    private int field9; // Violation!

    // 2. Global State (Static non-final field)
    public static int globalCounter = 0; // Violation!

    // 3. High Coupling (Distinct types > 6)
    // Types used so far: int (primitive, ignored), String (implicitly), File, List,
    // Map, Scanner, Set, ClassNode
    // Total external types: String, File, List, Map, Scanner, Set, ClassNode = 7 >
    // 6. Violation!

    // 4. Low Cohesion (LCOM4 > 1)
    // Group A: methods using fields 1-4
    public void methodA1() {
        field1 = 1;
    }

    public void methodA2() {
        field2 = 1;
    }

    public void methodA3() {
        field3 = 1;
    }

    public void methodA4() {
        field4 = 1;
    }

    public void methodA5() {
        field1 = field2 + field3;
    }

    // Group B: methods using fields 5-9 (completely disjoint from Group A)
    public void methodB1() {
        field5 = 1;
    }

    public void methodB2() {
        field6 = 1;
    }

    public void methodB3() {
        field7 = 1;
    }

    public void methodB4() {
        field8 = 1;
    }

    public void methodB5() {
        field9 = field5 + field6;
    }

    // 5. Large Class (Methods > 15)
    // We have 10 methods so far. Need 6 more.
    public void dummy1(File f) {
    }

    public void dummy2(List<String> l) {
    }

    public void dummy3(Map<String, String> m) {
    }

    public void dummy4(Scanner s) {
    }

    public void dummy5(Set<String> set) {
    }

    public void dummy6(ClassNode cn) {
    }
    // Total methods: 16 > 15. Violation!

    public void anotherOne() {
    }

    public void dummy7(String s) {
    }

    public void dummy8(ArrayList<String> list) {
    }
}
