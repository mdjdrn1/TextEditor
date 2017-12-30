package textEditor.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.StyledTextArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.TwoDimensional;
import textEditor.RMIClient;
import textEditor.model.*;
import textEditor.view.WindowSwitcher;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class EditorController implements Initializable, ClientInjectionTarget, WindowSwitcherInjectionTarget {
    @FXML
    private Menu fileMenu, editMenu, helpMenu;
    @FXML
    private ChoiceBox fontType, fontSize;
    @FXML
    private HBox searchBox;
    @FXML
    private Button searchButton, nextSearchButton, previousSearchButton, closeSearchBox;
    @FXML
    private ToggleButton boldButton, italicButton, underscoreButton,
            alignmentLeftButton, alignmentCenterButton, alignmentRightButton, alignmentAdjustButton;
    @FXML
    private InlineCssTextArea searchArea;
    @FXML
    private StyleClassedTextArea mainTextArea;

    private Clipboard clipboard;

    private EditorModel editorModel;
    private RMIClient rmiClient;
    private WindowSwitcher switcher;
    private RemoteObserver observer;

    // flag for avoiding cycling dependencies during updates after observer event
    private AtomicBoolean isTextUpdatedByObserverEvent = new AtomicBoolean(false);

    public EditorController() {
    }

    @Override
    public void injectClient(RMIClient client) {
        this.rmiClient = client;
    }

    @Override
    public void injectWindowSwitcher(WindowSwitcher switcher) {
        this.switcher = switcher;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        clipboard = Clipboard.getSystemClipboard();
        editorModel = (EditorModel) rmiClient.getModel("EditorModel");

        initTextArea();

        loadCssStyleSheet();

        initTextSelection();
    }

    private void initTextArea() {
        initObserver();

        mainTextArea.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if (!isTextUpdatedByObserverEvent.get()) {
                    editorModel.setTextString(newValue, observer);
                    editorModel.setTextStyle(new StyleSpansWrapper(0, mainTextArea.getStyleSpans(0, mainTextArea.getText().length())));
                }
            } catch (RemoteException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void initObserver() {
        try {
            System.out.println("init observer");
            observer = new RemoteObserverImpl(new EditorControllerObserver());

            editorModel.addObserver(observer);

            observer.update(editorModel);
        } catch (RemoteException e) {
            System.out.println(e.getMessage());
        }
    }

    private void loadCssStyleSheet() {
        mainTextArea.getStylesheets().add(EditorController.class.getResource("styles.css").toExternalForm());
    }

    private void initTextSelection() {
        // TODO: create another scalable solution
        //SOLUTION: Meybe we should create enum ? to make this easier ?
        // HIGHLY dependent on boldButtonClicked() and italicButtonClicked() methods
        mainTextArea.selectedTextProperty().addListener((observable, oldValue, newValue) -> {
            // make both buttons unselected, when user didn't select any text
            if (newValue.equals("")) {
                boldButton.setSelected(false);
                italicButton.setSelected(false);
                underscoreButton.setSelected(false);
                return;
            }

            // check if whole selected text is bold or whole selected text is italic
            boolean isWholeBold = true;
            boolean isWholeItalic = true;
            boolean isWholeUnderscore = true;
            IndexRange range = mainTextArea.getSelection();

            for (int i = range.getStart(); i < range.getEnd(); ++i) {
//                System.out.println("i=" + i + ", " + mainTextArea.getStyleOfChar(i));
                Collection<String> list = new ArrayList<>(mainTextArea.getStyleOfChar(i));
                if (isWholeBold && !list.contains("boldWeight")) {
                    isWholeBold = false;
                }

                if (isWholeItalic && !list.contains("italicStyle")) {
                    isWholeItalic = false;
                }

                if (isWholeUnderscore && !list.contains("underscoreDecoration")) {
                    isWholeUnderscore = false;
                }
            }

            boldButton.setSelected(isWholeBold);
            italicButton.setSelected(isWholeItalic);
            underscoreButton.setSelected(isWholeUnderscore);

            //check if paragraph styles
            paragraphStyleButtons();

        });
    }

    private void paragraphStyleButtons() {
        int startParagraphInSelection = mainTextArea.offsetToPosition(mainTextArea.getSelection().getStart(), TwoDimensional.Bias.Forward).getMajor();
        int lastParagraphInSelection = mainTextArea.offsetToPosition(mainTextArea.getSelection().getEnd(), TwoDimensional.Bias.Backward).getMajor();
        for (int paragraph = startParagraphInSelection; paragraph <= lastParagraphInSelection; paragraph++) {
            Collection style = mainTextArea.getParagraph(paragraph).getParagraphStyle();
            if (style.equals(Collections.singleton("alignmentRight"))) {
                alignmentRightButton.setSelected(true);
            } else if (style.equals(Collections.singleton("alignmentCenter"))) {
                alignmentCenterButton.setSelected(true);
            } else if (style.equals(Collections.singleton("alignmentJustify"))) {
                alignmentAdjustButton.setSelected(true);
            } else {
                alignmentLeftButton.setSelected(true);
            }
        }
    }

    private StyledTextArea getFocusedText() {
        if (mainTextArea.isFocused()) {
            return mainTextArea;

        } else if (searchArea.isFocused()) {
            return searchArea;
        }
        return null;
    }

    @FXML
    private void editUndoClicked() {
        mainTextArea.undo();
    }

    @FXML
    private void editRedoClicked() {
        mainTextArea.redo();
    }

    @FXML
    private void editCopyClicked() {
        ClipboardContent clipboardContent = new ClipboardContent();
        // getting text from focused area
        StyledTextArea textInput = getFocusedText();
        if (textInput != null) {
            clipboardContent.putString(textInput.getSelectedText());
            clipboard.setContent(clipboardContent);
        }
    }

    @FXML
    private void editCutClicked() {
        ClipboardContent clipboardContent = new ClipboardContent();
        StyledTextArea textInput = getFocusedText();
        if (textInput != null) {
            clipboardContent.putString(textInput.getSelectedText());
            // clearing coresponding area from cuted text
            IndexRange indexRange = textInput.getSelection();
            textInput.replaceText(indexRange, "");

            clipboard.setContent(clipboardContent);
        }
    }

    @FXML
    private void editPasteClicked() {
        //TODO: refactor this shit
        StyledTextArea textInput = getFocusedText();
        if (textInput != null) {
            textInput.appendText(textInput.getText(0, textInput.getCaretPosition()) + clipboard.getString() + textInput.getText(textInput.getCaretPosition(), textInput.getLength()));
        }
    }

    @FXML
    private void helpHelpClicked() {
        //TODO:  implement javafx stage appear with help content
    }

    @FXML
    private void helpAboutUsClicked() {
        //TODO:  implement javafx stage appear with aboutUs content
    }

    @FXML
    private void editSearchClicked() {
        searchBox.setVisible(true);
        //TODO:  implement search
    }

    @FXML
    private void fileNewClicked() {
        System.out.println("New file will be created");
        //TODO: how this should look like ?
    }

    @FXML
    private void fileOpenClicked() {
        System.out.println("File will be open");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose resource");
        File file = fileChooser.showOpenDialog(switcher.getStage());
        if (file != null) {
            //TODO: handle this
            //openFile(file);
        }
    }

    @FXML
    private void fileSaveClicked() {
        System.out.println("file will be save");
    }

    @FXML
    private void fileCloseClicked() {
        Platform.exit();
    }

    @FXML
    private void closeSearchBoxClicked() {
        searchBox.setVisible(false);
    }

    @FXML
    private void boldButtonClicked() {
        transformTextStyle(mainTextArea, boldButton, "boldWeight", "normalWeight");
    }

    @FXML
    private void italicButtonClicked() {
        transformTextStyle(mainTextArea, italicButton, "italicStyle", "normalStyle");
    }

    private void transformTextStyle(StyleClassedTextArea area, ToggleButton triggeringButton, String transformedStyle, String normalStyle) {
        IndexRange range = area.getSelection();

        boolean replaceNormalStyle = triggeringButton.isSelected();

        String newStyle = replaceNormalStyle ? transformedStyle : normalStyle;
        String oldStyle = replaceNormalStyle ? normalStyle : transformedStyle;

        StyleSpans<Collection<String>> spans = area.getStyleSpans(range);

        StyleSpans<Collection<String>> newSpans = spans.mapStyles(currentStyle -> {
            List<String> style = new ArrayList<>(Arrays.asList(newStyle));
            style.addAll(currentStyle);
            style.remove(oldStyle);
            return style;
        });
        System.out.println("Before " + area.getStyleSpans(0, area.getText().length()));
        area.setStyleSpans(range.getStart(), newSpans);
        System.out.println("After " + area.getStyleSpans(0, area.getText().length()));
        try {
            System.out.println("setting setTextStyle");
            editorModel.setTextStyle(new StyleSpansWrapper(0, area.getStyleSpans(0, area.getText().length())), observer);
            System.out.println("sett setTextStyle");
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        area.requestFocus();
    }

    @FXML
    private void underscoreButtonClicked() {
        transformTextStyle(mainTextArea, underscoreButton, "underscoreDecoration", "normalDecoration");
    }

    @FXML
    private void fontSizePlusButtonClicked() {

    }

    @FXML
    private void fontSizeMinusButtonClicked() {

    }

    @FXML
    private void alignmentLeftButtonClicked() {
        int startParagraphInSelection = mainTextArea.offsetToPosition(mainTextArea.getSelection().getStart(), TwoDimensional.Bias.Forward).getMajor();
        int lastParagraphInSelection = mainTextArea.offsetToPosition(mainTextArea.getSelection().getEnd(), TwoDimensional.Bias.Backward).getMajor();
        changeParagraphs(startParagraphInSelection, lastParagraphInSelection, alignmentLeftButton, "alignmentLeft");
    }

    @FXML
    private void alignmentCenterButtonClicked() {
        int startParagraphInSelection = mainTextArea.offsetToPosition(mainTextArea.getSelection().getStart(), TwoDimensional.Bias.Forward).getMajor();
        int lastParagraphInSelection = mainTextArea.offsetToPosition(mainTextArea.getSelection().getEnd(), TwoDimensional.Bias.Backward).getMajor();
        changeParagraphs(startParagraphInSelection, lastParagraphInSelection, alignmentCenterButton, "alignmentCenter");
    }

    @FXML
    private void alignmentRightButtonClicked() {
        int startParagraphInSelection = mainTextArea.offsetToPosition(mainTextArea.getSelection().getStart(), TwoDimensional.Bias.Forward).getMajor();
        int lastParagraphInSelection = mainTextArea.offsetToPosition(mainTextArea.getSelection().getEnd(), TwoDimensional.Bias.Backward).getMajor();
        changeParagraphs(startParagraphInSelection, lastParagraphInSelection, alignmentRightButton, "alignmentRight");
    }

    @FXML
    private void alignmentAdjustButtonClicked() {
        int startParagraphInSelection = mainTextArea.offsetToPosition(mainTextArea.getSelection().getStart(), TwoDimensional.Bias.Forward).getMajor();
        int lastParagraphInSelection = mainTextArea.offsetToPosition(mainTextArea.getSelection().getEnd(), TwoDimensional.Bias.Backward).getMajor();
        changeParagraphs(startParagraphInSelection, lastParagraphInSelection, alignmentAdjustButton, "alignmentJustify");

    }

    private void changeParagraphs(int firstParagraph, int lastParagraph, ToggleButton toggleButton, String style) {
        if (toggleButton.isSelected()) {
            for (int paragraph = firstParagraph; paragraph < lastParagraph + 1; paragraph++)
                mainTextArea.setParagraphStyle(paragraph, Collections.singleton(style));
        } else {
            for (int paragraph = firstParagraph; paragraph < lastParagraph + 1; paragraph++)
                mainTextArea.setParagraphStyle(paragraph, Collections.singleton("alignmentLeft"));
        }

    }

    @FXML
    private void searchButtonClicked() {

    }

    @FXML
    private void nextSearchButtonClicked() {

    }

    @FXML
    private void previousSearchButtonClicked() {

    }

    public class EditorControllerObserver implements Serializable, RemoteObserver {
        private class UpdateTextWrapper implements Runnable {
            private RemoteObservable observable;

            public UpdateTextWrapper(RemoteObservable observable) {
                this.observable = observable;
            }

            @Override
            public void run() {
                isTextUpdatedByObserverEvent.set(true);
                System.out.println("RUNNING UPDATE ONLY TEXT");
                int oldCaretPosition = mainTextArea.getCaretPosition();
                String oldText = mainTextArea.getText();
                try {
                    String newText = ((EditorModel) observable).getTextString();
                    mainTextArea.replaceText(newText);
                    int newCaretPosition = calculateNewCaretPosition(oldCaretPosition, oldText, newText);
                    mainTextArea.moveTo(newCaretPosition);
                } catch (RemoteException e) {
                    e.printStackTrace();
                } finally {
                    isTextUpdatedByObserverEvent.set(false);
                }
            }
        }

        private class UpdateStyleWrapper implements Runnable {
            private RemoteObservable observable;

            public UpdateStyleWrapper(RemoteObservable observable) {
                this.observable = observable;
            }

            @Override
            public void run() {
                System.out.println("RUNNING UPDATE ONLY STYLE");
                try {
                    StyleSpansWrapper newStyle = ((EditorModel) observable).getTextStyle();
                    if (newStyle != null && newStyle.getStyleSpans() != null) {
                        mainTextArea.setStyleSpans(newStyle.getStylesStart(), newStyle.getStyleSpans());
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void update(RemoteObservable observable) throws RemoteException {
            Platform.runLater(new UpdateTextWrapper(observable));
            Platform.runLater(new UpdateStyleWrapper(observable));
        }

        @Override
        public synchronized void update(RemoteObservable observable, RemoteObservable.UpdateTarget target) throws RemoteException {
            if (target == RemoteObservable.UpdateTarget.ONLY_TEXT) {
                Platform.runLater(new UpdateTextWrapper(observable));
            }

            if (target == RemoteObservable.UpdateTarget.ONLY_STYLE) {
                Platform.runLater(new UpdateStyleWrapper(observable));
            }
        }

        // issues when using redo/undo actions as clients can undo another client operations
        private int calculateNewCaretPosition(int oldCaretPosition, String oldText, String newText) {
            if (newText.length() == 0) {
                return 0;
            }

            int indexOfTextBeforeCaret = newText.indexOf(oldText.substring(0, oldCaretPosition));
            if (indexOfTextBeforeCaret != -1) {
                return oldCaretPosition;
            }

            int indexOfTextAfterCaret = newText.indexOf(oldText.substring(oldCaretPosition, oldText.length()));
            if (indexOfTextAfterCaret != -1) {
                return indexOfTextAfterCaret;
            }

            return findFirstDifferenceIndex(oldText, newText);
        }

        private int findFirstDifferenceIndex(String oldText, String newText) {
            int longestLength = oldText.length() > newText.length() ? oldText.length() : newText.length();

            for (int i = 0; i < longestLength; ++i) {
                if (oldText.charAt(i) != newText.charAt(i)) {
                    return i;
                }
            }

            return 0;
        }
    }
}
