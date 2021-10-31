import java.math.BigInteger;

public class Protocol {
    // Large port number to avoid issues
    private static int portNumber = 8765;
    private static String IPAddress = "localhost"; // Loopback
    private static int readRate = 5;


    private static String startMessageString = "###START_MESSAGE###";
    private static String endMessageString = "###END_MESSAGE###";
    private static String openConnectionString = "###OPEN_CONNECTION_MESSAGE###";
    private static String closeConnectionString = "###CLOSE_CONNECTION_MESSAGE###";
    private static String OKMessageString = "###OK_MESSAGE###";
    private static String errorMessageString = "###ERROR_MESSAGE###";

    public static int getPortNumber() {
        return portNumber;
    }

    public static String getIPAddress() {
        return IPAddress;
    }

    public static String getStartMessageString() {
        return startMessageString;
    }

    public static String getEndMessageString() {
        return endMessageString;
    }

    public static String getCloseConnectionString() {
        return closeConnectionString;
    }

    public static String getOpenConnectionString() {
        return openConnectionString;
    }

    public static String getOKMessageString() {
        return OKMessageString;
    }

    public static int getReadRate() {
        return readRate;
    }

    public static String getErrorMessageString() {
        return errorMessageString;
    }
}
