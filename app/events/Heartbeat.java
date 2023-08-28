package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.Observer;
import structures.basic.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * In the user’s browser, the game is running in an infinite loop, where there is around a 1 second delay
 * between each loop. Its during each loop that the UI acts on the commands that have been sent to it. A
 * heartbeat event is fired at the end of each loop iteration. As with all events this is received by the Game
 * Actor, which you can use to trigger game logic.
 * <p>
 * {
 * String messageType = “heartbeat”
 * }
 *
 * @author Dr. Richard McCreadie
 */
public class Heartbeat implements EventProcessor {

    @Override
    public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
//        check if AI player turn
        Player aiPlayer = gameState.getPlayerContainers()[1];

        if (gameState.getCurrentState() != null
                || gameState.getCurrentPlayer() != aiPlayer) {
            return;
        }
//			AI player turn
        BasicCommands.addPlayer1Notification(out, "AI's turn", 2);
//			do something
//          check hand cards and mana, find suitable cards
        gameState.setCurrentState(GameState.CurrentState.READY);
        gameState.addMana(gameState.getCurrentPlayer());

        sleep(2000);
        List<List<Integer>> actionList = new ArrayList<>();
        actionList.add(Arrays.asList(1, 2, 3));
        actionList.add(Arrays.asList(1, 3, 2));
        actionList.add(Arrays.asList(2, 3));
        actionList.add(Arrays.asList(3, 1, 2));
        actionList.add(Arrays.asList(3, 2));
        int actionIndex = new Random().nextInt(actionList.size());

        for (Integer action : actionList.get(actionIndex)) {
            List<Unit> aiUnits = new ArrayList<>();
            List<Tile> aiTiles = new ArrayList<>();
            // ai action, 1 move, 2 attack, 3 summon
            List<Integer> aiActions = new ArrayList<>();
            aiActions.add(1);

            List<Card> aiHandCardsList = getAiHandCardsList(gameState);

            getAiUnitsAndTiles(gameState, aiPlayer, aiUnits, aiTiles);
            Tile tile1 = aiTiles.get(new Random().nextInt(aiTiles.size()));
            List<Tile> canAttackTiles = canAttackTiles(tile1.getTilex(), tile1.getTiley(), gameState);

            List<Tile> canSummonTiles = getCanSummonTiles(aiHandCardsList, canAttackTiles);
            if (canSummonTiles.size() > 0) {
                aiActions.add(3);
            }
            Player humanPlayer = gameState.getPlayerContainers()[0];
            List<Tile> humanTiles = getHumanTiles(canAttackTiles, humanPlayer);
            if (humanTiles.size() > 0) {
                aiActions.add(2);
            }

            if (!aiActions.contains(action)) {
                continue;
            }

            // move
            if (action == 1) {
                List<Tile> canMoveTiles = canMoveTiles(tile1.getTilex(), tile1.getTiley(), gameState);
                Tile moveTo = canMoveTiles.get(new Random().nextInt(canMoveTiles.size()));
                Unit unit = tile1.getUnitOnTile();
                moveTo.move(unit, tile1, false);
            }

            // attack
            if (action == 2) {
                Tile attackTo = humanTiles.get(new Random().nextInt(humanTiles.size()));
                attackTo.attackedBroadcast(tile1.getUnitOnTile());
            }

            // summon or spell
            if (action == 3) {
                doSummonOrSpell(out, gameState, aiHandCardsList, canSummonTiles);
            }
        }

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        autoEndTurn(out, gameState);
    }

    private void getAiUnitsAndTiles(GameState gameState, Player aiPlayer, List<Unit> aiUnits, List<Tile> aiTiles) {
        for (Observer observer : gameState.getObservers()) {
            if (observer instanceof Tile) {
                Tile tile = (Tile) observer;
                if (tile.getUnitOnTile() != null && tile.getUnitOnTile().getOwner().equals(aiPlayer)) {
                    aiUnits.add(tile.getUnitOnTile());
                    aiTiles.add(tile);
                }
            }
        }
    }

    private List<Card> getAiHandCardsList(GameState gameState) {
        List<Card> aiHandCardsList = new ArrayList<>();
        Card[] aiHandCards = gameState.getCurrentPlayer().getHandCards();
        for (Card card : aiHandCards) {
            if (card != null) {
                aiHandCardsList.add(card);
            }
        }
        return aiHandCardsList;
    }

    private List<Tile> getCanSummonTiles(List<Card> aiHandCardsList, List<Tile> canAttackTiles) {
        List<Tile> canSummonTiles = new ArrayList<>();
        if (aiHandCardsList.size() > 0) {
            for (Tile canAttackTile : canAttackTiles) {
                if (canAttackTile.getUnitOnTile() == null) {
                    canSummonTiles.add(canAttackTile);
                }
            }
        }
        return canSummonTiles;
    }

    private List<Tile> getHumanTiles(List<Tile> canAttackTiles, Player humanPlayer) {
        List<Tile> humanTiles = new ArrayList<>();
        if (canAttackTiles.size() > 0) {
            for (Tile tile : canAttackTiles) {
                if (tile.getUnitOnTile() != null) {
                    if (tile.getUnitOnTile().getOwner().equals(humanPlayer)) {
                        humanTiles.add(tile);
                    }
                }
            }
        }
        return humanTiles;
    }

    private void doSummonOrSpell(ActorRef out, GameState gameState, List<Card> aiHandCardsList,
                                 List<Tile> canSummonTiles) {
        int mana = gameState.getCurrentPlayer().getMana();

        List<Card> cards = aiHandCardsList.stream()
                .filter(card -> card.getManacost() <= mana)
                .collect(Collectors.toList());

        if (cards.size() == 0) {
            return;
        }

        Card card = cards.get(new Random().nextInt(cards.size()));
        aiHandCardsList.remove(card);
        int manaCost = card.getManacost();

        gameState.setCardSelected(card);
//      -1 spell / 1 summon
        if (card.isCOrS() == 1) {
            Tile summonToTile = canSummonTiles.get(new Random().nextInt(canSummonTiles.size()));
            gameState.add(gameState.cardToUnit(card));

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("type", "summon");
            parameters.put("tilex", summonToTile.getTilex());
            parameters.put("tiley", summonToTile.getTiley());
            parameters.put("unit", gameState.cardToUnit(card));
            gameState.broadcastEvent(Tile.class, parameters);
        } else {
            BasicCommands.addPlayer1Notification(out, "AI spell " + card.getCardname(), 2);

            String cardname = card.getCardname();
            int tilex, tiley;
            if (cardname.startsWith("Staff")) {
                Unit aiAvatar = gameState.getAIAvatar();
                Position position = aiAvatar.getPosition();
                tilex = position.getTilex();
                tiley = position.getTiley();
            } else {
                // Entropic
                List<Unit> enemyUnits = gameState.getEnemyUnits(false);
                if (enemyUnits.size() == 0) {
                    return;
                }
                Unit enemyUnit = enemyUnits.get(new Random().nextInt(enemyUnits.size()));
                Position position = enemyUnit.getPosition();
                tilex = position.getTilex();
                tiley = position.getTiley();
            }
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("type", "spell");
            parameters.put("tilex", tilex);
            parameters.put("tiley", tiley);
            gameState.broadcastEvent(Tile.class, parameters);
        }
        // Keep summoning if AI still have mana
        if (mana - manaCost > 0) {
            sleep(1000);
            doSummonOrSpell(out, gameState, aiHandCardsList, canSummonTiles);
        }
        gameState.callbacks();
    }

    private void autoEndTurn(ActorRef out, GameState gameState) {
        Player currentPlayer = gameState.getCurrentPlayer();
        currentPlayer.setMana(0);
        currentPlayer.drawCard(out);
        sleep(1500);

        Player humanPlayer = gameState.getPlayerContainers()[0];
        gameState.setCurrentPlayer(humanPlayer);
        humanPlayer.setMana(gameState.getTurnCount());
        gameState.setCurrentState(GameState.CurrentState.READY);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "unitBeReady");
        gameState.broadcastEvent(Unit.class, parameters);

        BasicCommands.addPlayer1Notification(out, "Player turn", 2);

    }

    private void sleep(int t) {
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private List<Tile> canAttackTiles(int tilex, int tiley, GameState gameState) {
        List<Integer> x = Arrays.asList(tilex, tilex - 1, tilex + 1);
        List<Integer> y = Arrays.asList(tiley, tiley - 1, tiley + 1);

        List<Tile> canAttack = new ArrayList<>();
        for (Integer x1 : x) {
            for (Integer y1 : y) {
                if (x1 == tilex && y1 == tiley) {
                    continue;
                }
                Tile tile1 = gameState.getTile(x1, y1);
                if (tile1 != null) {
                    canAttack.add(tile1);
                }
            }
        }
        return canAttack;
    }


    private List<Tile> canMoveTiles(int tilex, int tiley, GameState gameState) {
        List<Tile> canMove = new ArrayList<>();
        canMove.addAll(canAttackTiles(tilex, tiley, gameState));
        canMove.add(gameState.getTile(tilex + 2, tiley));
        canMove.add(gameState.getTile(tilex - 2, tiley));
        canMove.add(gameState.getTile(tilex, tiley + 2));
        canMove.add(gameState.getTile(tilex, tiley - 2));
        return canMove.stream().filter(t -> t != null && t.getUnitOnTile() == null)
                .collect(Collectors.toList());
    }
}
