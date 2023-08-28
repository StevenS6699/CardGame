package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Unit;

import java.util.HashMap;
import java.util.Map;

/**
 * Indicates that the user has clicked an object on the game canvas, in this case
 * the end-turn button.
 * <p>
 * {
 * messageType = “endTurnClicked”
 * }
 *
 * @author Dr. Richard McCreadie
 */
public class EndTurnClicked implements EventProcessor {

    @Override
    public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
        // get cards of hand from initialise class
        // use drawcard method
        // if human player turn, change to AI player turn
        if (gameState.getCurrentPlayer() == gameState.getPlayerContainers()[0]) {
            BasicCommands.addPlayer1Notification(out, "player turn over", 2);
//            clear mana
            gameState.getCurrentPlayer().setMana(0);
            gameState.getCurrentPlayer().drawCard(out);
            gameState.setCurrentPlayer(gameState.getPlayerContainers()[1]);
            gameState.setCurrentState(null);
//            if human player click this button during AI's turn
        } else BasicCommands.addPlayer1Notification(out, "not your turn", 2);
    }
}
