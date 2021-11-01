import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
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
        System.out.println("");
        System.out.println("---------------------------------------------------");
        System.out.println("                SENG2250 - A3 CLIENT               ");
        System.out.println("---------------------------------------------------");
        System.out.println("");

        this.setupConnection(portNumber);

        BigInteger[] serverRSAPubKey = new BigInteger[2];
        String serverID;
        String sessionID;
        BigInteger DHPrivateKey;
        BigInteger DHPublicKey;
        BigInteger serverDHPublicKey;
        BigInteger sessionKey;
        BigInteger RSASignature;

        // Open connection
        this.sendMessage(Protocol.getOpenConnectionString());

        // Wait for connection to be accepted
        while (true) {
            Thread.sleep(250);
            String str = this.readMessage();
            if(str != null && str.equals(Protocol.getOKMessageString())) {
                System.out.println("Connected to Server!");
                break;
            }
        }

        System.out.println("---------------------- Start Setup ----------------------");

        String message;
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

        System.out.println("----------------------- End Setup -----------------------\n");
        // ------------------ End Setup ------------------

        // ------------------ Start Handshake ------------------
        System.out.println("-------------------- Start Handshake --------------------");
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

        // Receive RSA Signature
        message = this.readMessage();
        if(message.equals(Protocol.getCloseConnectionString())) {
            this.terminate();
        }
        RSASignature = new BigInteger(message);
        System.out.println("RSA Signature:\n" + RSASignature + "\n\n");

        // Verify RSA Signature
        if(!RSASignature.equals(Utilities.genRSASignature(serverDHPublicKey, serverRSAPubKey))) {
            this.terminate();
        }
        System.out.println("RSA Signature Verified!" + "\n\n");

        // Calculate hashed session key
        sessionKey = Utilities.SHA256(Utilities.modPow(serverDHPublicKey, DHPrivateKey, Protocol.getDHp()).toString());
        System.out.println("Session Key:\n" + sessionKey + "\n\n");

        // Send hashed session key
        this.sendMessage(sessionKey.toString());

        // Receive hashed session key
        message = this.readMessage();
        if(message.equals(Protocol.getCloseConnectionString())) {
            this.terminate();
        }
        // Verify against own session key
        if (!message.equals(sessionKey.toString())) {
            this.terminate();
        }
        System.out.println("Session keys verified!" + "\n\n");

        // Send encrypted finish message
        System.out.println("Sending finish handshake message...");
        sendMessage(Utilities.encryptAndHMACMessage(Protocol.getCheckSessionKeyMessage(), sessionKey));

        // Receive encrypted finish message
        message = this.readMessage();
        if(message.equals(Protocol.getCloseConnectionString())) {
            this.terminate();
        }
        try {
            message = Utilities.decryptAndVerifyMessage(message, sessionKey);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            this.terminate();
        }
        // Verify message
        if(!message.equals(Protocol.getCheckSessionKeyMessage())) {
            this.terminate();
        }
        System.out.println("Handshake Success!" + "\n\n");

        System.out.println("--------------------- End Handshake ---------------------\n");


        System.out.println("------------------ Start Data Transfer ------------------");

        String msg1 = "Hello, this is a message from the Client, Please accept this msg";
        String msg2 = "Systems and Network Security is very cool!, would recommend!!!..";

        System.out.println("Sending Message 1...");
        this.sendMessage(Utilities.encryptAndHMACMessage(msg1, sessionKey));

        // Receive encrypted message
        try {
            message = Utilities.decryptAndVerifyMessage(this.readMessage(), sessionKey);
        } catch (Exception ignored) {
            System.out.println("Invalid HMAC");
            this.terminate();
        }
        if(message.equals(Protocol.getCloseConnectionString())) {
            this.terminate();
        }

        // Send encrypted message
        System.out.println("Sending Message 2...");
        this.sendMessage(Utilities.encryptAndHMACMessage(msg2, sessionKey));

        // Receive encrypted message
        try {
            message = Utilities.decryptAndVerifyMessage(this.readMessage(), sessionKey);
        } catch (Exception ignored) {
            System.out.println("Invalid HMAC");
            this.terminate();
        }
        if(message.equals(Protocol.getCloseConnectionString())) {
            this.terminate();
        }

        System.out.println("------------------- End Data Transfer -------------------");
        this.terminate();
    }

    private void _sendString(String msg) {
        this.out.println(msg);
    }

    private void sendMessage(String msg) {
        this._sendString(Protocol.getStartMessageString());
        this._sendString(msg);
        this._sendString(Protocol.getEndMessageString());
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
        while(this.cSocket == null) {
            System.out.println("Attempting to connect to server...");
            try {
                this.cSocket = new Socket(Protocol.getIPAddress(), portNumber);
            } catch (Exception e) {}
            try {
                Thread.sleep(750);
            } catch (Exception e) {}

        }
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
