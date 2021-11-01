import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private ServerSocket sSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    public static void main(String[] args) {
        Server s = new Server();
        try {
            s.run(Protocol.getPortNumber());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void run(int portNumber) throws IOException, InterruptedException {
        System.out.println("");
        System.out.println("###################################################");
        System.out.println("###################################################");
        System.out.println("##              SENG2250 - A3 SERVER             ##");
        System.out.println("###################################################");
        System.out.println("###################################################");
        System.out.println("");

        this.setupConnection(portNumber);

        BigInteger[] RSAPrivateKey;
        BigInteger[] RSAPublicKey;
        String clientID;
        BigInteger DHPrivateKey;
        BigInteger DHPublicKey;
        BigInteger clientDHPublicKey;
        BigInteger RSASignature;
        BigInteger sessionKey;

        // Accept incoming connection
        while (true) {
            Thread.sleep(250);
            String str = this.readMessage();
            if(str != null && str.equals(Protocol.getOpenConnectionString())) {
                System.out.println("Client Connected!");
                this.sendMessage(Protocol.getOKMessageString());
                break;
            }
        }

        System.out.println("---------------------- Start Setup ----------------------");

        String message;
        message = this.readMessage();
        if (!message.equals("Hello") || message.equals(Protocol.getCloseConnectionString())) {
            this.terminate();
        }

        // Generate RSA keys
        RSAPrivateKey = Utilities.generateRSAKeys(Protocol.getRSAEncryptionBit());
        RSAPublicKey = new BigInteger[]{RSAPrivateKey[0], Protocol.getPubKeyE()};

        // Send public key
        System.out.println("Sending Public Key...");
        this.sendMessage(RSAPublicKey[0] + Protocol.getMessageDelimiter() + RSAPublicKey[1]);

        System.out.println("----------------------- End Setup -----------------------\n");
        // ------------------ End Setup ------------------

        // ------------------ Start Handshake ------------------
        System.out.println("-------------------- Start Handshake --------------------");
        // Receive Client ID from client
        message = this.readMessage();
        if (message.equals(Protocol.getCloseConnectionString())) {
            this.terminate();
        }
        clientID = message;
        System.out.println("Client ID Received:\n" + clientID + "\n");

        // Send Server ID and Session ID
        System.out.println("Sending Server ID and Session ID...\n");
        this.sendMessage(Protocol.getServerID() + Protocol.getMessageDelimiter() + Protocol.getSessionID());

        // Generate DH Private key
        DHPrivateKey = Utilities.genDHPrivateKey(Protocol.getDHp());
        // Generate DH Public key
        DHPublicKey = Utilities.genDHPublicKey(DHPrivateKey);

        // Receive DH Public key from client
        message = this.readMessage();
        if (message.equals(Protocol.getCloseConnectionString())) {
            this.terminate();
        }
        clientDHPublicKey = new BigInteger(message);
        System.out.println("Client DH Public Key:\n" + clientDHPublicKey);

        // Send DH Public key to client
        System.out.println("Sending DH Public Key to client...\n");
        this.sendMessage(DHPublicKey.toString());

        // Send RSA Signature to Client
        System.out.println("Sending RSA Signature to Client...\n");
        RSASignature = Utilities.genRSASignature(DHPublicKey, RSAPublicKey);
        this.sendMessage(RSASignature.toString());

        // Calculate hashed session key
        sessionKey = Utilities.SHA256(Utilities.modPow(clientDHPublicKey, DHPrivateKey, Protocol.getDHp()).toString());
        System.out.println("Calculated Session Key:\n" + sessionKey + "\n");

        // Send hashed session key
        System.out.println("Sending Session Key to Client...");
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
        System.out.println("Session keys verified!" + "\n");

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
        System.out.println("Handshake Success! (Message and HMAC verified)" + "\n");

        System.out.println("--------------------- End Handshake ---------------------\n");


        System.out.println("------------------ Start Data Transfer ------------------");

        String msg1 = "Hello, this is a message from the Server, Please accept this msg";
        String msg2 = "A bank system, including the internal and external sub-systems..";

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
        System.out.println("Message 1 Received from Client:\n" + message + "\n");

        // Send encrypted message
        System.out.println("Sending Message 1 to Client...\n");
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
        System.out.println("Message 2 Received from Client:\n" + message + "\n");

        // Send encrypted message
        System.out.println("Sending Message 2 to Client...\n");
        this.sendMessage(Utilities.encryptAndHMACMessage(msg2, sessionKey));


        System.out.println("------------------- End Data Transfer -------------------");
        this.terminate();
    }

    private Boolean _sendString(String msg) throws IOException {
        try {
            this.out.println(msg);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void sendMessage(String msg) throws IOException {
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
        this.sSocket = new ServerSocket(portNumber);
        System.out.println("Waiting for a client connection...");
        this.clientSocket = this.sSocket.accept();
        this.out = new PrintWriter(this.clientSocket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
    }

    private void terminate() throws IOException {
        System.out.println("Shutting Down...");
        this.sendMessage(Protocol.getCloseConnectionString());
        this.in.close();
        this.out.close();
        this.clientSocket.close();
        this.sSocket.close();
        System.exit(0);
    }
}
