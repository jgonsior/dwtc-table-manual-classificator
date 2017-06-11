package webreduce.extraction.mh.tools;

public class TableStats {
	public int rowIndex;
	public int colIndex;
	private int tableWidth;
	private int tableHeight;
	
	public TableStats(int width, int height) {
		this.tableHeight = height;
		this.tableWidth = width;
	}

	public int getTableWidth() {
		return tableWidth;
	}
	
	public int getTableHeight() {
		return tableHeight;
	}
}