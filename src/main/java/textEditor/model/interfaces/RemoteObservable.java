package textEditor.model.interfaces;

import java.rmi.RemoteException;

public interface RemoteObservable {
    // TODO: consider adding ONLY_PARAGRAPH
    public enum UpdateTarget {
        ONLY_TEXT, ONLY_STYLE
    }

    void addObserver(RemoteObserver observer) throws RemoteException;

    void deleteObserver(RemoteObserver observer) throws RemoteException;

    // TODO: add overloaded deleteObservers with List<RemoteObserver> argument
    void deleteObservers() throws RemoteException;

    void notifyObservers(UpdateTarget target) throws RemoteException;
}