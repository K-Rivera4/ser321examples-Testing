package server;

import java.util.*;
import java.io.*;

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

    public Game() {
        won = true; // setting it to true, since then in newGame() a new image will be created
        files.add("battle1.txt");
        files.add("battle2.txt");
        files.add("battle3.txt");
    }

    public void setWon() {
        won = true;
    }

    public void newGame() {
        if (won) {
            resetGame();
        }
    }

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

    public int getIdxMax() {
        return idxMax;
    }

    public void setIdxMax(int idxMax) {
        this.idxMax = idxMax;
    }

    public int getIdx() {
        return idx;
    }

    public void setIdx(int idx) {
        this.idx = idx;
    }

    public boolean isAlreadyHit(int row, int column) {
        return hidden[row][column] != 'X';
    }

    public boolean isHit(int row, int column) {
        return original[row][column] == 'x';
    }

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

    public synchronized boolean isLost() {
        return guesses > 42; // lose if more than 42 guesses
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public int getGuesses() {
        return guesses;
    }
}
