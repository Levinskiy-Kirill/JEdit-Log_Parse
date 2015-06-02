package org.log;

public class LogCompile extends LogEdit {

    public LogCompile() {
        super();
    }

    public LogCompile(final String text) {
        super(text);
        type = LogEventTypes.COMPILE_ACTION;
    }

    @Override
    public String getStringForm() {
        return super.getStringForm();
    }
}
