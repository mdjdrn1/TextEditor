package textEditor.controller;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import textEditor.controller.inject.ClientInjectionTarget;
import textEditor.controller.inject.ProjectInjectionTarget;
import textEditor.controller.inject.UserInjectionTarget;
import textEditor.controller.inject.WindowSwitcherInjectionTarget;
import textEditor.model.interfaces.DatabaseModel;
import textEditor.model.interfaces.Project;
import textEditor.model.interfaces.User;
import textEditor.utils.RMIClient;
import textEditor.view.WindowSwitcher;

import java.io.IOException;
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.ResourceBundle;


public class ProjectController implements Initializable, UserInjectionTarget, ClientInjectionTarget, WindowSwitcherInjectionTarget, ProjectInjectionTarget {
    @FXML
    private Label description;
    @FXML
    private Label contributors;
    @FXML
    private ListView<Project> projectListView;

    private WindowSwitcher switcher;
    private DatabaseModel dbService;
    private RMIClient client;
    private User user;

    private List<Project> projects;
    private Project project;

    @Override
    public void injectUser(User user) {
        this.user = user;
    }

    @Override
    public void injectWindowSwitcher(WindowSwitcher switcher) {
        this.switcher = switcher;
    }

    @Override
    public void injectClient(RMIClient client) {
        this.client = client;
    }

    @Override
    public void injectProject(Project project) {
        this.project = project;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            dbService = (DatabaseModel) client.getModel("DatabaseModel");
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
        fetchUserProjectsFromDatabase();
        setupProjectsListView();
    }

    private void fetchUserProjectsFromDatabase() {
        try {
            projects = dbService.getProjects(user);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void setupProjectsListView() {
        projectListView.setItems(FXCollections.observableArrayList(this.projects));
        projectListView.getSelectionModel().getSelectedItems().addListener((ListChangeListener<? super Project>) (e) -> {
            Project project = projectListView.getSelectionModel().getSelectedItem();
            if (project != null) {
                description.setText(project.getDescription());
                contributors.setText(project.getContributors().toString());
            } else {
                description.setText("");
                contributors.setText("");
            }
        });
    }

    @FXML
    public void onClickRemove() {
        Project projectToDelete = projectListView.getSelectionModel().getSelectedItem();
        final int selectedIdx = projectListView.getSelectionModel().getSelectedIndex();
        if (selectedIdx != -1) {
            try {
                dbService.removeProject(projectToDelete);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            projectListView.getItems().remove(selectedIdx);
        }

    }

    public void onClickOpen() {
        try {
            Project selectedProject = projectListView.getSelectionModel().getSelectedItem();
            final int selectedIdx = projectListView.getSelectionModel().getSelectedIndex();
            if (selectedIdx != -1) {
                this.project.setId(selectedProject.getId());
                this.project.setTitle(selectedProject.getTitle());
                this.project.setDescription(selectedProject.getDescription());
                this.project.setContributors(selectedProject.getContributors());
                switcher.loadWindow(WindowSwitcher.Window.EDITOR);
            }
        } catch (IOException ignored) {

        }
    }

    public void onClickExport() {
    }

    public void onClickImport() {
    }

    public void onClickEdit() {
        try {
            Project selectedProject = projectListView.getSelectionModel().getSelectedItem();
            final int selectedIdx = projectListView.getSelectionModel().getSelectedIndex();
            if (selectedIdx != -1) {
                project.setId(selectedProject.getId());
                project.setTitle(selectedProject.getTitle());
                project.setDescription(selectedProject.getDescription());
                project.setContributors(selectedProject.getContributors());
                switcher.loadWindow(WindowSwitcher.Window.EDIT_PROJECT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onClickAdd() {
        try {
            switcher.loadWindow(WindowSwitcher.Window.ADD_PROJECT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onClickBack() {
        try {
            switcher.loadWindow(WindowSwitcher.Window.CHOOSE_ACTION);
        } catch (IOException ignored) {

        }
    }
}
