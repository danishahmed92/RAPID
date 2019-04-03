package rapid.score.similaritymetric.fss;

public class MatrixForwardSkipping {
    private double getMaxSumSubMatrix(int rowIndex, int colIndex, double[][] simMatrix, double sum, double max) {
        if (rowIndex == simMatrix.length - 1 || colIndex == simMatrix[0].length - 1) { // last row
            double localMax = -1;
            for (int j = colIndex; j < simMatrix[0].length; j++) {
                if (simMatrix[rowIndex][j] > localMax)
                    localMax = simMatrix[rowIndex][j];
            }
            double localSum = sum + localMax;
            if (localSum > max)
                max = localSum;
            return max;
        }

        for (int i = rowIndex + 1; i < simMatrix.length; i++) {
            for (int j = colIndex + 1; j < simMatrix[0].length; j++) {
                double localSum = getMaxSumSubMatrix(i, j, simMatrix, sum + simMatrix[rowIndex][colIndex], max);
                if (localSum > max)
                    max = localSum;
            }
        }
        return max;
    }

    public double[][] generateSummationMatrix(double[][] matrix) {
        double[][] sumM = new double[matrix.length][matrix[0].length];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                sumM[i][j] = getMaxSumSubMatrix(i, j, matrix, 0, -1);
            }
        }
        return sumM;
    }

    public double getMaxValueMatrix(double[][] matrix) {
        double max = -100;
        for (double[] aMatrix : matrix) {
            for (double anAMatrix : aMatrix) {
                if (anAMatrix > max)
                    max = anAMatrix;
            }
        }
        return max;
    }

    public static void main(String[] args) {
        double[][] sm = {{7, 3, 4, 9},
                {3, 6, 5, 9},
                {4, 9, 5, 1}};

        MatrixForwardSkipping mfs = new MatrixForwardSkipping();

        double[][] sumM = mfs.generateSummationMatrix(sm);
        System.out.println("\nMax value:" + mfs.getMaxValueMatrix(sumM));
    }
}
