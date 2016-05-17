package visitor;

import syntaxtree.*;

import java.util.*;

import errorMsg.*;
import java.io.*;
import java.awt.Point;

//the purpose here is to annotate things with their offsets:
// - formal parameters, with respect to the (callee) frame
// - instance variables, with respect to their slot in the object
// - methods, with respect to their slot in the v-table
public class CG1Visitor extends ASTvisitor {
	
	// Error message object
	ErrorMsg errorMsg;
	
	// IO stream to which we will emit code
	CodeStream code;
	
	// v-table offset of next method we encounter
	int currentMethodOffset;
	
	// offset in object of next "object" instance variable we encounter
	int currentObjInstVarOffset;
	
	// offset in object of next "data" instance variable we encounter
	int currentDataInstVarOffset;
	
	// stack-offset of next formal parameter we encounter
	int currentFormalVarOffset;
	
	// stack method tables for current class and all superclasses
	Stack<Vector<String>> superclassMethodTables;
	
	// current method table
	Vector<String> currentMethodTable;
	
	public CG1Visitor(ErrorMsg e, PrintStream out) {
		errorMsg = e;
		initInstanceVars(e, out);
	}
	
	private void initInstanceVars(ErrorMsg e, PrintStream out) {
		errorMsg = e;
		currentMethodOffset = 0;
		currentObjInstVarOffset = 0;
		currentDataInstVarOffset = 0;
		code = new CodeStream(out, e);
		superclassMethodTables = new Stack<Vector<String>>();
		superclassMethodTables.addElement(new Vector<String>());
	}
	
	private ClassDecl getObjClassDecl(ClassDecl cd){
		if(cd.superLink == null)
			return cd;
		else
			return getObjClassDecl(cd.superLink);
	}
	
	private int numMethods(ClassDecl cd){
		if (cd == null)
			return 0;
		else if(cd.superLink == null)
			return cd.methodTable.size();		
		else {
			Object[] methodTableArray = cd.methodTable.keySet().toArray();
			int overlap = 0;
			for (Object method : methodTableArray){
				if (cd.superLink.methodTable.containsKey(method))
					overlap++;
			}
			return cd.methodTable.size() - overlap + numMethods(cd.superLink);
		}
	}
	
	private void registerMethodInTable(MethodDecl md){
		//&& !this.currentMethodTable.contains(md.name + "_" + md.classDecl.name)
		//if (!this.currentMethodTable.contains("fcn_" + md.uniqueId + "_" + md.name))
		String superMthdLabel = null;
		String userSuperMthdLabel = null;
		if(md.superMethod != null){
			superMthdLabel = md.superMethod.name + "_" + md.superMethod.classDecl.name;
			userSuperMthdLabel = "fcn_" + md.superMethod.uniqueId + "_" + md.superMethod.name; 
		}
		int superLabelIndx = this.currentMethodTable.indexOf(superMthdLabel);
		int userLabelIndx = this.currentMethodTable.indexOf(userSuperMthdLabel);
		if(superLabelIndx >= 0)
			if(md.pos < 0 )
				this.currentMethodTable.set(superLabelIndx, md.name + "_" + md.classDecl.name);
			else
				this.currentMethodTable.set(superLabelIndx, "fcn_" + md.uniqueId + "_" + md.name);
		else if (userLabelIndx >= 0)
			if (md.pos < 0)
				this.currentMethodTable.set(userLabelIndx, md.name + "_" + md.classDecl.name);
			else
				this.currentMethodTable.set(userLabelIndx, "fcn_" + md.uniqueId + "_" + md.name);

		else if(md.pos < 0 ){
			this.currentMethodTable.add(md.name + "_" + md.classDecl.name);
		}
		else {
			this.currentMethodTable.add("fcn_" + md.uniqueId + "_" + md.name);
		}	
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
	public Object visitProgram(Program n) { // done
		code.emit(n, ".data");
		getObjClassDecl(n.classDecls.elementAt(0)).accept(this);
		code.flush();
		return null;
	}
	
	public Object visitClassDecl(ClassDecl n){ // done
		this.currentMethodTable = (Vector<String>) this.superclassMethodTables.peek().clone();
		this.currentMethodOffset = 1 + numMethods(n.superLink);
		if(n.superLink == null)
			this.currentDataInstVarOffset = -16;
		else
			this.currentDataInstVarOffset = -16 - 4*(n.superLink.numDataInstVars);
		if(n.superLink == null)
			this.currentObjInstVarOffset = 0;
		else
			this.currentObjInstVarOffset = 4*(n.superLink.numObjInstVars);
		super.visitClassDecl(n);
		n.numDataInstVars = (-16 - this.currentDataInstVarOffset)/4;
		n.numObjInstVars = this.currentObjInstVarOffset/4;
		code.emit(n, "CLASS_" + n.name + ":");
		if(n.superLink == null)
			code.emit(n, ".word 0");
		else
			code.emit(n, ".word CLASS_" + n.superLink.name);
		for (String method: this.currentMethodTable)
			code.emit(n, ".word " + method);
		this.superclassMethodTables.push(this.currentMethodTable);
		n.subclasses.accept(this);
		this.superclassMethodTables.pop();
		code.emit(n, "CLASS_END_" + n.name + ":");
		return null;
	}
	
	public Object visitMethodDecl(MethodDecl n){ //done
		int num = this.wordsOnStackFrame(n.formals);
		n.thisPtrOffset = 4*(1 + num);
		this.currentFormalVarOffset = n.thisPtrOffset;
		super.visitMethodDecl(n);
		if(n.superMethod != null)
			n.vtableOffset = n.superMethod.vtableOffset;
		else {
			n.vtableOffset = this.currentMethodOffset;
			this.currentMethodOffset +=1;
		}
		this.registerMethodInTable(n);
		return null;
	}
	
	public Object visitInstVarDecl(InstVarDecl n){ //done
		super.visitInstVarDecl(n);
		if(isDataType(n.type)){
			n.offset = this.currentDataInstVarOffset;
			this.currentDataInstVarOffset -= 4;
		}
		else if (isObjectType(n.type)){
			n.offset = this.currentObjInstVarOffset;
			this.currentObjInstVarOffset += 4;
		}

		return null;
	}
	
	public Object visitFormalDecl(FormalDecl n){ //done
		super.visitFormalDecl(n);
		if(n.type instanceof IntegerType)
			this.currentFormalVarOffset -= 8;
		else
			this.currentFormalVarOffset -=4;
		n.offset = this.currentFormalVarOffset;
		return null;
	}
}
