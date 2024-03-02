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

	public static boolean superType(RefTypeNode subClass, RefTypeNode superClass) {
		return superType.get(subClass.classId).equals(superClass.classId);
	}

}
