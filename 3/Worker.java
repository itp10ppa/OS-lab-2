import java.util.concurrent.atomic.AtomicInteger;

public class Worker implements Runnable {

    private final long[][] A;
    private final long[][] B;
    private final long[][] C;
    private final int startRow;
    private final int endRow;
    private final AtomicInteger completedRows;

    public Worker(long[][] A, long[][] B, long[][] C,
                  int startRow, int endRow,
                  AtomicInteger completedRows) {
        this.A = A;
        this.B = B;
        this.C = C;
        this.startRow = startRow;
        this.endRow = endRow;
        this.completedRows = completedRows;
    }

    @Override
    public void run() {
        for (int i = startRow; i < endRow; i++) {
            for (int j = 0; j < B[0].length; j++) {
                long sum = 0;

                for (int t = 0; t < A[0].length; t++) {
                    sum += A[i][t] * B[t][j];
                }
                C[i][j] = sum;
            }
            completedRows.incrementAndGet();
        }
    }
}
