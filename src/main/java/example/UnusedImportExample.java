package example;

import java.util.List; // Used
import java.util.ArrayList; // Unused
import java.io.File; // Unused
import java.util.Set; // Used
import java.util.HashSet; // Used

public class UnusedImportExample {
    public void test() {
        List<String> l;
        Set<String> s = new HashSet<>();
    }
}
