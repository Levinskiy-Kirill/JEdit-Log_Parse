package org.log;

public class LogPaste extends LogEdit {

	private int position;

	public LogPaste() {
		super();
	}

	public LogPaste(final int position, final String text) {
		super(text);
		this.position = position;
		type = LogEventTypes.PASTE_ACTION;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(final int position) {
		this.position = position;
	}

	@Override
	public String getStringForm() {
		return super.getStringForm();
	}
}
