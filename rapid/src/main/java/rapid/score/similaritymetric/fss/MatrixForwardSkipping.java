package rapid.score.similaritymetric.fss;

public class MatrixForwardSkipping {
    /**
     * recursive algorithm, idea is to calculate maximum possible sum starting from matrix[row][column]
     * and save the score to the summation matrix
     * see thesis section of Forward skipping for further explanation
     * @param rowIndex matrix row index
     * @param colIndex matrix col index
     * @param simMatrix similarity matrix
     * @param sum current sum using forward skipping approach
     * @param max maxmium sum obtained using forward skipping
     * @return
     */
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

    /**
     *
     * @deprecated
     * @param matrix similarity matrix
     * @return new matrix having each block value of maximum possible sum using forward skipping
     */
    public double[][] generateSummationMatrix(double[][] matrix) {
        double[][] sumM = new double[matrix.length][matrix[0].length];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                sumM[i][j] = getMaxSumSubMatrix(i, j, matrix, 0, -1);
            }
        }
        return sumM;
    }

    /**
     *
     * @param matrix similarity matrix
     * @return new matrix having each block value of maximum possible sum using forward skipping
     */
    public double[][] generateSummationMatrixOptimized(double[][] matrix) {
        int row = matrix.length;
        int col = matrix[0].length;

        double[][] sumMatrix = new double[row][col];
        for (int i = 0; i < row; i++)
            sumMatrix[i][col - 1] = matrix[i][col - 1];
        for (int j = 0; j < col; j++)
            sumMatrix[row - 1][j] = matrix[row - 1] [j];

        for (int i = row - 2; i >= 0; i--) {
            for (int j = 0; j < col - 1; j++) {
                double max = -99;
                for (int k = j + 1; k < col; k++) {
                    if (matrix[i][j] + sumMatrix[i + 1][k] > max)
                        max = matrix[i][j] + sumMatrix[i + 1][k];
                }
                sumMatrix[i][j] = max;
            }
        }
        return sumMatrix;
    }

    /**
     *
     * @param matrix scored matrix
     * @return maximum value of a matrix
     */
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

        double[][] sumM = mfs.generateSummationMatrixOptimized(sm);
//        double[][] sumM = mfs.generateSummationMatrix(sm);
        System.out.println("\nMax value:" + mfs.getMaxValueMatrix(sumM));
    }
}
