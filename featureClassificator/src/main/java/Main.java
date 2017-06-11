import org.json.JSONArray;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class Main {
	
	public static void main(String[] args) {
		try {
			Class.forName("org.sqlite.JDBC");
			Connection connection = DriverManager.getConnection("jdbc:sqlite:dwtcTableManualClassificator/data.db");
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery("SELECT count(originalTableType), originalTableType FROM `table` GROUP BY originalTableType");
			while(resultSet.next()) {
				System.out.println("Original table type: " + resultSet.getString(2) + ": " + resultSet.getInt(1));
			}
			
			System.out.println("\n----------------------------------------\n");
			
			//convert ANDRA to OTHER etc. (caused by usage of google translator)
			PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `table` SET newTableType = ? WHERE newTableType=?");
			preparedStatement.setString(1, "OTHER");
			preparedStatement.setString(2, "ANDRA");
			
			preparedStatement.executeUpdate();
			
			preparedStatement.setString(1, "ENTITY");
			preparedStatement.setString(2, "ENTITET");
			
			preparedStatement.executeUpdate();
			
			preparedStatement.setString(1, "RELATIONSHIP");
			preparedStatement.setString(2, "RELATION");
			
			preparedStatement.executeUpdate();
			
			resultSet = statement.executeQuery("SELECT count(newTableType), newTableType FROM `table` GROUP BY newTableType");
			while(resultSet.next()) {
				System.out.println("New table type: " + resultSet.getString(2) + ": " + resultSet.getInt(1));
			}
			
			//parse database json contents
			resultSet = statement.executeQuery("SELECT * FROM `table`");
			
			Map<Integer, JSONArray> tables = new HashMap<Integer, JSONArray>();
			while(resultSet.next()) {
				tables.put(resultSet.getInt("id"), new JSONArray(resultSet.getString("cells")));
			}
			
			
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
