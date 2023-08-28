package structures;

import akka.actor.ActorRef;
import structures.basic.Card;
import structures.basic.Player;
import structures.basic.Tile;
import structures.basic.Unit;
import utils.BasicObjectBuilders;

import java.util.*;
import java.util.function.Function;

/**
 * This class can be used to hold information about the on-going game.
 * Its created with the GameActor.
 *
 * @author Dr. Richard McCreadie
 */
public class GameState extends Subject {
    //ID and Callbacks

    private Map<String, Function<Integer, Boolean>> cardSelectedCallbacks = new HashMap<>();
    private Map<String, Function<Integer, Boolean>> beforeSummonCallbacks = new HashMap<>();
    private Map<String, Function<Integer, Boolean>> attackCallbacks = new HashMap<>();
    private Map<String, Function<Integer, Boolean>> deathCallbacks = new HashMap<>();
    private Map<String, Function<Integer, Boolean>> spellCallbacks = new HashMap<>();
    private int turnCount = 0;
    private Player[] playerContainers = new Player[2];
    private CurrentState currentState = CurrentState.READY;
    private Card cardSelected = null;
    private Player currentPlayer;
    private Tile tileSelected = null;
    private ActorRef out;

    private Unit humanAvatar;
    private Unit AIAvatar;

    public void addPlayers(Player humanPlayer, Player AIPlayer) {

        if (playerContainers[0] == null && playerContainers[1] == null) {
            playerContainers[0] = humanPlayer;
            playerContainers[1] = AIPlayer;

            this.currentPlayer = humanPlayer;
            turnCount++;
            this.currentPlayer.setMana(turnCount + 1);
        }
    }

    //    add AIPlayer mana
    public void addMana(Player player) {
        player.setMana(turnCount + 1);
        turnCount++;
    }

    public int getTurnCount() {
        return turnCount + 1;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(Player currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public enum CurrentState {
        READY, SELECTED_CARD, SELECTED_UNIT
    }

    public CurrentState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(CurrentState currentState) {
        this.currentState = currentState;
    }


    public void setCardSelected(Card cardSelected) {

        this.cardSelected = cardSelected;
        if (cardSelected == null) {
            this.currentState = CurrentState.READY;
        } else {
            this.currentState = CurrentState.SELECTED_CARD;

        }
    }

    public Card getCardSelected() {
        return cardSelected;
    }


    public void setTileSelected(Tile tileSelected) {
        this.tileSelected = tileSelected;
    }

    public Tile getTileSelected() {
        return tileSelected;
    }


    public void setOut(ActorRef out) {
        this.out = out;

    }

    public ActorRef getOut() {
        return out;
    }

    public void clear() {
        this.playerContainers = new Player[2];
        this.currentPlayer = null;
        this.currentState = CurrentState.READY;
        this.turnCount = 0;
        this.cardSelected = null;
        super.clearObservers();
    }

    @Override
    public void broadcastEvent(Class target, Map<String, Object> parameters) {
        for (Observer observer : observers) {
            observer.trigger(target, parameters);
        }
    }


    public void callbacks() {

        GameState self = this;
        this.beforeSummonCallbacks.put(String.valueOf("3"), new Function<Integer, Boolean>() {
            @Override
            public Boolean apply(Integer integer) {

                Map<String, Object> parameters = new HashMap<>();
                parameters.put("type", "modifyUnit");

                int unitId = -1;

                if (self.getCurrentPlayer().isHumanOrAI()) {
                    unitId = 99;
                } else {
                    unitId = 100;
                }

                parameters.put("unitId", unitId);
                parameters.put("attack", 0);
                parameters.put("health", 3);
                parameters.put("limit", "max");

                self.broadcastEvent(Unit.class, parameters);

                return true;
            }
        });


        this.cardSelectedCallbacks.put(String.valueOf("7"), new Function<Integer, Boolean>() {
            @Override
            public Boolean apply(Integer integer) {

                Map<String, Object> parameters = new HashMap<>();
                parameters.put("type", "validSummonRangeHighlight");
                parameters.put("airdrop", "activate");

                self.broadcastEvent(Tile.class, parameters);

                return true;
            }
        });


        this.cardSelectedCallbacks.put(String.valueOf("10"), new Function<Integer, Boolean>() {
            @Override
            public Boolean apply(Integer integer) {

                Map<String, Object> parameters = new HashMap<>();
                parameters.put("type", "validSummonRangeHighlight");
                parameters.put("airdrop", "activate");

                self.broadcastEvent(Tile.class, parameters);

                return true;
            }
        });


        this.spellCallbacks.put(String.valueOf("2"), new Function<Integer, Boolean>() {
            @Override
            public Boolean apply(Integer integer) {

                Map<String, Object> parameters = new HashMap<>();
                parameters.put("type", "modifyUnit");
                parameters.put("unitId", integer);
                parameters.put("attack", 1);
                parameters.put("health", 1);
                parameters.put("limit", "enemyTurn");


                self.broadcastEvent(Unit.class, parameters);


                return true;
            }
        });


        this.attackCallbacks.put(String.valueOf("99"), new Function<Integer, Boolean>() {
            @Override
            public Boolean apply(Integer integer) {

                Map<String, Object> parameters = new HashMap<>();
                parameters.put("type", "modifyUnit");
                parameters.put("unitId", 4);
                parameters.put("attack", 2);
                parameters.put("health", 0);

                self.broadcastEvent(Unit.class, parameters);


                return true;
            }
        });


        this.beforeSummonCallbacks.put(String.valueOf("14"), new Function<Integer, Boolean>() {
            @Override
            public Boolean apply(Integer integer) {

                self.getPlayerContainers()[0].drawCard(out);
                self.getPlayerContainers()[1].drawCard(out);

                return true;
            }
        });

        this.deathCallbacks.put(String.valueOf("15"), new Function<Integer, Boolean>() {
            @Override
            public Boolean apply(Integer integer) {

                self.getCurrentPlayer().drawCard(out);

                return true;
            }
        });
    }


    //    add new
    public Unit cardToUnit(Card card) {

        String unit_path = card.getCardname().split(" ")[0].toLowerCase(Locale.ROOT);

        if (card.getCardname().split(" ").length > 1 && card.getCardname().split(" ")[1].toLowerCase(Locale.ROOT) != "") {
            unit_path += "_" + card.getCardname().split(" ")[1].toLowerCase(Locale.ROOT);
        }

        unit_path = "conf/gameconfs/units/" + unit_path + ".json";

        //create and set unit

        Unit unit = BasicObjectBuilders.loadUnit(unit_path, card.getId(), Unit.class, this);

//        gameState.add(unit);

        unit.setHealth(card.getBigCard().getHealth());
        unit.setAttack(card.getBigCard().getAttack());
        unit.setMaxHealth(card.getBigCard().getHealth());

        if (card.getBigCard().getRulesTextRows().length > 0) {
            String rule = card.getBigCard().getRulesTextRows()[0];
            if (rule.toLowerCase(Locale.ROOT).contains("ranged")) {
                unit.setRemoteAttack(true);
            } else unit.setRemoteAttack(false);

            if (rule.toLowerCase(Locale.ROOT).contains("twice")) {
                unit.setAttackNum(2);
                unit.setMoveNum(2);
                unit.setMaxAttackNum(2);
                unit.setMaxMoveNum(2);
            } else {
                unit.setAttackNum(1);
                unit.setMoveNum(1);
                unit.setMaxAttackNum(1);
                unit.setMaxMoveNum(1);
            }

            if (rule.toLowerCase(Locale.ROOT).contains("provoke")) {
                unit.setCanProvoke(true);
            } else {
                unit.setCanProvoke(false);
            }

            if (rule.toLowerCase(Locale.ROOT).contains("remoteMove")) {
                unit.setRemoteMove(true);
            } else {
                unit.setRemoteMove(false);
            }
        }
        return unit;
    }


    public Player[] getPlayerContainers() {
        return playerContainers;
    }

    public Map<String, Function<Integer, Boolean>> getCardSelectedCallbacks() {
        return cardSelectedCallbacks;
    }

    public Map<String, Function<Integer, Boolean>> getAttackCallbacks() {
        return attackCallbacks;
    }

    public Map<String, Function<Integer, Boolean>> getBeforeSummonCallbacks() {
        return beforeSummonCallbacks;
    }

    public Map<String, Function<Integer, Boolean>> getDeathCallbacks() {
        return deathCallbacks;
    }

    public Map<String, Function<Integer, Boolean>> getSpellCallbacks() {
        return spellCallbacks;
    }

    public void setHumanAvatar(Unit humanAvatar) {
        this.humanAvatar = humanAvatar;
    }

    public void setAIAvatar(Unit AIAvatar) {
        this.AIAvatar = AIAvatar;
    }

    public Unit getHumanAvatar() {
        return humanAvatar;
    }

    public Unit getAIAvatar() {
        return AIAvatar;
    }

    public Tile getTile(int x, int y) {
        for (Observer o : observers) {
            if (o instanceof Tile) {
                Tile t = (Tile) o;
                if (t.getTilex() == x && t.getTiley() == y) {
                    return t;
                }
            }
        }
        return null;
    }

    /**
     * Get the enemy's position
     *
     * @param includeAvatar
     * @return
     */
    public List<Unit> getEnemyUnits(boolean includeAvatar) {
        List<Unit> enemy = new ArrayList<>();
        for (Observer observer : observers) {
            if (observer instanceof Tile) {
                Tile tile = (Tile) observer;
                Unit unitOnTile = tile.getUnitOnTile();
                if (unitOnTile != null) {
                    if (!unitOnTile.getOwner().equals(getCurrentPlayer())) {
                        if (includeAvatar) {
                            enemy.add(unitOnTile);
                        } else {
                            if (!unitOnTile.equals(getAIAvatar())
                                    && !unitOnTile.equals(getHumanAvatar())) {
                                enemy.add(unitOnTile);
                            }
                        }
                    }
                }
            }
        }
        return enemy;
    }
}
