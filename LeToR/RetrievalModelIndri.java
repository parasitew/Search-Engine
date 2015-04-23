/**
 * Created by Wei on 2/15/15.
 */
public class RetrievalModelIndri extends RetrievalModel {
    private double mu;
    private double lambda;
    private boolean fb;
    private int fbDocs;
    private int fbTerms;
    private int fbMu;
    private double fbOrigWeight;
    private String fbExpansionQueryFile;
    private String fbInitialRankingFile;

    public String getFbInitialRankingFile() {
        return fbInitialRankingFile;
    }

    public void setFbInitialRankingFile(String fbInitialRankingFile) {
        this.fbInitialRankingFile = fbInitialRankingFile;
    }

    public RetrievalModelIndri() {
        this.fbExpansionQueryFile = null;
        this.fbInitialRankingFile = null;
    }

    public boolean isFb() {
        return fb;
    }

    public void setFb(boolean fb) {
        this.fb = fb;
    }

    public int getFbDocs() {
        return fbDocs;
    }

    public void setFbDocs(int fbDocs) {
        this.fbDocs = fbDocs;
    }

    public int getFbTerms() {
        return fbTerms;
    }

    public void setFbTerms(int fbTerms) {
        this.fbTerms = fbTerms;
    }

    public int getFbMu() {
        return fbMu;
    }

    public void setFbMu(int fbMu) {
        this.fbMu = fbMu;
    }

    public double getFbOrigWeight() {
        return fbOrigWeight;
    }

    public void setFbOrigWeight(double fbOrigWeight) {
        this.fbOrigWeight = fbOrigWeight;
    }

    public String getFbExpansionQueryFile() {
        return fbExpansionQueryFile;
    }

    public void setFbExpansionQueryFile(String fbExpansionQueryFile) {
        this.fbExpansionQueryFile = fbExpansionQueryFile;
    }

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
        this.lambda =  lambda;
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
