package syntaxtree;
import visitor.Visitor;

/**
 * declaration of an instance variable
 */
public class InstVarDecl extends VarDecl {

	/**
	 * constructor
	 * @param pos file position
	 * @param atype the type of the variable
	 * @param aname the name being declared
	 */
	public InstVarDecl(int pos, Type atype, String aname) {
		super(pos, atype, aname);
	}

	/*************** remaining methods are visitor- and display-related ****************/

	public Object accept(Visitor v) {
		return v.visitInstVarDecl(this);
	}
}
