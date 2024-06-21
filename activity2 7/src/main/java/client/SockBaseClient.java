package client;

import java.net.*;
import java.io.*;
import buffers.RequestProtos.Request;
import buffers.ResponseProtos.Response;
import buffers.ResponseProtos.Entry;

/**
 * SockBaseClient class to handle the client-side logic of the Battleship game.
 */
class SockBaseClient {
    public static void main(String args[]) throws Exception {
        Socket serverSock = null;
        OutputStream out = null;
        InputStream in = null;
        int port = 9099; // default port

        // Make sure two arguments are given
        if (args.length != 2) {
            System.out.println("Expected arguments: <host(String)> <port(int)>");
            System.exit(1);
        }
        String host = args[0];
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port] must be integer");
            System.exit(2);
        }

        // Ask user for username
        System.out.println("Please provide your name for the server.");
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        String strToSend = stdin.readLine();

        // Build the first request object just including the name
        Request op = Request.newBuilder()
                .setOperationType(Request.OperationType.NAME)
                .setName(strToSend).build();
        Response response;
        try {
            // connect to the server
            serverSock = new Socket(host, port);

            // write to the server
            out = serverSock.getOutputStream();
            in = serverSock.getInputStream();

            op.writeDelimitedTo(out);

            // read from the server
            response = Response.parseDelimitedFrom(in);

            // print the server response
            System.out.println(response.getMessage());
            System.out.println("* \nWhat would you like to do? \n 1 - to see the leader board \n 2 - to enter a game \n 3 - quit the game");

            while (true) {
                // Read user input for menu option
                String userInput = stdin.readLine();
                int option;
                try {
                    option = Integer.parseInt(userInput);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Please enter a number (1, 2, or 3).");
                    continue;
                }

                switch (option) {
                    case 1:
                        // Request to see the leaderboard
                        op = Request.newBuilder()
                                .setOperationType(Request.OperationType.LEADERBOARD)
                                .build();
                        op.writeDelimitedTo(out);
                        response = Response.parseDelimitedFrom(in);
                        for (Entry entry : response.getLeaderList()) {
                            System.out.println(entry.getName() + ": " + entry.getPoints() + " points, " + entry.getLogins() + " logins");
                        }
                        break;

                    case 2:
                        // Request to start a game
                        op = Request.newBuilder()
                                .setOperationType(Request.OperationType.START)
                                .build();
                        op.writeDelimitedTo(out);
                        response = Response.parseDelimitedFrom(in);
                        System.out.println(response.getMessage());
                        System.out.println(response.getBoard());
                        while (response.getNext() == Response.NextStep.TILE) {
                            System.out.println("Enter row and column separated by a space (e.g., 'a 1'), or type 'exit' to quit:");
                            userInput = stdin.readLine();
                            // Check if the user wants to exit the game
                            if (userInput.equalsIgnoreCase("exit")) {
                                op = Request.newBuilder()
                                        .setOperationType(Request.OperationType.QUIT)
                                        .build();
                                op.writeDelimitedTo(out);
                                response = Response.parseDelimitedFrom(in);
                                System.out.println(response.getMessage());
                                return;
                            }
                            // Split the user input into row and column
                            String[] parts = userInput.split(" ");
                            if (parts.length != 2) {
                                System.out.println("Invalid input. Please enter row and column separated by a space.");
                                continue;
                            }
                            int row;
                            int column;
                            try {
                                // Convert row letter to index and column number to index
                                row = parts[0].charAt(0) - 'a';
                                column = Integer.parseInt(parts[1]) - 1;
                            } catch (Exception e) {
                                System.out.println("Invalid input. Please enter valid row as a letter and column as a number.");
                                continue;
                            }
                            // Validate if the row and column are within bounds
                            if (row < 0 || row >= 7 || column < 0 || column >= 7) {
                                System.out.println("Invalid input. Please enter valid row as a letter (a-g) and column as a number (1-7).");
                                continue;
                            }
                            // Send the move to the server
                            op = Request.newBuilder()
                                    .setOperationType(Request.OperationType.ROWCOL)
                                    .setRow(row)
                                    .setColumn(column)
                                    .build();
                            op.writeDelimitedTo(out);
                            // Read the server's response
                            response = Response.parseDelimitedFrom(in);
                            System.out.println(response.getMessage());
                            System.out.println(response.getBoard());
                        }
                        break;

                    case 3:
                        // Request to quit the game
                        op = Request.newBuilder()
                                .setOperationType(Request.OperationType.QUIT)
                                .build();
                        op.writeDelimitedTo(out);
                        response = Response.parseDelimitedFrom(in);
                        System.out.println(response.getMessage());
                        return;

                    default:
                        System.out.println("Invalid option. Please enter 1, 2, or 3.");
                }
                System.out.println("* \nWhat would you like to do? \n 1 - to see the leader board \n 2 - to enter a game \n 3 - quit the game");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
            if (serverSock != null) serverSock.close();
        }
    }
}
