package visitor;

import syntaxtree.*;

import errorMsg.*;
import java.io.*;

public class CG3Visitor extends ASTvisitor {

	// the purpose here is to annotate things with their offsets:
	// - formal parameters, with respect to the (callee) frame
	// - local variables, with respect to the frame
	// - instance variables, with respect to their slot in the object
	// - methods, with respect to their slot in the v-table
	// - while statements, with respect to the stack-size at the time
	//   of loop-exit
	
	// Error message object
	ErrorMsg errorMsg;
	
	// IO stream to which we will emit code
	CodeStream code;

	// current stack height
	int stackHeight;
	
	// for constant evaluation
	ConstEvalVisitor conEvalVis;
	
	public CG3Visitor(ErrorMsg e, PrintStream out) {
		errorMsg = e;
		initInstanceVars(out);
		conEvalVis = new ConstEvalVisitor();
	}
	
	private void initInstanceVars(PrintStream out) {
		code = new CodeStream(out, errorMsg);
		stackHeight = 0;
	}
	
	
	public Object visitIntegerLiteral(IntegerLiteral n) {
		stackHeight += 8;
		code.emit(n, "subu $sp, $sp, 8");
		code.emit(n, "sw $s5, 4($sp)");
		code.emit(n, "li $t0, " + n.val);
		code.emit(n, "sw $t0, ($sp)");
		return null;
	}
	
	public Object visitNull(Null n){
		stackHeight +=4;
		code.emit(n, "subu $sp, $sp, 4");
		code.emit(n, "sw $zero, ($sp)");
		return null;
	}
	
	public Object visitTrue(True n){
		stackHeight +=4;
		code.emit(n, "subu $sp, $sp, 4");
		code.emit(n, "li $t0, 1");
		code.emit(n, "sw $t0, ($sp)"); 
		return null;
	}
	
	public Object visitFalse(False n){
		stackHeight +=4;
		code.emit(n, "subu $sp, $sp, 4");
		code.emit(n, "sw $zero, ($sp)"); 
		return null;
	}
	
	public Object visitStringLiteral(StringLiteral n) {
		stackHeight += 4;
		code.emit(n, "subu $sp, $sp, 4");
		code.emit(n, "la $t0, strLit_" + n.uniqueId);
		code.emit(n, "sw $t0, ($sp)");
		return null;
	}
	
	public Object visitThis(This n){
		stackHeight += 4;
		code.emit(n, "subu $sp, $sp, 4");
		code.emit(n, "sw $s2, ($sp)");
		return null;
	}
	
	public Object visitSuper(Super n){
		stackHeight += 4;
		code.emit(n, "subu $sp, $sp, 4");
		code.emit(n, "sw $s2, ($sp)");
		return null;
	}
	
	public Object visitIdentifierExp(IdentifierExp n){ // done
		if(n.link instanceof InstVarDecl){
			code.emit(n, "lw $t0, " + n.link.offset + "($s2)"); /////////////////////////////////
		}
		else{ ////// (n.link instanceof LocalVarDecl)
			int stackDepth = stackHeight + n.link.offset;
			code.emit(n, "lw $t0, " + stackDepth + "($sp)");
		}
		if(n.type instanceof IntegerType){
			stackHeight += 8;
			code.emit(n, "subu $sp, $sp, 8");
			code.emit(n, "sw $s5, 4($sp)");
			code.emit(n, "sw $t0, ($sp)");
		}
		else{
			stackHeight += 4;
			code.emit(n, "subu $sp, $sp, 4");
			code.emit(n, "sw $t0, ($sp)");
		}
		return null;
	}
	
	public Object visitNot(Not n){
		n.exp.accept(this);
		code.emit(n, "lw $t0, ($sp)");
		code.emit(n, "xor $t0, $t0, 1");
		code.emit(n, "sw $t0, ($sp)");
		return null;
	}
	
	public Object visitPlus(Plus n) {
		n.left.accept(this);
		n.right.accept(this);
		stackHeight -= 8;
		code.emit(n, "lw $t0, ($sp)");
		code.emit(n, "lw $t1, 8($sp)");
		code.emit(n, "addu $t0, $t0, $t1");
		code.emit(n, "addu $sp, $sp, 8");
		code.emit(n, "sw $t0, ($sp)");
		return null;
	}
	
	public Object visitMinus(Minus n) {
		n.left.accept(this);
		n.right.accept(this);
		stackHeight -= 8;
		code.emit(n, "lw $t0, ($sp)");
		code.emit(n, "lw $t1, 8($sp)");
		code.emit(n, "subu $t0, $t1, $t0");
		code.emit(n, "addu $sp, $sp, 8");
		code.emit(n, "sw $t0, ($sp)");
		return null;
	}
	
	public Object visitTimes(Times n) {
		n.left.accept(this);
		n.right.accept(this);
		stackHeight -= 8;
		code.emit(n, "lw $t0, ($sp)");
		code.emit(n, "lw $t1, 8($sp)");
		code.emit(n, "mult $t0, $t1");
		code.emit(n, "mflo $t0");
		code.emit(n, "addu $sp, $sp, 8");
		code.emit(n, "sw $t0, ($sp)");
		return null;
	}	
	
	public Object visitDivide(Divide n) {
		n.left.accept(this);
		n.right.accept(this);
		code.emit(n, "jal divide");
		stackHeight -= 8;
		return null;
	}	
	
	public Object visitRemainder(Remainder n) {
		n.left.accept(this);
		n.right.accept(this);
		code.emit(n, "jal remainder");
		stackHeight -= 8;
		return null;
	}	
	
	public Object visitEquals(Equals n){ //done*
		n.left.accept(this);
		n.right.accept(this);
		if (n.left.type instanceof IntegerType && n.right.type instanceof IntegerType){
			code.emit(n, "lw $t0, ($sp)");
			code.emit(n, "lw $t1, 8($sp)");
			code.emit(n, "seq $t0, $t0, $t1");
			code.emit(n, "addu $sp, $sp, 12");
			code.emit(n, "sw $t0, ($sp)");
			stackHeight -= 12;
		}
		else {
			code.emit(n, "lw $t0, ($sp)");
			code.emit(n, "lw $t1, 4($sp)");
			code.emit(n, "seq $t0, $t0, $t1");
			code.emit(n, "addu $sp, $sp, 4");
			code.emit(n, "sw $t0, ($sp)");
			stackHeight -= 4;
		}
		return null;
	}
	
	public Object visitGreaterThan(GreaterThan n){ //done
		n.left.accept(this);
		n.right.accept(this);
		code.emit(n, "lw $t0, ($sp)");
		code.emit(n, "lw $t1, 8($sp)");
		code.emit(n, "sgt $t0, $t1, $t0");
		code.emit(n, "addu $sp, $sp, 12");
		code.emit(n, "sw $t0, ($sp)");
		stackHeight -= 12;
		return null;
	}
	
	public Object visitLessThan(LessThan n){ //done
		n.left.accept(this);
		n.right.accept(this);
		code.emit(n, "lw $t0, ($sp)");
		code.emit(n, "lw $t1, 8($sp)");
		code.emit(n, "slt $t0, $t1, $t0");
		code.emit(n, "addu $sp, $sp, 12");
		code.emit(n, "sw $t0, ($sp)");
		stackHeight -= 12;
		return null;
	}
	
	public Object visitAnd(And n){ // done
		n.left.accept(this);
		code.emit(n, "lw $t0, ($sp)");
		code.emit(n, "beq $t0, $zero, skip_" + n.uniqueId);
		code.emit(n, "addu $sp, $sp, 4");
		stackHeight -= 4;
		n.right.accept(this);
		code.emit(n, "skip_" + n.uniqueId + ":");
		return null;
	}
	
	public Object visitOr(Or n){
		n.left.accept(this);
		code.emit(n, "lw $t0, ($sp)");
		code.emit(n, "beq $t0, $zero, skip_" + n.uniqueId);
		code.emit(n, "addu $sp, $sp, 4");
		stackHeight -= 4;
		n.right.accept(this);
		code.emit(n, "skip_" + n.uniqueId + ":");
		return null;
	}
	
	public Object visitArrayLength(ArrayLength n){
		n.exp.accept(this);
		code.emit(n, "lw $t0, ($sp)");
		code.emit(n, "beq $t0, $zero, nullPtrException");
		code.emit(n, "lw $t0, -4($t0)");
		code.emit(n, "sw $s5, ($sp)");
		code.emit(n, "subu $sp, 4");
		stackHeight += 4;
		code.emit(n, "sw $t0, ($sp)");
		return null;
	}
	
	public Object visitArrayLookup(ArrayLookup n){
		n.arrExp.accept(this);
		n.idxExp.accept(this);
		code.emit(n, "lw $t0, 8($sp)");
		code.emit(n, "beq $t0, $zero, nullPtrException");
		code.emit(n, "lw $t1, -4($t0)");
		code.emit(n, "lw $t2, ($sp)");
		code.emit(n, "bgeu $t2, $t1, arrayIndexOutOfBounds");
		code.emit(n, "sll $t2, $t2, 2");
		code.emit(n, "addu $t2, $t2, $t0");
		code.emit(n, "lw $t0, ($t2)");
		if(n.type instanceof IntegerType){
			code.emit(n, "sw $t0, 4($sp)");
			code.emit(n, "sw $s5, 8($sp)");
			code.emit(n, "addu $sp, $sp, 4");
			stackHeight -= 4;
		}
		else {
			code.emit(n, "sw $t0, 8($sp)");
			code.emit(n, "addu $sp, $sp, 8");
			stackHeight -= 8;
		}
		return null;
	}
	
	public Object visitInstVarAccess(InstVarAccess n){ // done
		n.exp.accept(this);
		code.emit(n, "lw $t0, ($sp)");
		code.emit(n, "beq $t0, $zero, nullPtrException");
		code.emit(n, "lw $t0, " + n.varDec.offset + "($t0)");
		if(n.varDec.type instanceof IntegerType){
			code.emit(n, "subu $sp, $sp, 4");
			code.emit(n, "sw $s5, 4($sp)");
			code.emit(n, "sw $t0, ($sp)");
			stackHeight += 4;
		}
		else {
			code.emit(n, "sw $t0, ($sp)");
		}
		return null;
	}
	
	public Object visitInstanceOf(InstanceOf n){
		n.exp.accept(this);
		code.emit(n, "la $t0, CLASS_" + ((IdentifierType)n.checkType).link.name);
		code.emit(n, "la $t1, CLASS_END_" + ((IdentifierType)n.checkType).link.name);
		code.emit(n, "jal instanceOf");
		return null;
	}
	
	public Object visitCast(Cast n){
		n.exp.accept(this);
		ClassDecl parent = ((IdentifierType)n.exp.type).link;
		ClassDecl child = ((IdentifierType)n.castType).link;
		if(isSuperClass(child, parent)){
			code.emit(n, "la $t0, CLASS_" + ((IdentifierType)n.castType).link.name);
			code.emit(n, "la $t1, CLASS_END_" + ((IdentifierType)n.castType).link.name);
			code.emit(n, "jal checkCast");
		}
		return null;
	}
	
	public Object visitNewObject(NewObject n){ //done
		int numObjInst = n.objType.link.numObjInstVars;
		int numDataInst = n.objType.link.numDataInstVars;
		stackHeight += 4;
		code.emit(n, "li $s6, " + (numDataInst +1));
		code.emit(n, "li $s7, " + numObjInst);
		code.emit(n, "jal newObject");
		code.emit(n, "la $t0, CLASS_" + n.objType.name);
		code.emit(n, "sw $t0, -12($s7)");
		return null;
	}
	
	public Object visitNewArray(NewArray n){
		n.sizeExp.accept(this);
		code.emit(n, "lw $s7, ($sp)");
		code.emit(n, "addu $sp, $sp, 8");
		stackHeight -= 8;
		if(isDataType(((ArrayType)n.objType).baseType))
			code.emit(n, "li $s6, -1");
		else
			code.emit(n, "li $s6, 1");
		code.emit(n, "jal newObject");
		stackHeight += 4;
		if(isDataType(((ArrayType)n.objType).baseType))
			code.emit(n, "la $t0, CLASS__DataArray");
		else
			code.emit(n, "la $t0, CLASS__ObjectArray");
		code.emit(n, "sw $t0, -12($s7)");
		return null;
	}
	
	public Object visitCall(Call n){ //done
		if(n.obj instanceof Super){
			int savedStackHeight = stackHeight;
			n.obj.accept(this);
			n.parms.accept(this);
			if(n.methodLink.pos < 0)
				code.emit(n, "jal " + n.methodLink.name + "_" + n.methodLink.classDecl.name);
			else 
				code.emit(n, "jal fcn_" +  n.methodLink.uniqueId + "_" + n.methodLink.name);

			stackHeight = savedStackHeight + 4*wordsOnStackFrame(n.type);
		}
		else {
			int savedStackHeight = stackHeight;
			n.obj.accept(this);
			n.parms.accept(this);
			code.emit(n, "lw $t0, " + (n.methodLink.thisPtrOffset - 4) + "($sp)");
			code.emit(n, "beq $t0, $zero, nullPtrException");
			code.emit(n, "lw $t0, -12($t0)");
			code.emit(n, "lw $t0, " + (4*n.methodLink.vtableOffset) + "($t0)");
			code.emit(n, "jalr $t0");
			stackHeight = savedStackHeight + 4*wordsOnStackFrame(n.type);
		}
		return null;
	}
	
	public Object visitLocalVarDecl(LocalVarDecl n){ //done
		n.initExp.accept(this);
		n.offset = -stackHeight;
		return null;
	}
	
	public Object visitCallStatement(CallStatement n){ // done
		n.callExp.accept(this);
		int popNum = 4*wordsOnStackFrame(n.callExp.type);
		stackHeight -= popNum;
		if(popNum != 0)
			code.emit(n, "addu $sp, $sp, " + popNum);
		return null;
	}
	
	public Object visitBlock(Block n){
		int savedStackHeight = stackHeight;
		n.stmts.accept(this);
		if (savedStackHeight != stackHeight)
			code.emit(n, "addu $sp " + (stackHeight - savedStackHeight));
		stackHeight = savedStackHeight;
		return null;
	}
	
	public Object visitIf(If n){
		n.exp.accept(this);
		stackHeight -= 4;
		code.emit(n, "lw $t0, ($sp)");
		code.emit(n, "addu $sp, $sp, 4");
		code.emit(n, "beq $t0, $zero, if_else_" + n.uniqueId);
		n.trueStmt.accept(this);
		code.emit(n, "j if_done_" + n.uniqueId);
		code.emit(n, "if_else_" + n.uniqueId + ":");
		n.falseStmt.accept(this);
		code.emit(n, "if_done_" + n.uniqueId + ":");
		return null;
	}
	
	public Object visitWhile(While n){ // done
		n.stackHeight = stackHeight;
		code.emit(n, "j while_enter_" + n.uniqueId);
		code.emit(n, "while_top_" + n.uniqueId + ":");
		n.body.accept(this);
		code.emit(n, "while_enter_" + n.uniqueId + ":");
		n.exp.accept(this);
		code.emit(n, "lw $t0, ($sp)");
		code.emit(n, "addu $sp, $sp, 4");
		code.emit(n, "bne $t0, $zero, while_top_" + n.uniqueId);
		code.emit(n, "break_target_" + n.uniqueId + ":");
		stackHeight -= 4;
		return null;
	}
	
	public Object visitBreak(Break n){
		int num = stackHeight - n.breakLink.stackHeight;
		if (num != 0)
			code.emit(n, "addu $sp, " + num);
		code.emit(n, "j break_target_" + n.breakLink.uniqueId);
		return null;
	}
	
	public Object visitAssign(Assign n){
		if(n.lhs instanceof IdentifierExp){
			n.rhs.accept(this);
			code.emit(n, "lw $t0, ($sp)");
			if(((IdentifierExp)n.lhs).link instanceof InstVarDecl)
				code.emit(n, "sw $t0, " + ((IdentifierExp)n.lhs).link.offset + "($s2)");
			else
				code.emit(n, "sw $t0, " + (stackHeight + ((IdentifierExp)n.lhs).link.offset) + "($sp)");
			code.emit(n, "addu $sp, $sp, "+ (4*wordsOnStackFrame(n.lhs.type)));
			stackHeight -= 4*wordsOnStackFrame(n.lhs.type);
		}
		else if(n.lhs instanceof InstVarAccess){
			((InstVarAccess)n.lhs).exp.accept(this);
			n.rhs.accept(this);
			code.emit(n, "lw $t0, ($sp)");
			code.emit(n, "lw $t1, " + (4*wordsOnStackFrame(n.lhs.type)) + "($sp)");
			code.emit(n, "beq $t1, $zero, nullPtrException");
			code.emit(n, "sw $t0, " + ((InstVarAccess)n.lhs).varDec.offset + "($t1)");
			code.emit(n, "addu $sp, $sp, " + (4*wordsOnStackFrame(n.lhs.type) + 4) );
			stackHeight -= 4*wordsOnStackFrame(n.lhs.type) + 4;	

		}
		else if(n.lhs instanceof ArrayLookup){
			((ArrayLookup)n.lhs).arrExp.accept(this);
			((ArrayLookup)n.lhs).idxExp.accept(this);
			n.rhs.accept(this);
			code.emit(n, "lw $t0, ($sp)");
			code.emit(n, "lw $t1, " + (wordsOnStackFrame(n.rhs.type)*4 + 8) + "($sp)");
			code.emit(n, "beq $t1, $zero, nullPtrException");
			code.emit(n, "lw $t2, "+ (wordsOnStackFrame(n.rhs.type)*4) + "($sp)");
			code.emit(n, "lw $t3, -4($t1)");
			code.emit(n, "bgeu $t2, $t3, arrayIndexOutOfBounds");
			code.emit(n, "sll $t2, $t2, 2");
			code.emit(n, "addu $t2, $t2, $t1");
			code.emit(n, "sw $t0, ($t2)");
			code.emit(n, "addu $sp, $sp, " + (wordsOnStackFrame(n.rhs.type)*4 + 12));
			stackHeight -= wordsOnStackFrame(n.rhs.type)*4 + 12;
		}
		return null;
	}
	
	public Object visitMethodDeclVoid(MethodDeclVoid n){ //done
		code.emit(n, ".global fcn_" + n.uniqueId + "_" + n.name);
		code.emit(n, "fcn_" + n.uniqueId + "_" + n.name + ":");
		code.emit(n, "subu $sp, $sp, 4");
		code.emit(n, "sw $s2, ($sp)");
		code.emit(n, "lw $s2, " + n.thisPtrOffset + "($sp)");
		code.emit(n, "sw $ra, " + n.thisPtrOffset + "($sp)");
		stackHeight = 0;
		n.stmts.accept(this);
		code.emit(n, "lw $ra, " + (n.thisPtrOffset + stackHeight)  + "($sp)");
		code.emit(n, "lw $s2, " + stackHeight + "($sp)");
		int popNum = stackHeight + 8 + 4*wordsOnStackFrame(n.formals);
		code.emit(n, "addu $sp, $sp, " + popNum);
		code.emit(n, "jr $ra");
		return null;
	}
	
	public Object visitMethodDeclNonVoid(MethodDeclNonVoid n){
		code.emit(n, ".global fcn_" + n.uniqueId + "_" + n.name);
		code.emit(n, "fcn_" + n.uniqueId + "_" + n.name + ":");
		code.emit(n, "subu $sp, $sp, 4");
		code.emit(n, "sw $s2, ($sp)");
		code.emit(n, "lw $s2, " + n.thisPtrOffset + "($sp)");
		code.emit(n, "sw $ra, " + n.thisPtrOffset + "($sp)");
		stackHeight = 0;
		n.stmts.accept(this);
		n.rtnExp.accept(this);
		code.emit(n, "lw $ra, " + (n.thisPtrOffset + stackHeight)  + "($sp)");
		code.emit(n, "lw $s2, " + stackHeight + "($sp)");
		code.emit(n, "lw $t0, ($sp)");
		int num = stackHeight + n.thisPtrOffset;
		if(n.rtnType instanceof IntegerType){
			code.emit(n, "sw $t0, " + (num - 4) + "($sp)");
			code.emit(n, "sw $s5, " + num + "($sp)");
		}
		else {
			code.emit(n, "sw $t0, " + num  + "($sp)");
		}
		int pop = stackHeight + 4 + 4*wordsOnStackFrame(n.formals) + 4 - 4*wordsOnStackFrame(n.rtnType);
		code.emit(n, "addu $sp, $sp, " + pop);
		code.emit(n, "jr $ra");
		return null;
	}
	
	public Object visitProgram(Program n) { //done
		code.indent(n);
		code.emit(n, ".text");
		code.emit(n, ".global main");
		code.emit(n, "main:");
		code.emit(n, "jal vm_init");
		stackHeight = 0;
		n.mainStatement.accept(this);
		code.emit(n, "li $v0, 10");
		code.emit(n, "syscall");
		n.classDecls.accept(this);
		code.flush();
		return null;
	}
	
	private int wordsOnStackFrame(Type t){
		int num = 0;
		if (t instanceof VoidType)
			num = 0;
		else if(t instanceof IntegerType)
			num = 2;
		else 
			num = 1;
		return num;
	}
	
	private int wordsOnStackFrame(VarDeclList vdl){
		int num = 0;
		for(VarDecl vd : vdl){
			if(vd.type instanceof IntegerType)
				num += 2;
			else
				num +=1;
		}
		return num;
	}
	
	private boolean isDataType(Type t){
		if (t instanceof IntegerType || t instanceof BooleanType)
			return true;
		return false;
	}
	private boolean isObjectType(Type t){
		if (t instanceof IntegerType || t instanceof BooleanType || t instanceof VoidType)
			return false;
		return true;
	}
	
	private boolean isSuperClass(ClassDecl c1, ClassDecl c2){
		if(c1.superLink == null)
			return false;
		if(c1.superLink.equals(c2))
			return true;
		else
			return isSuperClass(c1.superLink, c2);
	}

}


	
