package TicTacToe.Controllers;

import Framework.Dialogs.DialogInterface;
import Framework.Dialogs.ErrorDialog;
import Framework.Dialogs.MessageDialog;
import Framework.Networking.NetworkEvents;
import Framework.Networking.Response.*;
import TicTacToe.Start;
import javafx.application.Platform;

/**
 * Created by peterzen on 2017-04-06.
 * Part of the tictactoe project.
 */
public class NetworkEventsController implements NetworkEvents {

    @Override
    public void challengeCancelled(ChallengeCancelledResponse challengeCancelledResponse) {

    }

    @Override
    public void challengeReceived(ChallengeReceivedResponse challengeReceivedResponse) {

    }

    @Override
    public void gameEnded(GameEndResponse gameEndResponse) {
        // show GameEndedDialog
        String result = gameEndResponse.getResult();
        DialogInterface gameEndedDialog = new MessageDialog(
                "Game has ended!",
                "Game resulted in a " + result + "!",
                "Comment: " + gameEndResponse.getComment() + "\n"
                + "Player one score: " + gameEndResponse.getPlayerOneScore() + "\n"
                + "Player two score: " + gameEndResponse.getPlayerTwoScore()
        );
        gameEndedDialog.display();

        // reset / update game-logic models (via BoardController?)
        // reset / update BoardView look (via BoardController)
        Start.getBaseController().getBoardController().endGameHandler();

        // reset GUI (enable ControlsController buttons)
        Start.getBaseController().getControlsController().enableControls();
    }

    @Override
    public void gameListReceived(GameListResponse gameListResponse) {

    }

    @Override
    public void matchReceived(MatchReceivedResponse matchReceivedResponse) {

    }

    @Override
    public void moveReceived(MoveResponse moveResponse) {

    }

    @Override
    public void ourTurn(OurTurnResponse ourTurnResponse) {

    }

    @Override
    public void playerListReceived(PlayerListResponse playerListResponse) {

    }

    @Override
    public void errorReceived(ErrorResponse errorResponse) {
        DialogInterface errorDialog = new ErrorDialog("Network error occurred", "Possible cause: " + errorResponse.getRequest().toString());
        Platform.runLater(errorDialog::display);
    }
}
