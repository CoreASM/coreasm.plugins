package org.coreasm.ast2spec.ui;

import java.io.File;

import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

public class ExportAstDialog extends JFileChooser{

	private static final long serialVersionUID = 1L;

	private JCheckBox generateDefinitions = new JCheckBox("include definitions");
	private JCheckBox generateLineNumbers = new JCheckBox("include line numbers");

	private File selectedFile = null;

	public ExportAstDialog() {

		JPanel panel1 = (JPanel) this.getComponent(3);
		JPanel panel2 = (JPanel) panel1.getComponent(3);
		panel2.add(generateDefinitions);
		panel2.add(generateLineNumbers);

	}

	public File getSeletedFile(){
			return selectedFile;
	}

	public boolean withDefinitions(){
		return generateDefinitions.isSelected();
	}

	public boolean withLineNumbers(){
		return generateLineNumbers.isSelected();
	}

}