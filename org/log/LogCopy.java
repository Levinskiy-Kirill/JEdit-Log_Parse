package org.log;

public class LogCopy extends LogEdit {

	public LogCopy() {
		super();
	}

	public LogCopy(final String text) {
		super(text);
		type = LogEventTypes.COPY_ACTION;
	}

	@Override
	public String getStringForm() {
        return super.getStringForm();
	}
}
