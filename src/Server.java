import java.io.*;
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
        // --- Start Setup ---
        message = this.readMessage();
        if (!message.equals("Hello") || message.equals(Protocol.getCloseConnectionString())) {
            this.terminate();
        }
        this.sendMessage("My RSA public key");


        // --- End Setup ---

        // --- Start Handshake ---



        // --- End Handshake ---

        // --- Start Data Exchange ---



        // --- End Data Exchange ---






        this.terminate();

        //String msg;
        //while(true) {
        //    Thread.sleep(Protocol.getReadRate());
        //    msg = this.readMessage();
        //    // If close connection
        //    if (msg.equals(Protocol.getCloseConnectionString())) {
        //        break;
        //    }
        //
        //    System.out.println(msg);
        //}
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
