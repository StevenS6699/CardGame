package structures.basic;

import java.io.File;
import java.util.*;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import commands.BasicCommands;
import structures.GameState;
import structures.Observer;


/**
 * A basic representation of a tile on the game board. Tiles have both a pixel position
 * and a grid position. Tiles also have a width and height in pixels and a series of urls
 * that point to the different renderable textures that a tile might have.
 *
 * @author Dr. Richard McCreadie
 */
public class Tile extends Observer {


    enum State {
        NORMAL("normal", 0), WHITE("white", 1), RED("red", 2);
        private String name;
        private int mode;

        private State(String name, int mode) {
            this.name = name;
            this.mode = mode;
        }
    }

    List<String> tileTextures;
    int xpos;
    int ypos;
    int width;
    int height;
    int tilex;
    int tiley;
    private State state = State.NORMAL;
    private Unit unitOnTile;
    private Set<Tile> moveableTiles = new HashSet<>();
    private GameState gameState;

    @JsonIgnore
    private static ObjectMapper mapper = new ObjectMapper(); // Jackson Java Object Serializer, is used to read java objects from a file

    public Tile() {
    }

    public Tile(String tileTexture, int xpos, int ypos, int width, int height, int tilex, int tiley) {
        super();
        tileTextures = new ArrayList<String>(1);
        tileTextures.add(tileTexture);
        this.xpos = xpos;
        this.ypos = ypos;
        this.width = width;
        this.height = height;
        this.tilex = tilex;
        this.tiley = tiley;
    }

    public Tile(List<String> tileTextures, int xpos, int ypos, int width, int height, int tilex, int tiley) {
        super();
        this.tileTextures = tileTextures;
        this.xpos = xpos;
        this.ypos = ypos;
        this.width = width;
        this.height = height;
        this.tilex = tilex;
        this.tiley = tiley;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }


    //broadcast events
    @Override
    public void trigger(Class target, Map<String, Object> parameters) {

        if (this.getClass().equals(target)) {

            //find
            if (parameters.get("type").equals("searchUnit")) {

                if (this.unitOnTile != null) {
                    if (
                            (parameters.get("range").equals("enemy") && this.unitOnTile.getOwner() != gameState.getCurrentPlayer())
                                    || parameters.get("range").equals("all")
                                    || (parameters.get("range").equals("non_avatar") && this.unitOnTile.id < 99)
                    ) {
                        this.setState(State.WHITE);
                    } else if (parameters.get("range").equals("your_avatar")
                            && this.unitOnTile.id >= 99
                            && this.unitOnTile.getOwner().equals(gameState.getCurrentPlayer())) {
                        this.setState(State.WHITE);
                    } else if (parameters.get("range").equals("all_friends")
                            && !this.unitOnTile.getOwner().isHumanOrAI()) {
                        Player aiPlayer = (Player) gameState.getCurrentPlayer();
                    }
                }
            } else if (parameters.get("type").equals("airdropSummonRangeHighlight")) {
                if (this.unitOnTile == null) {
                    this.setState(State.WHITE);
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                return;
            }


            // find  tile
            else if (parameters.get("type").equals("validSummonRangeHighlight")) {
//                if (parameters.get("airdrop") != null
//                        && parameters.get("airdrop").equals("activate")) {
//                    if (this.unitOnTile == null) {
//                        this.setState(State.WHITE);
//                        try {
//                            Thread.sleep(10);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                    return;
//                }


                if (this.unitOnTile != null && this.unitOnTile.getOwner() == gameState.getCurrentPlayer()) {
                    int[] xpos = new int[]{
                            -1, -1, -1, 0, 0, 1, 1, 1
                    };
                    int[] ypos = new int[]{
                            -1, 0, 1, -1, 1, -1, 0, 1
                    };
                    for (int i = 0; i < xpos.length; i++) {
                        parameters = new HashMap<>();
                        parameters.put("type", "validSummonRangeHighlight-checkNeighbour");
                        parameters.put("tilex", this.tilex + xpos[i]);
                        parameters.put("tiley", this.tiley + ypos[i]);
                        gameState.broadcastEvent(Tile.class, parameters);
                    }
                }
            }

            //check
            else if (parameters.get("type").equals("validSummonRangeHighlight-checkNeighbour")) {
                if (this.unitOnTile == null
                        && (Integer) parameters.get("tilex") == this.tilex
                        && (Integer) parameters.get("tiley") == this.tiley
                ) {
                    this.setState(State.WHITE);
                }
            }

            // reset
            else if (parameters.get("type").equals("textureReset")) {
                if (!this.state.equals(State.NORMAL)) {
                    this.setState(State.NORMAL);

                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            //summon
            else if (parameters.get("type").equals("summon")) {
                if ((Integer) parameters.get("tilex") == this.tilex
                        && (Integer) parameters.get("tiley") == this.tiley) {

                    boolean isHuman = gameState.getCurrentPlayer().equals(gameState.getPlayerContainers()[0]);
                    if (gameState.getCurrentState().equals(GameState.CurrentState.SELECTED_CARD)) {
                        if (isHuman && !this.state.equals(State.WHITE)) {
                            return;
                        }
                        BasicCommands.addPlayer1Notification(gameState.getOut(), "Play card "
                                + gameState.getCardSelected().getCardname(), 2);
                    }
                    Unit unit = (Unit) parameters.get("unit");
                    unit.setUnitSummonTurn(gameState.getTurnCount());
                    unit.setCurrentState(Unit.UnitState.NOT_READY);
                    unit.setTilePosition(this);
                    this.unitOnTile = unit;
                    BasicCommands.drawUnit(gameState.getOut(), unit, this);

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (gameState.getCurrentState().equals(GameState.CurrentState.SELECTED_CARD)) {
                        gameState.getCurrentPlayer().removeHandCards(gameState.getCardSelected());
                    }

                    BasicCommands.setUnitHealth(gameState.getOut(), unit, unit.getHealth());
                    BasicCommands.setUnitAttack(gameState.getOut(), unit, unit.getAttack());

//set this unit's owner
                    if (unit.getOwner() == null) {
                        unit.setOwner(gameState.getCurrentPlayer());
                    }


                    int id = unit.id;
                    if (gameState.getBeforeSummonCallbacks().get(String.valueOf(id)) != null) {
                        gameState.getBeforeSummonCallbacks().get(String.valueOf(id)).apply(id);
                    }
                    gameState.broadcastEvent(Unit.class, parameters);
//                    set game state to ready
                    gameState.setCurrentState(GameState.CurrentState.READY);
                }
            } else if (parameters.get("type").equals("moveHighlight")) {
                if (Integer.parseInt(String.valueOf(parameters.get("tilex"))) == this.tilex
                        && Integer.parseInt(String.valueOf(parameters.get("tiley"))) == this.tiley) {
                    if (this.unitOnTile == null && this.state == State.NORMAL) {
                        if (parameters.get("count") != null) {
                            this.setState(State.WHITE);
                            gameState.getTileSelected().getMoveableTiles().add(this);
                            int count = Integer.parseInt(String.valueOf(parameters.get("count")));
                            this.moveHighlight(count);
                            this.attackHighlight();
                        }
                    }
                }
            } else if (parameters.get("type").equals("attackHighlight")) {
                if (Integer.parseInt(String.valueOf(parameters.get("tilex"))) == this.tilex
                        && Integer.parseInt(String.valueOf(parameters.get("tiley"))) == this.tiley) {
                    if (this.unitOnTile != null) {
                        if (!this.unitOnTile.getOwner().equals(gameState.getCurrentPlayer())
                                && this.state == State.NORMAL) {
                            this.setState(State.RED);
                        }
                    }
                }
            } else if (parameters.get("type").equals("deleteUnit")) {
                if (Integer.parseInt(String.valueOf(parameters.get("tilex"))) == this.tilex
                        && Integer.parseInt(String.valueOf(parameters.get("tiley"))) == this.tiley) {
                    this.unitOnTile = null;
                }
            } else if (parameters.get("type").equals("spell")) {
                if (Integer.parseInt(String.valueOf(parameters.get("tilex"))) == this.tilex
                        && Integer.parseInt(String.valueOf(parameters.get("tiley"))) == this.tiley) {

                    Card spellCard = gameState.getCardSelected();

                    String rule = spellCard.getBigCard().getRulesTextRows()[0];


                    if (gameState.getSpellCallbacks().size() != 0) {
                        for (Map.Entry<String, Function<Integer, Boolean>> entry : gameState.getSpellCallbacks().entrySet()
                        ) {
                            entry.getValue().apply(Integer.parseInt(entry.getKey()));
                        }
                    }

                    //if this is a tile with attackable unit
                    if (this.state.equals(State.WHITE)) {
                        Unit targetUnit = this.unitOnTile;

                        if (rule.toLowerCase(Locale.ROOT).contains("enemy")) {
                            targetUnit.changeHealth(targetUnit.getHealth() - 2, false);
                        } else if (rule.toLowerCase(Locale.ROOT).contains("non-avatar")) {
                            if (targetUnit.getId() < 99) {
                                targetUnit.changeHealth(0, false);
                            }
                        } else if (rule.toLowerCase(Locale.ROOT).contains("health")) {
                            targetUnit.changeHealth(targetUnit.getHealth() + 5, false);
                        } else if (rule.toLowerCase(Locale.ROOT).contains("gains")) {
                            if (targetUnit.getId() >= 99) {
                                targetUnit.changeAttack(targetUnit.getAttack() + 2);
                            }
                        }
                        if (spellCard != null) {
                            gameState.getCurrentPlayer().removeHandCards(spellCard);
                        }
                    }
                    this.resetTileSelected();
                }
            } else if (parameters.get("type").equals("rangedUnitAttackHighlight")) {
                if (this.unitOnTile != null) {
                    if (this.unitOnTile.getOwner() != gameState.getCurrentPlayer()) {
                        this.setState(State.RED);
                    }
                }
            } else if (parameters.get("type").equals("firstClickTile")) {
                if (Integer.parseInt(String.valueOf(parameters.get("tilex"))) == this.tilex
                        && Integer.parseInt(String.valueOf(parameters.get("tiley"))) == this.tiley) {
                    if (this.unitOnTile != null) {
                        if (unitOnTile.getId() < 99 && unitOnTile.getUnitSummonTurn() == gameState.getTurnCount()) {
                            BasicCommands.addPlayer1Notification(gameState.getOut(), "Cannot take any actions in first summon turn", 2);
                        }


                        Map<String, Object> newParameters;
                        newParameters = new HashMap<>();
                        newParameters.put("provokedUnit", this.unitOnTile);

                        int[] offsetx = new int[]{1, 1, 0, -1, -1, -1, 0, 1};
                        int[] offsety = new int[]{0, 1, 1, 1, 0, -1, -1, -1};

                        for (int i = 0; i < offsetx.length; i++) {

                            int newTileX = tilex + offsetx[i];
                            int newTileY = tiley + offsety[i];

                            if (newTileX >= 0 && newTileY >= 0) {
                                newParameters.put("type", "searchUnitCanProvoke");
                                newParameters.put("tilex", newTileX);
                                newParameters.put("tiley", newTileY);
                                gameState.broadcastEvent(Tile.class, newParameters);
                            }
                        }

                        if (!this.unitOnTile.isProvoked()) {
                            if (this.unitOnTile.getOwner().equals(gameState.getCurrentPlayer())) {
                                if (this.unitOnTile.getCurrentState().equals(Unit.UnitState.READY)) {
                                    gameState.setTileSelected(this);

                                    if (this.unitOnTile.remoteAttack) {
                                        allBroadcast("attackHighlight");
                                        this.moveHighlight(0);
                                    }
                                    if (this.unitOnTile.remoteMove) {
                                        allBroadcast("moveHighlight");
                                    } else {
                                        this.moveHighlight(0);
                                        this.attackHighlight();
                                    }
                                    gameState.setCurrentState(GameState.CurrentState.SELECTED_UNIT);
                                } else if (this.unitOnTile.getCurrentState().equals(Unit.UnitState.HAS_MOVED)) {
                                    gameState.setTileSelected(this);
                                    this.attackHighlight();
                                    gameState.setCurrentState(GameState.CurrentState.SELECTED_UNIT);
                                }
                            }
                        } else {
                            gameState.setCurrentState(GameState.CurrentState.SELECTED_UNIT);
                            gameState.setTileSelected(this);
                        }
                    }
                }
            } else if (parameters.get("type").equals("operateUnit")) {
                if (Integer.parseInt(String.valueOf(parameters.get("tilex"))) == this.tilex
                        && Integer.parseInt(String.valueOf(parameters.get("tiley"))) == this.tiley) {
                    Tile originTile = (Tile) parameters.get("originTileSelected");
                    Unit unit = originTile.getUnitOnTile();
                    if (this.state.equals(State.NORMAL)) {
                        if (this.unitOnTile != null && this.unitOnTile.getOwner().equals(gameState.getCurrentPlayer())) {
                            this.resetTileSelected();
                            this.moveableTiles.clear();


                            parameters = new HashMap<>();
                            parameters.put("type", "firstClickTile");
                            parameters.put("tilex", this.tilex);
                            parameters.put("tiley", this.tiley);
                            gameState.broadcastEvent(Tile.class, parameters);
                        } else {
                            this.resetTileSelected();
                        }
                    } else if (this.state.equals(State.WHITE)) {
                        this.checkMoveVertically(originTile);
                    } else if (this.state.equals(State.RED)) {

                        if (unit.remoteAttack) {
                            this.attackedBroadcast(unit);
                            originTile.getMoveableTiles().clear();
                        } else if (!unit.remoteAttack) {
                            if (unit.getCurrentState().equals(Unit.UnitState.HAS_MOVED)) {
                                this.attackedBroadcast(unit);
                            } else {
                                if (distanceOfTiles(originTile, this) <= 2) {
                                    this.attackedBroadcast(unit);
                                    originTile.getMoveableTiles().clear();
                                } else {
                                    for (Tile x : originTile.getMoveableTiles()) {
                                        if (x.getState().equals(State.WHITE) && distanceOfTiles(x, this) <= 2) {
                                            x.checkMoveVertically(originTile);
                                            try {
                                                Thread.sleep(2000);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }

                                            this.attackedBroadcast(unit);
                                            break;
                                        }
                                    }
                                }
                            }
                        }

                    }

                }
            } else if (parameters.get("type").equals("checkMoveVertically")) {
                if (Integer.parseInt(String.valueOf(parameters.get("tilex"))) == this.tilex
                        && Integer.parseInt(String.valueOf(parameters.get("tiley"))) == this.tiley) {

                    Tile originTile = (Tile) parameters.get("originTile");
                    Tile aimTile = (Tile) parameters.get("aimTile");
// unit move
                    if (this.state.equals(State.NORMAL)) {
                        aimTile.move(originTile.getUnitOnTile(), originTile, true);
                    } else {
                        aimTile.move(originTile.getUnitOnTile(), originTile, false);
                    }
                }
            } else if (parameters.get("type").equals("searchUnitCanProvoke")) {
                if (Integer.parseInt(String.valueOf(parameters.get("tilex"))) == this.tilex
                        && Integer.parseInt(String.valueOf(parameters.get("tiley"))) == this.tiley) {
                    if (this.unitOnTile != null) {
                        Unit provokedUnit = (Unit) parameters.get("provokedUnit");
                        if (this.unitOnTile.getCanProvoke() &&
                                !this.unitOnTile.getOwner().equals(gameState.getCurrentPlayer())) {
                            this.setState(State.RED);
                            provokedUnit.setProvoked(true);
                        }
                    }
                }
            } else if (parameters.get("type").equals("clearProvoke")) {
                if (Integer.parseInt(String.valueOf(parameters.get("tilex"))) == this.tilex
                        && Integer.parseInt(String.valueOf(parameters.get("tiley"))) == this.tiley) {
                    if (this.unitOnTile != null) {
                        if (!this.unitOnTile.getOwner().equals(gameState.getCurrentPlayer())) {
                            this.unitOnTile.setProvoked(false);
                        }
                    }
                }
            }
        }
    }


    private void moveHighlight(int count) {
        if (count > 1) {
            return;
        }

        count++;
        Map<String, Object> newParameters;

        int[] offsetx = new int[]{0, 1, -1, 0};
        int[] offsety = new int[]{1, 0, 0, -1};

        for (int i = 0; i < offsetx.length; i++) {

            int newTileX = tilex + offsetx[i];
            int newTileY = tiley + offsety[i];

            if (newTileX >= 0 && newTileY >= 0) {
                newParameters = new HashMap<>();
                newParameters.put("type", "moveHighlight");
                newParameters.put("tilex", newTileX);
                newParameters.put("tiley", newTileY);
                newParameters.put("count", count);
                newParameters.put("originTile", this);
                gameState.broadcastEvent(Tile.class, newParameters);
            }
        }
    }


    private void attackHighlight() {
        Map<String, Object> newParameters;

        int[] offsetx = new int[]{1, 1, 0, -1, -1, -1, 0, 1};
        int[] offsety = new int[]{0, 1, 1, 1, 0, -1, -1, -1};

        for (int i = 0; i < offsetx.length; i++) {

            int newTileX = tilex + offsetx[i];
            int newTileY = tiley + offsety[i];

            if (newTileX >= 0 && newTileY >= 0) {
                newParameters = new HashMap<>();
                newParameters.put("type", "attackHighlight");
                newParameters.put("tilex", newTileX);
                newParameters.put("tiley", newTileY);
                gameState.broadcastEvent(Tile.class, newParameters);
            }
        }
    }

    public void attackedBroadcast(Unit attackerUnit) {
        Unit attackedUnit = this.getUnitOnTile();

        BasicCommands.addPlayer1Notification(gameState.getOut(), "Attack from" + attackerUnit.getId() + " to " + attackedUnit.getId(), 2);

        Map<String, Object> newParameters = new HashMap<>();
        newParameters.put("type", "attacked");
        newParameters.put("attackedUnit", attackedUnit);
        newParameters.put("attackerUnit", attackerUnit);
        gameState.broadcastEvent(Unit.class, newParameters);

        attackerUnit.setAttackNum(attackerUnit.getAttackNum() - 1);
        if (attackerUnit.getAttackNum() < 1) {
            attackerUnit.setCurrentState(Unit.UnitState.HAS_ATTACKED);
        }

        resetTileSelected();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public void move(Unit unit, Tile originTile, boolean mode) {
        resetTileSelected();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (mode) {
            BasicCommands.moveUnitToTile(gameState.getOut(), unit, this, true);
        } else {
            BasicCommands.moveUnitToTile(gameState.getOut(), unit, this);
        }

        BasicCommands.addPlayer1Notification(gameState.getOut(), unit.getId() + " move to " + this.tilex + "," + this.tiley + "", 2);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        unit.setTilePosition(this);
        this.setUnitOnTile(unit);
        originTile.setUnitOnTile(null);
        originTile.getMoveableTiles().clear();

        unit.setMoveNum(unit.getMoveNum() - 1);
        if (unit.getMoveNum() < 1) {
            unit.setCurrentState(Unit.UnitState.HAS_MOVED);
        }

        if (unit.getCanProvoke()) {
            originTile.adjacentBroadcast("clearProvoke");
        }
    }


    private int distanceOfTiles(Tile tile1, Tile tile2) {
        int x_1 = tile1.getTilex();
        int y_1 = tile1.getTiley();
        int x_2 = tile2.getTilex();
        int y_2 = tile2.getTiley();
        return (x_1 - x_2) * (x_1 - x_2) + (y_1 - y_2) * (y_1 - y_2);
    }

    private void resetTileSelected() {
        gameState.setTileSelected(null);
        gameState.setCurrentState(GameState.CurrentState.READY);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "textureReset");
        gameState.broadcastEvent(Tile.class, parameters);
    }


    private void allBroadcast(String type) {
        Map<String, Object> newParameters = new HashMap<>();
        newParameters.put("type", type);
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 5; j++) {
                newParameters.put("tilex", i);
                newParameters.put("tiley", j);
                if (type.equals("moveHighlight")) {
                    newParameters.put("count", 0);
                }
                gameState.broadcastEvent(Tile.class, newParameters);
            }
        }
    }


    private void checkMoveVertically(Tile originTile) {
        Map<String, Object> parameters;

        if (distanceOfTiles(this, originTile) == 2) {
            int checkTileX = this.getTilex();
            int checkTileY = originTile.getTiley();

            parameters = new HashMap<>();
            parameters.put("type", "checkMoveVertically");
            parameters.put("tilex", checkTileX);
            parameters.put("tiley", checkTileY);
            parameters.put("originTile", originTile);
            parameters.put("aimTile", this);
            gameState.broadcastEvent(Tile.class, parameters);
        } else this.move(originTile.getUnitOnTile(), originTile, false);
    }


    private void adjacentBroadcast(String type) {
        Map<String, Object> newParameters;

        int[] offsetx = new int[]{1, 1, 0, -1, -1, -1, 0, 1};
        int[] offsety = new int[]{0, 1, 1, 1, 0, -1, -1, -1};

        for (int i = 0; i < offsetx.length; i++) {

            int newTileX = tilex + offsetx[i];
            int newTileY = tiley + offsety[i];

            if (newTileX >= 0 && newTileY >= 0) {
                newParameters = new HashMap<>();
                newParameters.put("type", type);
                newParameters.put("tilex", newTileX);
                newParameters.put("tiley", newTileY);
                gameState.broadcastEvent(Tile.class, newParameters);
            }
        }
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
        BasicCommands.drawTile(gameState.getOut(), this, this.state.mode);
    }

    public Unit getUnitOnTile() {
        return unitOnTile;
    }

    public void setUnitOnTile(Unit unitOnTile) {
        this.unitOnTile = unitOnTile;
    }

    public Set<Tile> getMoveableTiles() {
        return moveableTiles;
    }

    public List<String> getTileTextures() {
        return tileTextures;
    }

    public void setTileTextures(List<String> tileTextures) {
        this.tileTextures = tileTextures;
    }

    public int getXpos() {
        return xpos;
    }

    public void setXpos(int xpos) {
        this.xpos = xpos;
    }

    public int getYpos() {
        return ypos;
    }

    public void setYpos(int ypos) {
        this.ypos = ypos;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getTilex() {
        return tilex;
    }

    public void setTilex(int tilex) {
        this.tilex = tilex;
    }

    public int getTiley() {
        return tiley;
    }

    public void setTiley(int tiley) {
        this.tiley = tiley;
    }


    public static Tile constructTile(String configFile) {

        try {
            Tile tile = mapper.readValue(new File(configFile), Tile.class);
            return tile;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }
}
