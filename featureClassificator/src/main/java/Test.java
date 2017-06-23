import webreduce.extraction.mh.tools.CellTools;

import java.util.regex.Pattern;

/**
 * @author: Julius Gonsior
 */
public class Test {

	public static void main(String[] args) {
		String test = "uienrdve 11111ln g ugn evdg20104 2026uiagednlvggpn v gdq2555nudeig eduip ide 155215uieduiap pdl";
		System.out.println(CellTools.detectYear(test));
		
		
		if(Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE).matcher(test).find()) {
			System.out.println("ahu");
		}
	}
}