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

        BigInteger sessionKey;

        BigInteger RSASignature;

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
        System.out.println("Sending finish handshake message..." + "\n\n");
        sendMessage(Utilities.AESEncrypt(Protocol.getCheckSessionKeymessage(), sessionKey)[0] +
                Protocol.getMessageDelimiter() +
                Utilities.AESEncrypt(Protocol.getCheckSessionKeymessage(), sessionKey)[1]);

        // Receive encrypted finish message
        message = this.readMessage();
        if(message.equals(Protocol.getCloseConnectionString())) {
            this.terminate();
        }
        String[] checkSession = message.split(Protocol.getMessageDelimiter());
        checkSession[0] = Utilities.AESDecrypt(checkSession[0], sessionKey);

        // Check Message and HMAC are correct
        if(!checkSession[0].equals(Protocol.getCheckSessionKeymessage()) ||
        !checkSession[1].equals(Utilities.genHMAC(checkSession[0], sessionKey).toString())) {
            this.terminate();
        }

        System.out.println("Handshake Success!" + "\n\n");

        // ------------------ End Handshake ------------------

        // ------------------ Start Data Exchange ------------------

        // Send message 1
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

        // ------------------ End Data Exchange ------------------

        this.terminate();
    }

    private void sendString(String msg) {
        this.out.println(msg);
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
