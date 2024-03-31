package compiler;

import compiler.AST.*;
import compiler.lib.*;

import java.util.HashMap;
import java.util.Map;

public class TypeRels {

	// mapping id classe -> id super classe
	public static Map<String, String> superType = new HashMap<>();

	// valuta se il tipo "a" e' <= al tipo "b"
	public static boolean isSubtype(TypeNode a, TypeNode b) {
		return a.getClass().equals(b.getClass()) ||
				isPrimitiveSubType(a, b) ||
				isClassSubType(a, b);
	}

	private static boolean isPrimitiveSubType(TypeNode a, TypeNode b) {
		return (a instanceof BoolTypeNode) && (b instanceof IntTypeNode || b instanceof BoolTypeNode);
	}

	private static boolean isClassSubType(TypeNode a, TypeNode b) {
		return (a instanceof RefTypeNode &&
				b instanceof RefTypeNode &&
				superType((RefTypeNode) a, (RefTypeNode) b)) ||
					(a instanceof EmptyTypeNode &&
					b instanceof RefTypeNode);
	}

	public static TypeNode lowestCommonAncestor(TypeNode a, TypeNode b) {
		if (a instanceof EmptyTypeNode &&
				(b instanceof EmptyTypeNode || b instanceof RefTypeNode))
			return b;
		if (b instanceof EmptyTypeNode && a instanceof RefTypeNode)
			return a;
		if (a instanceof RefTypeNode &&
				b instanceof RefTypeNode) {
			return lowestCommonAncestorOfRefTypes((RefTypeNode) a, (RefTypeNode) b);
		}
		if ((a instanceof BoolTypeNode || a instanceof IntTypeNode) &&
				(b instanceof BoolTypeNode || b instanceof IntTypeNode)) {
			if (a instanceof BoolTypeNode && b instanceof BoolTypeNode) {
				return new BoolTypeNode();
			}
			return new IntTypeNode();
		}
		return null;
	}

	private static RefTypeNode lowestCommonAncestorOfRefTypes(RefTypeNode a, RefTypeNode b) {
		if(isSubtype(b, a)) {
			return b;
		} else if (superType.containsKey(a.classId)) {
			lowestCommonAncestor(new RefTypeNode(superType.get(a.classId)), b);
		}
		return null;
	}

	public static boolean superType(RefTypeNode subClass, RefTypeNode superClass) {
		return superType.get(subClass.classId).equals(superClass.classId);
	}

}
