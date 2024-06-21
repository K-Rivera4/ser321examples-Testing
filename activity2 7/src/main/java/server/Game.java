package server;

import java.util.*;
import java.io.*;

/**
 * Class: Game
 * Description: Game class that can load an ASCII image.
 * This class can be used to hold the persistent state for a game for different threads.
 * Synchronization is not taken care of.
 * You can change this Class in any way you like or decide to not use it at all.
 * I used this class in my SockBaseServer to create a new game and keep track of the current image even on different threads.
 * My threads each get a reference to this Game.
 */
public class Game {
    private int idx = 0; // current index where x could be replaced with original
    private int idxMax; // max index of image
    private char[][] original; // the original image
    private char[][] hidden; // the hidden image
    private int col; // columns in original, approx
    private int row; // rows in original and hidden
    private boolean won; // if the game is won or not
    private List<String> files = new ArrayList<String>(); // list of files, each file has one image
    private int guesses = 0; // track number of guesses

    /**
     * Constructor initializes the game with preset image files.
     */
    public Game() {
        won = true; // setting it to true, since then in newGame() a new image will be created
        files.add("battle1.txt");
        files.add("battle2.txt");
        files.add("battle3.txt");
    }

    /**
     * Starts a new game if the previous game was won.
     */
    public void newGame() {
        if (won) {
            resetGame();
        }
    }

    /**
     * Resets the game state and loads a new game board from a random file.
     */
    public void resetGame() {
        idx = 0;
        won = false;
        guesses = 0;
        List<String> rows = new ArrayList<String>();

        try {
            Random rand = new Random();
            col = 0;
            int randInt = rand.nextInt(files.size());
            String fileName = files.get(randInt);
            System.out.println("File " + fileName);

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    getClass().getClassLoader().getResourceAsStream(fileName)));

            String line;
            while ((line = br.readLine()) != null) {
                if (col < line.length()) {
                    col = line.length();
                }
                rows.add(line);
            }
        } catch (Exception e) {
            System.out.println("File load error: " + e);
        }

        String[] rowsASCII = rows.toArray(new String[0]);
        row = rowsASCII.length;

        original = new char[row][col];
        int xCount = 0;
        for (int i = 0; i < row; i++) {
            char[] splitRow = rowsASCII[i].toCharArray();
            for (int j = 0; j < splitRow.length; j++) {
                original[i][j] = splitRow[j];
                if (splitRow[j] == 'x') {
                    xCount++;
                }
            }
        }

        hidden = new char[row][col];
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                hidden[i][j] = 'X';
            }
        }
        setIdxMax(xCount);
    }

    /**
     * Returns the current state of the hidden game board as a string.
     * @return String representation of the hidden game board.
     */
    public String getImage() {
        StringBuilder sb = new StringBuilder();
        sb.append("  1 2 3 4 5 6 7\n");
        for (int i = 0; i < row; i++) {
            sb.append((char) ('a' + i)).append(" ");
            for (int j = 0; j < col; j++) {
                sb.append(hidden[i][j]).append(' ');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * Replaces one character on the hidden board with the corresponding character from the original board.
     * @param row The row index of the character to replace.
     * @param column The column index of the character to replace.
     * @return The updated hidden game board as a string.
     */
    public String replaceOneCharacter(int row, int column) {
        guesses++;
        if (original[row][column] == 'x') {
            hidden[row][column] = 'O'; // mark hit as 'O'
            idx++;
        } else {
            hidden[row][column] = ' '; // mark miss as ' '
        }
        return getImage();
    }

    /**
     * Returns the original state of the game board as a string.
     * @return String representation of the original game board.
     */
    public String getOriginalImage() {
        StringBuilder sb = new StringBuilder();
        sb.append("  1 2 3 4 5 6 7\n");
        for (int i = 0; i < row; i++) {
            sb.append((char) ('a' + i)).append(" ");
            for (int j = 0; j < col; j++) {
                sb.append(original[i][j]).append(' ');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * Sets the maximum index value for the game board.
     * @param idxMax The maximum index value.
     */
    public void setIdxMax(int idxMax) {
        this.idxMax = idxMax;
    }

    /**
     * Returns the current index value.
     * @return The current index value.
     */
    public int getIdx() {
        return idx;
    }

    /**
     * Checks if a given position has already been hit.
     * @param row The row index of the position to check.
     * @param column The column index of the position to check.
     * @return true if the position has already been hit, false otherwise.
     */
    public boolean isAlreadyHit(int row, int column) {
        return hidden[row][column] != 'X';
    }

    /**
     * Checks if a given position is a hit.
     * @param row The row index of the position to check.
     * @param column The column index of the position to check.
     * @return true if the position is a hit, false otherwise.
     */
    public boolean isHit(int row, int column) {
        return original[row][column] == 'x';
    }

    /**
     * Checks if the game is won.
     * @return true if the game is won, false otherwise.
     */
    public synchronized boolean isWon() {
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                if (original[i][j] == 'x' && hidden[i][j] != 'O') {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks if the game is lost.
     * @return true if the game is lost, false otherwise.
     */
    public synchronized boolean isLost() {
        return guesses > 42; // lose if more than 42 guesses
    }

    /**
     * Returns the number of rows in the game board.
     * @return The number of rows.
     */
    public int getRow() {
        return row;
    }

    /**
     * Returns the number of columns in the game board.
     * @return The number of columns.
     */
    public int getCol() {
        return col;
    }

    /**
     * Returns the number of guesses made so far.
     * @return The number of guesses.
     */
    public int getGuesses() {
        return guesses;
    }
}
