import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

import java.sql.SQLException;

/**
 * @author: Julius Gonsior
 */
public class SaveClassifiedResultsToDatabase {

	public static void main(String[] args) {
		try {
			
			ConverterUtils.DataSource source = new ConverterUtils.DataSource("data.arff");
			Instances dataSet = source.getDataSet();
			dataSet.setClassIndex(dataSet.numAttributes()-1);
			
			// train new classifier
			RandomForest randomForest = new RandomForest();
			randomForest.buildClassifier(dataSet);
			
			for(int i=0; i<dataSet.numInstances(); i++) {
				String label =dataSet.classAttribute().value((int) randomForest.classifyInstance(dataSet.instance(i)));
				System.out.println("New: " + label);
			}
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}  catch (Exception e) {
			e.printStackTrace();
		}
	}
}