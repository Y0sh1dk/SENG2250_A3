import java.math.BigInteger;
import java.security.SecureRandom;

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
}
