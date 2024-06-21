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

class SockBaseServer {
    static String logFilename = "logs.txt";
    static String leaderboardFilename = "leaderboard.txt";

    ServerSocket serv = null;
    InputStream in = null;
    OutputStream out = null;
    Socket clientSocket = null;
    int port = 9099; // default port
    Game game;
    static Map<String, Player> leaderboard = Collections.synchronizedMap(new HashMap<>());
    Set<String> playersInGame = Collections.synchronizedSet(new HashSet<>());
    Map<String, Integer> currentGamePoints = Collections.synchronizedMap(new HashMap<>());

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

    public void start() throws IOException {
        String name = "";
        System.out.println("Ready...");
        try {
            while (true) {
                Request op = Request.parseDelimitedFrom(in);
                if (op == null) break;

                switch (op.getOperationType()) {
                    case NAME:
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
                        sendGreeting(name);
                        break;

                    case LEADERBOARD:
                        sendLeaderboard();
                        break;

                    case START:
                        if (game.isWon() || game.isLost()) {
                            game.resetGame();
                        }
                        playersInGame.add(name);
                        sendGameStart();
                        break;

                    case ROWCOL:
                        handleMove(op.getRow(), op.getColumn(), name);
                        break;

                    case QUIT:
                        playersInGame.remove(name);
                        currentGamePoints.remove(name);
                        sendGoodbye();
                        return;

                    default:
                        sendError("Unknown request type");
                        break;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (out != null) out.close();
            if (in != null) in.close();
            if (clientSocket != null) clientSocket.close();
        }
    }

    private void sendGreeting(String name) throws IOException {
        Response response = Response.newBuilder()
                .setResponseType(Response.ResponseType.GREETING)
                .setMessage("Hello " + name + " and welcome to a simple game of battleship.")
                .setMenuoptions("1 - View Leaderboard\n2 - Play Game\n3 - Quit")
                .setNext(Response.NextStep.MENU)
                .build();
        response.writeDelimitedTo(out);
    }

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

    private void sendGameStart() throws IOException {
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

        System.out.println(game.getOriginalImage());
    }

    private void handleMove(int row, int column, String playerName) throws IOException {
        if (row < 0 || row >= game.getRow() || column < 0 || column >= game.getCol()) {
            sendError("Row or column out of bounds.");
            return;
        }

        Response.EvalType evalType;
        String message;
        int currentPoints = currentGamePoints.getOrDefault(playerName, 0);

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

        if (game.isWon()) {
            evalType = Response.EvalType.WON;
            message = "Congratulations, you won!";
            synchronized (leaderboard) {
                for (String playerInGame : playersInGame) {
                    Player player = leaderboard.get(playerInGame);
                    if (player != null) {
                        int finalPoints = currentGamePoints.getOrDefault(playerInGame, 0) + 1; // Add 1 point for winning
                        player.setPoints(finalPoints); // Update leaderboard with accumulated points
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

        Response response = Response.newBuilder()
                .setResponseType(game.isWon() || game.isLost() ? Response.ResponseType.DONE : Response.ResponseType.PLAY)
                .setBoard(game.getImage())
                .setEval(evalType)
                .setMessage(message + " Number of guesses: " + game.getGuesses())
                .setNext(game.isWon() || game.isLost() ? Response.NextStep.MENU : Response.NextStep.TILE)
                .build();
        response.writeDelimitedTo(out);

        System.out.println(game.getOriginalImage());
    }

    private void sendGoodbye() throws IOException {
        Response response = Response.newBuilder()
                .setResponseType(Response.ResponseType.BYE)
                .setMessage("Goodbye!")
                .build();
        response.writeDelimitedTo(out);
    }

    private void sendError(String message) throws IOException {
        Response response = Response.newBuilder()
                .setResponseType(Response.ResponseType.ERROR)
                .setMessage(message)
                .setNext(Response.NextStep.MENU)
                .build();
        response.writeDelimitedTo(out);
    }

    public static void writeToLog(String name, Message message) {
        try {
            Logs.Builder logs = readLogFile();

            Date date = java.util.Calendar.getInstance().getTime();
            System.out.println(date);

            logs.addLog(date.toString() + ": " + name + " - " + message);

            FileOutputStream output = new FileOutputStream(logFilename);
            Logs logsObj = logs.build();

            for (String log : logsObj.getLogList()) {
                System.out.println(log);
            }

            logsObj.writeTo(output);
        } catch (Exception e) {
            System.out.println("Issue while trying to save");
        }
    }

    public static Logs.Builder readLogFile() throws Exception {
        Logs.Builder logs = Logs.newBuilder();

        try {
            return logs.mergeFrom(new FileInputStream(logFilename));
        } catch (FileNotFoundException e) {
            System.out.println(logFilename + ": File not found. Creating a new file.");
            return logs;
        }
    }

    private synchronized void saveLeaderboard() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(leaderboardFilename))) {
            for (Player player : leaderboard.values()) {
                writer.write(player.getName() + "," + player.getPoints() + "," + player.getLogins());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void loadLeaderboard() {
        try (BufferedReader reader = new BufferedReader(new FileReader(leaderboardFilename))) {
            String line;
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
