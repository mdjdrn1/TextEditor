package textEditor.controller;

import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import textEditor.RMIClient;
import textEditor.controller.targetInjections.ClientInjectionTarget;
import textEditor.controller.targetInjections.ProjectInjectionTarget;
import textEditor.controller.targetInjections.UserInjectionTarget;
import textEditor.controller.targetInjections.WindowSwitcherInjectionTarget;
import textEditor.model.ProjectImpl;
import textEditor.model.interfaces.DatabaseModel;
import textEditor.model.interfaces.Project;
import textEditor.model.interfaces.User;
import textEditor.view.WindowSwitcher;

import java.io.IOException;
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.ResourceBundle;

public class EditProjectController implements Initializable, UserInjectionTarget, ClientInjectionTarget, WindowSwitcherInjectionTarget, ProjectInjectionTarget {
    public TextField projectNameField;
    public TextArea projectDescriptionField;
    public TextField contributorsField;
    private RMIClient rmiClient;
    private Project project;
    private User user;
    private WindowSwitcher windowSwitcher;

    private DatabaseModel databaseModel;


    public void cancelClicked(ActionEvent actionEvent) {
        try {
            windowSwitcher.loadWindow(WindowSwitcher.Window.PICK_PROJECT);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void applyClicked() {
        try {
            Project editedProject = new ProjectImpl(project.getId(), projectNameField.getText(), projectDescriptionField.getText(), Arrays.asList(contributorsField.getText().split(",")));
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            databaseModel = (DatabaseModel) rmiClient.getModel("DatabaseModel");
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }

        projectNameField.setText(project.getTitle());
        projectDescriptionField.setText(project.getDescription());
        contributorsField.setText(String.valueOf(project.getContributors()));
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
