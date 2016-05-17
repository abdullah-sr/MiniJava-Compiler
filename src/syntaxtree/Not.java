package syntaxtree;
import visitor.Visitor;

/**
 * a unary '!' expression
 */
public class Not extends UnExp {

	/**
	 * constructor
	 * @param pos file position
	 * @param ae the operand
	 */
	public Not(int pos, Exp ae) {
		super(pos, ae); 
	}
	
	/*************** remaining methods are visitor- and display-related ****************/
	
	public Object accept(Visitor v) {
		return v.visitNot(this);
	}
}
