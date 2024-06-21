package server;

import java.net.*;
import java.io.*;
import java.util.*;

import client.Player;
import buffers.RequestProtos.Request;
import buffers.RequestProtos.Logs;
import buffers.RequestProtos.Message;
import buffers.ResponseProtos.Response;
import buffers.ResponseProtos.Entry;

/**
 * SockBaseServer handles client connections, processes requests, and maintains game and leaderboard state.
 */
class SockBaseServer {
    static String logFilename = "logs.txt";
    static String leaderboardFilename = "leaderboard.txt";
    InputStream in = null;
    OutputStream out = null;
    Socket clientSocket = null;
    int port = 9099; // default port
    Game game;
    static Map<String, Player> leaderboard = Collections.synchronizedMap(new HashMap<>());
    Set<String> playersInGame = Collections.synchronizedSet(new HashSet<>());
    Map<String, Integer> currentGamePoints = Collections.synchronizedMap(new HashMap<>());

    /**
     * Constructor for SockBaseServer
     * @param sock - client socket
     * @param game - game instance
     */
    public SockBaseServer(Socket sock, Game game) {
        this.clientSocket = sock;
        this.game = game;
        try {
            in = clientSocket.getInputStream();
            out = clientSocket.getOutputStream();
        } catch (Exception e) {
            System.out.println("Error in constructor: " + e);
        }
        loadLeaderboard(); // Load the leaderboard when the server starts
    }

    /**
     * Starts the server to handle client requests.
     * @throws IOException
     */
    public void start() throws IOException {
        String name = "";
        System.out.println("Ready...");
        try {
            // Continuously listen for client requests
            while (true) {
                // Parse the incoming request from the client
                Request op = Request.parseDelimitedFrom(in);
                if (op == null) break; // Exit loop if no more input

                // Handle the request based on its type
                switch (op.getOperationType()) {
                    case NAME:
                        // Process NAME request: log the connection, update leaderboard, and send greeting
                        name = op.getName();
                        writeToLog(name, Message.CONNECT);
                        synchronized (leaderboard) {
                            Player player = leaderboard.get(name);
                            if (player == null) {
                                player = new Player(name, 0);
                                leaderboard.put(name, player);
                            }
                            player.setLogins(player.getLogins() + 1);
                            saveLeaderboard();
                        }
                        currentGamePoints.put(name, 0); // Initialize points for the new game
                        System.out.println("Got a connection and a name: " + name);
                        greetingMessage(name);
                        break;

                    case LEADERBOARD:
                        // Process LEADERBOARD request: send the leaderboard to the client
                        sendLeaderboard();
                        break;

                    case START:
                        // Process START request: reset the game if won or lost, and start a new game
                        if (game.isWon() || game.isLost()) {
                            game.resetGame();
                        }
                        playersInGame.add(name);
                        gameStart();
                        break;

                    case ROWCOL:
                        // Process ROWCOL request: handle the player's move
                        playerMove(op.getRow(), op.getColumn(), name);
                        break;

                    case QUIT:
                        // Process QUIT request: remove the player from the game and send a goodbye message
                        playersInGame.remove(name);
                        currentGamePoints.remove(name);
                        goodbyeMessage();
                        return;

                    default:
                        // Handle unknown request types
                        errorMessage("Unknown request type");
                        break;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            // Close the input, output, and client socket
            if (out != null) out.close();
            if (in != null) in.close();
            if (clientSocket != null) clientSocket.close();
        }
    }
    /**
     * Sends a greeting message to the client.
     * @param name - Name of the client
     * @throws IOException
     */
    private void greetingMessage(String name) throws IOException {
        Response response = Response.newBuilder()
                .setResponseType(Response.ResponseType.GREETING)
                .setMessage("Hello " + name + " and welcome to a simple game of battleship.")
                .setMenuoptions("1 - View Leaderboard\n2 - Play Game\n3 - Quit")
                .setNext(Response.NextStep.MENU)
                .build();
        response.writeDelimitedTo(out);
    }

    /**
     * Sends the leaderboard to the client.
     * @throws IOException
     */
    private void sendLeaderboard() throws IOException {
        Response.Builder responseBuilder = Response.newBuilder()
                .setResponseType(Response.ResponseType.LEADERBOARD)
                .setNext(Response.NextStep.MENU);
        synchronized (leaderboard) {
            for (Player player : leaderboard.values()) {
                Entry entry = Entry.newBuilder()
                        .setName(player.getName())
                        .setPoints(player.getPoints())
                        .setLogins(player.getLogins())
                        .build();
                responseBuilder.addLeader(entry);
            }
        }
        responseBuilder.setMenuoptions("1 - View Leaderboard\n2 - Play Game\n3 - Quit");
        responseBuilder.build().writeDelimitedTo(out);
    }

    /**
     * Sends the game start message and the current board to the client.
     * @throws IOException
     */
    private void gameStart() throws IOException {
        boolean isNewGame = game.getIdx() == 0;
        if (isNewGame) {
            game.newGame();
        }
        Response response = Response.newBuilder()
                .setResponseType(Response.ResponseType.START)
                .setBoard(game.getImage())
                .setMessage(isNewGame ? "Starting a new game." : "Resuming the current game.")
                .setNext(Response.NextStep.TILE)
                .build();
        response.writeDelimitedTo(out);

        // Print the original game board to the server console for debugging purposes
        System.out.println(game.getOriginalImage());
    }

    /**
     * Handles the move made by a player.
     * @param row - row index
     * @param column - column index
     * @param playerName - name of the player
     * @throws IOException
     */
    private void playerMove(int row, int column, String playerName) throws IOException {
        // Check if the row and column are within the game board bounds
        if (row < 0 || row >= game.getRow() || column < 0 || column >= game.getCol()) {
            errorMessage("Row or column out of bounds.");
            return;
        }

        Response.EvalType evalType;
        String message;
        int currentPoints = currentGamePoints.getOrDefault(playerName, 0);

        // Check if the spot was already guessed
        if (game.isAlreadyHit(row, column)) {
            evalType = Response.EvalType.OLD;
            message = "You already guessed this spot!";
        } else if (game.isHit(row, column)) {
            game.replaceOneCharacter(row, column);
            currentPoints += 1; // Add 1 point for a hit
            evalType = Response.EvalType.HIT;
            message = "That's a hit!";
        } else {
            game.replaceOneCharacter(row, column);
            currentPoints -= 1; // Subtract 1 point for a miss
            evalType = Response.EvalType.MISS;
            message = "You missed!";
        }

        currentGamePoints.put(playerName, currentPoints);

        // Check if the game is won or lost
        if (game.isWon()) {
            evalType = Response.EvalType.WON;
            message = "Congratulations, you won!";
            synchronized (leaderboard) {
                for (String playerInGame : playersInGame) {
                    Player player = leaderboard.get(playerInGame);
                    if (player != null) {
                        player.setPoints(player.getPoints() + 1);  // Add 1 point for winning
                    } else {
                        player = new Player(playerInGame, 0);
                        player.setPoints(1);
                        leaderboard.put(playerInGame, player);
                    }
                }
                saveLeaderboard();
            }
            playersInGame.clear();
            currentGamePoints.clear();
        } else if (game.isLost()) {
            evalType = Response.EvalType.LOST;
            message = "You lost the game!";
            playersInGame.clear();
            currentGamePoints.clear();
        }

        // Send the response to the client
        Response response = Response.newBuilder()
                .setResponseType(game.isWon() || game.isLost() ? Response.ResponseType.DONE : Response.ResponseType.PLAY)
                .setBoard(game.getImage())
                .setEval(evalType)
                .setMessage(message + " Number of guesses: " + game.getGuesses())
                .setNext(game.isWon() || game.isLost() ? Response.NextStep.MENU : Response.NextStep.TILE)
                .build();
        response.writeDelimitedTo(out);

        // Print the original game board to the server console for grading purposes
        System.out.println(game.getOriginalImage());
    }

    /**
     * Sends a goodbye message to the client.
     * @throws IOException
     */
    private void goodbyeMessage() throws IOException {
        Response response = Response.newBuilder()
                .setResponseType(Response.ResponseType.BYE)
                .setMessage("Goodbye!")
                .build();
        response.writeDelimitedTo(out);
    }

    /**
     * Sends an error message to the client.
     * @param message - error message
     * @throws IOException
     */
    private void errorMessage(String message) throws IOException {
        Response response = Response.newBuilder()
                .setResponseType(Response.ResponseType.ERROR)
                .setMessage(message)
                .setNext(Response.NextStep.MENU)
                .build();
        response.writeDelimitedTo(out);
    }

    /**
     * Writing a new entry to our log
     * @param name - Name of the person logging in
     * @param message - type Message from Protobuf which is the message to be written in the log (e.g. Connect)
     */
    public static void writeToLog(String name, Message message) {
        try {
            Logs.Builder logs = readLogFile(); // Read the existing log file

            Date date = java.util.Calendar.getInstance().getTime();
            System.out.println(date);

            // Add a new log entry to the logs object
            logs.addLog(date.toString() + ": " + name + " - " + message);

            // Write the updated logs object to the log file
            FileOutputStream output = new FileOutputStream(logFilename);
            Logs logsObj = logs.build();

            // Print the log entries to the console for debugging
            for (String log : logsObj.getLogList()) {
                System.out.println(log);
            }

            logsObj.writeTo(output);
        } catch (Exception e) {
            System.out.println("Issue while trying to save");
        }
    }

    /**
     * Reading the current log file
     * @return Logs.Builder a builder of a logs entry from protobuf
     * @throws Exception
     */
    public static Logs.Builder readLogFile() throws Exception {
        Logs.Builder logs = Logs.newBuilder();

        try {
            // Merge the existing log file content into the logs object
            return logs.mergeFrom(new FileInputStream(logFilename));
        } catch (FileNotFoundException e) {
            System.out.println(logFilename + ": File not found. Creating a new file.");
            return logs;
        }
    }

    /**
     * Saves the current state of the leaderboard to a file.
     */
    private synchronized void saveLeaderboard() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(leaderboardFilename))) {
            // Write each player's details to the leaderboard file
            for (Player player : leaderboard.values()) {
                writer.write(player.getName() + "," + player.getPoints() + "," + player.getLogins());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads the leaderboard from a file.
     */
    private synchronized void loadLeaderboard() {
        try (BufferedReader reader = new BufferedReader(new FileReader(leaderboardFilename))) {
            String line;
            // Read each line from the leaderboard file and update the leaderboard map
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    String name = parts[0];
                    int points = Integer.parseInt(parts[1]);
                    int logins = Integer.parseInt(parts[2]);
                    Player player = new Player(name, 0);
                    player.setPoints(points);
                    player.setLogins(logins);
                    leaderboard.put(name, player);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println(leaderboardFilename + ": File not found. Starting a new leaderboard.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Main method to start the server.
     * @param args - command line arguments
     * @throws Exception
     */
    public static void main(String args[]) throws Exception {
        Game game = new Game();

        if (args.length != 2) {
            System.out.println("Expected arguments: <port(int)> <delay(int)>");
            System.exit(1);
        }
        int port = 9099;
        int sleepDelay = 10000;
        try {
            port = Integer.parseInt(args[0]);
            sleepDelay = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port|sleepDelay] must be an integer");
            System.exit(2);
        }
        ServerSocket serv = new ServerSocket(port);

        while (true) {
            Socket clientSocket = serv.accept();
            SockBaseServer server = new SockBaseServer(clientSocket, game);
            new Thread(() -> {
                try {
                    server.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
            Thread.sleep(sleepDelay);
        }
    }
}
