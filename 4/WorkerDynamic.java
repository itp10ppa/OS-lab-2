import java.util.concurrent.atomic.AtomicInteger;

public class WorkerDynamic implements Runnable {
    private final long[][] A;
    private final long[][] B;
    private final long[][] C;
    private final AtomicInteger nextRow;
    private final int totalRows;
    private final AtomicInteger completedRows;

    public WorkerDynamic(long[][] A, long[][] B, long[][] C,
                         AtomicInteger nextRow, int totalRows,
                         AtomicInteger completedRows) {
        this.A = A;
        this.B = B;
        this.C = C;
        this.nextRow = nextRow;
        this.totalRows = totalRows;
        this.completedRows = completedRows;
    }

    @Override
    public void run() {
        while (true) {
            int i = nextRow.getAndIncrement();
            if (i >= totalRows) break;

            for (int j = 0; j < B[0].length; j++) {
                long sum = 0;
                for (int t = 0; t <= i; t++) {
                    sum += A[i][t] * B[t][j];
                }
                C[i][j] = sum;
            }
            completedRows.incrementAndGet();
        }
    }
}
