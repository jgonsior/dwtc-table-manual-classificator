import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.*;

/**
 * @author: Julius Gonsior
 */
public class Test {
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {
		Class.forName("org.sqlite.JDBC");
		Connection connection = DriverManager.getConnection("jdbc:sqlite:dwtcTableManualClassificator/data.db");
		
		Statement getAllTablesStatement = connection.createStatement();
		
		PreparedStatement addHtmlStatement = connection.prepareStatement("UPDATE `table` SET label = ? WHERE id=?");
		
		
		ResultSet resultSet = getAllTablesStatement.executeQuery("SELECT * FROM `table`");
		while (resultSet.next()) {
			String url = "https://commoncrawl.s3.amazonaws.com/" + resultSet.getString("s3Link").replaceFirst("^common-crawl/", "");
			System.out.println("Start processing " + url);
			
			InputStream inputstream = new URL(url).openStream();

			WarcReader warcReader = WarcReaderFactory.getReader(inputstream);
			WarcRecord warcRecord;
			
			while ((warcRecord = warcReader.getNextRecord()) != null) {
				if (warcRecord.getStartOffset() < resultSet.getLong("recordOffset")) {
					continue;
				}
				
				byte[] rawContent = IOUtils.toByteArray(warcRecord
						.getPayloadContent());
				
				Document doc;
				
				// try parsing with charset detected from doc
				doc = Jsoup.parse(new ByteArrayInputStream(
						rawContent), null, "");
				
				
				if (doc.title().equals(resultSet.getString("pageTitle"))) {
					Elements tables = doc.select("table");
					Element table = tables.get(resultSet.getInt("tableNum"));
					
					
					addHtmlStatement.setString(1, table.html());
					addHtmlStatement.setInt(2, resultSet.getInt("id"));
					
					addHtmlStatement.executeUpdate();
					
					System.out.println(warcReader.getStartOffset());
					System.out.println(warcReader.getOffset());
					
					inputstream.close();
					
					break;
				} else {
					System.out.println("not yet found");
				}
			}
			
		}
	}
}