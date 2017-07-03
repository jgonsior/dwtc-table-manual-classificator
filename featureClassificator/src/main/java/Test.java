import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jwat.warc.WarcReader;
import org.jwat.warc.WarcReaderFactory;
import org.jwat.warc.WarcRecord;

import java.io.*;

/**
 * @author: Julius Gonsior
 */
public class Test {
	
	public static void main(String[] args) {
		String warcFile = "/tmp/CC-MAIN-20140707234032-00010-ip-10-180-212-248.ec2.internal.warc.gz";
		long offset = 375128872;
		
		try {
			InputStream inputstream = new FileInputStream(warcFile);
			WarcReader warcReader = WarcReaderFactory.getReader(inputstream);
			
			//WarcRecord warcRecord = WarcReaderFactory.getReaderCompressed().getNextRecordFrom(inputstream, offset);
			WarcRecord warcRecord;
			
			
			while ((warcRecord = warcReader.getNextRecord()) != null) {
				if (warcRecord.getStartOffset() < offset)
					continue;
				byte[] rawContent = IOUtils.toByteArray(warcRecord
						.getPayloadContent());
				
				Document doc;
				
				// try parsing with charset detected from doc
				doc = Jsoup.parse(new ByteArrayInputStream(
						rawContent), null, "");
				
				Elements tables = doc.select("table");
				Element table = tables.get(16);
				System.out.println(table.html());
				
				
				if (doc.title().equals("Petition For Nick To Fix His \"Tooth\" - MMA Forum")) {
					//System.out.println(doc.html());
					System.out.println(warcReader.getStartOffset());
					System.out.println(warcReader.getOffset());
					
					long foundOffset = warcReader.getOffset();
					
					inputstream.close();
					
					
					InputStream inputstream2 = new FileInputStream(warcFile);
					
					WarcRecord warcRecord2 = WarcReaderFactory.getReaderCompressed().getNextRecordFrom(inputstream2, foundOffset);
					byte[] rawContent2 = IOUtils.toByteArray(warcRecord2
							.getPayloadContent());
					
					Document doc2;
					
					// try parsing with charset detected from doc
					doc2 = Jsoup.parse(new ByteArrayInputStream(
							rawContent2), null, "");
					System.out.println(doc2.html());
					
					
					return;
					
					
				}
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
}