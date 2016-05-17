package syntaxtree;
import treedisplay.TreeDisplayable;
import treedisplay.TreeDrawException;
import visitor.Visitor;

/**
 * an expression that creates a new array, as in "new int[4]"
 */
public class NewArray extends Exp {
	
	// instance variables filled in by constructor
	public Exp sizeExp; // the expression denoting the size of the array
	public Type objType; // the base type of the array

	/**
	 * constructor
	 * @param pos file position
	 * @param atype the base type of the array
	 * @param asizeExp the number of elements in the array
	 */
	public NewArray(int pos, Type atype, Exp asizeExp) {
		super(pos);
		objType=atype;
		sizeExp=asizeExp; 
	}

	/*************** remaining methods are visitor- and display-related ****************/

	public Object accept(Visitor v) {
		return v.visitNewArray(this);
	}


	public TreeDisplayable getDrawTreeSubobj(int n) throws TreeDrawException {
		switch (n) {
		case 0: return objType;
		case 1: return sizeExp;
		}
		throw new TreeDrawException();
	}
}
