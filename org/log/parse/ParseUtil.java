package org.log.parse;

import org.codehaus.jackson.map.ObjectMapper;
import org.log.LogCharacterKey;
import org.log.LogCloseFile;
import org.log.LogCompile;
import org.log.LogCopy;
import org.log.LogCut;
import org.log.LogEventTypes;
import org.log.LogItem;
import org.log.LogOpenFile;
import org.log.LogPaste;
import org.log.LogRun;
import org.log.LogSaveFile;
import org.log.LogSelection;
import org.log.LogSelectionClear;
import org.log.LogServiceKey;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

public class ParseUtil {
    public static LinkedList<LinkedList<LogItem>> parseFile(final String filename) throws Exception {
        final LinkedList<LinkedList<LogItem>> res = new LinkedList<LinkedList<LogItem>>();
        LinkedList<LogItem> temp = new LinkedList<LogItem>();
        final ObjectMapper mapper = new ObjectMapper();
        BufferedReader br = null;
        try {
            br = Files.newBufferedReader(Paths.get(filename), Charset.defaultCharset());
            String s;
            while ((s = br.readLine()) != null && (!"".equals(s))) {
                final LogItem item = mapper.readValue(s.getBytes(), LogItem.class);
                if (!temp.isEmpty()) {
                    if (temp.getFirst().getType() != item.getType()) {
                        res.add(temp);
                        temp = new LinkedList<LogItem>();
                    }
                }
                temp.add(getLogItem(s, item.getType(), mapper));
            }
            res.add(temp);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    System.out.println("Cannot close reader");
                }
            }
        }
        return res;
    }

	private static LogItem getLogItem(final String source, final LogEventTypes type, final ObjectMapper mapper) throws Exception {
		switch (type) {
			case CHARACTER_KEY:
				return mapper.readValue(source.getBytes(), LogCharacterKey.class);
			case SAVE_ACTION:
				return mapper.readValue(source.getBytes(), LogSaveFile.class);
			case SELECTION:
				return mapper.readValue(source.getBytes(), LogSelection.class);
			case SELECTION_CLEAR:
				return mapper.readValue(source.getBytes(), LogSelectionClear.class);
			case SERVICE_KEY:
				return mapper.readValue(source.getBytes(), LogServiceKey.class);
			case OPEN_ACTION:
				return mapper.readValue(source.getBytes(), LogOpenFile.class);
			case CLOSE_ACTION:
				return mapper.readValue(source.getBytes(), LogCloseFile.class);
			case COMPILE_ACTION:
				return mapper.readValue(source.getBytes(), LogCompile.class);
			case RUN_ACTION:
				return mapper.readValue(source.getBytes(), LogRun.class);
			case PASTE_ACTION:
				return mapper.readValue(source.getBytes(), LogPaste.class);
			case COPY_ACTION:
				return mapper.readValue(source.getBytes(), LogCopy.class);
			case CUT_ACTION:
				return mapper.readValue(source.getBytes(), LogCut.class);
			default:
				throw new IllegalArgumentException("there is no such type");
		}
	}
}
