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

import java.awt.Component;
import java.awt.event.KeyEvent;

public class LogCharacterKey extends LogKey {

	private char keyChar;

	public LogCharacterKey() {
	}

	@Override
	public KeyEvent createEvent(Component source)
	{
		return new KeyEvent(source, 1, System.currentTimeMillis(), 0, keyCode, keyChar);
	}

	public LogCharacterKey(final char keyChar, final int position, final int keyCode, final int mask) {
		super(position, mask, keyCode);
		this.type = LogEventTypes.CHARACTER_KEY;
		this.keyChar = keyChar;
	}


	@Override
	public String getStringForm() {
		return String.format("Character key with character %c and keycode %d. %d", keyChar, keyCode, timestamp);
	}

	public char getKeyChar() {
		return keyChar;
	}

	public void setKeyChar(final char keyChar) {
		this.keyChar = keyChar;
	}

}
