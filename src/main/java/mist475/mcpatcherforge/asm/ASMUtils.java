package mist475.mcpatcherforge.asm;

import java.util.Objects;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import mist475.mcpatcherforge.core.MCPatcherForgeCore;

/**
 * Convenience methods for a neater and saner core-modding experience
 */
public class ASMUtils {

    /**
     * Check the basic fields of nodes to see if they're equal
     * DOES NOT CHECK ALL FIELDS, CHECK BEFORE USING
     */
    public static boolean abstractInsnNodeEquals(AbstractInsnNode node1, AbstractInsnNode node2) {

        if (node1.getType() != node2.getType()) {
            return false;
        }

        // early return for efficiency
        if (node1.getOpcode() != node2.getOpcode()) {
            return false;
        }

        // check what type and properties
        // we only check what we need for proper comparison
        // missing some types, as I don't expect I'll need all for now
        switch (node1.getType()) {
            // only has opcode
            case AbstractInsnNode.INSN:
                return true;
            case AbstractInsnNode.INT_INSN:
                if (node1 instanceof IntInsnNode && node2 instanceof IntInsnNode) {
                    IntInsnNode intInsnNode1 = (IntInsnNode) node1;
                    IntInsnNode intInsnNode2 = (IntInsnNode) node2;
                    return intInsnNode1.operand == intInsnNode2.operand;
                }
                return false;
            case AbstractInsnNode.VAR_INSN:
                if (node1 instanceof VarInsnNode && node2 instanceof VarInsnNode) {
                    VarInsnNode varInsnNode1 = (VarInsnNode) node1;
                    VarInsnNode varInsnNode2 = (VarInsnNode) node2;
                    return varInsnNode1.var == varInsnNode2.var;
                }
                return false;
            case AbstractInsnNode.TYPE_INSN:
                if (node1 instanceof TypeInsnNode && node2 instanceof TypeInsnNode) {
                    TypeInsnNode typeInsnNode1 = (TypeInsnNode) node1;
                    TypeInsnNode typeInsnNode2 = (TypeInsnNode) node2;
                    return typeInsnNode1.desc.equals(typeInsnNode2.desc);
                }
                return false;
            case AbstractInsnNode.FIELD_INSN:
                if (node1 instanceof FieldInsnNode && node2 instanceof FieldInsnNode) {
                    FieldInsnNode fieldInsnNode1 = (FieldInsnNode) node1;
                    FieldInsnNode fieldInsnNode2 = (FieldInsnNode) node2;
                    return fieldInsnNode1.desc.equals(fieldInsnNode2.desc)
                        && fieldInsnNode1.name.equals(fieldInsnNode2.name)
                        && fieldInsnNode1.owner.equals(fieldInsnNode2.owner);
                }
                return false;
            // doesn't check itf
            case AbstractInsnNode.METHOD_INSN:
                if (node1 instanceof MethodInsnNode && node2 instanceof MethodInsnNode) {
                    MethodInsnNode methodInsnNode1 = (MethodInsnNode) node1;
                    MethodInsnNode methodInsnNode2 = (MethodInsnNode) node2;
                    return methodInsnNode1.desc.equals(methodInsnNode2.desc)
                        && methodInsnNode1.name.equals(methodInsnNode2.name)
                        && methodInsnNode1.owner.equals(methodInsnNode2.owner);
                }
                return false;
            // doesn't check bsm args
            case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
                if (node1 instanceof InvokeDynamicInsnNode && node2 instanceof InvokeDynamicInsnNode) {
                    InvokeDynamicInsnNode invokeDynamicInsnNode1 = (InvokeDynamicInsnNode) node1;
                    InvokeDynamicInsnNode invokeDynamicInsnNode2 = (InvokeDynamicInsnNode) node2;
                    return invokeDynamicInsnNode1.desc.equals(invokeDynamicInsnNode2.desc)
                        && invokeDynamicInsnNode1.name.equals(invokeDynamicInsnNode2.name)
                        && invokeDynamicInsnNode1.bsm.equals(invokeDynamicInsnNode2.bsm);
                }
                return false;
            // I expect this will still fail sometimes as Label doesn't override equals
            case AbstractInsnNode.JUMP_INSN:
                if (node1 instanceof JumpInsnNode && node2 instanceof JumpInsnNode) {
                    JumpInsnNode jumpInsnNode1 = (JumpInsnNode) node1;
                    JumpInsnNode jumpInsnNode2 = (JumpInsnNode) node2;
                    return jumpInsnNode1.label.getLabel()
                        .equals(jumpInsnNode2.label.getLabel());
                }
                return false;
            // Idem ditto
            case AbstractInsnNode.LABEL:
                if (node1 instanceof LabelNode && node2 instanceof LabelNode) {
                    LabelNode labelNode1 = (LabelNode) node1;
                    LabelNode labelNode2 = (LabelNode) node2;
                    return labelNode1.getLabel()
                        .equals(labelNode2.getLabel());
                }
                return false;
            case AbstractInsnNode.LDC_INSN:
                if (node1 instanceof LdcInsnNode && node2 instanceof LdcInsnNode) {
                    LdcInsnNode ldcInsnNode1 = (LdcInsnNode) node1;
                    LdcInsnNode ldcInsnNode2 = (LdcInsnNode) node2;
                    return ldcInsnNode1.cst.equals(ldcInsnNode2.cst);
                }
                return false;
            case AbstractInsnNode.IINC_INSN:
                if (node1 instanceof IincInsnNode && node2 instanceof IincInsnNode) {
                    IincInsnNode iincInsnNode1 = (IincInsnNode) node1;
                    IincInsnNode iincInsnNode2 = (IincInsnNode) node2;
                    return iincInsnNode1.incr == iincInsnNode2.incr && iincInsnNode1.var == iincInsnNode2.var;
                }
                return false;
            case AbstractInsnNode.FRAME:
                if (node1 instanceof FrameNode && node2 instanceof FrameNode) {
                    FrameNode frameNode1 = (FrameNode) node1;
                    FrameNode frameNode2 = (FrameNode) node2;
                    return frameNode1.type == frameNode2.type && Objects.equals(frameNode1.local, frameNode2.local)
                        && Objects.equals(frameNode1.stack, frameNode2.stack);
                }
                return false;
            case AbstractInsnNode.LINE:
                if (node1 instanceof LineNumberNode && node2 instanceof LineNumberNode) {
                    LineNumberNode lineNumberNode1 = (LineNumberNode) node1;
                    LineNumberNode lineNumberNode2 = (LineNumberNode) node2;
                    return lineNumberNode1.line == lineNumberNode2.line && lineNumberNode1.start.getLabel()
                        .equals(lineNumberNode2.start.getLabel());
                }
                return false;
            default:
                MCPatcherForgeCore.log.warn("Unchecked node found: " + node1.getClass());
                return node1.toString()
                    .equals(node2.toString());
        }
    }

    public static boolean matchesNodeSequence(AbstractInsnNode node, AbstractInsnNode... pattern) {
        AbstractInsnNode currentNode = node;

        for (AbstractInsnNode abstractInsnNode : pattern) {
            if (!abstractInsnNodeEquals(currentNode, abstractInsnNode)) {
                return false;
            }
            currentNode = currentNode.getNext();
            if (currentNode == null) {
                return false;
            }
        }

        return true;
    }
}
