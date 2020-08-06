
package net.imglib2.labkit.multi_image;

import net.imglib2.labkit.models.LabkitProjectModel;
import net.imglib2.trainable_segmentation.utils.SingletonContext;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class NewProjectDialog extends JDialog {

	private final JTextField textField;

	private final LabkitProjectModel model;

	private boolean approved = false;

	private NewProjectDialog(Component component) {
		super(component != null ? SwingUtilities.getWindowAncestor(component) : null);
		setTitle("New Project");
		setLayout(new MigLayout());
		add(new JLabel("Project folder:"));
		textField = new JTextField();
		add(textField, "growx, pushx");
		add(newButton("...", ignore -> onSelectFolderClicked()), "wrap");
		add(new JLabel("Add images to the project:"), "span, wrap");
		model = new LabkitProjectModel(SingletonContext.getInstance(), null, new ArrayList<>());
		add(new LabkitProjectEditor(model), "grow, span, wrap");
		add(newButton("Create Project", ignore -> onCreateProjectClicked()));
		add(newButton("Cancel", ignore -> onCancelClicked()));
	}

	private void onCancelClicked() {
		dispose();
	}

	private void onCreateProjectClicked() {
		this.approved = true;
		dispose();
	}

	private JButton newButton(String text, ActionListener actionListener) {
		JButton selectFolderButton = new JButton(text);
		selectFolderButton.addActionListener(actionListener);
		return selectFolderButton;
	}

	public static LabkitProjectModel show(Component component) {
		NewProjectDialog dialog = new NewProjectDialog(component);
		dialog.pack();
		dialog.setModal(true);
		dialog.setResizable(true);
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		dialog.setVisible(true);
		if (dialog.approved) {
			LabkitProjectModel newProject = dialog.getModel();
			try {
				LabkitProjectSerializer.save(newProject, new File(newProject.getProjectDirectory(),
					"labkit-project.yaml"));
			}
			catch (IOException e) {
				JOptionPane.showMessageDialog(component, "Error while saving:\n" + e.getMessage(),
					"Create New Project", JOptionPane.ERROR_MESSAGE);
				return null;
			}
			return newProject;
		}
		return null;
	}

	private LabkitProjectModel getModel() {
		String projectDirectory = textField.getText();
		return new LabkitProjectModel(model.context(), projectDirectory, model.labeledImages());
	}

	private void onSelectFolderClicked() {
		JFileChooser dialog = new JFileChooser();
		dialog.setCurrentDirectory(new File(textField.getText()));
		dialog.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		dialog.setDialogTitle("Select Project Folder");
		int returnValue = dialog.showOpenDialog(this);
		if (returnValue == JFileChooser.APPROVE_OPTION) {
			File file = dialog.getSelectedFile();
			if (checkFile(file))
				textField.setText(file.getAbsolutePath());
		}
	}

	private boolean checkFile(File file) {
		if (!file.isDirectory()) {
			JOptionPane.showMessageDialog(this, "Please select a directory.");
			return false;
		}
		if (containsLabkitProjectFiles(file)) {
			int option = JOptionPane.showConfirmDialog(this,
				"The selected directory seems to already contain a Labkit project. Do you want to override it?",
				"Warning", JOptionPane.YES_NO_OPTION);
			return option == JOptionPane.OK_OPTION;
		}
		return true;
	}

	private boolean containsLabkitProjectFiles(File file) {
		return new File(file, "labkit-project.yaml").exists();
	}

	public static void main(String... args) {
		show(null);
	}
}
