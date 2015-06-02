package org.log;

public class LogRun extends LogEdit {

    public LogRun() {
        super();
    }

    public LogRun(final String text) {
        super(text);
        type = LogEventTypes.RUN_ACTION;
    }

    @Override
    public String getStringForm() {
        return super.getStringForm();
    }
}
