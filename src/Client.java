import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;

public class Client {
    private Socket cSocket;
    private PrintWriter out;
    private BufferedReader in;

    public static void main(String[] args) {
        Client c = new Client();
        try {
            c.run(Protocol.getPortNumber());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void run(int portNumber) throws IOException, InterruptedException {
        this.setupConnection(portNumber);

        BigInteger[] serverRSAPubKey = new BigInteger[2];
        String serverID;
        String sessionID;

        BigInteger DHPrivateKey;
        BigInteger DHPublicKey;

        BigInteger serverDHPublicKey;

        // Open connection
        this.sendMessage(Protocol.getOpenConnectionString());

        // Wait for connection to be accepted
        while (true) {
            Thread.sleep(250);
            System.out.println("Attempting to connect...");
            String str = this.readMessage();
            if(str != null && str.equals(Protocol.getOKMessageString())) {
                System.out.println("Connected!");
                break;
            }
        }

        String message;

        // ------------------ Start Setup ------------------
        this.sendMessage("Hello");
        message = this.readMessage();
        if(message.equals(Protocol.getCloseConnectionString())) {
            this.terminate();
        }
        serverRSAPubKey[0] = new BigInteger(message.split(Protocol.getMessageDelimiter())[0]);
        serverRSAPubKey[1] = new BigInteger(message.split(Protocol.getMessageDelimiter())[1]);

        System.out.println("Server Public Key:\n" +
                "e: " + serverRSAPubKey[0] +
                "\nKey: " + serverRSAPubKey[1] + "\n\n");
        // ------------------ End Setup ------------------

        // ------------------ Start Handshake ------------------
        // Send client ID
        this.sendMessage(Protocol.getClientID());

        // Receive server ID and Session ID
        message = this.readMessage();
        if(message.equals(Protocol.getCloseConnectionString())) {
            this.terminate();
        }
        serverID = message.split(Protocol.getMessageDelimiter())[0];
        sessionID = message.split(Protocol.getMessageDelimiter())[1];
        System.out.println("Server ID:\n" + serverID + "\n\n");
        System.out.println("Session ID:\n" + sessionID + "\n\n");

        // Generate DH Private key
        DHPrivateKey = Utilities.genDHPrivateKey(Protocol.getDHp());
        // Generate DH Public key
        DHPublicKey = Utilities.genDHPublicKey(DHPrivateKey);

        // Send DH Public key to server
        System.out.println("Sending DHPublicKey to Server:\n" + DHPublicKey.toString() + "\n\n");
        this.sendMessage(DHPublicKey.toString());

        // Receive Servers DH Public Key
        message = this.readMessage();
        if(message.equals(Protocol.getCloseConnectionString())) {
            this.terminate();
        }
        serverDHPublicKey = new BigInteger(message);
        System.out.println("Server DH Public Key:\n" + serverDHPublicKey + "\n\n");

        // ------------------ End Handshake ------------------

        // ------------------ Start Data Exchange ------------------



        // ------------------ End Data Exchange ------------------

        this.terminate();
    }

    private void sendString(String msg) {
        this.out.println(msg);
    }

    private String readString() throws IOException {
        return this.in.readLine();
    }

    private void sendMessage(String msg) {
        this.sendString(Protocol.getStartMessageString());
        this.sendString(msg);
        this.sendString(Protocol.getEndMessageString());
    }

    private String readMessage() throws IOException {
        StringBuilder messageString = new StringBuilder();
        String str = this.in.readLine();

        boolean msgComplete = false;
        while(!msgComplete) {
            // Start of message
            if (str != null && str.equals(Protocol.getStartMessageString())) {
                while (true) {
                    str = this.in.readLine();
                    // End of message
                    if (str != null && str.equals(Protocol.getEndMessageString())) {
                        msgComplete = true;
                        break;
                    }
                    if (str != null) {
                        messageString.append(str);
                    }
                }
            }
        }
        return messageString.toString();
    }

    private void setupConnection(int portNumber) throws IOException {
        this.cSocket = new Socket(Protocol.getIPAddress(), portNumber);
        this.out = new PrintWriter(this.cSocket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(this.cSocket.getInputStream()));
    }

    private void terminate() throws IOException {
        System.out.println("Shutting Down...");
        this.sendMessage(Protocol.getCloseConnectionString());
        this.in.close();
        this.out.close();
        this.cSocket.close();
        System.exit(0);
    }
}
