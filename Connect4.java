/*  
	Connect4 written by Nikita Mingazov
	last modified: 24/10/24
	This just implements the game logic
	All networking to be done in a container class
*/

public class Connect4 {
	private int rows = 6;
	private int columns = 7;
	private int[][] board = new int[rows][columns];
	private int placedPieces = 0;

	public Connect4() {
		// nothing necessary here
	}
	// to print the game board
	public String toString() {
		StringBuilder output = new StringBuilder(); // more efficient than string +=
		for (int i = 0; i < rows; i++) {
			output.append(" ");
			for (int j = 0; j < columns; j++) {
				if (board[i][j] == 1) {
					output.append("1"); // fancy player-specific characters would go here (if I had an appropriate one)
				}
				else if (board[i][j] == 2) {
					output.append("2");
				}
				else {
					output.append(board[i][j]);
				}
				output.append(" "); // for spacing
			}
			output.append(System.lineSeparator());
		}
		return output.toString();
	}
	public int move(int column, int player) {
		if (column > columns || column < 1) {
			return -1; // error (column doesn't exist)
		}
		column--; // decrement to move to 0 indexing
		
		int placed = -1; // which row the piece was placed into (or -1 if not placed)
		for (int row = rows - 1; row >= 0; row--) { // moving downwards up the 2d array
			if (board[row][column] == 0) {
				board[row][column] = player; // placing the piece
				placed = row;
				placedPieces++; // recording how many pieces are on the board
				break;
			}
		}
		if (placed == -1) {
			return -1; // not placed due to full column but a column is available
		}
		if (placedPieces == rows*columns) {
			return -2; // all columns are full, signal for a draw
		}
		if (gameEndCheck(placed, column)) {
			return player; // the player who placed has won
		}
		else {
			return 0; // successfully placed, no win
		}
	}
	// due to being used after move, does not need to know which player won
	// given a position on the board, determines if it is in a chain of 4 of the same value
	private boolean gameEndCheck(int row, int column) {
		// I did derive a more elegant solution using complex numbers to cycle around the 8 directions
		// by using -1+i as a starting point and adding e^(ikτ/4) where k = {4,4,3,3,2,2,1,1} = ceil((8-i)/2) for i in [0,7]
		// and then using the real and imaginary part to make steps from the centre index
		// but due to the JDK not having complex numbers I will do the dumb method instead
		int counter = 0;
		// left/right / 0° / 0
		for (int i = 1; i <= 3; i++) { // right
			if (column + i < columns) { // out of bounds check
				if (board[row][column+i] == board[row][column]) {
					counter++; // matches the player, increment
				}
				else {
					break; // dead end, stop counting
				}
			}
		}
		for (int i = 1; i <= 3; i++) { // left
			if (column - i >= 0) { // out of bounds check
				if (board[row][column-i] == board[row][column]) {
					counter++; // matches the player, increment
				}
				else {
					break; // dead end, stop counting
				}
			}
		}
		if (counter >= 3) {
			return true; // 4 in a row found
		}
		else {
			counter = 0; // new direction, new sum
		}
		// rightward diagonal / 45° / τ/8
		for (int i = 1; i <= 3; i++) { // up-right
			if (row + i < rows && column + i < columns) { // out of bounds check
				if (board[row+i][column+i] == board[row][column]) {
					counter++;
				}
				else {
					break;
				}
			}
		}
		for (int i = 1; i <= 3; i++) { // down-left
			if (row - i >= 0 && column - i >= 0) { // out of bounds check
				if (board[row-i][column-i] == board[row][column]) {
					counter++;
				}
				else {
					break;
				}
			}
		}
		if (counter >= 3) {
			return true; // 4 in a row found
		}
		else {
			counter = 0; // new direction, new sum
		}
		// up/down / 90° / τ/4
		for (int i = 1; i <= 3; i++) { // up
			if (row + i < rows) { // out of bounds check
				if (board[row+i][column] == board[row][column]) {
					counter++;
				}
				else {
					break;
				}
			}
		}
		for (int i = 1; i <= 3; i++) { // down
			if (row - i >= 0) { // out of bounds check
				if (board[row-i][column] == board[row][column]) {
					counter++;
				}
				else {
					break;
				}
			}
		}
		if (counter >= 3) {
			return true; // 4 in a row found
		}
		else {
			counter = 0; // new direction, new sum
		}
		// leftward diagonal / 135° / 3τ/8
		for (int i = 1; i <= 3; i++) {
			if (row - i >= 0 && column + i < columns) { // out of bounds check
				if (board[row-i][column+i] == board[row][column]) {
					counter++;
				}
			}
			if (row + i < rows && column - i >= 0) { // out of bounds check
				if (board[row+i][column-i] == board[row][column]) {
					counter++;
				}
			}
		}
		if (counter >= 3) {
			return true; // 4 in a row found
		}
		else {
			counter = 0; // new direction, new sum
		}

		return false; // no winning chain found
	}
	public static void main(String[] args) {}
}