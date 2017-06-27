package webreduce.extraction.mh.tools;

import com.google.common.base.CharMatcher;
import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CellTools {
	
	
	// returns the ContentType a cell contains
	// the different types are exclusive herein!
	public static ContentType getContentType(Element cellContent) {
		
		// Tags are NOT valued equally, <form> takes priority
		// and <img> is the least important
		// e.g. an image within a form is most likely an icon or visual hint
		// e.g. an image within in an anchor tag is most likely the link's
		// visual representation
		// e.g. an anchor within a form is mostly likely a link to a help page
		// or something related to the form's input fields (forgot password link)
		if (cellContent.getElementsByTag("form").size() > 0) {
			return ContentType.FORM;
		} else if (cellContent.getElementsByTag("a").size() > 0) {
			return ContentType.HYPERLINK;
		} else if (cellContent.getElementsByTag("img").size() > 0) {
			return ContentType.IMAGE;
		} else {
			// no relevant tags -> inspect content
			
			// clean and replace all invisible characters (this removes white spaces!)
			String cellStr = cleanCell(cellContent.text()).replaceAll("\\s+", "");
			
			if (cellStr.length() > 0) {
				
				// count occurrences of alphabetical and numerical
				// characters within the content string
				int alphaCount = 0, digitCount = 0;
				for (char c : cellStr.toCharArray()) {
					if (Character.isAlphabetic(c)) {
						alphaCount++;
					} else if (Character.isDigit(c)) {
						digitCount++;
					}
				}
				
				if ((alphaCount + digitCount) == 0) {
					// neither alphabetical nor numerical
					return ContentType.OTHERS;
				} else {
					
					// determine dominant type
					if (digitCount > alphaCount) {
						return ContentType.DIGIT;
					} else {
						return ContentType.ALPHABETICAL;
					}
				}
				
			} else {
				// empty string
				return ContentType.EMPTY;
			}
		}
	}
	
	// returns the cell content's length for a given cell
	// used for features which calculate results using this value
	// all cleaning should be done herein
	public static int getCellLength(Element cell) {
		return cell.text().length();
	}
	
	public static boolean isNumericOnly(String str) {
		try {
			Double.parseDouble(str);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}
	
	// cleans up cell's string content using JSoup
	public static String cleanCell(String cell) {
		cell = Jsoup.clean(cell, Whitelist.simpleText());
		cell = StringEscapeUtils.unescapeHtml4(cell);
		cell = CharMatcher.WHITESPACE.trimAndCollapseFrom(cell, ' ');
		return cell;
	}
	
	public static Boolean containsWhitespace(String cellContent) {
		return Pattern.compile("\\s").matcher(cellContent).find();
	}
	
	/**
	 * Returns true if there is at least one four digits integer (exactly 4 digits!) which is in the range of 1000 to 2100
	 * @param cellContent
	 * @return
	 */
	public static boolean detectYear(String cellContent) {
		Matcher matcher = Pattern.compile("(?:^|\\D)(\\d{4})(?=\\D|$)").matcher(cellContent);
		while (matcher.find()) {
			if (Integer.parseInt(matcher.group(1)) > 1000 && Integer.parseInt(matcher.group(1)) < 2100) {
				return true;
			}
		}
		return false;
	}
	
	public static double caluclateAverageCellLengthRowStd(JSONArray jsonArrayTable) {
		int[] averageStds = new int[jsonArrayTable.length()];
		for (int i = 0; i < jsonArrayTable.length(); i++) {
			JSONArray row = jsonArrayTable.getJSONArray(i);
			for (int j = 0; j < row.length(); j++) {
				averageStds[i] += row.getString(j).length();
			}
		}
		double mean = Arrays.stream(averageStds).average().getAsDouble();
		int squareSum=0;
		for(int i=0; i<averageStds.length;i++) {
			squareSum += Math.pow(averageStds[i] - mean, 2) / averageStds.length;
		}
		return Math.sqrt(squareSum/(averageStds.length-1));
	}
	
	public static double caluclateAverageCellLengthColumnStd(JSONArray jsonArrayTable) {
		
		int[] averageStds = new int[jsonArrayTable.getJSONArray(0).length()];
		for (int i = 0; i < jsonArrayTable.length(); i++) {
			JSONArray row = jsonArrayTable.getJSONArray(i);
			for (int j = 0; j < row.length(); j++) {
				averageStds[j] += row.getString(j).length();
			}
		}
		
		double mean = Arrays.stream(averageStds).average().getAsDouble();
		int squareSum=0;
		for(int i=0; i<averageStds.length;i++) {
			squareSum += Math.pow(averageStds[i] - mean, 2) / averageStds.length;
		}
		return Math.sqrt(squareSum/(averageStds.length-1));
	}
	
	public static JSONArray transposeTable(JSONArray jsonArrayTable) {
		JSONArray result = new JSONArray();
		result.
		return result;
	}
}
