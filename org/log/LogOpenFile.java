package org.log;

public class LogOpenFile extends LogItem {

	private String path;

	public LogOpenFile() {
		this.type = LogEventTypes.OPEN_ACTION;
	}

	public LogOpenFile(final String path) {
		this();
		this.path = path;
	}

	public String getPath() {
		return path;
	}

	public void setPath(final String path) {
		this.path = path;
	}

	@Override
	public String getStringForm() {
		return super.getStringForm();
	}
}
