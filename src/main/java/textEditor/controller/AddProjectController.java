package textEditor.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import textEditor.RMIClient;
import textEditor.model.DatabaseModel;
import textEditor.view.WindowSwitcher;

import java.io.IOException;
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class AddProjectController implements Initializable, ClientInjectionTarget, WindowSwitcherInjectionTarget, UserInjectionTarget {
    @FXML
    public TextField projectNameField, contributorsField;
    @FXML
    public TextArea projectDescriptionField;


    private RMIClient rmiClient;
    private UserImpl user;
    private WindowSwitcher windowSwitcher;

    private DatabaseModel databaseModel;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            databaseModel = (DatabaseModel) rmiClient.getModel("DatabaseModel");
            contributorsField.setText(user.getUsername());
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void injectClient(RMIClient client) {
        this.rmiClient = client;
    }

    @Override
    public void injectUser(UserImpl user) {
        this.user = user;
    }

    @Override
    public void injectWindowSwitcher(WindowSwitcher switcher) {
        this.windowSwitcher = switcher;
    }

    public void addButtonClicked(ActionEvent actionEvent) {
        try {
            List<String> arrayList = Arrays.asList(contributorsField.getText().split(","));
            Project project = new ProjectImpl(0, projectNameField.getText(), projectDescriptionField.getText(), Arrays.asList(contributorsField.getText().split(",")));
            databaseModel.addProject(project);
            windowSwitcher.loadWindow(WindowSwitcher.Window.PICK_PROJECT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cancelButtonClicked(ActionEvent actionEvent) {
        try {
            windowSwitcher.loadWindow(WindowSwitcher.Window.PICK_PROJECT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}