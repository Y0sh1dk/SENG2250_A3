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

    private void run(int portNumber) throws IOException {
        this.setupConnection(portNumber);

        // DO stuff

        this.terminate();
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
