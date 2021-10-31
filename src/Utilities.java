import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class Utilities {


    public static BigInteger modPow(BigInteger base, BigInteger exp, BigInteger mod) {
        BigInteger rs = BigInteger.ONE;

        if (mod.equals(BigInteger.ONE)) {
            return BigInteger.ZERO;
        }
        while (exp.compareTo(BigInteger.ZERO) > 0) {
            if (exp.testBit(0)) {
                rs = (rs.multiply(base)).mod(mod);
            }
            exp = exp.shiftRight(1);
            base = base.multiply(base).mod(mod);
        }
        return rs;
    }

    public static BigInteger genDHPrivateKey(BigInteger DHp) {
        BigInteger key = new BigInteger(DHp.bitLength(), new SecureRandom());

        // 1 < key < DHp
        while(key.compareTo(BigInteger.ONE) != 1 && key.compareTo(DHp) != -1 ) {
            key = new BigInteger(DHp.bitLength(), new SecureRandom());
        }
        return key;
    }

    public static BigInteger genDHPublicKey(BigInteger DHPrivateKey) {
        return modPow(Protocol.getDHg(), DHPrivateKey, Protocol.getDHp());
    }


    public static BigInteger[] generateRSAKeys(int bitLength) {
        // Generate two prime numbers
        BigInteger p = genPrimeNumber(Protocol.getRSAEncryptionBit());
        BigInteger q = genPrimeNumber(Protocol.getRSAEncryptionBit());
        BigInteger n = p.multiply(q);
        BigInteger d = Protocol.getPubKeyE().modInverse(p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE)));

        BigInteger[] keys = {n,d}; // Modulus, Private key

        return keys;
    }

    public static BigInteger genPrimeNumber(int bitLength) {
        return BigInteger.probablePrime(bitLength/2, new SecureRandom());
    }


    public static BigInteger genRSASignature(BigInteger DHPublicKey, BigInteger[] RSAPublicKey) {
        BigInteger RSAHash = null;
        try {
            BigInteger hash = SHA256(DHPublicKey.toString()).abs(); // Make always positive
            RSAHash = Utilities.modPow(hash, RSAPublicKey[1], RSAPublicKey[0]);
        } catch (Exception e) {}
        return RSAHash;
    }

    public static BigInteger SHA256(String inStr) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(inStr.getBytes(StandardCharsets.UTF_8));
            return new BigInteger(hash);
        } catch (Exception e) {}
        return null;
    }

    public static String[] AESEncrypt(String inMsg, BigInteger sessionKey) {
        String[] messageHMAC = new String[2]; // Message, HMAC
        try {
            SecretKeySpec keySpec = new SecretKeySpec(sessionKey.toByteArray(), "AES");
            IvParameterSpec iv = new IvParameterSpec(Protocol.getInitialIV().getBytes(StandardCharsets.UTF_8));

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv);

            messageHMAC[0] = Base64.getEncoder().encodeToString(cipher.doFinal(inMsg.getBytes(StandardCharsets.UTF_8)));
            messageHMAC[1] = genHMAC(inMsg, sessionKey).toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return messageHMAC;
    }

    public static String AESDecrypt(String inMsg, BigInteger sessionKey) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(sessionKey.toByteArray(), "AES");
            IvParameterSpec iv = new IvParameterSpec(Protocol.getInitialIV().getBytes(StandardCharsets.UTF_8));

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);

            return new String(cipher.doFinal(Base64.getDecoder().decode(inMsg)));
        } catch(Exception e) {}
        return "Error Decrypting";
    }

    public static BigInteger genHMAC(String inMsg, BigInteger sessionKey) {
        byte[] bopad = new byte[32];
        byte[] bipad = new byte[32];

        for (int i = 0; i < bopad.length; ++i)
        {
            bopad[i] = 0x5c;
            bipad[i] = 0x36;
        }
        BigInteger opad = new BigInteger(bopad);
        BigInteger ipad = new BigInteger(bipad);

        return SHA256(sessionKey.xor(opad).toString() +
                Utilities.SHA256(sessionKey.xor(ipad) + inMsg));
    }

}
