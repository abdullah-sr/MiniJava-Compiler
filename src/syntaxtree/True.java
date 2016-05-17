package syntaxtree;
import visitor.Visitor;

/**
 * the expression, 'true'
 */
public class True extends Exp {

	/**
	 * constructor
	 * @param pos file position
	 */
	public True(int pos) {
		super(pos);
	}

	/*************** remaining methods are visitor- and display-related ****************/

	public Object accept(Visitor v) {
		return v.visitTrue(this);
	}
}
