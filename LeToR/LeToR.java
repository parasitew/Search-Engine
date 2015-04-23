import java.util.List;

/**
 * Created by Tong on 4/19/15.
 */
public class LeToR {
    public String getTrainingQueryFile() {
        return trainingQueryFile;
    }

    public void setTrainingQueryFile(String trainingQueryFile) {
        this.trainingQueryFile = trainingQueryFile;
    }

    public String getTrainingQrelsFile() {
        return trainingQrelsFile;
    }

    public void setTrainingQrelsFile(String trainingQrelsFile) {
        this.trainingQrelsFile = trainingQrelsFile;
    }

    public String getTrainingFeatureVectorsFile() {
        return trainingFeatureVectorsFile;
    }

    public void setTrainingFeatureVectorsFile(String trainingFeatureVectorsFile) {
        this.trainingFeatureVectorsFile = trainingFeatureVectorsFile;
    }

    public String getPageRankFile() {
        return pageRankFile;
    }

    public void setPageRankFile(String pageRankFile) {
        this.pageRankFile = pageRankFile;
    }

    public String getFeatureDisable() {
        return featureDisable;
    }

    public void setFeatureDisable(String featureDisable) {
        this.featureDisable = featureDisable;
    }

    public String getSvmRankLearnPath() {
        return svmRankLearnPath;
    }

    public void setSvmRankLearnPath(String svmRankLearnPath) {
        this.svmRankLearnPath = svmRankLearnPath;
    }

    public String getSvmRankClassifyPath() {
        return svmRankClassifyPath;
    }

    public void setSvmRankClassifyPath(String svmRankClassifyPath) {
        this.svmRankClassifyPath = svmRankClassifyPath;
    }

    public String getSvmRankParamC() {
        return svmRankParamC;
    }

    public void setSvmRankParamC(String svmRankParamC) {
        this.svmRankParamC = svmRankParamC;
    }

    public String getSvmRankModelFile() {
        return svmRankModelFile;
    }

    public void setSvmRankModelFile(String svmRankModelFile) {
        this.svmRankModelFile = svmRankModelFile;
    }

    public String getTestingFeatureVectorsFile() {
        return testingFeatureVectorsFile;
    }

    public void setTestingFeatureVectorsFile(String testingFeatureVectorsFile) {
        this.testingFeatureVectorsFile = testingFeatureVectorsFile;
    }

    public String getTestingDocumentScores() {
        return testingDocumentScores;
    }

    public void setTestingDocumentScores(String testingDocumentScores) {
        this.testingDocumentScores = testingDocumentScores;
    }

    public int getFeatureNum() {
        return featureNum;
    }

    public void setFeatureNum(int featureNum) {
        this.featureNum = featureNum;
    }

    public List<Double> getFeatures() {
        return features;
    }

    public void setFeatures(List<Double> features) {
        this.features = features;
    }

    private String trainingQueryFile;
    private String trainingQrelsFile;
    private String trainingFeatureVectorsFile;
    private String pageRankFile;
    private String featureDisable;
    private String svmRankLearnPath;
    private String svmRankClassifyPath;
    private String svmRankParamC;
    private String svmRankModelFile;
    private String testingFeatureVectorsFile;
    private String testingDocumentScores;

    private int featureNum;
    private List<Double> features;

    public LeToR() {

    }

}
