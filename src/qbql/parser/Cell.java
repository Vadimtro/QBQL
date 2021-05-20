package qbql.parser;

/**
 * Matrix element
 * @author Dim
 */
public interface Cell {
	/**
	 * @return length of the content: #of grammar symbols for CYK, #of rules for Earley
	 */
	int size();
	/**
	 * @param index
	 * @return Earley rule at index
	 */
	int getRule( int index );
	/**
	 * @param index
	 * @return position within Earley rule (partial parse) at cell index
	 */
	int getPosition( int index );
	
	/**
	 *  Internal methods
	 */
	long[] getContent();
    void insertContent( long cellElem );
}
