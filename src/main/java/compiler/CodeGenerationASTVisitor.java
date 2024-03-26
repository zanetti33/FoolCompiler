package compiler;

import compiler.AST.*;
import compiler.lib.*;
import compiler.exc.*;
import svm.ExecuteVM;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static compiler.lib.FOOLlib.*;

public class CodeGenerationASTVisitor extends BaseASTVisitor<String, VoidException> {

	private final List<List<String>> dispatchTables = new ArrayList<>();
	CodeGenerationASTVisitor() {}
	CodeGenerationASTVisitor(boolean debug) {super(false,debug);} //enables print for debugging

	@Override
	public String visitNode(ProgLetInNode n) {
		if (print) printNode(n);
		String declCode = null;
		for (Node dec : n.declist) declCode=nlJoin(declCode,visit(dec));
		return nlJoin(
			"push 0",	
			declCode, // generate code for declarations (allocation)			
			visit(n.exp),
			"halt",
			getCode()
		);
	}

	@Override
	public String visitNode(ProgNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.exp),
			"halt"
		);
	}

	@Override
	public String visitNode(FunNode n) {
		if (print) printNode(n,n.id);
		String declCode = null, popDecl = null, popParl = null;
		for (Node dec : n.declist) {
			declCode = nlJoin(declCode,visit(dec));
			popDecl = nlJoin(popDecl,"pop");
		}
		for (int i=0;i<n.parlist.size();i++) popParl = nlJoin(popParl,"pop");
		String funl = freshFunLabel();
		putCode(
			nlJoin(
				funl+":",
				"cfp", // set $fp to $sp value
				"lra", // load $ra value
				declCode, // generate code for local declarations (they use the new $fp!!!)
				visit(n.exp), // generate code for function body expression
				"stm", // set $tm to popped value (function result)
				popDecl, // remove local declarations from stack
				"sra", // set $ra to popped value
				"pop", // remove Access Link from stack
				popParl, // remove parameters from stack
				"sfp", // set $fp to popped value (Control Link)
				"ltm", // load $tm value (function result)
				"lra", // load $ra value
				"js"  // jump to to popped address
			)
		);
		return "push "+funl;		
	}

	@Override
	public String visitNode(VarNode n) {
		if (print) printNode(n,n.id);
		return visit(n.exp);
	}

	@Override
	public String visitNode(PrintNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.exp),
			"print"
		);
	}

	@Override
	public String visitNode(IfNode n) {
		if (print) printNode(n);
		String l1 = freshLabel();
	 	String l2 = freshLabel();		
		return nlJoin(
			visit(n.cond),
			"push 1",
			"beq "+l1,
			visit(n.el),
			"b "+l2,
			l1+":",
			visit(n.th),
			l2+":"
		);
	}

	@Override
	public String visitNode(NotNode n) {
		if (print) printNode(n);
		String l1 = freshLabel();
		String l2 = freshLabel();
		return nlJoin(
				"push 0",
				visit(n.arg),
				"beq "+l1,
				"push 0",
				"b "+l2,
				l1+":",
				"push 1",
				l2+":"
		);
	}

	@Override
	public String visitNode(EqualNode n) {
		if (print) printNode(n);
		String l1 = freshLabel();
	 	String l2 = freshLabel();
		return nlJoin(
			visit(n.left),
			visit(n.right),
			"beq "+l1,
			"push 0",
			"b "+l2,
			l1+":",
			"push 1",
			l2+":"
		);
	}

	@Override
	public String visitNode(AndNode n) {
		if (print) printNode(n);
		String itsFalse = freshLabel();
		String itsTrue = freshLabel();
		return nlJoin(
				"push 0",
				visit(n.left),
				"beq "+itsFalse,
				"push 0",
				visit(n.right),
				"beq "+itsFalse,
				"push 1",
				"b "+itsTrue,
				itsFalse+":",
				"push 0",
				itsTrue+":"
		);
	}

	@Override
	public String visitNode(OrNode n) {
		if (print) printNode(n);
		String firstIsFalse = freshLabel();
		String itsFalse = freshLabel();
		String itsTrue = freshLabel();
		return nlJoin(
				"push 0",
				visit(n.left),
				"beq "+firstIsFalse,
				"push 1",
				"b "+itsTrue,
				firstIsFalse+":",
				"push 0",
				visit(n.right),
				"beq "+itsFalse,
				"push 1",
				"b "+itsTrue,
				itsFalse+":",
				"push 0",
				itsTrue+":"
		);
	}

	// a>=b -> a==b || ! (a <= b)
	@Override
	public String visitNode(GreaterEqualNode n) {
		if (print) printNode(n);
		return visitNode(
				new OrNode(
						new EqualNode(n.left, n.right),
						new NotNode(
								new LessEqualNode(n.left, n.right)
						)
				)
		);
	}

	@Override
	public String visitNode(LessEqualNode n) {
		if (print) printNode(n);
		String l1 = freshLabel();
		String l2 = freshLabel();
		return nlJoin(
				visit(n.left),
				visit(n.right),
				"bleq "+l1,
				"push 0",
				"b "+l2,
				l1+":",
				"push 1",
				l2+":"
		);
	}

	@Override
	public String visitNode(TimesNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.left),
			visit(n.right),
			"mult"
		);	
	}

	@Override
	public String visitNode(DivNode n) {
		if (print) printNode(n);
		return nlJoin(
				visit(n.left),
				visit(n.right),
				"div"
		);
	}

	@Override
	public String visitNode(PlusNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.left),
			visit(n.right),
			"add"				
		);
	}

	@Override
	public String visitNode(MinusNode n) {
		if (print) printNode(n);
		return nlJoin(
				visit(n.left),
				visit(n.right),
				"sub"
		);
	}

	@Override
	public String visitNode(CallNode n) {
		if (print) printNode(n,n.id);
		String argCode = null, getAR = null;
		for (int i=n.arglist.size()-1;i>=0;i--) argCode=nlJoin(argCode,visit(n.arglist.get(i)));
		for (int i = 0;i<n.nl-n.entry.nl;i++) getAR=nlJoin(getAR,"lw");
		if (n.entry.type instanceof MethodTypeNode) {
			return nlJoin(
					"lfp", // load Control Link (pointer to frame of function "id" caller)
					argCode, // generate code for argument expressions in reversed order
					"lfp", getAR, // retrieve address of frame containing "id" declaration
					// by following the static chain (of Access Links)
					"stm", // set $tm to popped value (with the aim of duplicating top of stack)
					"ltm", // load Access Link (pointer to frame of function "id" declaration)
					"ltm", // duplicate top of stack
					"lw", // salvo l'indirizzo del metodo
					"push "+n.entry.offset, "add", // compute address of "id" declaration
					"lw", // load address of "id" function
					"js"  // jump to popped address (saving address of subsequent instruction in $ra)
			);
		} else {
			return nlJoin(
					"lfp", // load Control Link (pointer to frame of function "id" caller)
					argCode, // generate code for argument expressions in reversed order
					"lfp", getAR, // retrieve address of frame containing "id" declaration
					// by following the static chain (of Access Links)
					"stm", // set $tm to popped value (with the aim of duplicating top of stack)
					"ltm", // load Access Link (pointer to frame of function "id" declaration)
					"ltm", // duplicate top of stack
					"push "+n.entry.offset, "add", // compute address of "id" declaration
					"lw", // load address of "id" function
					"js"  // jump to popped address (saving address of subsequent instruction in $ra)
			);
		}
	}

	@Override
	public String visitNode(IdNode n) {
		if (print) printNode(n,n.id);
		String getAR = null;
		for (int i = 0;i<n.nl-n.entry.nl;i++) getAR=nlJoin(getAR,"lw");
		return nlJoin(
			"lfp", getAR, // retrieve address of frame containing "id" declaration
			              // by following the static chain (of Access Links)
			"push "+n.entry.offset, "add", // compute address of "id" declaration
			"lw" // load value of "id" variable
		);
	}

	@Override
	public String visitNode(BoolNode n) {
		if (print) printNode(n,n.val.toString());
		return "push "+(n.val?1:0);
	}

	@Override
	public String visitNode(IntNode n) {
		if (print) printNode(n,n.val.toString());
		return "push "+n.val;
	}

	@Override
	public String visitNode(ClassNode n) {
		if (print) printNode(n,n.id);
		// aggiundo alla dispatch table la classe con relativi metodi
		List<String> dispatchTable = new ArrayList<>();
		String loadMethodsInHeap = "";
		for (MethodNode method : n.methods) {
			visit(method);
			dispatchTable.add(method.label);
			// salvo l'id sull'heap e incremento il pointer, per ogni metodo
			loadMethodsInHeap = nlJoin(
					loadMethodsInHeap,
					"push " + method.label,
					"lhp",
					"sw",
					"lhp",
					"push 1",
					"add",
					"shp"
			);
		}
		dispatchTables.add(dispatchTable);
		return nlJoin(
				"lhp",
				loadMethodsInHeap
		);
	}

	@Override
	public String visitNode(MethodNode n) {
		if (print) printNode(n,n.id);
		String declCode = null, popDecl = null, popParl = null;
		for (Node dec : n.declist) {
			declCode = nlJoin(declCode,visit(dec));
			popDecl = nlJoin(popDecl,"pop");
		}
		for (int i=0;i<n.parlist.size();i++) popParl = nlJoin(popParl,"pop");
		String methodLabel = freshLabel();
		n.label = methodLabel;
		putCode(
				nlJoin(
						methodLabel+":",
						"cfp", // set $fp to $sp value
						"lra", // load $ra value
						declCode, // generate code for local declarations (they use the new $fp!!!)
						visit(n.exp), // generate code for function body expression
						"stm", // set $tm to popped value (function result)
						popDecl, // remove local declarations from stack
						"sra", // set $ra to popped value
						"pop", // remove Access Link from stack
						popParl, // remove parameters from stack
						"sfp", // set $fp to popped value (Control Link)
						"ltm", // load $tm value (function result)
						"lra", // load $ra value
						"js"  // jump to to popped address
				)
		);
		return null;
	}

	@Override
	public String visitNode(ClassCallNode n) throws VoidException {
		if (print) printNode(n, n.classId + "." + n.methodId + " at nesting level: " + n.nestingLevel);
		String argCode = null, getAR = null;
		for (int i=n.args.size()-1;i>=0;i--) argCode=nlJoin(argCode,visit(n.args.get(i)));
		for (int i = 0;i<n.nestingLevel-n.entry.nl;i++) getAR=nlJoin(getAR,"lw");
		String controlLinkAndMethodValues = nlJoin(
				"lfp", // load Control Link (pointer to frame of function "id" caller)
				argCode, // generate code for argument expressions in reversed order
				"lfp", getAR // retrieve address of frame containing "id" declaration
		);
		String loadObjectPointer = nlJoin(
				"push "+n.entry.offset,
				"add", // compute address of "id" declaration
				"lw" // load value of "id" variable
		);
		return nlJoin(
				controlLinkAndMethodValues,
				loadObjectPointer,
				"stm", //setto access link
				"ltm", //poi lo carico
				"ltm", //e lo copio
				"lw",
				"push "+n.methodEntry.offset,
				"add", //calcolo l'offset del metodo
				"lw", //carico l'indirizzo
				"js" //ci vado
		);

	}

	@Override
	public String visitNode(NewNode n) throws VoidException {
		if (print) printNode(n, n.classId);
		String argCode = null;
		for (int i=0;i<n.args.size();i++) argCode=nlJoin(argCode,visit(n.args.get(i)));
		String loadValuesInHeap = "";
		for (int i=0;i<n.args.size();i++)
			loadValuesInHeap = nlJoin(
					loadValuesInHeap,
					"lhp", //metto su stack hp
					"sw", //salvo in memoria il valore dell'argomento
					"lhp",
					"push 1",
					"add",
					"shp" //aumento hp di uno
			);
		String loadDispatchPointer = nlJoin(
				"push " + (ExecuteVM.MEMSIZE + n.entry.offset),
				"lw", //prendo l'indirizzo
				"lhp", //heap pointer
				"sw", //carico indirizzo sul heap
				"lhp", //metto sullo stack hp
				"lhp",
				"push 1",
				"add",
				"shp" //aumento hp di uno
		);
		return nlJoin(
				argCode,
				loadValuesInHeap,
				loadDispatchPointer
		);
	}

	@Override
	public String visitNode(EmptyNode n) throws VoidException {
		if (print) printNode(n);
		return "push -1";
	}
}