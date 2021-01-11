package tccc.bib.transform.functions;

import tccc.bib.transform.interfaces.TransformToHashMap;
import com.microsoft.azure.functions.ExecutionContext;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;

public class CSVtoHashMap implements TransformToHashMap {
	
	private static final String LF = "\n";
	private static final String CR = "\r";
	private static final String EMPTY = "";
	private static final String QUOTE = "\"";

	@Override
	public Object transform(String payload, AbstractMap<String, Object> properties, final ExecutionContext context) throws Exception {

		Boolean header = (Boolean) properties.get("sourceHeader");
		Boolean quotes = (Boolean) properties.get("sourceQuotes");
		String delimiter = (String) properties.get("sourceDelimiter");

		LinkedHashMap<String, Object> nameMap = null;
		boolean isFirstLine = true;
		LinkedList<Object> result = new LinkedList<>();
		
		for (String line : payload.split(LF)) {
			if (line.isEmpty()) continue;
			
			line = line.replaceAll(CR, EMPTY).replaceAll(QUOTE, EMPTY);
			
			Integer count = 0;
			LinkedHashMap<String, Object> map = new LinkedHashMap<>();
			for (String token : line.split(delimiter)) {
				count++;

				if (nameMap != null) {
					map.put((String) nameMap.get(count.toString()), token);
				}
				map.put(count.toString(), token);

			}

			if (isFirstLine && header) {
				nameMap = map;
			} else {
				result.add(map);
			}
			
			isFirstLine = false;
		}

		return result;
	}
}
