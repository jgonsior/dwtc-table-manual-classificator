import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

import java.sql.SQLException;
import java.util.Random;

/**
 * @author: Julius Gonsior
 */
public class TrainAndCompareClassifier {
	
	public static void main(String[] args) {
		try {
			
			DataSource source = new DataSource("data.arff");
			Instances unfilteredDataSet = source.getDataSet();
			unfilteredDataSet.setClassIndex(unfilteredDataSet.numAttributes() - 1);
			
			
			// train new classifier
			RandomForest randomForest = new RandomForest();
			randomForest.setOptions(weka.core.Utils.splitOptions("-K 0 -S 1"));
			//randomForest.buildClassifier(dataSet);
			
			// filter out id
			Remove remove = new Remove();
			remove.setAttributeIndices("1");
			remove.setInputFormat(unfilteredDataSet);
			
			Instances dataSet = Filter.useFilter(unfilteredDataSet, remove);
			
			// evaluate
			Evaluation newEvaluation = new Evaluation(dataSet);
			newEvaluation.crossValidateModel(randomForest, dataSet, 10, new Random());
			
			System.out.println(newEvaluation.toSummaryString());
			System.out.println(newEvaluation.toClassDetailsString());
			System.out.println(newEvaluation.toMatrixString());
			
			System.out.println("----------------------------------------");
			
			weka.core.SerializationHelper.write("RandomForest2017_P2.mdl", randomForest);
			
			
			// compare with results from old Classifier
			Classifier oldPhase2Classifier = (Classifier) weka.core.SerializationHelper.read("RandomForest_P2.mdl");
			
			Evaluation oldEvaluation = new Evaluation(dataSet);
			oldEvaluation.evaluateModel(oldPhase2Classifier, dataSet);
			
			System.out.println(oldEvaluation.toSummaryString());
			System.out.println(oldEvaluation.toClassDetailsString());
			System.out.println(oldEvaluation.toMatrixString());
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}