package structures.basic;

import commands.BasicCommands;
import structures.GameState;

import java.util.*;

import akka.actor.ActorRef;

/**
 * A basic representation of the Player. A player
 * has health and mana.
 *
 * @author Dr. Richard McCreadie
 */
public class Player {

    int health;
    int mana;
    int attack;
    private List<Card> deck = new ArrayList<>();
    protected Card[] handCards = new Card[6];
    protected GameState gameState;
    int i;

    public Player() {
        super();
        this.health = 20;
        this.mana = 0;
    }

    public Player(int health, int attack, int mana, GameState gameState) {
        super();
        this.health = health;
        this.mana = mana;
        this.gameState = gameState;
        this.attack = attack;
    }


    public boolean isHumanOrAI() {
        if (this == gameState.getPlayerContainers()[0]) {
            //human
            return true;
        }
        //AI
        else return false;
    }


    public void drawCard(ActorRef out) {
        //if deck is empty, game over
        if (deck.size() == 0) {
            if (this == gameState.getPlayerContainers()[0]) {
                BasicCommands.addPlayer1Notification(out, "AI won", 2);
//                clear and end game
                gameState.clear();
            } else {
                BasicCommands.addPlayer1Notification(out, "Player won", 2);
//                clear and end game
                gameState.clear();
            }
            return;
        }

        int randomInt = new Random().nextInt(deck.size());
        Card card = this.deck.get(randomInt);
        this.deck.remove(card);


        for (i = 0; i < 6; i++) {
            if (this.handCards[i] == null) {
                this.handCards[i] = card;
                if (this.isHumanOrAI()) {
                    BasicCommands.drawCard(out,
                            card, i + 1, 0);
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        if (i == 6) {
            BasicCommands.addPlayer1Notification(out, "Player have more than 6 cards, discard new card", 2);
//            hand is full, continue drop card from desk
        }
    }

    public static <T> int findObject(T[] arr, T element) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == element) {
                return i;
            }
        }
        return -1;
    }


    public void removeHandCards(Card card) {
        this.setMana(mana - card.getManacost());

        clearSelected();

        int index = findObject(this.handCards, card);

        if (this.isHumanOrAI()) {
            BasicCommands.deleteCard(gameState.getOut(), index + 1);
        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.handCards[index] = null;

        gameState.setCardSelected(null);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "textureReset");
        gameState.broadcastEvent(Tile.class, parameters);
    }


    public void cardSelected(int handPosition, ActorRef out) {
        if (handPosition - 1 < 0 || handPosition - 1 > 5) {
            return;
        }

        Card cardSelected = this.handCards[handPosition - 1];

        if (cardSelected.getManacost() <= this.mana) {

            if (gameState.getCurrentState().equals(GameState.CurrentState.SELECTED_CARD)) {
                clearSelected();
            }

            gameState.setCardSelected(cardSelected);

            // front end
            if (this.isHumanOrAI()) {
                BasicCommands.drawCard(out, cardSelected,
                        handPosition
                        , 1);
            }

            validRange(cardSelected);

            int id = cardSelected.getId();
            if (gameState.getCardSelectedCallbacks().get(String.valueOf(id)) != null) {
                gameState.getCardSelectedCallbacks().get(String.valueOf(id)).apply(id);
            }
        } else {
            if (this.isHumanOrAI()) {
                BasicCommands.addPlayer1Notification(out, "Mana not enough", 2);
            }
            return;
        }
    }


    public void clearSelected() {
        if (gameState.getCurrentState().equals(GameState.CurrentState.SELECTED_CARD)) {
            if (this.isHumanOrAI()) {
                BasicCommands.drawCard(gameState.getOut(), gameState.getCardSelected(),
                        findObject(handCards, gameState.getCardSelected()) + 1
                        , 0);
            }
            gameState.setCardSelected(null);
            gameState.setTileSelected(null);
        }
    }


    public void validRange(Card cardSelected) {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("type", "textureReset");
        gameState.broadcastEvent(Tile.class, parameters);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //spell -1
        if (cardSelected.isCOrS() == -1) {
            String rule = cardSelected.getBigCard().getRulesTextRows()[0];
            parameters = new HashMap<>();
            parameters.put("type", "searchUnit");
            // unit
            if (rule.toLowerCase(Locale.ROOT).contains("unit")) {
                if (rule.toLowerCase(Locale.ROOT).contains("enemy")) {
                    parameters.put("range", "enemy");

                    gameState.broadcastEvent(Tile.class, parameters);
                } else if (rule.toLowerCase(Locale.ROOT).contains("non-avatar")) {
                    parameters.put("range", "non_avatar");
                    gameState.broadcastEvent(Tile.class, parameters);
                } else {
                    parameters.put("range", "all");
                    gameState.broadcastEvent(Tile.class, parameters);
                }
            }
            // avatar
            else if (rule.toLowerCase(Locale.ROOT).contains("avatar")) {
                if (rule.toLowerCase(Locale.ROOT).contains("your avatar")) {
                    {
                        parameters.put("range", "your_avatar");
                        gameState.broadcastEvent(Tile.class, parameters);
                    }
                }
            }
        }
        // creature 1
        else if (cardSelected.isCOrS() == 1) {
            parameters = new HashMap<>();
            boolean airdrop = false;

            if (cardSelected.getBigCard().getRulesTextRows().length > 0) {
                String rule = cardSelected.getBigCard().getRulesTextRows()[0];
                if (rule.toLowerCase(Locale.ROOT).contains("airdrop")) {
                    airdrop = true;
                    parameters.put("type", "airdropSummonRangeHighlight");
                }
            }

            if (!airdrop) {
                parameters.put("type", "validSummonRangeHighlight");
            }

            gameState.broadcastEvent(Tile.class, parameters);
        }
    }


    public void setDeck(List<Card> list) {
        for (Card card : list) {
            this.deck.add(card);
        }
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
        if (this.isHumanOrAI()) {
            BasicCommands.setPlayer1Health(gameState.getOut(), this);
        } else {
            BasicCommands.setPlayer2Health(gameState.getOut(), this);
        }

    }

    public int getMana() {
        return mana;
    }

    public void setMana(int mana) {
        int newMana = mana;
        if (newMana > 9) {
            newMana = 9;
        }
        this.mana = newMana;
        if (gameState.getCurrentPlayer() == gameState.getPlayerContainers()[0])
            BasicCommands.setPlayer1Mana(gameState.getOut(), this);
        else
            BasicCommands.setPlayer2Mana(gameState.getOut(), this);
    }

    public int getAttack() {
        return attack;
    }

    public void setAttack(int attack) {
        this.attack = attack;
    }

    public List<Card> getDeck() {
        return deck;
    }

    public Card[] getHandCards() {
        return handCards;
    }
}
