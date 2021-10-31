import java.math.BigInteger;

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

}
