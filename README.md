# SENG2250 - A3

A client and a server are planning to do data exchange. They decide to use a simplified SSL
handshake (see Figure 1) to establish a secure channel (session key) then exchange data. The
simplified SSL handshake removes the messages for alert, change cipher spec, certificate, etc. 

## Compilation
    
    cd src
    javac -d ../out Client.java Server.java

## Running
Client and Server need to be started in separate terminal sessions.
It does not matter what order they are started in.

### Start Server

    java -cp ./out Server

### Start Client

    java -cp ./out Client