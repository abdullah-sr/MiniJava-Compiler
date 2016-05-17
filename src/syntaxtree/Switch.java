package syntaxtree;
import treedisplay.TreeDisplayable;
import treedisplay.TreeDrawException;
import visitor.Visitor;

/**
 * a 'switch' statement
 */
public class Switch extends BreakTarget {
	
	// instance variables filled in by constructor
	public Exp exp; // the switch-expression
	public StatementList stmts; // the statements in the switch-statment's body

	/**
	 * constructor
	 * @param pos file position
	 * @param aexp the switch-expression
	 * @param astmts the statements in the switch-statement's body
	 */
	public Switch(int pos, Exp aexp, StatementList astmts) {
		super(pos);
		exp=aexp; stmts = astmts;
	}

	/*************** remaining methods are visitor- and display-related ****************/
	
	public Object accept(Visitor v) {
		return v.visitSwitch(this);
	}

	public TreeDisplayable getDrawTreeSubobj(int n) throws TreeDrawException {
		switch (n) {
		case 0: return exp;
		case 1: return stmts;
		}
		throw new TreeDrawException();
	}
}

