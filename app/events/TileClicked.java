package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Card;
import structures.basic.Tile;
import structures.basic.Unit;

import java.util.HashMap;
import java.util.Map;

/**
 * Indicates that the user has clicked an object on the game canvas, in this case a tile.
 * The event returns the x (horizontal) and y (vertical) indices of the tile that was
 * clicked. Tile indices start at 1.
 * <p>
 * {
 * messageType = “tileClicked”
 * tilex = <x index of the tile>
 * tiley = <y index of the tile>
 * }
 *
 * @author Dr. Richard McCreadie
 */
public class TileClicked implements EventProcessor {

    @Override
    public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
        int tilex = message.get("tilex").asInt();
        int tiley = message.get("tiley").asInt();

        Map<String, Object> parameters;


        if (gameState.getCurrentState().equals(GameState.CurrentState.SELECTED_CARD)) {
            parameters = new HashMap<>();

            Card cardSelected = gameState.getCardSelected();
//            check if the card is a creature 1 or spell -1
            if (cardSelected.isCOrS() == 1) {
                Unit unit = gameState.cardToUnit(cardSelected);

                unit.setOwner(gameState.getCurrentPlayer());
                gameState.add(unit);
                parameters.put("type", "summon");
                parameters.put("tilex", tilex);
                parameters.put("tiley", tiley);
                parameters.put("unit", unit);
                gameState.broadcastEvent(Tile.class, parameters);

            } else {
                BasicCommands.addPlayer1Notification(out, "Player spell " + cardSelected.getCardname(), 2);
                parameters.put("type", "spell");
                parameters.put("tilex", tilex);
                parameters.put("tiley", tiley);
                gameState.broadcastEvent(Tile.class, parameters);
            }
        } else if (gameState.getCurrentState().equals(GameState.CurrentState.READY)) {
            parameters = new HashMap<>();
            parameters.put("type", "firstClickTile");
            parameters.put("tilex", tilex);
            parameters.put("tiley", tiley);
            gameState.broadcastEvent(Tile.class, parameters);
        } else if (gameState.getCurrentState().equals(GameState.CurrentState.SELECTED_UNIT)) {
            parameters = new HashMap<>();
            parameters.put("type", "operateUnit");
            parameters.put("tilex", tilex);
            parameters.put("tiley", tiley);
            parameters.put("originTileSelected", gameState.getTileSelected());
            gameState.broadcastEvent(Tile.class, parameters);
        }
    }
}
