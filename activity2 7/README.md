## Assignment 4

### Battleship Game

#### Description:
This project is a simplified version of the Battleship game using Java and Protocol Buffers (Protobuf). It includes a client-server setup where multiple clients can play and view a persistent leaderboard. The server manages the game state, tracks player guesses, and ensures leaderboard data persists. Players can join ongoing games, collaborate to find ships, and compete for points tracked on the leaderboard.

#### Purpose:
Demonstrate simple Client and Server communication using `SocketServer` and `Socket`classes.

This is the starter code for assignment 4 of a simplified battleship game. 

### Protocol:

#### General Request Format:

    {
    "type": "<request_type>",
    "name": "<name>", // Optional, used for some requests
    "row": <row>, // Optional, used for move requests
    "column": <column> // Optional, used for move requests
    }

#### General Response Format:
    
    {
    "type": "<response_type>",
    "ok": <bool>,
    "message": "<message>", // The message or error description
    "points": <int>, // The current points, if applicable
    "board": "<board_state>" // The state of the board, if applicable
    }

#### name:
Request:

    {
    "type": "name",
    "name": "<name>"
    }

General Response:

    {
    "type": "greeting",
    "ok": true,
    "message": "Hello <name> and welcome to a simple game of battleship.",
    "menu": "1 - View Leaderboard\n2 - Play Game\n3 - Quit"
    }

Error:

    {
    "type": "error",
    "ok": false,
    "message": "Unknown request type"
    }

#### leaderboard:

Request:

    {
    "type": "leaderboard"
    }

General Response:

    {
    "type": "leaderboard",
    "ok": true,
    "message": "<leaderboard_content>"
    }

Error:
    
    {
    "type": "error",
    "ok": false,
    "message": "Unknown request type"
    }

#### start:

Request:

    {
     "type": "start"
    }

General Response:

    {
    "type": "start",
    "ok": true,
    "message": "Starting a new game.",
    "board": "<initial_board_state>"
    }

Error:

    {
    "type": "error",
    "ok": false,
    "message": "Unknown request type"
    }
   

#### rowcol:

Request:

    {
    "type": "rowcol",
    "row": <row>,
    "column": <column>
    }
General Response:

    {
    "type": "move",
    "ok": true,
    "message": "<hit_or_miss_message>",
    "points": <current_points>,
    "board": "<current_board_state>"
    }

Error:

    {
    "type": "error",
    "ok": false,
    "message": "Invalid input. Please enter row and column separated by a space."
    }

    {
    "type": "error",
    "ok": false,
    "message": "Invalid input. Please enter valid row as a letter and column as a number."
    }

     {
    "type": "error",
    "ok": false,
    "message": "Invalid input. Please enter valid row as a letter (a-g) and column as a number (1-7)."
    }

#### quit:

Request:

    {
     "type": "quit"
    }

General Response:

    {
    "type": "bye",
    "ok": true,
    "message": "Goodbye!",
    "points": <current_points>
    }

Error:

    {
    "type": "error",
    "ok": false,
    "message": "Unknown request type"
    }

### How to run it: 

#### (optional)
The proto file can be compiled using

``gradle generateProto``

This will also be done when building the project. 

You should see the compiled proto file in Java under build/generated/source/proto/main/java/buffers

Now you can run the client and server 

#### Default: 
Server is Java
Per default on 9099
Gradle task is: runServer

You have one example client in Java using the Protobuf protocol

Clients runs per default on 
Per default host localhost, port 9099
Gradle task is: runClient


#### With parameters:
Java
gradle runClient -Pport=9099 -Phost='localhost'
gradle runServer -Pport=9099

### Requirements Checklist:

- [x] **1. Project runs through Gradle:** Gradle file is properly configured.
- [x] **2. Implement Protobuf protocol:** Protocol files and Protobuf example are implemented.
- [x] **3. Client sends name, server responds with greeting:** Basic client-server communication established.
- [x] **4. Main menu options:** Options for leaderboard, play game, and quit are implemented and sent by the server.
- [x] **5. Easy user interaction design:** User calls and interactions are designed for ease of use.
- [x] **6. Leaderboard display:** Leaderboard is shown when option 1 is chosen.
- [x] **7. Thread-safe leaderboard:** Leaderboard is thread-safe and consistent across clients.
- [x] **8. Persistent leaderboard:** Leaderboard data persists even if the server crashes.
- [x] **9. Leaderboard tracks points and logins:** Points/wins and logins are tracked; high scores are overwritten.
- [x] **10. Start game request:** Server responds with a battleship board for new or ongoing games.
- [x] **11. Display board information:** Client displays board and game status.
- [x] **12. Row/column input format:** Client informs player of input format and checks for input errors.
- [x] **13. Server processes input:** Server processes row and column input, checks bounds, and determines hit/miss/old status.
- [x] **14. Multiple clients in the same game:** Multiple clients can join the same game, and thread safety is ensured.
- [x] **15. Win conditions:** Clients win when all ships are found; all players in the game receive a point.
- [x] **16. Lose conditions:** Clients lose if more than 42 guesses are made collectively.
- [x] **17. Point system based on guesses:** Points are awarded based on the number of guesses, fewer guesses result in more points.
- [x] **18. Graceful quit:** Game quits gracefully when option 3 is chosen.
- [x] **19. Client exit during game:** Client exits gracefully when "exit" is typed.
- [x] **20. Server robustness:** Server does not crash if a client disconnects unexpectedly.
- [x] **21. General robustness:** Server and client handle invalid inputs and wrong requests/responses without crashing.

### Screencast Link:
https://drive.google.com/file/d/1N_QxnYbPyDnLeDH-czvGMJlkpuBDef4w/view?usp=sharing