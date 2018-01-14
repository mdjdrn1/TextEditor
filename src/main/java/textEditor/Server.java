package textEditor;

import textEditor.model.DatabaseModelImpl;
import textEditor.model.ProjectManagerImpl;
import textEditor.model.interfaces.DatabaseModel;
import textEditor.model.interfaces.ProjectManager;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;

public class Server {
    public static void main(String[] args) {
        Registry registry = null;
        try {
            registry = LocateRegistry.createRegistry(4321);

            //Creating models implementation of our classes
//            EditorModel editorModel = new EditorModelImpl();
            DatabaseModel databaseModel = new DatabaseModelImpl();
            ProjectManager projectManager = new ProjectManagerImpl(registry);
//            ActiveUserHandler activeUsersHandler = new ActiveUsersHandlerImpl();

            //Exporting models interface to client
//            EditorModel editorModelExport = (EditorModel) UnicastRemoteObject.exportObject(editorModel, 0);
            DatabaseModel databaseModelExport = (DatabaseModel) UnicastRemoteObject.exportObject(databaseModel, 0);
//            ActiveUserHandler activeUserHandlerExport = (ActiveUserHandler) UnicastRemoteObject.exportObject(activeUsersHandler, 0);
            ProjectManager projectManagerExport = (ProjectManager) UnicastRemoteObject.exportObject(projectManager, 0);

            //Binding names and models interfaces
//            registry.rebind("EditorModel", editorModelExport);
            registry.rebind("DatabaseModel", databaseModelExport);
//            registry.rebind("ActiveUserHandler", activeUserHandlerExport);
            registry.rebind("ProjectManager", projectManagerExport);

        } catch (RemoteException | SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
