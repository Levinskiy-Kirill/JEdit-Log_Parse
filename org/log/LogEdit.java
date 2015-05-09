package org.log;

public abstract class LogEdit extends LogItem {
	protected String text;

	protected LogEdit() {
		super();
	}

	protected LogEdit(final String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}

	public void setText(final String text) {
		this.text = text;
	}

    //public String getStringForm() {return super.getStringForm();}
}
