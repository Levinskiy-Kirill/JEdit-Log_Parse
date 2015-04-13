/*
 * JEditTextArea.java - jEdit's text component
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2005 Slava Pestov
 * Portions copyright (C) 2000 Ollie Rutherfurd
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.jedit.textarea;

//{{{ Imports

import org.codehaus.jackson.map.ObjectMapper;
import org.gjt.sp.jedit.Abbrevs;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.EditBus.EBHandler;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.Macros;
import org.gjt.sp.jedit.ServiceManager;
import org.gjt.sp.jedit.TextUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.msg.PositionChanging;
import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.jedit.options.GlobalOptions;
import org.gjt.sp.util.Log;
import org.log.LogCharacterKey;
import org.log.LogEventTypes;
import org.log.LogItem;
import org.log.LogSelection;
import org.log.LogServiceKey;
import org.log.parse.ParseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
//}}}

/**
 * jEdit's text component.<p>
 *
 * Unlike most other text editors, the selection API permits selection and
 * concurrent manipulation of multiple, non-contiguous regions of text.
 * Methods in this class that deal with selecting text rely upon classes derived
 * the {@link Selection} class.
 *
 * @author Slava Pestov
 * @author John Gellene (API documentation)
 * @version $Id: JEditTextArea.java 23221 2013-09-29 20:03:32Z shlomy $
 */
public class JEditTextArea extends TextArea
{

	private static final Logger log = LoggerFactory.getLogger(JEditTextArea.class);
	private ObjectMapper mapper = new ObjectMapper();
	private List<LogItem> items = new ArrayList<LogItem>();
	private LogItem current;
	private boolean hasSelection;

	//{{{ JEditTextArea constructor
	/**
	 * Creates a new JEditTextArea.
	 */
	public JEditTextArea(View view)
	{
		super(jEdit.getPropertyManager(), view);
		enableEvents(AWTEvent.FOCUS_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
		this.view = view;
		setRightClickPopupEnabled(true);
		painter.setLineExtraSpacing(jEdit.getIntegerProperty("options.textarea.lineSpacing", 0));
		EditBus.addToBus(this);

		addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(final KeyEvent e) {
				if (isServiceKey(e)) {
					final LogServiceKey key = new LogServiceKey(
						e.getKeyCode(),
						JEditTextArea.this.getCaretPosition(),
						e.getModifiers()
					);
					try {
						log.info(mapper.writeValueAsString(key));
					} catch (Exception ex) {
						Log.log(1, null, "cannot write json:\n", ex);
					}
				}
			}

			@Override
			public void keyPressed(final KeyEvent e) {
				if (isPrintableKey(e)) {
					final LogCharacterKey key = new LogCharacterKey(
						e.getKeyChar(),
						JEditTextArea.this.getCaretPosition(),
						e.getKeyCode(),
						e.getModifiers()
					);
					try {
						log.info(mapper.writeValueAsString(key));
					} catch (Exception ex) {
						Log.log(1, null, "cannot write json:\n", ex);
					}
				} else if (isServiceKey(e)) {
					final LogServiceKey key = new LogServiceKey(
						e.getKeyCode(),
						JEditTextArea.this.getCaretPosition(),
						e.getModifiers()
					);
					try {
						log.info(mapper.writeValueAsString(key));
					} catch (Exception ex) {
						Log.log(1, null, "cannot write json:\n", ex);
					}
				}
			}

			@Override
			public void keyReleased(final KeyEvent e) {
				if (isServiceKey(e)) {
					final LogServiceKey key = new LogServiceKey(
						e.getKeyCode(),
						JEditTextArea.this.getCaretPosition(),
						e.getModifiers()
					);
					try {
						log.info(mapper.writeValueAsString(key));
					} catch (Exception ex) {
						Log.log(1, null, "cannot write json:\n", ex);
					}
				}
			}
		});
	} //}}}

	private static boolean isAltMask(final KeyEvent e) {
		return (e.getModifiersEx() & KeyEvent.ALT_DOWN_MASK) == KeyEvent.ALT_DOWN_MASK;
	}

	private static boolean isCtrlMask(final KeyEvent e) {
		return (e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) == KeyEvent.CTRL_DOWN_MASK;
	}

	private static boolean isPrintableKey(final KeyEvent e) {
		if (isAltMask(e) || isCtrlMask(e)) {
			return false;
		}
		final int keyCode = e.getKeyCode();
		if (keyCode >= KeyEvent.VK_COMMA && keyCode <= KeyEvent.VK_9) {
			return true;
		}
		if (keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_CLOSE_BRACKET) {
			return true;
		}
		return keyCode == KeyEvent.VK_SEMICOLON
				|| keyCode == KeyEvent.VK_EQUALS
				|| keyCode == KeyEvent.VK_QUOTE
				|| keyCode == KeyEvent.VK_BACK_QUOTE
				|| keyCode == KeyEvent.VK_SPACE;
	}

	private static boolean isServiceKey(final KeyEvent event) {
		if (isCtrlMask(event) || isAltMask(event)) {
			return false;
		}
		int keyCode = event.getKeyCode();
		return (keyCode >= KeyEvent.VK_LEFT && keyCode<= KeyEvent.VK_DOWN)
				|| keyCode == KeyEvent.VK_TAB
				|| keyCode == KeyEvent.VK_ENTER
				|| keyCode == KeyEvent.VK_BACK_SPACE
				|| keyCode == KeyEvent.VK_DELETE
				|| keyCode == KeyEvent.VK_CAPS_LOCK
				|| keyCode == KeyEvent.VK_INSERT;
	}

	public void parseLog() {
		try {
			if (openLogFile()) {
				current = items.get(0);
				log.info("Current action item: " + current);
			}
		} catch (Exception ex) {
			Log.log(Log.ERROR, this, "Something went wrong", ex);
		}
	}

	public void compileBuffer(final Buffer toCompile) {
		toCompile.save(getView(), toCompile.getPath());
		final File output = Paths.get("out", "compileOut").toFile();
		String path = Paths.get("out").toAbsolutePath().toString() + "/" + toCompile.getName();
		toCompile.save(getView(), path);
		ArrayList<String> commands = new ArrayList<>();
		commands.add(jEdit.getProperty("java.compiler"));
		commands.add(path);
		try {
			final Process process = new ProcessBuilder(commands).redirectOutput(output).start();
			process.waitFor();
			JOptionPane.showMessageDialog(this, getContentOfFile(output), "Output of compilation", JOptionPane.INFORMATION_MESSAGE);
			Files.deleteIfExists(Paths.get("out", "compileOut"));
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Cannot open file for output of compiler");
		} catch (InterruptedException e) {
			JOptionPane.showMessageDialog(this, "Compiling was interrupted");
		}
	}

	private String getContentOfFile(final File file) throws IOException {
		BufferedReader br = Files.newBufferedReader(file.toPath(), Charset.defaultCharset());
		final StringBuffer res = new StringBuffer();
		String s;
		while ((s = br.readLine()) != null) {
			res.append(s);
		}
		br.close();
		return res.toString();
	}

	public void runBuffer(final Buffer toRun) throws IOException {
		final File output = Paths.get("out", "runOut").toFile();
		ArrayList<String> commands = new ArrayList<>();
		commands.add(jEdit.getProperty("java.start"));
		commands.add(toRun.getName().replace(".java", ""));
		try {
			final Process process = new ProcessBuilder(commands).directory(Paths.get("out").toFile()).redirectOutput(output).start();
			process.waitFor();
			JOptionPane.showMessageDialog(this, getContentOfFile(output), "Output of run:", JOptionPane.INFORMATION_MESSAGE);
			Files.deleteIfExists(Paths.get("out", "runOut"));
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Cannot open file for output of compiler");
		} catch (InterruptedException e) {
			JOptionPane.showMessageDialog(this, "Run was interrupted");
		}
	}

	public void nextAction() {
		log.info("Current in nextAction(): " + current);
		if (current != null) {
			log.info("Processing " + current);
			try {
				pressKey(current);
				log.info("Key pressed");
			} catch (AWTException e) {
			 	log.info("Cannot instantiate robot");
			}
			int nextIndex = items.indexOf(current) + 1;
			if (items.size() > nextIndex) {
				current = items.get(nextIndex);
				log.info("Next item is " + current);
			} else {
				current = null;
			}
		}
	}

	private void pressKey(LogItem item) throws AWTException {
		LogEventTypes type = item.getType();
		if (type == LogEventTypes.SERVICE_KEY) {
			pressServiceKey((LogServiceKey)item);
		} else if (type == LogEventTypes.CHARACTER_KEY) {
			pressCharKey((LogCharacterKey)item);
		} else if (type == LogEventTypes.SELECTION) {
			hasSelection = true;
			addSelection((LogSelection)item);
		} else if (type == LogEventTypes.SELECTION_CLEAR) {
			setCaretPosition(0);
			hasSelection = false;
		}
	}

	private void addSelection(final LogSelection item) {
		this.setSelection(item.createSelection());
	}

	private void pressCharKey(final LogCharacterKey item) throws AWTException {
		final Robot robot = new Robot();
		ensurePosition(item.getPosition());
		if (isShiftRequired(item)) { //Emulate characters with pressed shift
			robot.keyPress(KeyEvent.VK_SHIFT);
			robot.keyPress(item.getKeyCode());
			robot.keyRelease(item.getKeyCode());
			robot.keyRelease(KeyEvent.VK_SHIFT);
		} else {
			robot.keyPress(item.getKeyCode());
			robot.keyRelease(item.getKeyCode());
		}

	}

	private void ensurePosition(int position)
	{
		if (!hasSelection) {
			setCaretPosition(position);
		} else {
			moveCaretPosition(position);
		}
	}

	private boolean isShiftRequired(LogCharacterKey item)
	{
		return item.getMask() == KeyEvent.SHIFT_MASK;
	}

	private void pressServiceKey(final LogServiceKey item) throws AWTException {
		final Robot robot = new Robot();
		ensurePosition(getCaretForServiceKey(item));
		robot.keyPress(item.getKeyCode());
		robot.keyRelease(item.getKeyCode());
	}

	private int getCaretForServiceKey(LogServiceKey item)
	{
		if (item.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
			return item.getPosition() + 1;
		} else if (buffer.getLength() < item.getPosition()) {
			return buffer.getLength();
		} else {
			return item.getPosition();
		}
	}

	private boolean openLogFile() throws Exception {
		JFileChooser chooser = new JFileChooser(System.getProperty("user.home"));
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			items = new ArrayList<LogItem>(ParseUtil.parseFile(chooser.getSelectedFile().getAbsolutePath()));
			log.info("All items: " + items);
			return true;
		} else {
			return false;
		}
	}

	//{{{ dispose() method
	@Override
	public void dispose()
	{
		EditBus.removeFromBus(this);
		super.dispose();
	} //}}}

	//{{{ getFoldPainter() method
	@Override
	public FoldPainter getFoldPainter()
	{
		FoldPainter foldPainter = (FoldPainter) ServiceManager.getService(
				FOLD_PAINTER_SERVICE, getFoldPainterName());
		if (foldPainter == null)
			foldPainter = (FoldPainter) ServiceManager.getService(
				FOLD_PAINTER_SERVICE,
				DEFAULT_FOLD_PAINTER_SERVICE);
		return foldPainter;
	} //}}}

	// {{{ Overrides for macro recording.
	//{{{ home() method
	/**
	 * An override to record the acutual action taken for home().
	 */
	@Override
	public void home(boolean select)
	{
		Macros.Recorder recorder = view.getMacroRecorder();
		switch(getInputHandler().getLastActionCount() % 2)
		{
		case 1:
			if(recorder != null)
				recorder.record("textArea.goToStartOfWhiteSpace(" + select + ");");			
			goToStartOfWhiteSpace(select);
			break;
		default:
			if(recorder != null)
				recorder.record("textArea.goToStartOfLine(" + select + ");");			
			goToStartOfLine(select);
			break;
		}
	} //}}}

	//{{{ end() method
	/**
	 * An override to record the acutual action taken for end().
	 */
	@Override
	public void end(boolean select)
	{
		Macros.Recorder recorder = view.getMacroRecorder();

		switch(getInputHandler().getLastActionCount() % 2)
		{
		case 1:
			if(recorder != null)
				recorder.record("textArea.goToEndOfWhiteSpace(" + select + ");");
			goToEndOfWhiteSpace(select);
			break;
		default:
			if(recorder != null)
				recorder.record("textArea.goToEndOfLine(" + select + ");");
			goToEndOfLine(select);
			break;
		}
	} //}}}

	//{{{ smartHome() method
	/**
	 * An override to record the acutual action taken for smartHome().
	 */
	@Override
	public void smartHome(boolean select)
	{
		Macros.Recorder recorder = view.getMacroRecorder();

		switch(view.getInputHandler().getLastActionCount())
		{
		case 1:
			if(recorder != null)
				recorder.record("textArea.goToStartOfWhiteSpace(" + select + ");");

			goToStartOfWhiteSpace(select);
			break;
		case 2:
			if(recorder != null)
				recorder.record("textArea.goToStartOfLine(" + select + ");");

			goToStartOfLine(select);
			break;
		default: //case 3:
			if(recorder != null)
				recorder.record("textArea.goToFirstVisibleLine(" + select + ");");

			goToFirstVisibleLine(select);
			break;
		}
	} //}}}

	//{{{ smartEnd() method
	/**
	 * An override to record the acutual action taken for smartHome().
	 */
	@Override
	public void smartEnd(boolean select)
	{
		Macros.Recorder recorder = view.getMacroRecorder();

		switch(view.getInputHandler().getLastActionCount())
		{
		case 1:
			if(recorder != null)
				recorder.record("textArea.goToEndOfWhiteSpace(" + select + ");");

			goToEndOfWhiteSpace(select);
			break;
		case 2:
			if(recorder != null)
				recorder.record("textArea.goToEndOfLine(" + select + ");");

			goToEndOfLine(select);
			break;
		default: //case 3:
			if(recorder != null)
				recorder.record("textArea.goToLastVisibleLine(" + select + ");");
			goToLastVisibleLine(select);
			break;
		}
	} //}}}
	// }}}

	// {{{ overrides from the base class that are EditBus  aware
	public void goToBufferEnd(boolean select)
	{
		EditBus.send(new PositionChanging(this));
		super.goToBufferEnd(select);
	}

	//{{{ goToMatchingBracket() method
	/**
	 * Moves the caret to the bracket matching the one before the caret.
	 * Also sends PositionChanging if it goes somewhere.
	 * @since jEdit 4.3pre18
	 */
	public void goToMatchingBracket()
	{
		if(getLineLength(caretLine) != 0)
		{
			int dot = caret - getLineStartOffset(caretLine);

			int bracket = TextUtilities.findMatchingBracket(
				buffer,caretLine,Math.max(0,dot - 1));
			if(bracket != -1)
			{
				EditBus.send(new PositionChanging(this));
				selectNone();
				moveCaretPosition(bracket + 1,false);
				return;
			}
		}
		getToolkit().beep();
	} //}}}


	public void goToBufferStart(boolean select)
	{
		EditBus.send(new PositionChanging(this));
		super.goToBufferStart(select);
	} // }}}

	// {{{ replaceSelection(String)
	@Override
	public int replaceSelection(String selectedText)
	{
		EditBus.send(new PositionChanging(this));
		return super.replaceSelection(selectedText);
	}//}}}

	//{{{ showGoToLineDialog() method
	/**
	 * Displays the 'go to line' dialog box, and moves the caret to the
	 * specified line number.
	 * @since jEdit 2.7pre2
	 */
	public void showGoToLineDialog()
	{
		int maxLine = Integer.valueOf(buffer.getLineCount());
		String line = GUIUtilities.input(view,"goto-line",new Integer[] {1, maxLine},null);
		if(line == null)
			return;

		try
		{
			int lineNumber = Integer.parseInt(line) - 1;
			if(lineNumber > --maxLine)
				lineNumber = maxLine;
			EditBus.send(new PositionChanging(this));
			setCaretPosition(getLineStartOffset(lineNumber));
		}
		catch(Exception e)
		{
			getToolkit().beep();
		}
	} //}}}

	//{{{ userInput() method
	/**
	 * Handles the insertion of the specified character. It performs the
	 * following operations in addition to TextArea#userInput(char):
	 * <ul>
	 * <li>Inserting a space with automatic abbrev expansion enabled will
	 * try to expand the abbrev
	 * </ul>
	 *
	 * @param ch The character
	 * @since jEdit 2.7pre3
	 */
	@Override
	public void userInput(char ch)
	{
		if(ch == ' ' && Abbrevs.getExpandOnInput()
			&& Abbrevs.expandAbbrev(view,false))
			return;

		super.userInput(ch);
	} //}}}

	//{{{ addExplicitFold() method
	/**
	 * Surrounds the selection with explicit fold markers.
	 * @since jEdit 4.0pre3
	 */
	@Override
	public void addExplicitFold()
	{
		try
		{
			super.addExplicitFold();
		}
		catch (TextAreaException e)
		{
			GUIUtilities.error(view,"folding-not-explicit",null);
		}
	} //}}}

	//{{{ formatParagraph() method
	/**
	 * Formats the paragraph containing the caret.
	 * @since jEdit 2.7pre2
	 */
	@Override
	public void formatParagraph()
	{
		try
		{
			super.formatParagraph();
		}
		catch (TextAreaException e)
		{
			GUIUtilities.error(view,"format-maxlinelen",null);
		}
	} //}}}

	//{{{ doWordCount() method
	protected static void doWordCount(View view, String text)
	{
		char[] chars = text.toCharArray();
		int characters = chars.length;
		int words = 0;
		int lines = 1;

		boolean word = true;
		for (char aChar : chars)
		{
			switch (aChar)
			{
				case '\r':
				case '\n':
					lines++;
				case ' ':
				case '\t':
					word = true;
					break;
				default:
					if (word)
					{
						words++;
						word = false;
					}
					break;
			}
		}

		Object[] args = { characters, words, lines };
		GUIUtilities.message(view,"wordcount",args);
	} //}}}

	//{{{ showWordCountDialog() method
	/**
	 * Displays the 'word count' dialog box.
	 * @since jEdit 2.7pre2
	 */
	public void showWordCountDialog()
	{
		String selection = getSelectedText();
		if(selection != null)
		{
			doWordCount(view,selection);
			return;
		}

		doWordCount(view,buffer.getText(0,buffer.getLength()));
	} //}}}

	//{{{ Getters and setters

	//{{{ getView() method
	/**
	 * Returns this text area's view.
	 * @since jEdit 4.2pre5
	 */
	public View getView()
	{
		return view;
	} //}}}

	//}}}

	//{{{ Private members

	//{{{ Instance variables
	private View view;
	//}}}
	//}}}

	//{{{ Fold painters
	/**
	 * Fold painter service.
	 * @since jEdit 4.3pre16
	 */
	public static final String FOLD_PAINTER_PROPERTY = "foldPainter";
	public static final String FOLD_PAINTER_SERVICE = "org.gjt.sp.jedit.textarea.FoldPainter";
	public static final String DEFAULT_FOLD_PAINTER_SERVICE = "Triangle";

	//{{{ getFoldPainterService() method
	public static String getFoldPainterName()
	{
		return jEdit.getProperty(FOLD_PAINTER_PROPERTY, DEFAULT_FOLD_PAINTER_SERVICE);
	} //}}}

	//}}} Fold painters

	//{{{ handlePopupTrigger() method
	/**
	 * Do the same thing as right-clicking on the text area. The Gestures
	 * plugin uses this API.
	 * @since jEdit 4.2pre13
	 */
	@Override
	public void handlePopupTrigger(MouseEvent evt)
	{

		if(popup.isVisible())
			popup.setVisible(false);
		else
		{
			// Rebuild popup menu every time the menu is requested.
			createPopupMenu(evt);

			int x = evt.getX();
			int y = evt.getY();

			int dragStart = xyToOffset(x,y,
				!(painter.isBlockCaretEnabled()
				|| isOverwriteEnabled()));

			if(getSelectionCount() == 0 || multi)
				moveCaretPosition(dragStart,false);
			GUIUtilities.showPopupMenu(popup,painter,x,y);
		}
	} //}}}

	//{{{ createPopupMenu() method
	/**
	 * Creates the popup menu.
	 * @since 4.3pre15
	 */
	@Override
	public void createPopupMenu(MouseEvent evt)
	{
		popup = GUIUtilities.loadPopupMenu("view.context", this, evt);
		if (!jEdit.getBooleanProperty("options.context.includeOptionsLink"))
			return;
		JMenuItem customize = new JMenuItem(jEdit.getProperty(
			"view.context.customize"));
		customize.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				new GlobalOptions(view,"context");
			}
		});
		popup.addSeparator();
		popup.add(customize);
	} //}}}

	//{{{ handlePropertiesChanged() method
	@EBHandler
	public void handlePropertiesChanged(PropertiesChanged msg)
	{
		painter.setLineExtraSpacing(jEdit.getIntegerProperty("options.textarea.lineSpacing", 0));
	} //}}}
}
