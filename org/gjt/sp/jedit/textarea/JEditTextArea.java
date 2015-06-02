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

import javafx.scene.shape.Path;
import org.codehaus.jackson.map.ObjectMapper;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.EditBus.EBHandler;
import org.gjt.sp.jedit.msg.PositionChanging;
import org.gjt.sp.jedit.msg.PropertiesChanged;
import org.gjt.sp.jedit.options.GlobalOptions;
import org.gjt.sp.util.Log;
import org.log.*;
import org.log.parse.ParseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
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
    private ParseUtil parse;

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
        parse = new ParseUtil(this);

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
        parse.parseLog();
    }

    public void nextAction() {
        parse.nextAction(buffer);
    }

    public void previousAction() {
        parse.previousAction(buffer);
    }

    public void compileBuffer(final Buffer toCompile) throws IOException {

        if (toCompile.isDirty()) {
            toCompile.save(getView(), toCompile.getPath());
        }

        final File output = Paths.get("out", "compileOut").toFile();
        final File error = Paths.get("out", "compileError").toFile();
        ArrayList<String> commands = new ArrayList<>();
        commands.add(jEdit.getProperty("java.compiler"));
        commands.add(toCompile.getName());
        try {
            String s;
            Files.copy(Paths.get(toCompile.getPath()), Paths.get("out", toCompile.getName()), StandardCopyOption.REPLACE_EXISTING);
            final Process process = new ProcessBuilder(commands).directory(Paths.get("out").toFile()).redirectOutput(output).redirectError(error).start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                s = getContentOfFile(error);
                JOptionPane.showMessageDialog(this, s, "Результат компиляции", JOptionPane.ERROR_MESSAGE);
            } else {
                s = getContentOfFile(output);
                JOptionPane.showMessageDialog(this, s, "Результат компиляции", JOptionPane.INFORMATION_MESSAGE);
            }

            try {
                log.info(mapper.writeValueAsString(new LogCompile(s)));
            } catch (Exception e) {
                Log.log(Log.ERROR, null, "Cannot write copy action to json", e);
            }

            Files.deleteIfExists(output.toPath());
            Files.deleteIfExists(error.toPath());
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
            res.append("\n");
		}
		br.close();
		return res.toString();
	}

    public void runBuffer(final Buffer toRun) {
        final File output = Paths.get("out", "runOut").toFile();
        final File error = Paths.get("out", "runError").toFile();
        ArrayList<String> commands = new ArrayList<>();
        commands.add(jEdit.getProperty("java.start"));
        commands.add(toRun.getName().replace(".java", ""));

        try {
            String s;
            final Process process = new ProcessBuilder(commands).directory(Paths.get("out").toFile()).redirectOutput(output).redirectError(error).start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                s = getContentOfFile(error);
                JOptionPane.showMessageDialog(this, s, "Результат запуска", JOptionPane.ERROR_MESSAGE);
            } else {
                s = getContentOfFile(output);
                JOptionPane.showMessageDialog(this, s, "Результат запуска", JOptionPane.INFORMATION_MESSAGE);
            }
            try {
                log.info(mapper.writeValueAsString(new LogRun(s)));
            } catch (Exception e) {
                Log.log(Log.ERROR, null, "Cannot write copy action to json", e);
            }
            Files.deleteIfExists(output.toPath());
            Files.deleteIfExists(error.toPath());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Cannot open file for output of compiler");
        } catch (InterruptedException e) {
            JOptionPane.showMessageDialog(this, "Run was interrupted");
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
