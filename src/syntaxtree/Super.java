package syntaxtree;
import visitor.Visitor;

/**
 * the expression 'super'
 */
public class Super extends Exp {

	/**
	 * constructor
	 * @param pos file position
	 */
	public Super(int pos) {
		super(pos);
	}
	
	/*************** remaining methods are visitor- and display-related ****************/

	public Object accept(Visitor v) {
		return v.visitSuper(this);
	}
}
