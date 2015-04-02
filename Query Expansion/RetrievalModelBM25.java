/**
 * Created by Wei on 2/9/15.
 */
public class RetrievalModelBM25 extends RetrievalModel {
    private double k1;
    private double k3;
    private double b;

    public double getK1() {
        return k1;
    }

    public void setK1(double k1) {
        this.k1 = k1;
    }

    public double getK3() {
        return k3;
    }

    public void setK3(double k3) {
        this.k3 = k3;
    }

    public double getB() {
        return b;
    }

    public void setB(double b) {
        this.b = b;
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
