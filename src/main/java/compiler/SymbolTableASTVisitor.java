package compiler;

import java.util.*;
import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;

public class SymbolTableASTVisitor extends BaseASTVisitor<Void,VoidException> {
	
	private List<Map<String, STentry>> symTable = new ArrayList<>();
	private Map<String, Map<String, STentry>> classTable = new HashMap<>();
	private int nestingLevel=0; // current nesting level
	private int decOffset=-2; // counter for offset of local declarations at current nesting level 
	int stErrors=0;

	SymbolTableASTVisitor() {}
	SymbolTableASTVisitor(boolean debug) {super(debug);} // enables print for debugging

	private STentry stLookup(String id) {
		int j = nestingLevel;
		STentry entry = null;
		while (j >= 0 && entry == null) 
			entry = symTable.get(j--).get(id);	
		return entry;
	}

	@Override
	public Void visitNode(ProgLetInNode n) {
		if (print) printNode(n);
		Map<String, STentry> hm = new HashMap<>();
		symTable.add(hm);
	    for (Node dec : n.declist) visit(dec);
		visit(n.exp);
		symTable.remove(0);
		return null;
	}

	@Override
	public Void visitNode(ProgNode n) {
		if (print) printNode(n);
		visit(n.exp);
		return null;
	}
	
	@Override
	public Void visitNode(FunNode n) {
		if (print) printNode(n);
		Map<String, STentry> hm = symTable.get(nestingLevel);
		List<TypeNode> parTypes = new ArrayList<>();  
		for (ParNode par : n.parlist) parTypes.add(par.getType()); 
		STentry entry = new STentry(nestingLevel, new ArrowTypeNode(parTypes,n.retType),decOffset--);
		//inserimento di ID nella symtable
		if (hm.put(n.id, entry) != null) {
			System.out.println("Method id " + n.id + " at line "+ n.getLine() +" already declared");
			stErrors++;
		} 
		//creare una nuova hashmap per la symTable
		nestingLevel++;
		Map<String, STentry> hmn = new HashMap<>();
		symTable.add(hmn);
		int prevNLDecOffset=decOffset; // stores counter for offset of declarations at previous nesting level 
		decOffset=-2;
		
		int parOffset=1;
		for (ParNode par : n.parlist)
			if (hmn.put(par.id, new STentry(nestingLevel,par.getType(),parOffset++)) != null) {
				System.out.println("Par id " + par.id + " at line "+ n.getLine() +" already declared");
				stErrors++;
			}
		for (Node dec : n.declist) visit(dec);
		visit(n.exp);
		//rimuovere la hashmap corrente poiche' esco dallo scope               
		symTable.remove(nestingLevel--);
		decOffset=prevNLDecOffset; // restores counter for offset of declarations at previous nesting level 
		return null;
	}
	
	@Override
	public Void visitNode(VarNode n) {
		if (print) printNode(n);
		visit(n.exp);
		Map<String, STentry> hm = symTable.get(nestingLevel);
		STentry entry = new STentry(nestingLevel,n.getType(),decOffset--);
		//inserimento di ID nella symtable
		if (hm.put(n.id, entry) != null) {
			System.out.println("Var id " + n.id + " at line "+ n.getLine() +" already declared");
			stErrors++;
		}
		return null;
	}

	@Override
	public Void visitNode(PrintNode n) {
		if (print) printNode(n);
		visit(n.exp);
		return null;
	}

	@Override
	public Void visitNode(IfNode n) {
		if (print) printNode(n);
		visit(n.cond);
		visit(n.th);
		visit(n.el);
		return null;
	}

	@Override
	public Void visitNode(NotNode n) {
		if (print) printNode(n);
		visit(n.arg);
		return null;
	}

	@Override
	public Void visitNode(AndNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(OrNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(EqualNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(LessEqualNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(GreaterEqualNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}
	
	@Override
	public Void visitNode(TimesNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(DivNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}
	
	@Override
	public Void visitNode(PlusNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(MinusNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(CallNode n) {
		if (print) printNode(n);
		STentry entry = stLookup(n.id);
		if (entry == null) {
			System.out.println("Fun id " + n.id + " at line "+ n.getLine() + " not declared");
			stErrors++;
		} else {
			n.entry = entry;
			n.nl = nestingLevel;
		}
		for (Node arg : n.arglist) visit(arg);
		return null;
	}

	@Override
	public Void visitNode(IdNode n) {
		if (print) printNode(n);
		STentry entry = stLookup(n.id);
		if (entry == null) {
			System.out.println("Var or Par id " + n.id + " at line "+ n.getLine() + " not declared");
			stErrors++;
		} else {
			n.entry = entry;
			n.nl = nestingLevel;
		}
		return null;
	}

	@Override
	public Void visitNode(BoolNode n) {
		if (print) printNode(n, n.val.toString());
		return null;
	}

	@Override
	public Void visitNode(IntNode n) {
		if (print) printNode(n, n.val.toString());
		return null;
	}

	@Override
	public Void visitNode(ClassNode n) throws VoidException {
		if (print) printNode(n);
		//nella Symbol Table (livello 0) viene aggiunto il nome
		//della classe mappato ad una nuova STentry
		Map<String, STentry> globalScope = symTable.get(0);
		boolean hasSuperClass = n.superId != null;
		ClassTypeNode classType = new ClassTypeNode(new ArrayList<>(), new ArrayList<>());
		if (hasSuperClass) {
			if (globalScope.get(n.superId) != null &&
				classTable.containsKey(n.superId)) {
				classType = new ClassTypeNode(
						((ClassTypeNode) globalScope.get(n.superId).type).fieldTypes,
						((ClassTypeNode) globalScope.get(n.superId).type).methodTypes
				);
			} else {
				System.out.println("Class " + n.superId + " at line " + n.getLine() + " not declared");
				stErrors++;
			}
		}
		globalScope.put(n.id, new STentry(0, classType, decOffset--));
		//nella Class Table viene aggiunto il nome della classe
		//mappato ad una nuova Virtual Table
		Map<String, STentry> virtualTable = new HashMap<>();
		if (hasSuperClass) {
			virtualTable.putAll(classTable.get(n.superId));
		}
		classTable.put(n.id, virtualTable);
		//viene creato un nuovo livello per la Symbol Table
		symTable.add(virtualTable);
		nestingLevel++;
		int prevNLDecOffset=decOffset; // stores counter for offset of declarations at previous nesting level
		decOffset=-2;

		//visito tutti i campi all'interno della classe
		int fieldsOffset=-classType.fieldTypes.size()-1;
		for (FieldNode field : n.fields) {
			int fieldOffset;
			//controllo se il campo esiste già nella super classe
			if (hasSuperClass && virtualTable.containsKey(field.id)) {
				// controllo se sto cercando di sovrascrivere un metodo
				if (virtualTable.get(field.id).type instanceof MethodTypeNode) {
					System.out.println("Field id " + field.id + " at line " + n.getLine() + " cannot override method");
					stErrors++;
				} else {
					//uso il riferimento della super classe, sovrascrivendolo
					fieldOffset = virtualTable.get(field.id).offset;
					virtualTable.put(field.id, new STentry(nestingLevel, field.getType(), fieldOffset));
					classType.fieldTypes.set(-fieldOffset-1, field.getType());
				}
			} else {
				// creo un nuovo riferimento al campo
				fieldOffset = fieldsOffset--;
				if (virtualTable.put(field.id, new STentry(nestingLevel, field.getType(), fieldOffset)) != null) {
					System.out.println("Field id " + field.id + " at line " + n.getLine() + " already declared");
					stErrors++;
				} else {
					classType.fieldTypes.add(-fieldOffset-1, field.getType());
				}
			}
		}
		//visito tutti i metodi all'interno della classe
		for (MethodNode method : n.methods) {
			visit(method);
			classType.methodTypes.add(method.offset, method.getType());
		}
		//rimuovere la hashmap corrente poiche' esco dallo scope
		symTable.remove(nestingLevel--);
		decOffset=prevNLDecOffset; // restores counter for offset of declarations at previous nesting level
		return null;
	}

	@Override
	public Void visitNode(FieldNode n) throws VoidException {
		if (print) printNode(n);
		return null;
	}

	@Override
	public Void visitNode(MethodNode n) throws VoidException {
		if (print) printNode(n);
		Map<String, STentry> hm = symTable.get(nestingLevel);
		List<TypeNode> parTypes = new ArrayList<>();
		for (ParNode par : n.parlist) parTypes.add(par.getType());
		if (hm.containsKey(n.id)) {
			if (hm.get(n.id).type instanceof MethodTypeNode) {
				hm.put(n.id, new STentry(
						nestingLevel,
						new MethodTypeNode(new ArrowTypeNode(parTypes, n.retType)),
						hm.get(n.id).offset)
				);
			} else {
				System.out.println("Method id " + n.id + " at line "+ n.getLine() +" cannot override field");
				stErrors++;
			}
		} else {
			hm.put(n.id, new STentry(
					nestingLevel,
					new MethodTypeNode(new ArrowTypeNode(parTypes,n.retType)),
					decOffset--)
			);
		}
		//creare una nuova hashmap per la symTable
		nestingLevel++;
		Map<String, STentry> hmn = new HashMap<>();
		symTable.add(hmn);
		int prevNLDecOffset=decOffset; // stores counter for offset of declarations at previous nesting level
		decOffset=-2;

		int parOffset=1;
		for (ParNode par : n.parlist)
			if (hmn.put(par.id, new STentry(nestingLevel,par.getType(),parOffset++)) != null) {
				System.out.println("Par id " + par.id + " at line "+ n.getLine() +" already declared");
				stErrors++;
			}
		for (Node dec : n.declist) visit(dec);
		visit(n.exp);
		//rimuovere la hashmap corrente poiche' esco dallo scope
		symTable.remove(nestingLevel--);
		decOffset=prevNLDecOffset; // restores counter for offset of declarations at previous nesting level
		return null;
	}

	@Override
	public Void visitNode(ClassCallNode n) throws VoidException {
		if (print) printNode(n);
		STentry entry = stLookup(n.classId);
		RefTypeNode refType;
		if (entry == null) {
			System.out.println("Class id " + n.classId + " at line "+ n.getLine() + " not declared");
			stErrors++;
		} else if (!(entry.type instanceof RefTypeNode)) {
			System.out.println("Class id " + n.classId + " at line "+ n.getLine() + " not ref type");
			stErrors++;
		} else {
			refType = (RefTypeNode) entry.type;
			n.entry = entry;
			n.nestingLevel = nestingLevel;
			STentry methodEntry = classTable.get(refType.classId).get(n.methodId);
			if (methodEntry == null) {
				System.out.println("Method id " + n.methodId + " at line "+ n.getLine() + " not declared for class " + n.classId);
				stErrors++;
			} else {
				n.methodEntry = methodEntry;
			}
		}
		for (Node arg : n.args) visit(arg);
		return null;
	}

	@Override
	public Void visitNode(NewNode n) throws VoidException {
		if (print) printNode(n);
		if (!classTable.containsKey(n.classId) ||
			!symTable.get(0).containsKey(n.classId)) {
			System.out.println("Class id " + n.classId + " at line "+ n.getLine() +" not declared");
			stErrors++;
		}
		n.entry = symTable.get(0).get(n.classId);
		for (Node arg : n.args) visit(arg);
		return null;
	}

	@Override
	public Void visitNode(EmptyNode n) throws VoidException {
		if (print) printNode(n);
		return null;
	}
}
