/*
 * jEdit - Programmer's Text Editor
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2014 jEdit contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.log;

import org.gjt.sp.jedit.textarea.Selection;

public class LogSelection extends LogItem {

	private int start;
	private int end;
    private String text;

	public LogSelection() {
		super();
	}

	public LogSelection(final int start, final int end, final String text) {
		super();
		assert end > start;
        this.text = text;
		this.start = start;
		this.end = end;
		this.type = LogEventTypes.SELECTION;
	}

    public LogSelection(final int start, final int end) {
        super();
        assert end > start;
        this.start = start;
        this.end = end;
        this.type = LogEventTypes.SELECTION;
    }

	public Selection createSelection() {
		return new Selection.Range(start, end);
	}

	public int getSelectionLength() {
		return end - start;
	}

	public int getStart() {
		return start;
	}

	public void setStart(final int start) {
		this.start = start;
	}

	public int getEnd() {
		return end;
	}

	public void setEnd(final int end) {
		this.end = end;
	}

    public String getText() {
        return this.text;
    }

    public void setText(final String text) {
        this.text = text;
    }

	@Override
	public String getStringForm() {
		return String.format("Selection from %d to %d. %d", start, end, timestamp);
	}

}
