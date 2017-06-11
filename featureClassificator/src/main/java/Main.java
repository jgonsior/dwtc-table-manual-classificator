import java.sql.*;
public class Main {
	
	public static void main(String[] args) {
		//1. load database tables into jsoup objects
		Connection connection = null;
		try {
			Class.forName("org.sqlite.JDBC");
			connection = DriverManager.getConnection("jdbc:sqlite:dwtcTableManualClassificator/data.db");
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery("SELECT count(originalTableType), originalTableType FROM `table` GROUP BY originalTableType");
			while(resultSet.next()) {
				System.out.println("Original table type: " + resultSet.getString(2) + ": " + resultSet.getInt(1));
			}
			
			System.out.println("\n----------------------------------------\n");
			
			resultSet = statement.executeQuery("SELECT count(newTableType), newTableType FROM `table` GROUP BY newTableType");
			while(resultSet.next()) {
				System.out.println("New table type: " + resultSet.getString(2) + ": " + resultSet.getInt(1));
			}
			
			
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
