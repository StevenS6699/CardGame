package structures.basic;

import akka.actor.ActorRef;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import commands.BasicCommands;
import structures.GameState;
import structures.Observer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.Map.Entry;

/**
 * This is a representation of a Unit on the game board.
 * A unit has a unique id (this is used by the front-end.
 * Each unit has a current UnitAnimationType, e.g. move,
 * or attack. The position is the physical position on the
 * board. UnitAnimationSet contains the underlying information
 * about the animation frames, while ImageCorrection has
 * information for centering the unit on the tile.
 *
 * @author Dr. Richard McCreadie
 */
public class Unit extends Observer {

    @JsonIgnore
    protected static ObjectMapper mapper = new ObjectMapper(); // Jackson Java Object Serializer, is used to read java objects from a file


    int id;
    ActorRef out;
    UnitAnimationType animation;
    Position position;
    UnitAnimationSet animations;
    ImageCorrection correction;
    private int maxHealth;
    private UnitState currentState = UnitState.NOT_READY;
    GameState gameState;
    private Player owner;
    private int attack = 0;
    private int health = 0;
    private int unitSummonTurn = 0;
    boolean remoteAttack = false;
    boolean remoteMove = false;
    boolean canProvoke = false;
    boolean isProvoked = false;


    int maxAttackNum = 1;
    int maxMoveNum = 1;
    int attackNum = 1;
    int moveNum = 1;

    public enum UnitState {
        NOT_READY, READY, HAS_MOVED, HAS_ATTACKED
    }

    public Unit() {
    }

    public Unit(int id, UnitAnimationSet animations, ImageCorrection correction) {
        super();
        this.id = id;
        this.animation = UnitAnimationType.idle;

        position = new Position(0, 0, 0, 0);
        this.correction = correction;
        this.animations = animations;
    }

    public Unit(int id, UnitAnimationSet animations, ImageCorrection correction, Tile currentTile) {
        super();
        this.id = id;
        this.animation = UnitAnimationType.idle;

        position = new Position(currentTile.getXpos(), currentTile.getYpos(), currentTile.getTilex(), currentTile.getTiley());
        this.correction = correction;
        this.animations = animations;
    }

    public Unit(int id, UnitAnimationType animation, Position position, UnitAnimationSet animations,
                ImageCorrection correction) {
        super();
        this.id = id;
        this.animation = animation;
        this.position = position;
        this.animations = animations;
        this.correction = correction;
    }

//    count unit summon turn


    public int getUnitSummonTurn() {
        return unitSummonTurn;
    }

    public void setUnitSummonTurn(int unitSummonTurn) {
        this.unitSummonTurn = unitSummonTurn;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;

    }

    public void setOut(ActorRef out) {
        this.out = out;
    }


    @JsonIgnore
    public void setTilePosition(Tile tile) {
        position = new Position(tile.getXpos(), tile.getYpos(), tile.getTilex(), tile.getTiley());
    }

    public UnitState getUnitState() {
        return currentState;
    }

    public void setUnitState(UnitState state) {
        this.currentState = state;
    }


    public void changeHealth(int health, boolean canTakeOverMax) {
        if (health > maxHealth) {
            if (canTakeOverMax) {
                BasicCommands.setUnitHealth(this.gameState.getOut(), this, health);

            } else {
                health = maxHealth;
                BasicCommands.setUnitHealth(this.gameState.getOut(), this, health);
            }
        } else if (health < 1) {
            int id = this.getId();
            if (gameState.getDeathCallbacks().get(String.valueOf(id)) != null) {

                gameState.getDeathCallbacks().get(String.valueOf(id)).apply(id);
            }

            health = 0;
            BasicCommands.setUnitHealth(gameState.getOut(), this, health);
            BasicCommands.playUnitAnimation(gameState.getOut(), this, UnitAnimationType.death);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            BasicCommands.deleteUnit(gameState.getOut(), this);

            Map<String, Object> newParameters = new HashMap<>();
            newParameters.put("type", "deleteUnit");
            newParameters.put("tilex", this.getPosition().getTilex());
            newParameters.put("tiley", this.getPosition().getTiley());
            gameState.broadcastEvent(Tile.class, newParameters);
            if (this.getId() == 100) {
                BasicCommands.addPlayer1Notification(gameState.getOut(), "Player won", 2);
//                clear and end game
                gameState.clear();
            } else if (this.getId() == 99) {
                BasicCommands.addPlayer1Notification(gameState.getOut(), "Player lost", 2);
//                clear and end game
                gameState.clear();
            }
        } else {
            BasicCommands.setUnitHealth(gameState.getOut(), this, health);

        }

        this.setHealth(health);
    }


    public void changeAttack(int attack) {
        this.attack = attack;
        BasicCommands.setUnitAttack(gameState.getOut(), this, this.attack);
    }

    @Override
    public void trigger(Class target, Map<String, Object> parameters) {

        if (this.getClass().equals(target)) {
            if (parameters.get("type").equals("unitBeReady")) {
//                if (this.owner == gameState.getCurrentPlayer()) {
//                    this.currentState = UnitState.READY;
//                    this.setAttackNum(this.maxAttackNum);
//                    this.setMoveNum(this.maxMoveNum);
//                } else {
//                    this.currentState = UnitState.NOT_READY;
//                }

                this.currentState = UnitState.READY;
                this.setAttackNum(this.maxAttackNum);
                this.setMoveNum(this.maxMoveNum);


            } else if (parameters.get("type").equals("attacked")) {
                Unit unit = (Unit) parameters.get("attackedUnit");

                if (unit == this) {
                    Unit attackedUnit = (Unit) parameters.get("attackedUnit");
                    Unit attackerUnit = (Unit) parameters.get("attackerUnit");

                    attackedUnit.attacked(attackerUnit, true);
                } else if (unit.getId() == this.id) {
//                    for general unit
                    Unit attackedUnit = (Unit) parameters.get("attackedUnit");
                    Unit attackerUnit = (Unit) parameters.get("attackerUnit");

                    attackedUnit.attacked(attackerUnit, true);
                }
            } else if (parameters.get("type").equals("modifyUnit")) {
                if (this.id == (Integer) parameters.get("unitId")) {
                    int newHealth = this.health + (Integer) parameters.get("health");
                    int newAttack = this.attack + (Integer) parameters.get("attack");

                    if (parameters.get("limit") != null) {
                        if (parameters.get("limit").equals("max") && newHealth > maxHealth) {
                            BasicCommands.addPlayer1Notification(gameState.getOut(), "Max health", 2);
                            newHealth = maxHealth;
                        }
                        if (parameters.get("limit").equals("enemyTurn") && gameState.getCurrentPlayer() == this.owner) {
                            return;
                        }
                    }
                    this.setHealth(newHealth);
                    this.setAttack(newAttack);
                    displayAttackAndHealth();
                }
            }
        }
    }


    //   could not use this
    protected void displayAttackAndHealth() {
        BasicCommands.setUnitHealth(gameState.getOut(), this, this.health);
        BasicCommands.setUnitAttack(gameState.getOut(), this, this.attack);
    }


    private void attacked(Unit attacker, boolean allowCounterAttack) {

        int id = attacker.getId();
        if (id == 99) {
            if (gameState.getAttackCallbacks().size() != 0) {

                for (Entry<String, Function<Integer, Boolean>> entry : gameState.getAttackCallbacks().entrySet()
                ) {
                    entry.getValue().apply(Integer.parseInt(entry.getKey()));
                }
            }
        }


        BasicCommands.playUnitAnimation(gameState.getOut(), attacker, UnitAnimationType.attack);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        BasicCommands.playUnitAnimation(gameState.getOut(), attacker, UnitAnimationType.idle);

        if ((this.getId() == 99 && attacker.getId() == 100) || this.getId() == 100 && attacker.getId() == 99)
            this.changeHealth(this.getOwner().getHealth() - attacker.getOwner().getAttack(), false);

        else
            this.changeHealth(this.getHealth() - attacker.getAttack(), true);

//        Synchronize health
        if (this.getId() == 99) {
            gameState.getPlayerContainers()[0].setHealth(health);
            BasicCommands.setPlayer1Health(gameState.getOut(), gameState.getPlayerContainers()[0]);
        } else if (this.getId() == 100) {
            gameState.getPlayerContainers()[1].setHealth(health);
            BasicCommands.setPlayer2Health(gameState.getOut(), gameState.getPlayerContainers()[1]);
        }

        if (allowCounterAttack && this.health >= 1
                && this.checkTarget(attacker.getPosition().getTilex(), attacker.getPosition().getTiley())) {
            attacker.attacked(this, false);

        }
    }


    private boolean checkTarget(int tilex, int tiley) {
        int[] offsetx = new int[]{1, 1, 0, -1, -1, -1, 0, 1};
        int[] offsety = new int[]{0, 1, 1, 1, 0, -1, -1, -1};
        for (int i = 0; i < offsetx.length; i++) {
            int newTileX = this.getPosition().getTilex() + offsetx[i];
            int newTileY = this.getPosition().getTiley() + offsety[i];
            if (tilex == newTileX && tiley == newTileY) {
                return true;
            }
        }
        return false;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public UnitAnimationType getAnimation() {
        return animation;
    }

    public void setAnimation(UnitAnimationType animation) {
        this.animation = animation;
    }

    public ImageCorrection getCorrection() {
        return correction;
    }

    public void setCorrection(ImageCorrection correction) {
        this.correction = correction;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public UnitAnimationSet getAnimations() {
        return animations;
    }

    public void setAnimations(UnitAnimationSet animations) {
        this.animations = animations;
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
    }

    public void setCurrentState(UnitState currentState) {
        this.currentState = currentState;
    }

    public UnitState getCurrentState() {
        return currentState;
    }

    public Player getOwner() {
        return owner;
    }

    public void setOwner(Player owner) {
        this.owner = owner;
    }

    public void setAttack(int attack) {
        this.attack = attack;
    }

    public int getAttack() {
        return attack;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
        if (this.getId() == 99 || this.getId() == 100) {
            this.getOwner().setHealth(health);
        }
    }

    public int getAttackNum() {
        return attackNum;
    }

    public void setAttackNum(int attackNum) {
        this.attackNum = attackNum;
    }

    public int getMoveNum() {
        return moveNum;
    }

    public void setMoveNum(int moveNum) {
        this.moveNum = moveNum;
    }

    public boolean getCanProvoke() {
        return this.canProvoke;
    }

    public void setCanProvoke(boolean canProvoke) {
        this.canProvoke = canProvoke;
    }

    public boolean isProvoked() {
        return isProvoked;
    }

    public void setProvoked(boolean isProvoked) {
        this.isProvoked = isProvoked;
    }

    public boolean isRemoteAttack() {
        return remoteAttack;
    }

    public void setRemoteAttack(boolean remoteAttack) {
        this.remoteAttack = remoteAttack;
    }

    public boolean isRemoteMove() {
        return remoteMove;
    }

    public void setRemoteMove(boolean remoteMove) {
        this.remoteMove = remoteMove;
    }

    public boolean isCanProvoke() {
        return canProvoke;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public int getMaxAttackNum() {
        return maxAttackNum;
    }

    public void setMaxAttackNum(int maxAttackNum) {
        this.maxAttackNum = maxAttackNum;
    }

    public int getMaxMoveNum() {
        return maxMoveNum;
    }

    public void setMaxMoveNum(int maxMoveNum) {
        this.maxMoveNum = maxMoveNum;
    }
}
