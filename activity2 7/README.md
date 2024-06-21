#### Purpose:
Demonstrate simple Client and Server communication using `SocketServer` and `Socket`classes.

This is the starter code for assignment 4 of a simplified battleship game. 

### How to run it (optional)
The proto file can be compiled using

``gradle generateProto``

This will also be done when building the project. 

You should see the compiled proto file in Java under build/generated/source/proto/main/java/buffers

Now you can run the client and server 

#### Default 
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