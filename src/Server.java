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
            System.out.println("Awaiting Client Connection...");
            String str = this.readMessage();
            if(str != null && str.equals(Protocol.getOpenConnectionString())) {
                System.out.println("Client Connected");
                this.sendMessage(Protocol.getOKMessageString());
                break;
            }
        }

        String message;
        // ------------------ Start Setup ------------------
        message = this.readMessage();
        if (!message.equals("Hello") || message.equals(Protocol.getCloseConnectionString())) {
            this.terminate();
        }

        // we want to send the server public RSA key

        // Generate RSA keys
        RSAPrivateKey = Utilities.generateRSAKeys(Protocol.getRSAEncryptionBit());
        RSAPublicKey = new BigInteger[]{RSAPrivateKey[0], Protocol.getPubKeyE()};
        //pubKey = Utilities.modPow(Protocol.getDHg(), Protocol.getPubKeyE(), Protocol.getDHp());

        // Send public key
        System.out.println("Sending Public Key:\n" + RSAPublicKey[0] + Protocol.getMessageDelimiter() + RSAPublicKey[1] + "\n\n");
        this.sendMessage(RSAPublicKey[0] + Protocol.getMessageDelimiter() + RSAPublicKey[1]);
        // ------------------ End Setup ------------------

        // ------------------ Start Handshake ------------------
        // Receive Client ID from client
        message = this.readMessage();
        if (message.equals(Protocol.getCloseConnectionString())) {
            this.terminate();
        }
        clientID = message;

        // Send Server ID and Session ID
        System.out.println("Sending ServerID,SessionID:\n" + Protocol.getServerID() + "," + Protocol.getSessionID() + "\n\n");
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

        // Send DH Public key to client
        System.out.println("Sending DH Public Key to client\n");
        this.sendMessage(DHPublicKey.toString());

        // Send RSA Signature to Client
        RSASignature = Utilities.genRSASignature(DHPublicKey, RSAPublicKey);
        this.sendMessage(RSASignature.toString());
        //System.out.println("RSASignature:\n" + RSASignature + "\n\n");

        // Calculate hashed session key
        sessionKey = Utilities.SHA256(Utilities.modPow(clientDHPublicKey, DHPrivateKey, Protocol.getDHp()).toString());
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



        // ------------------ End Data Exchange ------------------

        this.terminate();
    }

    private Boolean sendString(String msg) throws IOException {
        try {
            this.out.println(msg);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String readString() throws IOException {
        return this.in.readLine();
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


    private void sendMessage(String msg) throws IOException {
        this.sendString(Protocol.getStartMessageString());
        this.sendString(msg);
        this.sendString(Protocol.getEndMessageString());
    }

    private void setupConnection(int portNumber) throws IOException {
        this.sSocket = new ServerSocket(portNumber);
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
