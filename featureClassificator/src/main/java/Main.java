import com.google.common.base.Optional;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import webreduce.extraction.mh.features.FeaturesP2;
import webreduce.extraction.mh.tools.TableConvert;

import java.sql.*;

public class Main {
	
	public static void main(String[] args) {
		try {
			FeaturesP2 phase2Features = new FeaturesP2();
			TableConvert tableConvert = new TableConvert(2,2);
			
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
			
			resultSet = statement.executeQuery("SELECT * FROM `table`");
			
			while(resultSet.next()) {
				
				//parse database json contents
				JSONArray jsonArrayTable = new JSONArray(resultSet.getString("cells"));
				
				//convert json to html code
				String htmlTable = "<table>";
				for(int i=0; i<jsonArrayTable.length(); i++) {
					htmlTable += "<tr>";
					JSONArray row = jsonArrayTable.getJSONArray(i);
					for(int j=0; j<row.length(); j++) {
						String cell = row.getString(j);
						htmlTable += "<td>" + cell+ "</td>";
					}
					htmlTable += "</tr>";
				}
				htmlTable += "</table>";
				
				//let jsoup parse the html code again
				Document document = Jsoup.parse(htmlTable);
				
				Element table = document.select("table").get(0);
				Optional<Element[][]> convertedTable = tableConvert.toTable(table);
				if(!convertedTable.isPresent()) {
					System.out.println("No converted table present…exiting because computer is sad…");
					return;
				}
				
				
				
				
				System.out.println("test");
				
			}
			
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
