package syntaxtree;
import visitor.Visitor;

/**
 * a default-label within a switch statement
 */
public class Default extends Label {

	/**
	 * constructor
	 * @param pos file position
	 */
	public Default(int pos) {
		super(pos);
	}

	/*************** remaining methods are visitor- and display-related ****************/

	public Object accept(Visitor v) {
		return v.visitDefault(this);
	}

}

