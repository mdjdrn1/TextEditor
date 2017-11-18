package textEditor.view.ui.editor;

import javafx.scene.control.Button;
import textEditor.model.StyleAction;

public class ToolBarView extends javafx.scene.control.ToolBar {
    private Button bold;
    private Button italic;
    private Button underscore;

    public ToolBarView() {
        super();
        init();
        getItems().addAll(bold, italic, underscore);
    }

    private void init() {
        bold = new StyleButton("Bold", StyleAction.BOLD);
        italic = new StyleButton("Italic", StyleAction.ITALIC);
        underscore = new StyleButton("Underscore", StyleAction.UNDERSCORE);
    }

    public Button getBoldButton() {
        return bold;
    }

    public Button getItalicButton() {
        return italic;
    }

    public Button getUnderscoreButton() {
        return underscore;
    }
}
