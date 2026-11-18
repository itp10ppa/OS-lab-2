import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.DataInputStream;
import java.io.IOException;

public class MatMul {

    public static void main(String[] args) throws InterruptedException, IOException {
        int n = Integer.parseInt(args[0]);
        int m = Integer.parseInt(args[1]);
        int k = Integer.parseInt(args[2]);
        int T = Integer.parseInt(args[3]);
        boolean benchMode = args.length > 4 && args[4].equals("bench");
        String mode = args.length > 5 ? args[5] : "static";

        long genStartTime = System.nanoTime();

        ProcessBuilder pb = new ProcessBuilder("java", "MatrixGenerator",
                String.valueOf(n), String.valueOf(m), String.valueOf(k));
        Process p = pb.start();

        DataInputStream in = new DataInputStream(p.getInputStream());

        int nIn = in.readInt();
        int mIn = in.readInt();
        int kIn = in.readInt();
        if (nIn != n || mIn != m || kIn != k) {
            throw new IOException("Размеры от генератора не совпадают");
        }

        long[][] A = readMatrix(in, n, m);
        long[][] B = readMatrix(in, m, k);
        p.waitFor();

        long genEndTime = System.nanoTime();
        double genSeconds = (genEndTime - genStartTime) / 1_000_000_000.0;
        System.out.println("Время генерации и передачи: " + genSeconds + " s");

        long[][] C = new long[n][k];

        AtomicInteger completedRows = new AtomicInteger(0);
        long startTime = System.nanoTime();

        Thread[] threads = startWorkers(A, B, C, T, completedRows, mode);

        Thread progressThread = null;
        if (!benchMode) {
            progressThread = createProgressThread(completedRows, n);
            progressThread.start();
        }
        for (int t = 0; t < T; t++) {
            threads[t].join();
        }
        if (progressThread != null) {
            progressThread.join();
        }

        long endTime = System.nanoTime();
        double seconds = (endTime - startTime) / 1_000_000_000.0;

        if (!benchMode && n <= 10 && m <= 10 && k <= 10) {
            printMatrix(A, "A");
            printMatrix(B, "B");
            printMatrix(C, "C");
        }

        System.out.println("\nTime: " + seconds + " s");
        System.out.println("Checksum: " + checksum(C));

        if (!benchMode) {
            long[][] Cexpected = multiplySequential(A, B);
            System.out.println("Correct: " + matricesEqual(C, Cexpected));
        }

        if (!benchMode && n == 1500) {
            System.out.println("\nТест оптимизации...");

            long t1 = System.nanoTime();
            long[][] Cseq = multiplySequential(A, B);
            long t2 = System.nanoTime();

            long[][] Copt = multiplySequentialOptim(A, B);
            long t3 = System.nanoTime();

            System.out.println("Обычный порядок (i->j->t): " + (t2 - t1) / 1e9 + " s");
            System.out.println("Оптимизированный (i->t->j): " + (t3 - t2) / 1e9 + " s");
            System.out.println("Результаты совпадают: " + matricesEqual(Cseq, Copt));
        }
    }

    static long[][] readMatrix(DataInputStream in, int rows, int cols) throws IOException {
        long[][] M = new long[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                M[i][j] = in.readLong();
            }
        }
        return M;
    }

    static Thread[] startWorkers(long[][] A, long[][] B, long[][] C,
                                 int T, AtomicInteger completedRows, String mode) {
        int n = A.length;
        Thread[] threads = new Thread[T];

        if (mode.equals("dynamic")) {
            AtomicInteger nextRow = new AtomicInteger(0);
            for (int t = 0; t < T; t++) {
                threads[t] = new Thread(new WorkerDynamic(A, B, C, nextRow, n, completedRows));
                threads[t].start();
            }
        }
        else {
            int base = n / T;
            int remainder = n % T;
            int currentRow = 0;
            for (int t = 0; t < T; t++) {
                int rowsT = base + (t < remainder ? 1 : 0);
                int startRow = currentRow;
                int endRow = currentRow + rowsT;
                currentRow = endRow;
                threads[t] = new Thread(new Worker(A, B, C, startRow, endRow, completedRows));
                threads[t].start();
            }
        }
        return threads;
    }

    static Thread createProgressThread(AtomicInteger completedRows, int totalRows) {
        return new Thread(() -> {
            int lastPercent = -1;
            while (true) {
                int done = completedRows.get();
                int percent = (int)((long)done * 100 / totalRows);
                if (percent != lastPercent) {
                    System.out.println("Прогресс: " + percent + "%");
                    lastPercent = percent;
                }
                if (done == totalRows) break;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
    }

    static long checksum(long[][] M) {
        long sum = 0;
        for (long[] row : M)
            for (long v : row)
                sum += v;
        return sum;
    }

    static long[][] multiplySequential(long[][] A, long[][] B) {
        int n = A.length;
        int m = A[0].length;
        int k = B[0].length;
        long[][] C = new long[n][k];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < k; j++) {
                long sum = 0;
                for (int t = 0; t <= i; t++) {
                    sum += A[i][t] * B[t][j];
                }
                C[i][j] = sum;
            }
        }
        return C;
    }

    static long[][] multiplySequentialOptim(long[][] A, long[][] B) {
        int n = A.length;
        int m = A[0].length;
        int k = B[0].length;
        long[][] C = new long[n][k];

        for (int i = 0; i < n; i++) {
            for (int t = 0; t <= i; t++) {
                long a = A[i][t];
                for (int j = 0; j < k; j++) {
                    C[i][j] += a * B[t][j];
                }
            }
        }
        return C;
    }

    static boolean matricesEqual(long[][] X, long[][] Y) {
        if (X.length != Y.length) return false;

        for (int i = 0; i < X.length; i++) {
            if (X[i].length != Y[i].length) return false;

            for (int j = 0; j < X[i].length; j++)
                if (X[i][j] != Y[i][j]) return false;
        }
        return true;
    }

    static void printMatrix(long[][] M, String title) {
        System.out.println("\nMatrix " + title);
        for (long[] row : M) {
            for (long v : row) System.out.print(v + " ");
            System.out.println();
        }
    }
}
