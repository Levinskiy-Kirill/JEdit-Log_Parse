package org.log;

public class LogSelectionClear extends LogItem {

	public LogSelectionClear() {
		super();
		this.type = LogEventTypes.SELECTION_CLEAR;
	}


	@Override
	public String getStringForm() {
		return String.format("Selection cleared. %d", timestamp);
	}

}
