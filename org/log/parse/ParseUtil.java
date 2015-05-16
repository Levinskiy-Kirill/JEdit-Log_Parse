package org.log.parse;

import org.codehaus.jackson.map.ObjectMapper;
import org.gjt.sp.jedit.Registers;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.Selection;
import org.gjt.sp.util.Log;
import org.log.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParseUtil {
    private static final Logger log = LoggerFactory.getLogger(ParseUtil.class);
    private ObjectMapper mapper = new ObjectMapper();
    private JEditTextArea textArea;
    private LinkedList<LinkedList<LogItem>> items;
    //private LinkedList<LogItem> itemsOneList;
    private ListIterator<LinkedList<LogItem>> iter;
    //private LinkedList<LogItem> current;

    public ParseUtil(JEditTextArea textArea) {
        this.textArea = textArea;
        items = new LinkedList<LinkedList<LogItem>>();
    }

    public void parseLog() {
        try {
            if(openLogFile()) {
                //itemsOneList = items.getFirst();
                //iter = items.listIterator();
                iter = items.listIterator();
                ListIterator<LinkedList<LogItem>> iter2 = items.listIterator();
                while(iter2.hasNext()) {
                    LinkedList<LogItem> itemsOneList = iter2.next();
                    log.info("Index lists: " + iter2.nextIndex());
                    for(LogItem item : itemsOneList) {
                        log.info("" + item);
                    }
                }
            }
        } catch (Exception e) {
            Log.log(Log.ERROR, this, "Something went wrong", e);
        }
    }

    private boolean openLogFile() throws Exception {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(Paths.get("logs").toFile());
        if (chooser.showOpenDialog(textArea) == JFileChooser.APPROVE_OPTION) {
            parseFile(chooser.getSelectedFile().getAbsolutePath());
            return true;
        } else {
            return false;
        }
    }

    private void parseFile(final String filename) throws Exception {
        LinkedList<LogItem> itemsOneBlock = new LinkedList<LogItem>();
        mapper = new ObjectMapper();
        BufferedReader br = null;
        try {
            br = Files.newBufferedReader(Paths.get(filename), Charset.defaultCharset());
            LogItem item = readOneObject(br);
            while(item != null) {
                if(isTypesetting(item)) {
                    while(isTypesetting(item)) {
                        itemsOneBlock.add(item);
                        item = readOneObject(br);
                    }
                    items.add(itemsOneBlock);
                    itemsOneBlock = new LinkedList<LogItem>();
                    continue;
                }
                else if(isMovingCursorType(item)) {
                    while(isMovingCursorType(item)) {
                        itemsOneBlock.add(item);
                        item = readOneObject(br);
                    }
                    items.add(itemsOneBlock);
                    itemsOneBlock = new LinkedList<LogItem>();
                    continue;
                }
                else if(isDeleteItem(item)) {
                    while(isDeleteItem(item)) {
                        itemsOneBlock.add(item);
                        item = readOneObject(br);
                    }
                    items.add(itemsOneBlock);
                    itemsOneBlock = new LinkedList<LogItem>();
                    continue;
                }
                else {
                    itemsOneBlock.add(item);
                    items.add(itemsOneBlock);
                    itemsOneBlock = new LinkedList<LogItem>();
                    item = readOneObject(br);
                }
            }

        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    System.out.println("Cannot close reader");
                }
            }
        }
    }

    private boolean isTypesetting(LogItem item) {
        LogEventTypes type = item.getType();
        switch (type) {
            case CHARACTER_KEY:
                return true;
            case SERVICE_KEY:
                if(((LogServiceKey)item).getKeyCode() == KeyEvent.VK_ENTER)
                    return true;
        }
        return false;
    }

    private boolean isMovingCursorType(LogItem item) {
        if(item.getType() == LogEventTypes.SERVICE_KEY) {
            int keyCode = ((LogServiceKey)item).getKeyCode();
            if(keyCode >= KeyEvent.VK_LEFT && keyCode <= KeyEvent.VK_DOWN)
                return true;
        }
        return false;
    }

    private boolean isDeleteItem(LogItem item) {
        if(item.getType() == LogEventTypes.SERVICE_KEY) {
            int keyCode = ((LogServiceKey)item).getKeyCode();
            if(keyCode == KeyEvent.VK_BACK_SPACE || keyCode == KeyEvent.VK_DELETE)
                return true;
        }
        return false;
    }

    private LogItem readOneObject(BufferedReader br) {
        LogItem item = null;
        try {
            String s;
            if((s = br.readLine()) != null) {
                item = mapper.readValue(s.getBytes(), LogItem.class);
                item = getLogItem(s, item.getType());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return item;
    }

    private LogItem getLogItem(final String source, final LogEventTypes type) throws Exception {
        switch (type) {
            case CHARACTER_KEY:
                return mapper.readValue(source.getBytes(), LogCharacterKey.class);
            case SAVE_ACTION:
                return mapper.readValue(source.getBytes(), LogSaveFile.class);
            case SELECTION:
                return mapper.readValue(source.getBytes(), LogSelection.class);
            case SELECTION_CLEAR:
                return mapper.readValue(source.getBytes(), LogSelectionClear.class);
            case SERVICE_KEY:
                return mapper.readValue(source.getBytes(), LogServiceKey.class);
            case OPEN_ACTION:
                return mapper.readValue(source.getBytes(), LogOpenFile.class);
            case CLOSE_ACTION:
                return mapper.readValue(source.getBytes(), LogCloseFile.class);
            case COMPILE_ACTION:
                return mapper.readValue(source.getBytes(), LogCompile.class);
            case RUN_ACTION:
                return mapper.readValue(source.getBytes(), LogRun.class);
            case PASTE_ACTION:
                return mapper.readValue(source.getBytes(), LogPaste.class);
            case COPY_ACTION:
                return mapper.readValue(source.getBytes(), LogCopy.class);
            case CUT_ACTION:
                return mapper.readValue(source.getBytes(), LogCut.class);
            default:
                throw new IllegalArgumentException("there is no such type");
        }
    }

    public void nextAction() {
        //do {
        if(iter.hasNext()) {
            LinkedList<LogItem> itemsOneList;
            itemsOneList = iter.next();
            for(LogItem item : itemsOneList) {
                try {
                    processItem(item);
                } catch (AWTException e) {
                    log.info("Cannot instantiate robot");
                }
            }
        }
        /*if (current != null) {
            try {
                //log.info("Call processItem");
                processItem();
                //log.info("After work processItem");
            } catch (AWTException e) {
                //log.info("Cannot instantiate robot");
            }
            int nextIndex = items.indexOf(current) + 1;
            if (items.size() > nextIndex) {
                previous = current;
                //log.info("Befor change current item. Current: " + current);
                current = items.get(nextIndex);
                //log.info("After change current item. New current: " + current);
            } else {
                current = null;
            }
        }*/
        //log.info("\n\n\n");
        //} while(current.getType() != LogEventTypes.SERVICE_KEY);
    }

    private void processItem(LogItem item) throws AWTException {
        LogEventTypes type = item.getType();
        switch (type) {
            case SERVICE_KEY:
                pressServiceKey((LogServiceKey)item);
                break;
            case CHARACTER_KEY:
                pressCharKey((LogCharacterKey)item);
                break;
            case SELECTION:
                //hasSelection = true;
                addSelection((LogSelection)item);
                break;
            case SELECTION_CLEAR:
                textArea.setSelection((Selection) null);
                //hasSelection = false;
                break;
            case PASTE_ACTION:
                if (textArea.getSelection() == null) {
                    textArea.setCaretPosition(((LogPaste) item).getPosition());
                }
                Registers.paste(textArea, '$', false);
                textArea.setCaretPosition(((LogPaste) item).getPosition());
                break;
            case CUT_ACTION:
                Registers.cut(textArea,'$');
                break;
            case COPY_ACTION:
                Registers.copy(textArea,'$');
                break;
            default:
                JOptionPane.showMessageDialog(textArea, item.getStringForm(), "Current action:", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private boolean isFlagItem(LogItem item) {
        LogEventTypes type = item.getType();
        switch(type) {
            case SERVICE_KEY:
                int keyCode = ((LogServiceKey) item).getKeyCode();
                if (keyCode >= KeyEvent.VK_LEFT && keyCode <= KeyEvent.VK_DOWN ||
                        keyCode == KeyEvent.VK_BACK_SPACE || keyCode == KeyEvent.VK_DELETE) {
                    return true;
                }
                break;
            case SELECTION:
                return true;
            case SELECTION_CLEAR:
                return true;
            case CUT_ACTION:
                return true;
            case COPY_ACTION:
                return true;
            case PASTE_ACTION:
                return true;
        }
        return false;
    }

    private void pressCharKey(LogCharacterKey item) throws AWTException {
        final Robot robot = new Robot();
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

    private boolean isShiftRequired(LogCharacterKey item)
    {
        return item.getMask() == KeyEvent.SHIFT_MASK;
    }

    private void pressServiceKey(LogServiceKey item) throws AWTException {
        final Robot robot = new Robot();
        //ensurePosition(getCaretForServiceKey((LogServiceKey)current));
        robot.keyPress(item.getKeyCode());
        robot.keyRelease(item.getKeyCode());
    }

    public void addSelection(LogSelection item) {
        textArea.setSelection(item.createSelection());
    }
}


