package evaluation;

/**
 * @author DANISH AHMED on 3/24/2019
 */
public class EvaluationUtils {
    public static double calculatePrecision(double truePositive, double falsePositive) {
        double precision = (truePositive / (truePositive + falsePositive));
        if (Double.isNaN(precision))
            return 0.0;
        return precision;
    }

    public static double calculateRecall(double truePositive, double falseNegative) {
        double recall = (truePositive / (truePositive + falseNegative));
        if (Double.isNaN(recall))
            return 0.0;
        return recall;
    }

    public static double fMeasure(double precision, double recall) {
        double fMeasure = (2 * precision * recall) / (precision + recall);
        if (Double.isNaN(fMeasure))
            return 0.0;
        return fMeasure;
    }
}