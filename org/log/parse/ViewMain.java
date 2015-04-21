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

package org.log.parse;

import javax.swing.*;

public class ViewMain extends JFrame {
/*

	public static final int WINDOW_WIDTH = 800;
	public static final int WINDOW_HEIGHT = 600;

	private final Collection<LogItem> items = new ArrayList<LogItem>();
	private final JEditTextArea textArea;

	static {
		try {
			System.setOut(new PrintStream(Paths.get("jedit-main.log").toFile()));
		} catch (IOException e) {
			Log.log(Log.ERROR, null, "Cannot change system out", e);
		}
	}

	public ViewMain(Collection<LogItem> items, JEditTextArea textArea) throws HeadlessException {
		super();
		this.items.addAll(items);
		this.textArea = textArea;
		setSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setTitle("Prototype");
		setContentPane(createContent());
		setVisible(true);
		pack();
	}

	private JPanel createContent() {
		return new ViewPanel();
	}

	private class ViewPanel extends JPanel {

		private final JFileChooser chooser = new JFileChooser(System.getProperty("user.home"));
		private final JScrollPane scrollPane = new JScrollPane();
		private DefaultListModel<LogItem> model = new DefaultListModel<LogItem>();
		private JList<LogItem> list;

		private ViewPanel() {
			super(new GridBagLayout());
			setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
			((GridBagLayout)getLayout()).columnWidths = new int[] {0, 0, 0, 0, 0, 234, 0};
			((GridBagLayout)getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
			((GridBagLayout)getLayout()).columnWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};
			((GridBagLayout)getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};
			scrollPane.setPreferredSize(new Dimension(100, 200));
			scrollPane.setViewportView(createList(items));
			add(scrollPane, new GridBagConstraints(0, 27, 6, 8, 0.0, 0.0,
							       GridBagConstraints.CENTER, GridBagConstraints.BOTH,
							       new Insets(0, 0, 5, 0), 0, 0));
			add(createOpenLogButton(),  new GridBagConstraints(0, 35, 4, 1, 0.0, 0.0,
									   GridBagConstraints.CENTER, GridBagConstraints.BOTH,
									   new Insets(0, 0, 5, 5), 0, 0));
			pack();
		}

		private JTextArea createTextArea() {
			return null;
		}

		private JList<LogItem> createList(final Collection<LogItem> items) {
			int i = 0;
			for (final LogItem item : items) {
				model.add(i, item);
				i++;
			}
			list = new JList<LogItem>(model);
			for (final MouseListener ml : list.getMouseListeners()) {
				list.removeMouseListener(ml);
			}
			list.setLayoutOrientation(JList.VERTICAL);
			list.setVisibleRowCount(-1);
			list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//			list.setCellRenderer(new MyCellRenderer());
			list.addListSelectionListener(new ListSelectionListener()
			{
				@Override
				public void valueChanged(ListSelectionEvent e)
				{
					LogItem item = list.getModel().getElementAt(e.getLastIndex());
					System.out.println("Processing " + item);
					if (item.getType() == LogEventTypes.CHARACTER_KEY
					    || item.getType() == LogEventTypes.SERVICE_KEY)
					{
						KeyEvent event = ((LogKey) item).createEvent(textArea);
						try {
							Robot robot = new Robot();
							robot.setAutoDelay(300);
							textArea.getView().requestFocus();
//							Thread.sleep(500);
							robot.keyPress(event.getKeyCode());
//							Thread.sleep(500);
							ViewMain.this.requestFocus();
						} catch (AWTException ex) {
							System.out.println("Cannot create robot");
//						} catch (InterruptedException ex) {
//							System.out.println("Interrupted");
						}
						System.out.println("Dispatching event from item " + item);
						textArea.processKeyEvent(event);
						textArea.getInputHandler().handleKey(new KeyEventTranslator.Key("", event.getKeyCode(), event.getKeyChar()), false);
					} else if (item.getType() == LogEventTypes.SELECTION)
					{
						textArea.setSelection(((LogSelection) item).createSelection());
					} else
					{
						//TODO:create alert?
					}
				}
			});
			return list;
		}

		private JButton createOpenLogButton() {
			final JButton openFileButton = new JButton("Open log file");
			openFileButton.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					int res = chooser.showOpenDialog(ViewPanel.this);
					if (res == JFileChooser.APPROVE_OPTION) {
						try {
							items.addAll(ParseUtil.parseFile(chooser.getSelectedFile().getAbsolutePath()));
						} catch (Exception ex) {
							System.out.println("Cannot parse log file");
							System.out.println(ex.getMessage());
							for (final StackTraceElement ste : ex.getStackTrace()) {
								System.out.println(ste);
							}
						}
						model.clear();
						int i = 0;
						for (final LogItem item : items) {
							model.add(i, item);
							i++;
						}
						list.setSelectedIndex(0);
					}
				}
			});
			return openFileButton;
		}
	}

	private static class LogListModel<LogItem> extends AbstractListModel<LogItem> {

		private List<LogItem> items;

		private LogListModel(Collection<LogItem> items)
		{
			this.items = new ArrayList<LogItem>(items);
		}

		@Override
		public int getSize()
		{
			return items.size();
		}

		@Override
		public LogItem getElementAt(int index)
		{
			return items.get(index);
		}
	}

	private static class MyCellRenderer extends JLabel implements ListCellRenderer<LogItem> {
		public MyCellRenderer() {
			setOpaque(true);
		}

		public Component getListCellRendererComponent(
			JList<? extends LogItem> list,
			LogItem value,
			int index,
			boolean isSelected,
			boolean cellHasFocus
		) {

			setText(value.getStringForm());

			Color background;
			Color foreground;

			// check if this cell represents the current DnD drop location
			JList.DropLocation dropLocation = list.getDropLocation();
			if (dropLocation != null
			    && !dropLocation.isInsert()
			    && dropLocation.getIndex() == index) {

				background = Color.BLUE;
				foreground = Color.WHITE;

				// check if this cell is selected
			} else if (isSelected) {
				background = Color.RED;
				foreground = Color.WHITE;

				// unselected, and not the DnD drop location
			} else {
				background = Color.WHITE;
				foreground = Color.BLACK;
			}

			setBackground(background);
			setForeground(foreground);

			return this;
		}
	}
*/

}
