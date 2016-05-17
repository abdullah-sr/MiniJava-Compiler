package syntaxtree;
import visitor.Visitor;

/**
 * a method declaration that does not return a value
 */
public class MethodDeclVoid extends MethodDecl {

	/**
	 * constructor
	 * @param pos file position
	 * @param as the name being declared
	 * @param afl the list of formal parameter declarations
	 * @param asl the statements that are the method's body
	 */
	public MethodDeclVoid(int pos, String as, VarDeclList afl,
			StatementList asl) {
		super(pos, as, afl, asl);
	}

	/*************** remaining methods are visitor- and display-related ****************/

	public Object accept(Visitor v) {
		return v.visitMethodDeclVoid(this);
	}
}
