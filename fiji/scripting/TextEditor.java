package fiji.scripting;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JPanel;
import javax.swing.JFileChooser;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JMenuBar;
import javax.swing.BorderFactory;
import javax.swing.KeyStroke;
import javax.swing.ToolTipManager;
import javax.swing.JOptionPane;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.BorderLayout;
import java.awt.event.WindowEvent;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.InputMethodEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.awt.event.InputMethodListener;
import java.awt.event.WindowListener;
import javax.swing.event.ChangeListener;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.CaretEvent;
import javax.swing.text.Document;
import java.io.File;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.FileWriter;
import java.io.IOException;
import java.awt.image.BufferedImage;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.IJ;
import ij.Prefs;
import javax.imageio.ImageIO;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.ToolTipSupplier;
import org.fife.ui.rtextarea.IconGroup;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import common.RefreshScripts;
import com.sun.jdi.connect.VMStartException;

import fiji.scripting.completion.ClassCompletionProvider;
import fiji.scripting.completion.DefaultProvider;


class TextEditor extends JFrame implements ActionListener, ItemListener, ChangeListener, MouseMotionListener, MouseListener, CaretListener, InputMethodListener, DocumentListener, WindowListener {

	// TODO: clean up unnecessary variables
	boolean fileChanged = false;
	boolean isFileUnnamed = true;
	String language = new String();
	InputMethodListener l;
	File file, f;
	CompletionProvider provider1;
	RSyntaxTextArea textArea;
	JTextArea screen = new JTextArea();
	Document doc;
	JMenuItem new1, open, save, saveas, compileAndRun, debug, quit, undo, redo, cut, copy, paste, find, replace, selectAll, autocomplete, resume, terminate;
	JRadioButtonMenuItem[] lang = new JRadioButtonMenuItem[8];
	FileInputStream fin;
	// TODO: fix (enableReplace(boolean))
	FindAndReplaceDialog replaceDialog;
	AutoCompletion autocomp;
	// TODO: probably language can go
	ClassCompletionProvider provider;
	StartDebugging debugging;
	Gutter gutter;
	IconGroup iconGroup;

	public TextEditor(String path1) {
		JPanel cp = new JPanel(new BorderLayout());
		textArea = new RSyntaxTextArea();
		textArea.addInputMethodListener(l);
		textArea.addCaretListener(this);
		// TODO: is this necessary?
		textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
		// TODO: much better naming required
		// TODO: remove unnecessary curly brackets
		if (provider1 == null) {
			provider1 = createCompletionProvider();
		}
		autocomp = new AutoCompletion(provider1);
		// TODO: is this really needed?
		autocomp.setListCellRenderer(new CCellRenderer());
		autocomp.setShowDescWindow(true);
		autocomp.setParameterAssistanceEnabled(true);
		autocomp.install(textArea);
		textArea.setToolTipSupplier((ToolTipSupplier)provider);
		ToolTipManager.sharedInstance().registerComponent(textArea);
		// TODO: do we need doc?
		doc = textArea.getDocument();
		doc.addDocumentListener(this);
		RTextScrollPane sp = new RTextScrollPane(textArea);
		sp.setPreferredSize(new Dimension(600, 350));
		sp.setIconRowHeaderEnabled(true);
		gutter = sp.getGutter();
		iconGroup = new IconGroup("bullets", "images/", null, "png", null);
		gutter.setBookmarkIcon(iconGroup.getIcon("var"));
		gutter.setBookmarkingEnabled(true);
		screen.setEditable(false);
		screen.setLineWrap(true);
		Font font = new Font("Courier", Font.PLAIN, 12);
		screen.setFont(font);
		JScrollPane scroll = new JScrollPane(screen);
		scroll.setPreferredSize(new Dimension(600, 80));
		JSplitPane panel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, sp, scroll);
		panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		panel.setResizeWeight(350.0 / 430.0);
		setContentPane(panel);
		// TODO: Unnamed
		setTitle();
		addWindowListener(this);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		/*********** Creating the menu options in the text editor ****************/

		JMenuBar mbar = new JMenuBar();
		// TODO: is this not too early?
		setJMenuBar(mbar);

		/*******  creating the menu for the File option **********/
		JMenu file = new JMenu("File");
		file.setMnemonic(KeyEvent.VK_F);
		// TODO: this cannot work, new1 must be assigned
		addToMenu(file, new1, "New", 0, KeyEvent.VK_N, ActionEvent.CTRL_MASK);
		addToMenu(file, open, "Open...", 0, KeyEvent.VK_O, ActionEvent.CTRL_MASK);
		addToMenu(file, save, "Save", 0, KeyEvent.VK_S, ActionEvent.CTRL_MASK);
		addToMenu(file, save, "Save as...", 1, 0, 0);
		file.addSeparator();
		addToMenu(file, quit, "Quit", 0, KeyEvent.VK_X, ActionEvent.ALT_MASK);

		mbar.add(file);

		/********The file menu part ended here  ***************/

		/*********The Edit menu part starts here ***************/

		JMenu edit = new JMenu("Edit");
		addToMenu(edit, undo, "Undo", 0, KeyEvent.VK_Z, ActionEvent.CTRL_MASK);
		addToMenu(edit, redo, "Redo", 0, KeyEvent.VK_Y, ActionEvent.CTRL_MASK);
		edit.addSeparator();
		addToMenu(edit, cut, "Cut", 0, KeyEvent.VK_X, ActionEvent.CTRL_MASK);
		addToMenu(edit, copy, "Copy", 0, KeyEvent.VK_C, ActionEvent.CTRL_MASK);
		addToMenu(edit, paste, "Paste", 0, KeyEvent.VK_V, ActionEvent.CTRL_MASK);
		edit.addSeparator();
		addToMenu(edit, find, "Find...", 0, KeyEvent.VK_F, ActionEvent.CTRL_MASK);
		addToMenu(edit, replace, "Find and Replace...", 0, KeyEvent.VK_H, ActionEvent.CTRL_MASK);
		edit.addSeparator();
		// TODO: this belongs higher, no?
		addToMenu(edit, selectAll, "Select All", 0, KeyEvent.VK_A, ActionEvent.CTRL_MASK);
		mbar.add(edit);

		/******** The Edit menu part ends here *****************/

		// TODO: remove useless comments
		/********The options menu part starts here**************/
		// TODO: add accelerator keys for the menus, too
		JMenu options = new JMenu("Options");
		// TODO: CTRL, ALT
		addToMenu(options, autocomplete, "Autocomplete", 0, KeyEvent.VK_SPACE, ActionEvent.CTRL_MASK);
		options.addSeparator();

		mbar.add(options);

		/*********The Language parts starts here********************/
		JMenu languages = new JMenu("Language");
		ButtonGroup group = new ButtonGroup();
		for (Languages.Language language :
		                Languages.getInstance().languages) {
			JRadioButtonMenuItem item =
			        new JRadioButtonMenuItem(language.menuLabel);
			if (language.shortCut != 0)
				item.setMnemonic(language.shortCut);
			item.addActionListener(this);
			item.setActionCommand(language.extension);

			group.add(item);
			languages.add(item);
			language.item = item;
		}
		Languages.getInstance().get("").item.setSelected(true);
		mbar.add(languages);

		JMenu run = new JMenu("Run");
		// TODO: allow outside-of-plugins/ sources
		addToMenu(run, compileAndRun, "Compile and Run", 0, KeyEvent.VK_F11, ActionEvent.CTRL_MASK);
		run.addSeparator();
		addToMenu(run, debug, "Start Debugging", 0, KeyEvent.VK_F11, 0);
		mbar.add(run);

		JMenu breakpoints = new JMenu("Breakpoints");
		addToMenu(breakpoints, resume, "Resume", 1, 0, 0);
		addToMenu(breakpoints, terminate, "Terminate", 1, 0, 0);
		mbar.add(breakpoints);




		/*********** The menu part ended here    ********************/

		pack();
		getToolkit().setDynamicLayout(true);            //added to accomodate the autocomplete part
		// TODO: is this needed?
		setLocationRelativeTo(null);
		setVisible(true);
		if (path1 != null && !path1.equals(""))
			open(path1);
	}

	public void addToMenu(JMenu menu, JMenuItem menuitem, String menuEntry, int keyEvent, int keyevent, int actionevent) {
		menuitem = new JMenuItem(menuEntry);
		menu.add(menuitem);
		if (keyEvent == 0) // == 0?  Not != 0?
			menuitem.setAccelerator(KeyStroke.getKeyStroke(keyevent, actionevent));
		menuitem.addActionListener(this);
	}

	public void createNewDocument() {
		//TODO: Hmm.
		doc.removeDocumentListener(this);
		textArea.setText("");
		file = null;
		isFileUnnamed = true;
		fileChanged = false;
		setTitle();
		doc.addDocumentListener(this);
	}

	public boolean handleUnsavedChanges() {
		if (!fileChanged)
			return true;

		switch (JOptionPane.showConfirmDialog(this,
				"Do you want to save changes?")) {
		case JOptionPane.NO_OPTION:
			return true;
		case JOptionPane.YES_OPTION:
			if (save())
				return true;
		}

		return false;
	}

	public void actionPerformed(ActionEvent ae) {
		String command = ae.getActionCommand();
		// TODO: NO!!!!
		if (command.equals("New")) {
			if (!handleUnsavedChanges())
				return;
			// TODO: NO!!!!
			else
				createNewDocument();
		}

		if (command.equals("Open...")) {
			if (!handleUnsavedChanges())
				return;

			OpenDialog dialog = new OpenDialog("Open..", "");
			String name = dialog.getFileName();
			if (name != null)
				open(dialog.getDirectory() + name);
			return;
		}

		if (command.equals("Save"))
			save();
		if (command.equals("Save as..."))
			saveAs();
		if (command.equals("Compile and Run"))
			// TODO: s/Script//
			runScript();
		if (command.equals("Start Debugging")) {
			BreakpointManager manager = new BreakpointManager(gutter, textArea, iconGroup);
			debugging = new StartDebugging(file.getPath(), manager.findBreakpointsLineNumber());

			try {
				System.out.println(debugging.startDebugging().exitValue());
				// TODO: at least use printStackTrace()
			} catch (Exception e) {}
		}
		if (command.equals("Quit"))
			processWindowEvent( new WindowEvent(this, WindowEvent.WINDOW_CLOSING) );

		if (command.equals("Cut"))
			textArea.cut();
		if (command.equals("Copy"))
			textArea.copy();
		if (command.equals("Paste"))
			textArea.paste();
		if (command.equals("Undo"))
			textArea.undoLastAction();
		if (command.equals("Redo"))
			textArea.redoLastAction();
		if (command.equals("Find..."))
			setFindAndReplace(false);
		if (command.equals("Find and Replace...")) {						//here should the code to close all other dialog boxes
			try {
				setFindAndReplace(true);

			} catch (Exception e) {
				e.printStackTrace(); // TODO: huh?
			}
		}


		if (command.equals("Select All")) {
			textArea.setCaretPosition(0);
			textArea.moveCaretPosition(textArea.getDocument().getLength());
		}

		if (command.equals("Autocomplete")) {
			try {
				autocomp.doCompletion();
			} catch (Exception e) {}
		}

		//setting actionPerformed for language menu
		// TODO: handle "None"
		if (command.startsWith("."))
			setLanguageByExtension(command);
		if (command.equals("Resume"))
			debugging.resumeVM();
		if (command.equals("Terminate")) { }

	}

	protected RSyntaxDocument getDocument() {
		return (RSyntaxDocument)textArea.getDocument();
	}

	// TODO: nonono.
	public void setFindAndReplace(boolean ifReplace) {
		if (replaceDialog != null) {						//here should the code to close all other dialog boxes
			if (replaceDialog.isReplace() != ifReplace) {
				replaceDialog.dispose();
				replaceDialog = null;
			}
		}
		if (replaceDialog == null) {
			replaceDialog = new FindAndReplaceDialog(this, textArea, ifReplace);
			replaceDialog.setResizable(true);
			replaceDialog.pack();
			replaceDialog.setLocationRelativeTo(this);
		}
		replaceDialog.show();
		replaceDialog.toFront();
	}

	public void open(String path) {
		try {
			file = new File(path);
		} catch (Exception e) {
			System.out.println("problem in opening");
		}
		// TODO: Why?
		doc.removeDocumentListener(this);
		try {
			if (file != null) {
				fileChanged = false;
				setFileName(file);
				fin = new FileInputStream(file);
				BufferedReader din = new BufferedReader(new InputStreamReader(fin));
				StringBuilder text = new StringBuilder();
				String line;
				while ((line = din.readLine()) != null)
					text.append(line).append("\n");
				textArea.setText(text.toString());
				fin.close();
			} else {
				// TODO: unify error handling.  Don't mix JOptionPane with IJ.error as if we had no clue what we want
				JOptionPane.showMessageDialog(this, "The file name " + file.getName() + " not found.");
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (null != fin) {
				try {
					fin.close();
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}
			doc.addDocumentListener(this);
		}

	}

	public boolean saveAs() {
		SaveDialog sd = new SaveDialog("Save as ", "New_", "");
		String name = sd.getFileName();
		if (name == null)
			return false;

		String path = sd.getDirectory() + name;
		return saveAs(path, checkForReplace(sd.getDirectory(), name));
	}

	// TODO: this is racy at best
	public boolean checkForReplace(String directory, String name) {
		return(new File(directory, name).exists());

	}

	public void saveAs(String path) {
		saveAs(path, true);
	}

	public boolean saveAs(String path, boolean askBeforeReplacing) {
		file = new File(path);
		if (file.exists() && !askBeforeReplacing &&
				JOptionPane.showConfirmDialog(this,
					"Do you want to replace " + path + "?",
					"Replace " + path + "?",
					JOptionPane.YES_NO_OPTION)
				!= JOptionPane.YES_OPTION)
			return false;
		if (!write(file))
			return false;
		setFileName(file);
		return true;
	}

	public boolean save() {
		if (isFileUnnamed) // TODO: this should be "file == null"
			return saveAs();
		if (!write(file))
			return false;
		setTitle();
		return true;
	}

	public boolean write(File file) {
		try {
			BufferedWriter outFile =
				new BufferedWriter(new FileWriter(file));
			outFile.write(textArea.getText());
			outFile.close();
			fileChanged = false;
			return true;
		} catch (IOException e) {
			IJ.error("Could not save " + file.getName());
			e.printStackTrace();
			return false;
		}
	}

	public static String getExtension(String fileName) {
		int dot = fileName.lastIndexOf(".");
		return dot < 0 ?  "" : fileName.substring(dot);
	}

	private void setLanguageByExtension(String extension) {
		Languages.Language info = Languages.getInstance().get(extension);

		// TODO: these should go to upstream RSyntaxTextArea
		if (extension.equals(".clj"))
			getDocument().setSyntaxStyle(new ClojureTokenMaker());
		else if (extension.equals(".m"))
			getDocument().setSyntaxStyle(new MatlabTokenMaker());
		else
			textArea.setSyntaxEditingStyle(info.syntaxStyle);
		provider.setProviderLanguage(info.menuLabel);

		info.item.setSelected(true);
	}

	public void setFileName(File file) {
		isFileUnnamed = false;
		setTitle();
		setLanguageByExtension(getExtension(file.getName()));
	}

	private void setTitle() {
		String fileName = file == null ? "New_" : file.getName();
		String title = (fileChanged ? "*" : "") + fileName;
		setTitle(title);
	}

	public void runScript() {
		if (!handleUnsavedChanges())
			return;
		runSavedScript();
	}

	// TODO: do not require saving
	public void runSavedScript() {
		String ext = getExtension(file.getName());
		final RefreshScripts interpreter =
		        Languages.getInstance().get(ext).interpreter;
		if (interpreter == null) {
			IJ.error("There is no interpreter for " + ext
			         + " files!");
			return;
		}
		new Thread() {
			public void run() {
				interpreter.runScript(file.getPath());
			}
		}.start();
	}

	public void windowClosing(WindowEvent e) {
		if (!handleUnsavedChanges())
			return;
		dispose();

	}

	//next function is for the InputMethodEvent changes
	public void inputMethodTextChanged(InputMethodEvent event) {
		updateStatusOnChange();
	}
	public void caretPositionChanged(InputMethodEvent event) {
		updateStatusOnChange();
	}

	public void insertUpdate(DocumentEvent e) {
		updateStatusOnChange();
	}
	public void removeUpdate(DocumentEvent e) {
		updateStatusOnChange();
	}

	// TODO: rename into "markDirty"
	private void updateStatusOnChange() {
		fileChanged = true;
		setTitle();
	}

	private CompletionProvider createCompletionProvider() {
		// TODO: why the member variable?
		provider = new ClassCompletionProvider(new DefaultProvider(), textArea, language);
		return provider;

	}

	// TODO: use an anonymous WindowAdapter, MouseAdapter, etc instead
	public void windowClosed(WindowEvent e) {}
	public void windowDeactivated(WindowEvent e) {}
	public void windowDeiconified(WindowEvent e) {}
	public void windowIconified(WindowEvent e) {}
	public void windowOpened(WindowEvent e) {}
	public void itemStateChanged(ItemEvent ie) {}
	public void stateChanged(ChangeEvent e) {}
	public void mouseMoved(MouseEvent me) {}
	public void mouseClicked(MouseEvent me) {}
	public void mouseEntered(MouseEvent me) {}
	public void mouseExited(MouseEvent me) {}
	public void mouseDragged(MouseEvent me) {}
	public void mouseReleased(MouseEvent me) {}
	public void mousePressed(MouseEvent me) {}
	public void caretUpdate(CaretEvent ce) {}
	public void changedUpdate(DocumentEvent e) {}
	public void windowActivated(WindowEvent e) {}
}
// TODO: check all files for whitespace issues
