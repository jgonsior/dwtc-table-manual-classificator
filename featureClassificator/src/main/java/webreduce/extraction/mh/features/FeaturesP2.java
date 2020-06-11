package webreduce.extraction.mh.features;

/*
 * Class contains only feature calculations for phase 2
 * (multiclass classification for non-layout tables)
 */

import org.jsoup.nodes.Element;
import webreduce.extraction.mh.tools.CellTools;
import webreduce.extraction.mh.tools.ContentType;
import webreduce.extraction.mh.tools.TableStats;
import webreduce.extraction.mh.tools.Tools;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;

import java.util.*;
import java.util.regex.Pattern;

public class FeaturesP2 {
	
	private static String usedAttributes = "1,3,4,6,23,46,52,54,65,77,88,110,121,127,139,148,149,151,156,158,174,190,217,223,224";
	//"1,2,4,5,11,24,39,47,53,66,84,121,123,128,149,151,152,157,162,179,192,224";
	// ""; //"1,2,13,15,27,40,47,52,53,54,55,58,60,63,64,65,67,78,88,90,92,94,95,97";
	
	// most of the local features are calculated in batches for all rows/colums
	// we need a whitelist to filter out those columns and rows we don't need
	private static String featureWhiteList = //"ID, " +
			"CUMULATIVE_CONTENT_CONSISTENCY, " +
			"AVG_CELL_LENGTH, " +
			"AVG_COLS, " +
			"AVG_ROWS, " +
			"RATIO_ALPHABETICAL, " +
			"STD_DEV_COLS, " +
			"MAX_ROWS, " +
			"MAX_COLS, " +
			"AREA_SIZE, " +
			"RATIO_EMPTY_CELLS, " +
			"STD_DEV_ROWS, " +
			"";
	
	static {
		List<String> whiteListFeatures = new LinkedList<>();
		whiteListFeatures.add("LOCAL_RATIO_EMPTY");
		whiteListFeatures.add("LOCAL_EMPTY_VARIANCE");
		whiteListFeatures.add("LOCAL_DIGIT_AMOUNT_VARIANCE");
		whiteListFeatures.add("LOCAL_AVG_LENGTH");
		whiteListFeatures.add("LOCAL_LENGTH_VARIANCE");
		whiteListFeatures.add("LOCAL_RATIO_ANCHOR");
		whiteListFeatures.add("LOCAL_RATIO_IMAGE");
		whiteListFeatures.add("LOCAL_RATIO_INPUT");
		whiteListFeatures.add("LOCAL_RATIO_SELECT");
		whiteListFeatures.add("LOCAL_RATIO_COLON");
		whiteListFeatures.add("LOCAL_RATIO_COMMA");
		whiteListFeatures.add("LOCAL_RATIO_CONTAINS_NUMBER");
		whiteListFeatures.add("LOCAL_RATIO_HEADER");
		whiteListFeatures.add("LOCAL_RATIO_CONTAINS_WHITESPACE");
		whiteListFeatures.add("LOCAL_RATIO_SPECIAL_CHAR");
		whiteListFeatures.add("LOCAL_RATIO_PERCENTAGE");
		whiteListFeatures.add("LOCAL_RATIO_CONTAINS_YEAR");
		whiteListFeatures.add("LOCAL_RATIO_IS_NUMBER");
		
		
		/**
		 * 0,1 -> first two rows/cols
		 * 2,3 -> the two middle most rows
		 * 4,5 -> the last tow rows
		 */
		for (String feature : whiteListFeatures) {
			featureWhiteList += feature + "_COL_0, ";
			featureWhiteList += feature + "_COL_1, ";
			featureWhiteList += feature + "_COL_2, ";
			featureWhiteList += feature + "_COL_3, ";
			featureWhiteList += feature + "_COL_4, ";
			featureWhiteList += feature + "_COL_5, ";
			featureWhiteList += feature + "_ROW_0, ";
			featureWhiteList += feature + "_ROW_1, ";
			featureWhiteList += feature + "_ROW_2, ";
			featureWhiteList += feature + "_ROW_3, ";
			featureWhiteList += feature + "_ROW_4, ";
			featureWhiteList += feature + "_ROW_5, ";
		}
		
	}
	
	/**
	 * <p>
	 * neue features:
	 * - std_dev for empty cells?
	 * - CUMULATIVE_CONTENT_CONSISTENCY zusätzlich für alles außer 0,1,2 (row + column), außer es gibt nicht so viele
	 * - testen ob es Sinn  macht sowas wie CUMULATIVE_CONTENT_CONSISTENCY für ohne 0,1,2 auch für sachen wie LOCAL zu implementieren (ERST AM ENDE)
	 */
	
	private ArrayList<AbstractTableListener> globalListeners;
	private ArrayList<AbstractTableListener> localListeners;
	private ArrayList<Attribute> attributeList;
	
	private FastVector attributeVector; // vector of all attributes PLUS class attribute
	private FastVector classAttrVector; // vector of strings of all possible class values
	private Attribute classAttr;
	
	public FeaturesP2() {
		attributeList = new ArrayList<Attribute>();
		attributeVector = new FastVector();


		Attribute idAttr = new Attribute("ID");
		attributeList.add(idAttr);
		attributeVector.addElement(idAttr);


		for (String s : FeaturesP2.getFeatureNames()) {
			Attribute newAttr = new Attribute(s); // create new Feature with name from whitelist
			attributeList.add(newAttr);
			attributeVector.addElement(newAttr);
		}

		classAttrVector = new FastVector(4);
		classAttrVector.addElement("LAYOUT");
		classAttrVector.addElement("RELATION");
		classAttrVector.addElement("ENTITY");
		classAttrVector.addElement("MATRIX");
		classAttrVector.addElement("OTHER");
		classAttr = new Attribute("CLASS", classAttrVector);
		
		attributeVector.addElement(classAttr);

	}
	
	public static List<String> getFeatureNames() {
		ArrayList<String> allFeatureNames = new ArrayList<>(Arrays.asList(featureWhiteList.split(", ")));
		if (usedAttributes.equals("")) {
			return allFeatureNames;
		}
		List<String> featureNames = new LinkedList<>();
		for (String attributeNumberString : usedAttributes.split(",")) {
			featureNames.add(allFeatureNames.get(Integer.parseInt(attributeNumberString) - 1));
		}

		return allFeatureNames;
	}
	
	// returns a FastVector containing all attributes plus
	// the class attribute as the last element
	public FastVector getAttrVector() {
		return attributeVector;
	}
	
	// returns a FastVector that contains all possible class
	// values, each represented as a String
	public FastVector getClassVector() {
		return classAttrVector;
	}
	
	// returns an ArrayList of all attributes that
	// are used for this feature phase
	public ArrayList<Attribute> getAttrList() {
		return attributeList;
	}
	
	// adds all desired features to the computation list
	public void initializeFeatures() {
		// Add global features to computation list
		globalListeners = new ArrayList<AbstractTableListener>();
		globalListeners.add(new RatioEmptyCells());
		globalListeners.add(new AreaSize());
		globalListeners.add(new MaxCols());
		globalListeners.add(new MaxRows());
		globalListeners.add(new AvgRows());
		globalListeners.add(new AvgCols());
		globalListeners.add(new AvgCellLength());
		globalListeners.add(new StdDevRows());
		globalListeners.add(new StdDevCols());
		globalListeners.add(new ContentRatios());
		globalListeners.add(new CumulativeContentTypeConsistency());
		
		// Add local features to computation list
		localListeners = new ArrayList<AbstractTableListener>();
		localListeners.add(new LocalAvgLength());
		localListeners.add(new LocalContentRatios());
		localListeners.add(new LocalLengthVariance());
		localListeners.add(new LocalEmptyRatio());
		localListeners.add(new LocalEmptyVariance());
		localListeners.add(new LocalDigitAmountVariance());
		
	}
	
	public Instance computeFeatures(Element[][] convertedTable) {
		HashMap<String, Double> resultMap = new HashMap<String, Double>();

		TableStats tStats = new TableStats(convertedTable[0].length, convertedTable.length);
		
		initializeFeatures();
		
		// GLOBAL FEATURES
		
		// initialization event
		for (AbstractTableListener listener : globalListeners) {
			listener.start(tStats);
		}
		
		for (tStats.rowIndex = 0; tStats.rowIndex < tStats.getTableHeight(); tStats.rowIndex++) {
			for (tStats.colIndex = 0; tStats.colIndex < tStats.getTableWidth(); tStats.colIndex++) {
				
				// onCell event
				for (AbstractTableListener listener : globalListeners) {
					listener.computeCell(convertedTable[tStats.rowIndex][tStats.colIndex], tStats);
				}
				
			}
			
		}
		
		// end event
		for (AbstractTableListener listener : globalListeners) {
			listener.end();
		}
		
		// compute results of all listeners and put them into the result map
		for (AbstractTableListener listener : globalListeners) {
			resultMap.putAll(listener.getResults());
		}
		
		// LOCAL FEATURES
		
		// PER-ROW
		// get the 2 first and last rowS or last row?
		
		List<Integer> localRowIndexes = new LinkedList<>();
		localRowIndexes.add(0);
		localRowIndexes.add(1);
		localRowIndexes.add((int) Math.floor(tStats.getTableHeight() / 2));
		localRowIndexes.add((int) Math.ceil(tStats.getTableHeight() / 2));
		localRowIndexes.add(tStats.getTableHeight() - 2);
		localRowIndexes.add(tStats.getTableHeight() - 1);
		
		int i = 0;
		
		for (Integer currentRowIndex : localRowIndexes) {
			// initialization event
			for (AbstractTableListener listener : localListeners) {
				listener.start(tStats);
			}
			
			// iterate cells within row
			for (tStats.colIndex = 0; tStats.colIndex < tStats.getTableWidth(); tStats.colIndex++) {
				// onCell event
				for (AbstractTableListener listener : localListeners) {
					listener.computeCell(convertedTable[currentRowIndex][tStats.colIndex], tStats);
				}
			}
			
			// onRowEnd event
			for (AbstractTableListener listener : localListeners) {
				listener.end();
			}
			
			// compute results of all listeners, rename them and put them
			// into the result map
			for (AbstractTableListener listener : localListeners) {
				HashMap<String, Double> results = listener.getResults();
				for (Map.Entry<String, Double> entry : results.entrySet()) {
					// insert as ORIGINAL_ATTRIBUTE_NAME_ROW_X where ROW_X is the
					// specific row of the current loop
					resultMap.put(entry.getKey() + "_ROW_" + i, entry.getValue());
				}
				
			}
			i++;
		}
		
		// PER-COL
		// get the 2 first and last columns
		List<Integer> localColIndexes = new LinkedList<>();
		localColIndexes.add(0);
		localColIndexes.add(1);
		localColIndexes.add((int) Math.floor(tStats.getTableWidth() / 2));
		localColIndexes.add((int) Math.ceil(tStats.getTableWidth() / 2));
		localColIndexes.add(tStats.getTableWidth() - 2);
		localColIndexes.add(tStats.getTableWidth() - 1);
		
		i = 0;
		for (Integer currentColIndex : localColIndexes) {
			
			// initialization event
			for (AbstractTableListener listener : localListeners) {
				listener.start(tStats);
			}
			
			// iterate cells within column
			for (tStats.rowIndex = 0; tStats.rowIndex < tStats.getTableHeight(); tStats.rowIndex++) {
				// onCell event
				for (AbstractTableListener listener : localListeners) {
					listener.computeCell(convertedTable[tStats.rowIndex][currentColIndex], tStats);
				}
			}
			
			// onColEnd event
			for (AbstractTableListener listener : localListeners) {
				listener.end();
			}
			
			// compute results of all listeners, rename them and put them
			// into the result map
			for (AbstractTableListener listener : localListeners) {
				HashMap<String, Double> results = listener.getResults();
				for (Map.Entry<String, Double> entry : results.entrySet()) {
					// insert as ORIGINAL_ATTRIBUTE_NAME_COL_X where COL_X is the
					// specific column of the current loop
					resultMap.put(entry.getKey() + "_COL_" + i, entry.getValue());
				}
				
			}
			i++;
		}
		
		// Create WEKA instance

//		Instance resultInstance = new Instance(featureCount);
//		
//		// only use features within whitelist
//		for (String entry : resultMap.keySet()) {
//			if (featureWhiteList.contains(entry)) {
//				Attribute newAttr = new Attribute(entry);
//				resultInstance.setValue(newAttr, resultMap.get(entry));
//			}
//		}

		// just put a random dummy value here, get's overwritten later anyway
		resultMap.put("ID", new Double(42));


		return Tools.createInstanceFromData(resultMap, attributeList, attributeVector);
	}
	
	
	/**
	 * Features are implemented according to an Observer Pattern
	 */
	public abstract class AbstractTableListener {
		
		protected String featureName = "ABSTRACT_TABLE_LISTENER";
		
		public void start(TableStats stats) {
			initialize(stats);
		}
		
		/**
		 * should be called once the table is ready for iteration
		 *
		 * @param stats
		 */
		protected abstract void initialize(TableStats stats);
		
		
		public void computeCell(Element content, TableStats stats) {
			onCell(content, stats);
		}
		
		/**
		 * should be called each time a cell is inspected by the subject
		 */
		protected abstract void onCell(Element content, TableStats stats);
		
		public void end() {
			finalize();
		}
		
		public String getFeatureName() {
			return featureName;
		}
		
		/**
		 * should be called once the table iteration has finished
		 */
		protected abstract void finalize();
		
		/**
		 * pairs of feature names and feature values are given as result
		 */
		public abstract HashMap<String, Double> getResults();
	}
	
	////
	// GLOBAL FEATURES DEFINITION
	///
	
	// template
	public class BlankTableListener extends AbstractTableListener {
		
		public BlankTableListener() {
			featureName = "BLANK_FEATURE";
		}
		
		public void initialize(TableStats stats) {
		
		}
		
		public void onCell(Element content, TableStats stats) {
		
		}
		
		public void finalize() {
		
		}
		
		public HashMap<String, Double> getResults() {
			HashMap<String, Double> result = new HashMap<String, Double>();
			result.put(featureName, new Double(0));
			return result;
		}
	}
	
	public class AreaSize extends AbstractTableListener {
		
		private double areaSize;
		
		public AreaSize() {
			featureName = "AREA_SIZE";
		}
		
		public void initialize(TableStats stats) {
			areaSize = stats.getTableWidth() * stats.getTableHeight();
		}
		
		public void onCell(Element content, TableStats stats) {
		
		}
		
		public void finalize() {
		
		}
		
		public HashMap<String, Double> getResults() {
			HashMap<String, Double> result = new HashMap<String, Double>();
			result.put(featureName, new Double(areaSize));
			return result;
		}
	}
	
	public class RatioEmptyCells extends AbstractTableListener {
		
		private double cellCountEmpty;
		private double cellCount;
		
		public RatioEmptyCells() {
			featureName = "RATIO_EMPTY_CELLS";
		}
		
		public void initialize(TableStats stats) {
			cellCountEmpty = 0;
			cellCount = stats.getTableWidth() * stats.getTableHeight();
		}
		
		public void onCell(Element content, TableStats stats) {
			if (content == null) {
				cellCountEmpty++;
			}
		}
		
		public void finalize() {
		
		}
		
		public HashMap<String, Double> getResults() {
			HashMap<String, Double> result = new HashMap<String, Double>();
			
			// the ratio is 1 if there are no empty cells, and 0 if there are a lot
			result.put(featureName, new Double(cellCountEmpty / cellCount));
			return result;
		}
	}
	
	public class MaxCols extends AbstractTableListener {
		
		private double maxCols;
		
		public MaxCols() {
			featureName = "MAX_COLS";
		}
		
		public void initialize(TableStats stats) {
			maxCols = stats.getTableWidth();
		}
		
		public void onCell(Element content, TableStats stats) {
		
		}
		
		public void finalize() {
		
		}
		
		public HashMap<String, Double> getResults() {
			HashMap<String, Double> result = new HashMap<String, Double>();
			result.put(featureName, new Double(maxCols));
			return result;
		}
	}
	
	
	public class MaxRows extends AbstractTableListener {
		
		private double maxRows;
		
		public MaxRows() {
			featureName = "MAX_ROWS";
		}
		
		public void initialize(TableStats stats) {
			maxRows = stats.getTableHeight();
		}
		
		public void onCell(Element content, TableStats stats) {
		
		}
		
		public void finalize() {
		
		}
		
		public HashMap<String, Double> getResults() {
			HashMap<String, Double> result = new HashMap<String, Double>();
			result.put(featureName, new Double(maxRows));
			return result;
		}
	}
	
	public class AvgCols extends AbstractTableListener {
		
		private int cellCountNotEmpty;
		private double avgCols;
		private int tableHeight;
		
		public AvgCols() {
			featureName = "AVG_COLS";
		}
		
		public void initialize(TableStats stats) {
			cellCountNotEmpty = 0;
			tableHeight = stats.getTableHeight();
		}
		
		public void onCell(Element content, TableStats stats) {
			if (content != null) {
				cellCountNotEmpty++;
			}
		}
		
		public void finalize() {
			avgCols = ((double) cellCountNotEmpty) / ((double) tableHeight);
		}
		
		public HashMap<String, Double> getResults() {
			HashMap<String, Double> result = new HashMap<String, Double>();
			result.put(featureName, new Double(avgCols));
			return result;
		}
	}
	
	public class AvgCellLength extends AbstractTableListener {
		
		private int cellCountNotEmpty;
		private int totalLength;
		private double avgLength;
		
		public AvgCellLength() {
			featureName = "AVG_CELL_LENGTH";
		}
		
		public void initialize(TableStats stats) {
			cellCountNotEmpty = 0;
			totalLength = 0;
		}
		
		public void onCell(Element content, TableStats stats) {
			if (content != null) {
				cellCountNotEmpty++;
				// totalLength += content.text().length();
				totalLength += CellTools.getCellLength(content);
			}
		}
		
		public void finalize() {
			avgLength = ((double) totalLength) / ((double) cellCountNotEmpty);
		}
		
		public HashMap<String, Double> getResults() {
			HashMap<String, Double> result = new HashMap<String, Double>();
			result.put(featureName, new Double(avgLength));
			return result;
		}
	}
	
	public class StdDevRows extends AbstractTableListener {
		
		private double stdDevRows;
		private int[] rowNum;
		
		public StdDevRows() {
			featureName = "STD_DEV_ROWS";
		}
		
		public void initialize(TableStats stats) {
			stdDevRows = 0;
			rowNum = new int[stats.getTableWidth()];
			
			for (int i = 0; i < rowNum.length; i++) {
				rowNum[i] = 0;
			}
		}
		
		public void onCell(Element content, TableStats stats) {
			if (content != null) {
				rowNum[stats.colIndex] += 1;
			}
		}
		
		public void finalize() {
			double sum = 0;
			double avgRows = getAvgRows();
			for (int colIndex = 0; colIndex < rowNum.length; colIndex++) {
				double temp = rowNum[colIndex] - avgRows;
				sum += Math.pow(temp, 2);
			}
			stdDevRows = Math.sqrt(sum / rowNum.length);
		}
		
		private double getAvgRows() {
			int tempSum = 0;
			for (int colIndex = 0; colIndex < rowNum.length; colIndex++) {
				tempSum += rowNum[colIndex];
			}
			return (double) tempSum / (double) rowNum.length;
		}
		
		public HashMap<String, Double> getResults() {
			HashMap<String, Double> result = new HashMap<String, Double>();
			result.put(featureName, new Double(stdDevRows));
			return result;
		}
	}
	
	public class StdDevCols extends AbstractTableListener {
		
		private double stdDevCols;
		private int[] colNum;
		
		public StdDevCols() {
			featureName = "STD_DEV_COLS";
		}
		
		public void initialize(TableStats stats) {
			stdDevCols = 0;
			colNum = new int[stats.getTableHeight()];
			
			for (int i = 0; i < colNum.length; i++) {
				colNum[i] = 0;
			}
		}
		
		public void onCell(Element content, TableStats stats) {
			if (content != null) {
				colNum[stats.rowIndex] += 1;
			}
		}
		
		public void finalize() {
			double sum = 0;
			double avgCols = getAvgCols();
			for (int rowIndex = 0; rowIndex < colNum.length; rowIndex++) {
				double temp = colNum[rowIndex] - avgCols;
				sum += Math.pow(temp, 2);
			}
			stdDevCols = Math.sqrt(sum / colNum.length);
		}
		
		private double getAvgCols() {
			int tempSum = 0;
			for (int rowIndex = 0; rowIndex < colNum.length; rowIndex++) {
				tempSum += colNum[rowIndex];
			}
			return (double) tempSum / (double) colNum.length;
		}
		
		public HashMap<String, Double> getResults() {
			HashMap<String, Double> result = new HashMap<String, Double>();
			result.put(featureName, new Double(stdDevCols));
			return result;
		}
	}
	
	public class AvgRows extends AbstractTableListener {
		
		private int cellCountNotEmpty;
		private double avgRows;
		private int tableWidth;
		
		public AvgRows() {
			featureName = "AVG_ROWS";
		}
		
		public void initialize(TableStats stats) {
			cellCountNotEmpty = 0;
			tableWidth = stats.getTableWidth();
		}
		
		public void onCell(Element content, TableStats stats) {
			if (content != null) {
				cellCountNotEmpty++;
			}
		}
		
		public void finalize() {
			avgRows = ((double) cellCountNotEmpty) / ((double) tableWidth);
		}
		
		public HashMap<String, Double> getResults() {
			HashMap<String, Double> result = new HashMap<String, Double>();
			result.put(featureName, new Double(avgRows));
			return result;
		}
	}
	
	public class ContentRatios extends AbstractTableListener {
		
		private int cellCountNotEmpty, images, alphabetical, digits;
		private double image_ratio, alphabetical_ratio,
				digit_ratio;
		
		public ContentRatios() {
			featureName = "GROUP_GLOBAL_CONTENT_RATIOS";
		}
		
		public void initialize(TableStats stats) {
			cellCountNotEmpty = 0;
			images = 0;
			alphabetical = 0;
			digits = 0;
		}
		
		public void onCell(Element content, TableStats stats) {
			if (content != null) {
				cellCountNotEmpty++;
				
				ContentType ct = CellTools.getContentType(content);
				
				switch (ct) {
					case IMAGE:
						images++;
						break;
					case ALPHABETICAL:
						alphabetical++;
						break;
					case DIGIT:
						digits++;
						break;
					default:
						break;
				}
			}
		}
		
		public void finalize() {
			image_ratio = (cellCountNotEmpty > 0) ? ((double) images / (double) cellCountNotEmpty) : 0.0;
			alphabetical_ratio = (cellCountNotEmpty > 0) ? ((double) alphabetical / (double) cellCountNotEmpty) : 0.0;
			digit_ratio = (cellCountNotEmpty > 0) ? ((double) digits / (double) cellCountNotEmpty) : 0.0;
		}
		
		public HashMap<String, Double> getResults() {
			HashMap<String, Double> result = new HashMap<String, Double>();
			result.put("RATIO_IMG", new Double(image_ratio));
			result.put("RATIO_ALPHABETICAL", new Double(alphabetical_ratio));
			result.put("RATIO_DIGIT", new Double(digit_ratio));
			return result;
		}
	}
	
	
	public class CumulativeContentTypeConsistency extends AbstractTableListener {
		
		private double ctc_sum_r, ctc_r; // Cumulative Length Consistency of rows
		private double ctc_sum_c, ctc_c; // Cumulative Length Consistency of columns
		private double tableWidth, tableHeight;
		private double ctc;
		
		private ArrayList<ContentType> typesOfRow;
		private ArrayList<ContentType>[] typesOfCols;
		
		public CumulativeContentTypeConsistency() {
			featureName = "CUMULATIVE_CONTENT_CONSISTENCY";
		}
		
		public void initialize(TableStats stats) {
			
			// typesOfRow is a temporary array, recreated for each row, that keeps track
			// of each ContentType within the row
			typesOfRow = new ArrayList<ContentType>();
			tableWidth = stats.getTableWidth();
			tableHeight = stats.getTableHeight();
			
			// typesOfCols has one ArrayList-slot for each column in the table
			// thus each column has its own ArrayList of ContentTypes
			typesOfCols = new ArrayList[stats.getTableWidth()];
			for (int i = 0; i < typesOfCols.length; i++) {
				typesOfCols[i] = new ArrayList<ContentType>();
			}
		}
		
		// returns the dominant ContentType within a List of ContentType values
		// takes the ContentType enum's priority order into account
		private ContentType getDominantType(List<ContentType> list) {
			ContentType dominantType = ContentType.EMPTY;
			HashMap<ContentType, Integer> frequencyMap = new HashMap<ContentType, Integer>();
			
			// put all occurrences of ContentTypes into a map together with their frequency count
			for (ContentType ct : list) {
				if (!frequencyMap.containsKey(ct)) {
					frequencyMap.put(ct, Collections.frequency(list, ct));
				}
			}
			
			int maxCount = 0;
			for (ContentType ct : frequencyMap.keySet()) {
				int currentCount = frequencyMap.get(ct);
				
				if (currentCount > maxCount) {
					dominantType = ct; // new dominant type determined
				} else if (currentCount == maxCount) {
					
					// as the ContentType enum itself is sorted by priority of the
					// content types compare the ordinal values
					
					// replace the dominant type with the current one only if they both
					// have the same frequency but the current one has higher priority
					// (= lesser ordinal value)
					if (ct.ordinal() < dominantType.ordinal()) {
						dominantType = ct;
					}
				}
			}
			
			return dominantType;
		}
		
		public void onCell(Element content, TableStats stats) {
			
			// every cell that is non-empty
			if (content != null) {
				ContentType cellType = CellTools.getContentType(content);
				typesOfRow.add(cellType);
				typesOfCols[stats.colIndex].add(cellType);
			}
			
			/// ROWS - handle row end
			if (stats.colIndex == stats.getTableWidth() - 1) { // last column = row end reached
				
				// determine dominant type for row
				ContentType dominantType = getDominantType(typesOfRow);
				
				// compute the CTC for the current row
				double sumD = 0.0;
				for (ContentType ct : typesOfRow) {
					double d = (ct == dominantType) ? (d = 1.0) : (d = -1.0);
					sumD += d;
				}
				ctc_sum_r += sumD;
				typesOfRow.clear();
				
			}
			/// ROWS.END
			
			/// COLS - handle column end
			if (stats.rowIndex == stats.getTableHeight() - 1) { // last row = column end reached
				
				int colIndex = stats.colIndex; // index of the current column
				
				// determine dominant content type for column
				ContentType dominantType = getDominantType(typesOfCols[colIndex]);
				
				// compute the CTC for the current column
				double sumD = 0.0;
				for (ContentType ct : typesOfCols[colIndex]) {
					double d = (ct == dominantType) ? (d = 1.0) : (d = -1.0);
					sumD += d;
				}
				ctc_sum_c += sumD;
			}
			/// COLS.END
		}
		
		public void finalize() {
			// compute Cumulative Length Consistency over all rows
			ctc_r = ctc_sum_r / tableHeight;
			ctc_c = ctc_sum_c / tableWidth;
			ctc = Math.max(ctc_r, ctc_c);
		}
		
		public HashMap<String, Double> getResults() {
			HashMap<String, Double> result = new HashMap<String, Double>();
			result.put(featureName, new Double(ctc));
			return result;
		}
	}
	
	////
	// LOCAL FEATURES DEFINITION
	///
	
	public class LocalAvgLength extends AbstractTableListener {
		
		private ArrayList<Integer> cellLengths;
		private double average;
		
		public LocalAvgLength() {
			featureName = "LOCAL_AVG_LENGTH";
		}
		
		public void initialize(TableStats stats) {
			cellLengths = new ArrayList<Integer>();
		}
		
		public void onCell(Element content, TableStats stats) {
			if (content != null) {
				cellLengths.add(CellTools.getCellLength(content));
			}
		}
		
		public void finalize() {
			double sum = 0.0;
			for (Integer length : cellLengths) {
				sum += length;
			}
			double totalCells = (double) cellLengths.size();
			average = (totalCells > 0) ? (sum / totalCells) : 0.0;
		}
		
		public HashMap<String, Double> getResults() {
			HashMap<String, Double> result = new HashMap<String, Double>();
			result.put(featureName, new Double(average));
			return result;
		}
	}
	
	public class LocalEmptyRatio extends AbstractTableListener {
		private int countRatioEmptyCells;
		private int totalLocalCellCount;
		
		public LocalEmptyRatio() {
			featureName = "LOCAL_RATIO_EMPTY";
		}
		
		public void initialize(TableStats stats) {
			countRatioEmptyCells = 0;
			totalLocalCellCount = 0;
		}
		
		public void onCell(Element content, TableStats stats) {
			if (content == null) {
				countRatioEmptyCells++;
			}
			totalLocalCellCount++;
		}
		
		public void finalize() {
		}
		
		public HashMap<String, Double> getResults() {
			HashMap<String, Double> result = new HashMap<String, Double>();
			result.put(featureName, new Double(countRatioEmptyCells / totalLocalCellCount));
			return result;
		}
	}
	
	
	public class LocalEmptyVariance extends AbstractTableListener {
		
		private ArrayList<Integer> cellLengths;
		private double average, variance;
		
		public LocalEmptyVariance() {
			featureName = "LOCAL_EMPTY_VARIANCE";
		}
		
		public void initialize(TableStats stats) {
			cellLengths = new ArrayList<Integer>();
		}
		
		public void onCell(Element content, TableStats stats) {
			if (content == null) {
				cellLengths.add(1);
			}
		}
		
		public void finalize() {
			double sum = 0.0;
			for (Integer length : cellLengths) {
				sum += length;
			}
			
			double totalCells = (double) cellLengths.size();
			average = (totalCells > 0) ? (sum / totalCells) : 0.0;
			
			double varSum = 0.0;
			for (Integer length : cellLengths) {
				double inner = (length - average);
				double temp = Math.pow(inner, 2);
				varSum += temp;
			}
			variance = (totalCells > 0) ? (varSum / totalCells) : 0.0;
		}
		
		public HashMap<String, Double> getResults() {
			HashMap<String, Double> result = new HashMap<String, Double>();
			result.put(featureName, new Double(variance));
			return result;
		}
	}
	
	public class LocalLengthVariance extends AbstractTableListener {
		
		private ArrayList<Integer> cellLengths;
		private double average, variance;
		
		public LocalLengthVariance() {
			featureName = "LOCAL_LENGTH_VARIANCE";
		}
		
		public void initialize(TableStats stats) {
			cellLengths = new ArrayList<Integer>();
		}
		
		public void onCell(Element content, TableStats stats) {
			if (content != null) {
				cellLengths.add(CellTools.getCellLength(content));
			}
		}
		
		public void finalize() {
			double sum = 0.0;
			for (Integer length : cellLengths) {
				sum += length;
			}
			
			double totalCells = (double) cellLengths.size();
			average = (totalCells > 0) ? (sum / totalCells) : 0.0;
			
			double varSum = 0.0;
			for (Integer length : cellLengths) {
				double inner = (length - average);
				double temp = Math.pow(inner, 2);
				varSum += temp;
			}
			variance = (totalCells > 0) ? (varSum / totalCells) : 0.0;
		}
		
		public HashMap<String, Double> getResults() {
			HashMap<String, Double> result = new HashMap<String, Double>();
			result.put(featureName, new Double(variance));
			return result;
		}
	}
	
	public class LocalDigitAmountVariance extends AbstractTableListener {
		
		private ArrayList<Integer> digitAmounts;
		private double average, variance;
		
		public LocalDigitAmountVariance() {
			featureName = "LOCAL_DIGIT_AMOUNT_VARIANCE";
		}
		
		public void initialize(TableStats stats) {
			digitAmounts = new ArrayList<Integer>();
		}
		
		public void onCell(Element content, TableStats stats) {
			if (content != null) {
				digitAmounts.add(CellTools.cleanCell(content.text()).replaceAll("\\D", "").length());
			}
		}
		
		public void finalize() {
			double sum = 0.0;
			for (Integer length : digitAmounts) {
				sum += length;
			}
			
			double totalCells = (double) digitAmounts.size();
			average = (totalCells > 0) ? (sum / totalCells) : 0.0;
			
			double varSum = 0.0;
			for (Integer length : digitAmounts) {
				double inner = (length - average);
				double temp = Math.pow(inner, 2);
				varSum += temp;
			}
			variance = (totalCells > 0) ? (varSum / totalCells) : 0.0;
		}
		
		public HashMap<String, Double> getResults() {
			HashMap<String, Double> result = new HashMap<String, Double>();
			result.put(featureName, new Double(variance));
			return result;
		}
	}
	
	public class LocalContentRatios extends AbstractTableListener {
		
		private int cellCountNotEmpty, count_th, count_anchor, count_img, count_input, count_select,
				count_contains_number, count_is_number, count_colon, count_comma, count_contains_whitespace,
				count_contains_year, count_percentage, count_special_char;
		private double ratio_th, ratio_anchor, ratio_img, ratio_input, ratio_select,
				ratio_contains_number, ratio_is_number, ratio_colon, ratio_comma, ratio_contains_whitespace,
				ratio_contains_year, ratio_percentage, ratio_special_char;
		
		public LocalContentRatios() {
			featureName = "GROUP_LOCAL_CONTENT_RATIOS";
		}
		
		public void initialize(TableStats stats) {
			cellCountNotEmpty = count_th = count_anchor = count_img = count_input = count_select =
					count_contains_number = count_is_number = count_colon = count_comma = count_contains_whitespace =
							count_contains_year = count_percentage = count_special_char = 0;
		}
		
		public void onCell(Element content, TableStats stats) {
			if (content == null) {
				return;
			}
			
			// local content types are not exclusive!
			// thus they aren't determined via getContentType(...) - which is exclusive
			if (content.getElementsByTag("th").size() > 0) {
				count_th++;
			}
			if (content.getElementsByTag("a").size() > 0) {
				count_anchor++;
			}
			if (content.getElementsByTag("img").size() > 0) {
				count_img++;
			}
			if (content.getElementsByTag("input").size() > 0) {
				count_input++;
			}
			if (content.getElementsByTag("select").size() > 0) {
				count_select++;
			}
			String cleanedContent = CellTools.cleanCell(content.text());
			if (cleanedContent.endsWith(":")) {
				count_colon++;
			}
			if (cleanedContent.contains(",")) {
				count_comma++;
			}
			// check for digit
			if (cleanedContent.matches(".*\\d.*")) {
				count_contains_number++;
			}
			
			// check if only digit
			if (CellTools.isNumericOnly(cleanedContent)) {
				count_is_number++;
			}
			
			if (CellTools.containsWhitespace(content.text())) {
				count_contains_whitespace++;
			}
			
			if (CellTools.detectYear(cleanedContent)) {
				count_contains_year++;
			}
			
			if (cleanedContent.contains("%")) {
				count_percentage++;
			}
			
			if (Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE).matcher(cleanedContent).find()) {
				count_special_char++;
			}
			
			cellCountNotEmpty++;
		}
		
		public void finalize() {
			ratio_th = (cellCountNotEmpty > 0) ? ((double) count_th / (double) cellCountNotEmpty) : 0.0;
			ratio_anchor = (cellCountNotEmpty > 0) ? ((double) count_anchor / (double) cellCountNotEmpty) : 0.0;
			ratio_img = (cellCountNotEmpty > 0) ? ((double) count_img / (double) cellCountNotEmpty) : 0.0;
			ratio_input = (cellCountNotEmpty > 0) ? ((double) count_input / (double) cellCountNotEmpty) : 0.0;
			ratio_select = (cellCountNotEmpty > 0) ? ((double) count_select / (double) cellCountNotEmpty) : 0.0;
			ratio_colon = (cellCountNotEmpty > 0) ? ((double) count_colon / (double) cellCountNotEmpty) : 0.0;
			ratio_contains_number =
					(cellCountNotEmpty > 0) ? ((double) count_contains_number / (double) cellCountNotEmpty) : 0.0;
			ratio_is_number = (cellCountNotEmpty > 0) ? ((double) count_is_number / (double) cellCountNotEmpty) : 0.0;
			ratio_comma = (cellCountNotEmpty > 0) ? ((double) count_comma / (double) cellCountNotEmpty) : 0.0;
			ratio_contains_whitespace = (cellCountNotEmpty > 0) ? ((double) count_contains_whitespace / (double) cellCountNotEmpty) : 0.0;
			ratio_contains_year = (cellCountNotEmpty > 0) ? ((double) count_contains_year / (double) cellCountNotEmpty) : 0.0;
			ratio_percentage = (cellCountNotEmpty > 0) ? ((double) count_percentage / (double) cellCountNotEmpty) : 0.0;
			ratio_special_char = (cellCountNotEmpty > 0) ? ((double) count_special_char / (double) cellCountNotEmpty) : 0.0;
		}
		
		public HashMap<String, Double> getResults() {
			HashMap<String, Double> result = new HashMap<String, Double>();
			result.put("LOCAL_RATIO_HEADER", new Double(ratio_th));
			result.put("LOCAL_RATIO_ANCHOR", new Double(ratio_anchor));
			result.put("LOCAL_RATIO_IMAGE", new Double(ratio_img));
			result.put("LOCAL_RATIO_INPUT", new Double(ratio_input));
			result.put("LOCAL_RATIO_SELECT", new Double(ratio_select));
			result.put("LOCAL_RATIO_COLON", new Double(ratio_colon));
			result.put("LOCAL_RATIO_CONTAINS_NUMBER", new Double(ratio_contains_number));
			result.put("LOCAL_RATIO_IS_NUMBER", new Double(ratio_is_number));
			result.put("LOCAL_RATIO_COMMA", new Double(ratio_comma));
			result.put("LOCAL_RATIO_CONTAINS_YEAR", new Double(ratio_contains_year));
			result.put("LOCAL_RATIO_PERCENTAGE", new Double(ratio_percentage));
			result.put("LOCAL_RATIO_SPECIAL_CHAR", new Double(ratio_special_char));
			result.put("LOCAL_RATIO_CONTAINS_WHITESPACE", new Double(ratio_contains_whitespace));
			
			return result;
		}
	}
}