package utils;

import java.util.ArrayList;
import java.util.List;

import structures.basic.Card;

/**
 * This is a utility class that provides methods for loading the decks for each
 * player, as the deck ordering is fixed. 
 * @author Richard
 *
 */
public class OrderedCardLoader {

	/**
	 * Returns all of the cards in the human player's deck in order
	 * @return
	 */
	public static List<Card> getPlayer1Cards() {
	
		List<Card> cardsInDeck = new ArrayList<Card>(20);
		
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_comodo_charger, 0, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_pureblade_enforcer, 1, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_fire_spitter, 2, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_silverguard_knight, 3, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_truestrike, 4, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_azure_herald, 5, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_ironcliff_guardian, 6, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_azurite_lion, 7, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_sundrop_elixir, 8, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_hailstone_golem, 9, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_silverguard_knight, 10, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_fire_spitter, 11, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_comodo_charger, 12, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_pureblade_enforcer, 13, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_truestrike, 14, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_azure_herald, 15, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_ironcliff_guardian, 16, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_azurite_lion, 17, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_sundrop_elixir, 18, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_hailstone_golem, 19, Card.class, null));
		
		return cardsInDeck;
	}
	
	
	/**
	 * Returns all of the cards in the human player's deck in order
	 * @return
	 */
	public static List<Card> getPlayer2Cards() {
	
		List<Card> cardsInDeck = new ArrayList<Card>(20);
		
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_rock_pulveriser, 20, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_bloodshard_golem, 21, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_staff_of_ykir, 22, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_blaze_hound, 23, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_windshrike, 24, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_pyromancer, 25, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_serpenti, 26, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_entropic_decay, 27, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_planar_scout, 28, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_hailstone_golem, 29, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_rock_pulveriser, 30, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_bloodshard_golem, 31, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_staff_of_ykir, 32, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_blaze_hound, 33, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_windshrike, 34, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_pyromancer, 35, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_serpenti, 36, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_entropic_decay, 37, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_planar_scout, 38, Card.class, null));
		cardsInDeck.add(BasicObjectBuilders.loadCard(StaticConfFiles.c_hailstone_golem, 39, Card.class, null));
		
		return cardsInDeck;
	}
	
}
