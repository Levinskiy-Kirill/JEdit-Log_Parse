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

public abstract class LogKey extends LogItem {

	protected int position;
	protected int mask;
	protected int keyCode;

	protected LogKey() {
	}

	protected LogKey(final int position, final int mask, final int keyCode) {
		super();
		this.position = position;
		this.mask = mask;
		this.keyCode = keyCode;
	}

	public int getPosition()
	{
		return position;
	}

	public void setPosition(int position)
	{
		this.position = position;
	}

	public abstract KeyEvent createEvent(Component source);

	@Override
	public String toString() {
		return "LogKey{" +
				"position=" + position +
				'}';
	}

	public int getKeyCode() {
		return keyCode;
	}

	public void setKeyCode(final int keyCode) {
		this.keyCode = keyCode;
	}

	public int getMask()
	{
		return mask;
	}

	public void setMask(int mask)
	{
		this.mask = mask;
	}
}

