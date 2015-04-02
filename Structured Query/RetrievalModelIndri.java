/**
 * Created by Wei on 2/15/15.
 */
public class RetrievalModelIndri extends RetrievalModel {
    private double mu;
    private double lambda;

    public double getMu() {
        return mu;
    }

    public void setMu(double mu) {
        this.mu = mu;
    }

    public double getLambda() {
        return lambda;
    }

    public void setLambda(double lambda) {
        this.lambda = lambda;
    }

    @Override
    public boolean setParameter(String parameterName, double value) {
        return false;
    }

    @Override
    public boolean setParameter(String parameterName, String value) {
        return false;
    }
}
