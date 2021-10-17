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

    private void run(int portNumber) throws IOException {
        this.setupConnection(portNumber);

        // Do stuff

        this.terminate();
    }

    private void setupConnection(int portNumber) throws IOException {
        this.sSocket = new ServerSocket(portNumber);
        this.clientSocket = this.sSocket.accept();
        this.out = new PrintWriter(this.clientSocket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
    }

    private void terminate() throws IOException {
        this.in.close();
        this.out.close();
        this.clientSocket.close();
        this.sSocket.close();
    }
}
