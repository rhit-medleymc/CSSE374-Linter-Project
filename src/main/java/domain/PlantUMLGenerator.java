package domain;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import datastorage.ASMReader;

public class PlantUMLGenerator implements Linter {
    
    private ASMReader asmReader;
    
    public PlantUMLGenerator() {
        this.asmReader = new ASMReader();
    }

    @Override
    public String lint(List<File> files) {
        try {
            StringBuilder output = new StringBuilder();
            output.append("@startuml\n");
            
            // Get all classes from the input files
            List<ClassNode> classes = asmReader.getClasses(files);
            
            // Generate PlantUML for each class
            for (ClassNode classNode : classes) {
                generateClassDiagram(classNode, output);
            }
            
            // Generate relationships (inheritance and interfaces)
            for (ClassNode classNode : classes) {
                generateRelationships(classNode, output, classes);
            }
            
            output.append("@enduml\n");
            return output.toString();
            
        } catch (IOException e) {
            return "@startuml\n@enduml\n";
        }
    }
    
    /**
     * Generates the PlantUML class definition with fields and methods
     */
    private void generateClassDiagram(ClassNode classNode, StringBuilder output) {
        String className = extractClassName(classNode.name);
        
        // Start class definition
        if ((classNode.access & Opcodes.ACC_INTERFACE) != 0) {
            output.append("interface ").append(className).append(" {\n");
        } else if ((classNode.access & Opcodes.ACC_ABSTRACT) != 0) {
            output.append("abstract class ").append(className).append(" {\n");
        } else {
            output.append("class ").append(className).append(" {\n");
        }
        
        // Add fields
        @SuppressWarnings("unchecked")
        List<FieldNode> fields = (List<FieldNode>) classNode.fields;
        for (FieldNode field : fields) {
            output.append("  ").append(getAccessModifier(field.access))
                   .append(field.name).append(": ")
                   .append(extractTypeName(field.desc))
                   .append("\n");
        }
        
        // Add methods
        @SuppressWarnings("unchecked")
        List<MethodNode> methods = (List<MethodNode>) classNode.methods;
        for (MethodNode method : methods) {
            // Skip synthetic methods and constructors for clarity (optional)
            if ((method.access & Opcodes.ACC_SYNTHETIC) != 0) {
                continue;
            }
            
            output.append("  ").append(getAccessModifier(method.access));
            
            String methodName = method.name;
            if (!methodName.equals("<init>") && !methodName.equals("<clinit>")) {
                String returnType = extractReturnType(method.desc);
                String parameters = extractParameters(method.desc);
                
                output.append(methodName).append("(").append(parameters).append("): ")
                       .append(returnType).append("\n");
            } else if (methodName.equals("<init>")) {
                String parameters = extractParameters(method.desc);
                output.append(extractClassName(classNode.name)).append("(").append(parameters).append(")\n");
            }
        }
        
        output.append("}\n\n");
    }
    
    /**
     * Generates relationships (inheritance, interface implementation)
     */
    private void generateRelationships(ClassNode classNode, StringBuilder output, List<ClassNode> allClasses) {
        String className = extractClassName(classNode.name);
        
        // Handle superclass
        if (classNode.superName != null && !classNode.superName.equals("java/lang/Object")) {
            String superClassName = extractClassName(classNode.superName);
            output.append(className).append(" --|> ").append(superClassName).append("\n");
        }
        
        // Handle interfaces
        @SuppressWarnings("unchecked")
        List<String> interfaces = (List<String>) classNode.interfaces;
        for (String interfaceName : interfaces) {
            String interfaceClassName = extractClassName(interfaceName);
            output.append(className).append(" ..|> ").append(interfaceClassName).append("\n");
        }
    }
    
    /**
     * Extracts simple class name from fully qualified name
     */
    private String extractClassName(String fullName) {
        return fullName.substring(fullName.lastIndexOf('/') + 1);
    }
    
    /**
     * Converts ASM descriptor to type name
     */
    private String extractTypeName(String descriptor) {
        if (descriptor.startsWith("L")) {
            // Object type
            String className = descriptor.substring(1, descriptor.length() - 1);
            return extractClassName(className);
        } else if (descriptor.startsWith("[")) {
            // Array type
            return extractTypeName(descriptor.substring(1)) + "[]";
        } else {
            // Primitive types
            switch (descriptor) {
                case "V": return "void";
                case "Z": return "boolean";
                case "C": return "char";
                case "B": return "byte";
                case "S": return "short";
                case "I": return "int";
                case "J": return "long";
                case "F": return "float";
                case "D": return "double";
                default: return descriptor;
            }
        }
    }
    
    /**
     * Extracts return type from method descriptor
     */
    private String extractReturnType(String methodDescriptor) {
        // Method descriptor format: (params)ReturnType
        int closeParen = methodDescriptor.lastIndexOf(')');
        String returnDesc = methodDescriptor.substring(closeParen + 1);
        return extractTypeName(returnDesc);
    }
    
    /**
     * Extracts parameters from method descriptor
     */
    private String extractParameters(String methodDescriptor) {
        // Method descriptor format: (params)ReturnType
        int startParen = methodDescriptor.indexOf('(');
        int endParen = methodDescriptor.indexOf(')');
        String paramsDesc = methodDescriptor.substring(startParen + 1, endParen);
        
        StringBuilder params = new StringBuilder();
        int i = 0;
        int paramCount = 0;
        
        while (i < paramsDesc.length()) {
            if (paramCount > 0) {
                params.append(", ");
            }
            
            if (paramsDesc.charAt(i) == 'L') {
                // Object type
                int semicolonIndex = paramsDesc.indexOf(';', i);
                String className = paramsDesc.substring(i + 1, semicolonIndex);
                params.append("arg").append(paramCount).append(": ").append(extractClassName(className));
                i = semicolonIndex + 1;
            } else if (paramsDesc.charAt(i) == '[') {
                // Array type
                i++;
                while (i < paramsDesc.length() && paramsDesc.charAt(i) == '[') {
                    i++;
                }
                if (paramsDesc.charAt(i) == 'L') {
                    int semicolonIndex = paramsDesc.indexOf(';', i);
                    String className = paramsDesc.substring(i + 1, semicolonIndex);
                    params.append("arg").append(paramCount).append(": ").append(extractClassName(className)).append("[]");
                    i = semicolonIndex + 1;
                } else {
                    params.append("arg").append(paramCount).append(": ").append(extractTypeName(String.valueOf(paramsDesc.charAt(i)))).append("[]");
                    i++;
                }
            } else {
                // Primitive type
                params.append("arg").append(paramCount).append(": ").append(extractTypeName(String.valueOf(paramsDesc.charAt(i))));
                i++;
            }
            paramCount++;
        }
        
        return params.toString();
    }
    
    /**
     * Gets the access modifier symbol for PlantUML
     */
    private String getAccessModifier(int access) {
        if ((access & Opcodes.ACC_PUBLIC) != 0) {
            return "+ ";
        } else if ((access & Opcodes.ACC_PRIVATE) != 0) {
            return "- ";
        } else if ((access & Opcodes.ACC_PROTECTED) != 0) {
            return "# ";
        } else {
            return "~ ";  // package-private
        }
    }
}
