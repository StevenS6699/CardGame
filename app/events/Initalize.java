package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.*;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

import utils.OrderedCardLoader;

import java.util.HashMap;
import java.util.Map;

/**
 * Indicates that both the core game loop in the browser is starting, meaning
 * that it is ready to recieve commands from the back-end.
 * <p>
 * {
 * messageType = “initalize”
 * }
 *
 * @author Dr. Richard McCreadie
 */
public class Initalize implements EventProcessor {

    @Override
    public void processEvent(ActorRef out, GameState gameState, JsonNode message) {

        // start
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 5; j++) {
                Tile tile = BasicObjectBuilders.loadTile(StaticConfFiles.tileConf, i, j, gameState);
                gameState.add(tile);
                BasicCommands.drawTile(out, tile, 0);
            }
        }

        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        Player Player = new Player(20, 2, 0, gameState);
        Player AIPlayer = new Player(20, 2, 0, gameState);

        BasicCommands.setPlayer1Health(out, Player);
        BasicCommands.setPlayer2Health(out, AIPlayer);

        Player.setDeck(OrderedCardLoader.getPlayer1Cards());
        AIPlayer.setDeck(OrderedCardLoader.getPlayer2Cards());

        BasicCommands.addPlayer1Notification(out, "Player turn", 2);

        Map<String, Object> parameters = new HashMap<>();

        Unit humanAvatar = BasicObjectBuilders.loadUnit(
                StaticConfFiles.humanAvatar,
                99, Unit.class,
                gameState
        );

        humanAvatar.setOwner(Player);

        gameState.add(humanAvatar);
        gameState.setHumanAvatar(humanAvatar);
        gameState.addPlayers(Player, AIPlayer);

//		set player health and attack
        humanAvatar.setMaxHealth(Player.getHealth());
        humanAvatar.setHealth(Player.getHealth());
        humanAvatar.setAttack(Player.getAttack());
        parameters = new HashMap<>();
        parameters.put("type", "summon");
        parameters.put("tilex", 1);
        parameters.put("tiley", 2);
        parameters.put("unit", humanAvatar);
        gameState.broadcastEvent(Tile.class, parameters);

        Unit AiAvatar = BasicObjectBuilders.loadUnit(
                StaticConfFiles.aiAvatar,
                100, Unit.class,
                gameState
        );
        gameState.add(AiAvatar);
        gameState.setAIAvatar(AiAvatar);

        AiAvatar.setOwner(AIPlayer);
//		set ai health and attack
        AiAvatar.setMaxHealth(AIPlayer.getHealth());
        AiAvatar.setHealth(AIPlayer.getHealth());
        AiAvatar.setAttack(AIPlayer.getAttack());

        parameters = new HashMap<>();
        parameters.put("type", "summon");
        parameters.put("tilex", 7);
        parameters.put("tiley", 2);
        parameters.put("unit", AiAvatar);
        gameState.broadcastEvent(Tile.class, parameters);

        gameState.addPlayers(Player, AIPlayer);

        gameState.getCurrentPlayer().drawCard(out);
        gameState.getCurrentPlayer().drawCard(out);
        gameState.getCurrentPlayer().drawCard(out);

        AIPlayer.drawCard(out);
        AIPlayer.drawCard(out);
        AIPlayer.drawCard(out);

        parameters = new HashMap<>();
        parameters.put("type", "unitBeReady");
        gameState.broadcastEvent(Unit.class, parameters);

        gameState.callbacks();
    }
}


