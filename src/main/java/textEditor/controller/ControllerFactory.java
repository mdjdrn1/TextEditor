package textEditor.controller;

import javafx.util.Callback;
import textEditor.controller.inject.ClientInjectionTarget;
import textEditor.controller.inject.ProjectInjectionTarget;
import textEditor.controller.inject.UserInjectionTarget;
import textEditor.controller.inject.WindowSwitcherInjectionTarget;
import textEditor.model.interfaces.Project;
import textEditor.model.interfaces.User;
import textEditor.utils.RMIClient;
import textEditor.view.WindowSwitcher;

public class ControllerFactory implements Callback<Class<?>, Object> {
    private RMIClient rmiClient;
    private WindowSwitcher switcher;
    private User user;
    private Project project;

    public ControllerFactory(RMIClient rmiClient, WindowSwitcher switcher, User user, Project project) {
        this.rmiClient = rmiClient;
        this.switcher = switcher;
        this.user = user;
        this.project = project;
    }

    @Override
    public Object call(Class<?> p) {
        Object controller;
        try {
            controller = p.newInstance();

            if (controller instanceof ClientInjectionTarget) {
                ((ClientInjectionTarget) controller).injectClient(rmiClient);
            }

            if (controller instanceof WindowSwitcherInjectionTarget) {
                ((WindowSwitcherInjectionTarget) controller).injectWindowSwitcher(switcher);
            }

            if (controller instanceof UserInjectionTarget) {
                ((UserInjectionTarget) controller).injectUser(user);
            }

            if (controller instanceof ProjectInjectionTarget) {
                ((ProjectInjectionTarget) controller).injectProject(project);
            }

            return controller;
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }
}
