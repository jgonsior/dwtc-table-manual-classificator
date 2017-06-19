import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author: Julius Gonsior
 */
public class SaveClassifiedResultsToDatabase {

	public static void main(String[] args) {
		try {
			
			ConverterUtils.DataSource source = new ConverterUtils.DataSource("data.arff");
			Instances unfilteredDataSet = source.getDataSet();
			unfilteredDataSet.setClassIndex(unfilteredDataSet.numAttributes()-1);
			
			Instances trainingDataSet = unfilteredDataSet;
			Instances testDataSet = new Instances(unfilteredDataSet);
			
			Connection connection = DriverManager.getConnection("jdbc:sqlite:dwtcTableManualClassificator/data.db");
			PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `table` SET label = ? WHERE id=?");
			
			
			// train new classifier
			RandomForest randomForest = new RandomForest();
			randomForest.buildClassifier(trainingDataSet);
			
			
			Evaluation eval = new Evaluation(trainingDataSet);
			eval.evaluateModel(randomForest, testDataSet);
			System.out.println(eval.toSummaryString("\nResults\n======\n", false));
			System.out.println(eval.toMatrixString());
			
			
			for(int i=0; i<unfilteredDataSet.numInstances(); i++) {
				String label = unfilteredDataSet.classAttribute().value((int) randomForest.classifyInstance(unfilteredDataSet.instance(i)));
				double id = unfilteredDataSet.instance(i).value(0);
				
				preparedStatement.setString(1, label);
				preparedStatement.setInt(2, (int) id);
				
				preparedStatement.executeUpdate();
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