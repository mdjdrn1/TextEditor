package textEditor.controller;

import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import org.controlsfx.control.CheckComboBox;
import textEditor.controller.inject.ClientInjectionTarget;
import textEditor.controller.inject.ProjectInjectionTarget;
import textEditor.controller.inject.UserInjectionTarget;
import textEditor.controller.inject.WindowSwitcherInjectionTarget;
import textEditor.model.ProjectImpl;
import textEditor.model.interfaces.DatabaseModel;
import textEditor.model.interfaces.Project;
import textEditor.model.interfaces.User;
import textEditor.utils.RMIClient;
import textEditor.view.WindowSwitcher;

import java.io.IOException;
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class EditProjectController implements Initializable, UserInjectionTarget, ClientInjectionTarget, WindowSwitcherInjectionTarget, ProjectInjectionTarget {
    public TextField projectNameField;
    public TextArea projectDescriptionField;
    public CheckComboBox contributorsField;
    public Label information;
    private RMIClient rmiClient;
    private Project project;
    private User user;
    private WindowSwitcher windowSwitcher;

    private DatabaseModel databaseModel;


    public void cancelClicked() {
        try {
            windowSwitcher.loadWindow(WindowSwitcher.Window.PICK_PROJECT);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void applyClicked() {
        try {
            List contributors = Arrays.asList(contributorsField.getCheckModel().getCheckedItems().toArray());
            if (contributors.isEmpty()) {
                setInformation("Project can't exist without users!");
                return;
            }
            if (projectNameField.getText().isEmpty()) {
                setInformation("You need to fill project name");
                return;
            }
            Project editedProject = new ProjectImpl(project.getId(), projectNameField.getText(), projectDescriptionField.getText(), contributors);
            if (editedProject == project) {
                windowSwitcher.loadWindow(WindowSwitcher.Window.PICK_PROJECT);
                return;
            }
            databaseModel.editProject(editedProject);
            windowSwitcher.loadWindow(WindowSwitcher.Window.PICK_PROJECT);

        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    private void setInformation(String text) {
        information.setVisible(true);
        information.setTextFill(Color.RED);
        information.setText(text);
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            databaseModel = (DatabaseModel) rmiClient.getModel("DatabaseModel");
            projectNameField.setText(project.getTitle());
            projectDescriptionField.setText(project.getDescription());

            //Add contributors and friends
            contributorsField.getItems().addAll(project.getContributors());
            databaseModel.getFriends(user).forEach(user -> {
                if (!contributorsField.getItems().contains(user)) {
                    contributorsField.getItems().add(user);
                }
            });

            //Check all contributors
            project.getContributors().forEach(user -> {
                if (contributorsField.getItems().contains(user)) {
                    contributorsField.getCheckModel().check(user);
                }
            });
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void injectClient(RMIClient client) {
        this.rmiClient = client;
    }

    @Override
    public void injectProject(Project project) {
        this.project = project;
    }

    @Override
    public void injectUser(User user) {
        this.user = user;
    }

    @Override
    public void injectWindowSwitcher(WindowSwitcher switcher) {
        this.windowSwitcher = switcher;
    }
}
