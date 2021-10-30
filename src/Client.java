import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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

        int i = 0;
        while(true) {
            Thread.sleep(100);
            this.sendMessage("Message from client!" + i);
            i++;

            if (i == 100) {
                break;
            }
        }
        this.sendMessage(Protocol.getCloseConnectionString());
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
        this.in.close();
        this.out.close();
        this.cSocket.close();
    }
}
