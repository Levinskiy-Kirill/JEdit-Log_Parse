package org.log.parse;

import org.codehaus.jackson.map.ObjectMapper;
import org.gjt.sp.jedit.Registers;
import org.gjt.sp.jedit.buffer.JEditBuffer;
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
import sun.awt.image.ImageWatched;

public class ParseUtil {
    private static final Logger log = LoggerFactory.getLogger(ParseUtil.class);
    private ObjectMapper mapper = new ObjectMapper();
    private JEditTextArea textArea;
    private LinkedList<LinkedList<LogItem>> items;
    //private LinkedList<LogItem> itemsOneList;
    private ListIterator<LinkedList<LogItem>> iter;
    //private LinkedList<LogItem> current;
    private boolean hasSelection;
    JEditBuffer buffer;

    public ParseUtil(JEditTextArea textArea) {
        this.textArea = textArea;
        items = new LinkedList<LinkedList<LogItem>>();
        hasSelection = false;
        //buffer = textArea.getBuffer();
    }

    public void parseLog() {
        try {
            if(openLogFile()) {
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
                else if(isBackSpaceItem(item)) {
                    while(isBackSpaceItem(item)) {
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
            if((keyCode >= KeyEvent.VK_LEFT && keyCode <= KeyEvent.VK_DOWN)
                    || keyCode == KeyEvent.VK_PAGE_UP
                    || keyCode == KeyEvent.VK_PAGE_DOWN
                    || keyCode == KeyEvent.VK_HOME
                    || keyCode == KeyEvent.VK_END)
                return true;
        }
        return false;
    }

    private boolean isDeleteItem(LogItem item) {
        if(item.getType() == LogEventTypes.SERVICE_KEY) {
            int keyCode = ((LogServiceKey)item).getKeyCode();
            if(keyCode == KeyEvent.VK_DELETE)
                return true;
        }
        return false;
    }

    private boolean isBackSpaceItem(LogItem item) {
        if(item.getType() == LogEventTypes.SERVICE_KEY) {
            int keyCode = ((LogServiceKey)item).getKeyCode();
            if(keyCode == KeyEvent.VK_BACK_SPACE)
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

    public void nextAction(JEditBuffer buffer) {
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
                hasSelection = true;
                addSelection((LogSelection)item);
                break;
            case SELECTION_CLEAR:
                textArea.setSelection((Selection) null);
                JOptionPane.showMessageDialog(textArea, item.getStringForm(), "Current action:", JOptionPane.INFORMATION_MESSAGE);
                hasSelection = false;
                break;
            case PASTE_ACTION:
                if (textArea.getSelection() == null) {
                    textArea.setCaretPosition(((LogPaste) item).getPosition());
                }
                Registers.setRegister('$', ((LogPaste)item).getText());
                Registers.paste(textArea, '$', false);
                //textArea.setCaretPosition(((LogPaste) item).getPosition());
                break;
            case CUT_ACTION:
                textArea.setSelectedText("");
                break;
            case COPY_ACTION:
                //Registers.copy(textArea,'$');
                break;
            case COMPILE_ACTION:
                JOptionPane.showMessageDialog(textArea, ((LogCompile)item).getText(), "Результат компиляции", JOptionPane.INFORMATION_MESSAGE);
                break;
            case RUN_ACTION:
                JOptionPane.showMessageDialog(textArea, ((LogRun)item).getText(), "Результат запуска", JOptionPane.INFORMATION_MESSAGE);
                break;
            default:
                JOptionPane.showMessageDialog(textArea, item.getStringForm(), "Current action:", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void pressCharKey(LogCharacterKey item) {
        try {
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
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    private boolean isShiftRequired(LogCharacterKey item)
    {
        return item.getMask() == KeyEvent.SHIFT_MASK;
    }

    private void pressServiceKey(LogServiceKey item) throws AWTException {
        final Robot robot = new Robot();
        if (!isMovingCursorType(item) && !isDeleteItem(item) && !isBackSpaceItem(item)) {
            ensurePosition(item.getPosition());
        }
        robot.keyPress(item.getKeyCode());
        robot.keyRelease(item.getKeyCode());

    }

    private void ensurePosition(int position) {
        int bufferLength = textArea.getBufferLength();
        if (bufferLength < position) {
            textArea.setCaretPosition(bufferLength);
        } else {
            textArea.setCaretPosition(position);
        }
    }

    private void addSelection(LogSelection item) {
        textArea.setSelection(item.createSelection());
    }

    public void previousAction(JEditBuffer buffer) {
        if(iter.hasPrevious()) {
            LinkedList<LogItem> itemsOneList;
            itemsOneList = iter.previous();
            cancelProcess(itemsOneList);
        }
    }

    private void cancelProcess (LinkedList<LogItem> itemsOneList) {
        LogItem checkItem = itemsOneList.getFirst();

        if(isTypesetting(checkItem)) {
            canselTypesetting(itemsOneList);
        }
        else if(isMovingCursorType(checkItem)) {
            canselMoving((LogServiceKey) checkItem);
        }
        else if(isBackSpaceItem(checkItem)) {
            canselBackSpace(itemsOneList.size(), ((LogServiceKey)itemsOneList.getLast()).getPosition());
        }
        else if(isDeleteItem(checkItem)) {
            canselDelete(itemsOneList.size(), ((LogServiceKey)itemsOneList.getFirst()).getPosition());
        }
        else {
            LogEventTypes type = checkItem.getType();
            switch (type) {
                case SELECTION:
                    textArea.setSelection((Selection) null);
                    hasSelection = false;
                    break;
                case SELECTION_CLEAR:
                    canselSelectionClear();
                case CUT_ACTION:
                    Registers.setRegister('$', ((LogCut)checkItem).getText());
                    Registers.paste(textArea, '$');
                    canselSelectionClear();
                    break;
                case PASTE_ACTION:
                    canselPaste((LogPaste)checkItem);
                    break;
                case COMPILE_ACTION:
                    JOptionPane.showMessageDialog(textArea, ((LogCompile)checkItem).getText(), "Результат компиляции", JOptionPane.INFORMATION_MESSAGE);
                    break;
                case RUN_ACTION:
                    JOptionPane.showMessageDialog(textArea, ((LogRun)checkItem).getText(), "Результат запуска", JOptionPane.INFORMATION_MESSAGE);
                    break;
                default:
                    JOptionPane.showMessageDialog(textArea, checkItem.getStringForm(), "Canceled action:", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void canselMoving(LogServiceKey item) {
        int posFirstItem = item.getPosition();
        int keyCode = item.getKeyCode();
        switch (keyCode) {
            case KeyEvent.VK_RIGHT:
                ensurePosition(posFirstItem - 1);
                break;
            case KeyEvent.VK_LEFT:
                ensurePosition(posFirstItem + 1);
                break;
            case KeyEvent.VK_DOWN:
                textArea.goToPrevLine(false);
                break;
            case KeyEvent.VK_UP:
                textArea.goToNextLine(false);
                break;
        }
    }

    private void canselSelectionClear() {
        ListIterator<LinkedList<LogItem>> tempIter = iter;
        LinkedList<LogItem> itemsOneList = tempIter.previous();
        LogItem tempItem = itemsOneList.getFirst();
        hasSelection = true;

        while(tempItem.getType() != LogEventTypes.SELECTION) {
            cancelProcess(itemsOneList);
            itemsOneList = tempIter.previous();
            tempItem = itemsOneList.getFirst();
        }
        addSelection((LogSelection)tempItem);
    }

    private void canselPaste(LogPaste item) {
        String s = item.getText();
        ensurePosition(item.getPosition() - s.length());

        for (int i = 0; i < s.length(); i++) {
            textArea.delete();
        }

        if(hasSelection) {
            ListIterator<LinkedList<LogItem>> tempIter = iter;
            LinkedList<LogItem> itemsOneList = tempIter.previous();
            LogItem tempItem = itemsOneList.getFirst();

            while (tempItem.getType() != LogEventTypes.SELECTION) {
                cancelProcess(itemsOneList);
                itemsOneList = tempIter.previous();
                tempItem = itemsOneList.getFirst();
            }

            Registers.setRegister('$', ((LogSelection)tempItem).getText());
            Registers.paste(textArea, '$');
        }
    }

    private void canselBackSpace(int listSize, int endPosition)  {
        ListIterator<LinkedList<LogItem>> tempIter = items.listIterator(iter.previousIndex());
        LinkedList<LogItem> searchList = tempIter.next();
        LogItem tempItem;

        while(tempIter.hasPrevious()) {
            tempItem = searchList.getFirst();
            if (isTypesetting(tempItem)) {
                for (LogItem item : searchList) {
                    if (((LogKey) item).getPosition() == endPosition) {
                        log.info("endPosition" + endPosition);
                        log.info("search item = " + item.getStringForm());
                        try {
                            processItem(item);
                        } catch (AWTException e) {
                            e.printStackTrace();
                        }
                        log.info("Carret position after pressCharKey = " + textArea.getCaretPosition());
                        endPosition++;
                        listSize--;
                    }

                    if(listSize == 0)
                        return;
                }
            }
            searchList = tempIter.previous();
        }
    }

    private void canselDelete(int listSize, int endPosition) {

    }

    private void canselTypesetting(LinkedList<LogItem> itemsOneList) {
        int startPosition = ((LogKey)itemsOneList.getFirst()).getPosition();
        int endPosition = ((LogKey)itemsOneList.getLast()).getPosition() + 1;
        ensurePosition(endPosition);

        for (int i = 0; i < endPosition - startPosition; i++) {
            textArea.backspace();
        }
    }
}


