package org.log;

public class LogCloseFile extends LogItem {
	public LogCloseFile() {
		this.type = LogEventTypes.CLOSE_ACTION;
	}

	@Override
	public String getStringForm() {
		return "Closed file at " + timestamp;
	}
}
