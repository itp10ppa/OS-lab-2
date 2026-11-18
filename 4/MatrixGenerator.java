import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Random;

public class MatrixGenerator {

    public static void main(String[] args) throws IOException {
        int n = Integer.parseInt(args[0]);
        int m = Integer.parseInt(args[1]);
        int k = Integer.parseInt(args[2]);

        Random rand = new Random();
        DataOutputStream out = new DataOutputStream(System.out);

        out.writeInt(n);
        out.writeInt(m);
        out.writeInt(k);

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (j <= i) {
                    out.writeLong(rand.nextInt(10));
                }
                else {
                    out.writeLong(0);
                }
            }
        }

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < k; j++) {
                out.writeLong(rand.nextInt(10));
            }
        }
        out.flush();
    }
}
