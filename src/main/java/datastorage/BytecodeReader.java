package datastorage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Data layer class responsible for reading and parsing Java bytecode files using ASM.
 */
public class BytecodeReader {

    /**
     * Reads a .class file and returns an ASM ClassNode for analysis.
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
     * @return a List of FieldNode objects representing all fields in the class
     */
    public List<FieldNode> getFields(File classFile) throws IOException {
        ClassNode classNode = readClassFile(classFile);
        return (List<FieldNode>) classNode.fields;
    }

    /**
     * Gets all methods from a .class file.
     * @return a List of MethodNode objects representing all methods in the class
     */
    public List<MethodNode> getMethods(File classFile) throws IOException {
        ClassNode classNode = readClassFile(classFile);
        return (List<MethodNode>) classNode.methods;
    }
}
