import java.math.BigInteger;

public class Protocol {

    private static int portNumber = 8765;
    private static String IPAddress = "localhost"; // Loopback
    private static int readRate = 5;

    private static String clientID = "Client12345";
    private static String serverID = "Server12345";
    private static String sessionID = "Session1";
    private static String checkSessionKeyMessage = "checkSessionKeyMessage";

    private static BigInteger DHp = new BigInteger("17801190547854226652823756245015999014523215636912067427327445031444" +
            "28657887370207706126952521234630795671567847784664499706507709207278" +
            "57050009668388144034129745221171818506047231150039301079959358067395" +
            "34871706631980226201971496652413506094591370759495651467285569060679" +
            "4135837542707371727429551343320695239");

    private static BigInteger DHg = new BigInteger("17406820753240209518581198012352343653860449079456135097849583104059" +
            "99534884558231478515974089409507253077970949157594923683005742524387" +
            "61037084473467180148876118103083043754985190983472601550494691329488" +
            "08339549231385000036164648264460849230407872181895999905649609776936" +
            "8017749273708962006689187956744210730");

    private static BigInteger pubKeyE = new BigInteger("65537");

    private static int RSAEncryptionBit = 2048;

    private static String initialIV = "InitialIVString1";

    private static String startMessageString = "###START_MESSAGE###";
    private static String endMessageString = "###END_MESSAGE###";
    private static String openConnectionString = "###OPEN_CONNECTION_MESSAGE###";
    private static String closeConnectionString = "###CLOSE_CONNECTION_MESSAGE###";
    private static String OKMessageString = "###OK_MESSAGE###";
    private static String errorMessageString = "###ERROR_MESSAGE###";
    private static String messageDelimiter = "#";

    public static BigInteger getDHg() {
        return DHg;
    }

    public static BigInteger getDHp() {
        return DHp;
    }

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

    public static BigInteger getPubKeyE() {
        return pubKeyE;
    }

    public static String getClientID() {
        return clientID;
    }

    public static String getServerID() {
        return serverID;
    }

    public static String getSessionID() {
        return sessionID;
    }

    public static String getMessageDelimiter() {
        return messageDelimiter;
    }

    public static int getRSAEncryptionBit() {
        return RSAEncryptionBit;
    }

    public static String getInitialIV() {
        return initialIV;
    }

    public static String getCheckSessionKeyMessage() {
        return checkSessionKeyMessage;
    }
}
