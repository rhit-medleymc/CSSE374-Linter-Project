package domain;

import datastorage.ASMReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Detects a simple form of the Strategy pattern.
 *
 * Heuristics used:
 * - The class invokes methods on an interface type and uses a non-void return value from that call.
 * - The same class either (a) constructs two or more different concrete implementors of that
 *   interface or (b) has a field/array whose element type is the interface or a concrete implementor.
 *
 * This is intentionally conservative (avoids false positives) and reports a candidate when both
 * an interface-method-result is used and multiple concrete implementors for that interface are
 * present/constructed by the class.
 */
public class StrategyPatternLinter extends AbstractASMLinter {

    private final Map<String, Set<String>> interfaceToImplementors = new HashMap<>();
    private final Set<String> internalClassNames = new HashSet<>();

    public StrategyPatternLinter() {
        super();
    }

    public StrategyPatternLinter(ASMReader asmReader) {
        super(asmReader);
    }

    @Override
    protected void preProcessClasses(List<ClassNode> classes) {
        interfaceToImplementors.clear();
        internalClassNames.clear();

        for (ClassNode cn : classes) {
            internalClassNames.add(cn.name);
        }

        for (ClassNode cn : classes) {
            // Collect implementors for declared interfaces
            @SuppressWarnings("unchecked")
            List<String> ifaces = (List<String>) cn.interfaces;
            for (String iface : ifaces) {
                interfaceToImplementors.computeIfAbsent(iface, k -> new TreeSet<>()).add(cn.name);
            }
        }
    }

    @Override
    protected int lintClass(ClassNode classNode, List<ClassNode> allClasses, StringBuilder result) {
        StrategyCandidate candidate = analyzeClass(classNode);
        if (candidate.isStrategy) {
            appendCandidate(result, candidate);
            return 1;
        }
        return 0;
    }

    private StrategyCandidate analyzeClass(ClassNode classNode) {
        Set<String> constructedTypes = new TreeSet<>();
        Set<String> fieldTypes = collectFieldTypes(classNode);

        // Map: interface -> whether a call on that interface returned a value that appears to be used
        Map<String, Boolean> interfaceReturnUsed = new HashMap<>();

        @SuppressWarnings("unchecked")
        List<MethodNode> methods = (List<MethodNode>) classNode.methods;
        for (MethodNode method : methods) {
            if (isSkippableMethod(method)) {
                continue;
            }

            for (AbstractInsnNode instruction = method.instructions.getFirst();
                 instruction != null; instruction = instruction.getNext()) {

                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode mi = (MethodInsnNode) instruction;

                    // Record constructions (invoked constructor)
                    if ("<init>".equals(mi.name) && internalClassNames.contains(mi.owner)) {
                        constructedTypes.add(mi.owner);
                    }

                    // If the called owner is a known interface, check return usage
                    if (interfaceToImplementors.containsKey(mi.owner)) {
                        boolean returnUsed = false;
                        Type ret = Type.getReturnType(mi.desc);
                        if (ret.getSort() != Type.VOID) {
                            AbstractInsnNode next = instruction.getNext();
                            if (next != null) {
                                int op = next.getOpcode();
                                // If the immediate next opcode is POP/POP2 we assume the return is discarded
                                if (op != Opcodes.POP && op != Opcodes.POP2) {
                                    returnUsed = true;
                                }
                            } else {
                                // no following instruction -> assume not used
                                returnUsed = false;
                            }
                        }

                        interfaceReturnUsed.merge(mi.owner, returnUsed, (a, b) -> a || b);
                    }
                }
            }
        }

        // Decide candidate: for some interface, there must be >=2 concrete implementors either
        // constructed by the class or present as field types, AND the class must call an interface
        // method and use its return value.
        for (Map.Entry<String, Set<String>> e : interfaceToImplementors.entrySet()) {
            String iface = e.getKey();
            Set<String> implementors = e.getValue();

            // how many implementors does this class construct?
            Set<String> constructedMatch = new TreeSet<>();
            for (String c : constructedTypes) {
                if (implementors.contains(c)) {
                    constructedMatch.add(c);
                }
            }

            // how many implementors are present as fields?
            Set<String> fieldMatch = new TreeSet<>();
            for (String f : fieldTypes) {
                if (implementors.contains(f) || f.equals(iface)) {
                    fieldMatch.add(f);
                }
            }

            boolean multipleImplsPresent = constructedMatch.size() >= 2 || fieldMatch.size() >= 2
                    || (constructedMatch.size() >= 1 && fieldMatch.size() >= 1);
            boolean callsAndUsesReturn = interfaceReturnUsed.getOrDefault(iface, false);

            if (multipleImplsPresent && callsAndUsesReturn) {
                // Report the interface and the concrete implementors we observed
                Set<String> observedImpls = new TreeSet<>();
                observedImpls.addAll(constructedMatch);
                observedImpls.addAll(fieldMatch);
                return new StrategyCandidate(classNode.name, iface, observedImpls, true);
            }
        }

        return new StrategyCandidate(classNode.name, null, Set.of(), false);
    }

    private boolean isSkippableMethod(MethodNode method) {
        if ((method.access & Opcodes.ACC_ABSTRACT) != 0) {
            return true;
        }
        if ((method.access & Opcodes.ACC_SYNTHETIC) != 0) {
            return true;
        }
        return (method.access & Opcodes.ACC_BRIDGE) != 0;
    }

    private Set<String> collectFieldTypes(ClassNode classNode) {
        Set<String> types = new TreeSet<>();
        @SuppressWarnings("unchecked")
        List<FieldNode> fields = (List<FieldNode>) classNode.fields;
        for (FieldNode field : fields) {
            Type t = Type.getType(field.desc);
            collectObjectTypes(t, types);
        }
        return types;
    }

    private void collectObjectTypes(Type t, Set<String> types) {
        if (t.getSort() == Type.OBJECT) {
            types.add(t.getInternalName());
            return;
        }
        if (t.getSort() == Type.ARRAY) {
            collectObjectTypes(t.getElementType(), types);
        }
    }

    private void appendCandidate(StringBuilder result, StrategyCandidate candidate) {
        result.append("Class: ").append(candidate.className.replace('/', '.')).append(System.lineSeparator());
        result.append("  Interface: ").append(candidate.interfaceName.replace('/', '.')).append(System.lineSeparator());
        if (!candidate.implementations.isEmpty()) {
            result.append("  Implementations observed: ")
                    .append(candidate.implementations.stream().map(s -> s.replace('/', '.')).collect(Collectors.joining(", ")))
                    .append(System.lineSeparator());
        }
    }

    @Override
    protected String getNoViolationsMessage() {
        return "No strategy pattern candidates found.";
    }

    @Override
    protected String getViolationsMessage(int violationCount, StringBuilder result) {
        return "Total strategy pattern candidates: " + violationCount + System.lineSeparator() + result.toString();
    }

    private static class StrategyCandidate {
        private final String className;
        private final String interfaceName;
        private final Set<String> implementations;
        private final boolean isStrategy;

        private StrategyCandidate(String className, String interfaceName, Set<String> implementations, boolean isStrategy) {
            this.className = className;
            this.interfaceName = interfaceName == null ? "" : interfaceName;
            this.implementations = implementations;
            this.isStrategy = isStrategy;
        }
    }
}
