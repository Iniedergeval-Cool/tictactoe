package Game.Controllers;

import Framework.AI.BotInterface;
import Framework.Config;
import Framework.Dialogs.DialogInterface;
import Framework.Dialogs.ErrorDialog;
import Framework.GUI.Board;
import Framework.Game.GameLogicInterface;
import Framework.Networking.Request.MoveRequest;
import Framework.Networking.Request.Request;
import Game.Models.AI;
import Game.Models.TTTGame;
import Game.StartGame;
import Game.Views.CustomLabel;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by femkeh on 06/04/17.
 */
public class BoardController extends Board {
    private static final int BOARDSIZE = 3;
    public TTTGame ttt;
    private AI tttAI;
    private Label[] listOfLabels;
    private boolean isOurTurn = false;

    private static final String gridCellStyle = "-fx-border-color: black; -fx-border-width:1;";
    private static final String cellTakenStyle = "-fx-border-color: red; -fx-border-width:1;";
    private static final String preGameGridStyle = "-fx-border-color: yellow;-fx-border-width:3;-fx-padding: 10 10 10 10;-fx-border-insets: 10 10 10 10;";
    private static final String ourTurnGridStyle = "-fx-border-color: green;-fx-border-width:3;-fx-padding: 10 10 10 10;-fx-border-insets: 10 10 10 10;";
    private static final String theirTurnGridStyle = "-fx-border-color: red;-fx-border-width:3;-fx-padding: 10 10 10 10;-fx-border-insets: 10 10 10 10;";
    private static double cellWidth;
    private static double cellHeight;

    public BotInterface getAI() {
        return tttAI;
    }

    public GameLogicInterface getGameLogic() {
        return ttt;
    }

    public void resetGameLogic() {
        ttt = new TTTGame();
    }

    public void initialize() {
        ttt = new TTTGame();
        try {
            tttAI = new AI(ttt, Config.get("game", "useCharacterForOpponent").charAt(0));
        } catch (IOException e) {
            DialogInterface errDialog = new ErrorDialog("Config error", "Could not load property: useCharacterForPlayer." +
                    "\nPlease check your game.properties file.");
            errDialog.display();
        }

        cellWidth = (gridPane.getPrefWidth() / BOARDSIZE) - 2;
        cellHeight = (gridPane.getPrefWidth() / BOARDSIZE) - 2;
        drawGrid(BOARDSIZE);
        loadGrid();
    }

    private void loadGrid() {
        int i;
        int j;
        for (i = 0; i < BOARDSIZE; i++) {
            for (j = 0; j < BOARDSIZE; j++) {
                Image image = new Image(BoardController.class.getClassLoader().getResourceAsStream("Empty.png"));
                ImageView imageView = new ImageView();
                imageView.setFitHeight(cellHeight - 5);
                imageView.setFitWidth(cellWidth - 5);
                imageView.setImage(image);
                CustomLabel label = new CustomLabel();
                label.setPrefSize(cellWidth, cellHeight);
                label.setX(i);
                label.setY(j);
                label.setOnMouseClicked(this::clickToDoMove);
                gridPane.setHalignment(label, HPos.CENTER);
                label.setStyle(gridCellStyle);
                label.setGraphic(imageView);

                final int finali = i;
                final int finalj = j;
                Platform.runLater(() -> gridPane.add(label, finalj, finali));
            }
        }
        gridPane.setStyle(preGameGridStyle);
    }

    // Move received from within game
    public void clickToDoMove(MouseEvent mouseEvent) {
        if (!isOurTurn) {
            DialogInterface errDialog = new ErrorDialog("Not your turn!", "Please wait until the borders are green");
            errDialog.display();
            return;
        }

        CustomLabel label = (CustomLabel) mouseEvent.getSource();
        int x = label.getX();
        int y = label.getY();
        String turn = " ";
        try {
            turn = Config.get("game", "useCharacterForPlayer");
            turn = turn != null ? turn : " ";
        } catch (IOException e) {
            DialogInterface errorDialog = new ErrorDialog("Config error", "Could not load property: useCharacterForPlayer." +
                    "\nPlease check your game.properties file.");
            Platform.runLater(errorDialog::display);
        }
        CustomLabel newLabel = makeLabel(x, y, turn);
        gridPane.getChildren().remove(label);
        gridPane.add(newLabel, y, x);

        // update models
        char turnChar = turn.charAt(0);
        ttt.doTurn(y, x, turnChar);

        // send MoveRequest to game server
        int pos = x * BOARDSIZE + y;
        Request moveRequest = new MoveRequest(StartGame.getConn(), pos);
        try {
            moveRequest.execute();
        } catch (IOException | InterruptedException e) {
            DialogInterface errDialog = new ErrorDialog("IO|InterruptedException: could not send moveRequest to the server", "Please contact the project developers.");
            errDialog.display();
        }

        // set isOurTurn false
        isOurTurn = false;
        Platform.runLater(() -> gridPane.setStyle(theirTurnGridStyle));
    }

    // Move received from server
    public void setMove(int x, int y, String player) throws IOException {
        CustomLabel newLabel = makeLabel(x, y, player);
        ObservableList<Node> childrenList = gridPane.getChildren();
        for (Node node : childrenList) {
            if (gridPane.getRowIndex(node) == y && gridPane.getColumnIndex(node) == x) {
                Platform.runLater(() -> gridPane.getChildren().remove(node));
                break;
            }
        }
        // gridPane updaten with move
        Platform.runLater(() -> gridPane.add(newLabel, x, y));

        // model updaten
        char turn = player.charAt(0);
        System.out.println("x"+turn);

        if(player.equals(StartGame.getBaseController().getLoggedInPlayer())) {
            ttt.doTurn(y, x, Config.get("game", "useCharacterForPlayer").charAt(0));
        }
        else {
            ttt.doTurn(y, x, turn);
        }
    }

    private CustomLabel makeLabel(int x, int y, String turn) {
        CustomLabel newLabel = new CustomLabel();
        ImageView imageView = new ImageView();
        imageView.setFitHeight(50.0);
        imageView.setFitWidth(50.0);
        newLabel.setStyle(cellTakenStyle);
        if (turn.equals("X")) {
            Image image = new Image(BoardController.class.getClassLoader().getResourceAsStream("X.png"));
            imageView.setImage(image);
            newLabel.setGraphic(imageView);
            newLabel.setX(x);
            newLabel.setY(y);
            gridPane.setHalignment(newLabel, HPos.CENTER);
        } else {
            Image image = new Image(BoardController.class.getClassLoader().getResourceAsStream("O.png"));
            imageView.setImage(image);
            newLabel.setGraphic(imageView);
            newLabel.setX(x);
            newLabel.setY(y);
            gridPane.setHalignment(newLabel, HPos.CENTER);
        }
        return newLabel;
    }

    // List of coordinates
    public Map<Integer, int[]> getListOfCoordinates() {
        Map<Integer, int[]> listOfCoordinates = new HashMap<>();
        int key = 0;
        for (int y = 0; y < BOARDSIZE; y++) {
            for (int x = 0; x < BOARDSIZE; x++) {
                listOfCoordinates.put(key, new int[]{x, y});
                key++;
            }
        }
        return listOfCoordinates;
    }

    public void loadPreGameBoardState() {
        System.out.println("PreGameBoardState Loaded!");
        Platform.runLater(() -> gridPane.getChildren().clear());
        Platform.runLater(this::loadGrid);
    }
    public void setOurTurn() {
        System.out.println("ourTurn called: setStyle!");
        Platform.runLater(() -> gridPane.setStyle(ourTurnGridStyle));
        isOurTurn = true;
    }

    public void doAIMove() {
        int[] moveCoords = getAI().doTurn(getGameLogic().getBoard());
        try {
            // setMove updates gameLogic and GUI
            setMove(moveCoords[0], moveCoords[1], Config.get("game", "useCharacterForPlayer"));

            // send moveRequest to game server
            int pos = moveCoords[1] * BOARDSIZE + moveCoords[0];
            System.out.println("AI MOVE GEN: " + moveCoords[0] + "," + moveCoords[1] + " == " + pos);
            Request moveRequest = new MoveRequest(StartGame.getConn(), pos);
            moveRequest.execute();

            // set isOurTurn false
            isOurTurn = false;
            Platform.runLater(() -> gridPane.setStyle(theirTurnGridStyle));
        } catch (IOException e) {
            DialogInterface errDialog = new ErrorDialog("Config error", "Could not load property: useCharacterForPlayer." +
                    "\nPlease check your game.properties file.");
            Platform.runLater(errDialog::display);
        } catch (InterruptedException e) {
            DialogInterface errDialog = new ErrorDialog("InterruptedException", "Could not send request: moveRequest.");
            Platform.runLater(errDialog::display);
        }
    }
}
