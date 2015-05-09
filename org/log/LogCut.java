package org.log;

public class LogCut extends LogEdit {

	public LogCut() {
		super();
	}

	public LogCut(final String text) {
		super(text);
		type = LogEventTypes.CUT_ACTION;
	}

	@Override
	public String getStringForm() {
        return super.getStringForm();
	}
}
