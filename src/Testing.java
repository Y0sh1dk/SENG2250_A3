import java.math.BigInteger;

public class Testing {
    public static void main(String[] args) {
        BigInteger test;

        test = Utilities.modPow(new BigInteger("3785"), new BigInteger("8395"), new BigInteger("65537"));
        System.out.println(test);
    }



}
