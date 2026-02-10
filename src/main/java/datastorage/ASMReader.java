package datastorage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Data layer class responsible for reading and parsing Java bytecode files
 * using ASM.
 */
public class ASMReader {

    /**
     * Reads a .class file and returns an ASM ClassNode for analysis.
     * 
     * @return a ClassNode representing the parsed bytecode
     */
    public ClassNode readClassFile(File classFile) throws IOException {
        byte[] bytes = Files.readAllBytes(classFile.toPath());
        ClassReader reader = new ClassReader(bytes);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, ClassReader.EXPAND_FRAMES);
        return classNode;
    }

    /**
     * Gets all fields from a .class file.
     * 
     * @return a List of FieldNode objects representing all fields in the class
     */
    public List<FieldNode> getFields(File classFile) throws IOException {
        ClassNode classNode = readClassFile(classFile);
        return (List<FieldNode>) classNode.fields;
    }

    /**
     * Gets all methods from a .class file.
     * 
     * @return a List of MethodNode objects representing all methods in the class
     */
    public List<MethodNode> getMethods(File classFile) throws IOException {
        ClassNode classNode = readClassFile(classFile);
        return (List<MethodNode>) classNode.methods;
    }

    /**
     * Reads multiple .class files (including from directories) and returns a list
     * of ASM ClassNodes.
     * 
     * @param files list of files and/or directories
     * @return a List of ClassNode objects
     */
    public List<ClassNode> getClasses(List<File> files) throws IOException {
        List<ClassNode> classes = new ArrayList<>();
        for (File file : files) {
            recurseFiles(file, classes);
        }
        return classes;
    }

    private void recurseFiles(File file, List<ClassNode> classes) throws IOException {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    recurseFiles(f, classes);
                }
            }
        } else if (file.getName().endsWith(".class")) {
            classes.add(readClassFile(file));
        }
    }

    /**
     * Gets all methods from a list of files and/or directories.
     * 
     * @param files list of files and/or directories
     * @return a List of MethodNode objects representing all methods in the found
     *         classes
     */
    public List<MethodNode> getAllMethods(List<File> files) throws IOException {
        List<MethodNode> methods = new ArrayList<>();
        List<ClassNode> classes = getClasses(files);
        for (ClassNode classNode : classes) {
            methods.addAll((List<MethodNode>) classNode.methods);
        }
        return methods;
    }
}
