package qbql.parser;

public interface Cell {
	int size();
	int getSymbol( int index );
	int getRule( int index );
	int getPosition( int index );
	// CYK legacy
	int[] getContent();
}
