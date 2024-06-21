package client;

import java.util.*;
import java.util.stream.Collectors;
import java.io.Serializable;

/**
 * Class: Player 
 * Description: Class that represents a Player, I only used it in my Client 
 * to sort the LeaderBoard list
 * You can change this class, decide to use it or not to use it, up to you.
 */

public class Player implements Comparable<Player> {

    private int wins;
    private int logins;
    private String name;
    private int points;

    /**
     * Constructor to initialize a Player with a name and initial wins count.
     * @param name The name of the player.
     * @param wins The initial number of wins for the player.
     */
    public Player(String name, int wins){
      this.wins = wins;
      this.name = name;
      this.logins = 1;
      this.points = 0;
    }

    /**
     * Gets the number of wins.
     * @return The number of wins.
     */
    public int getWins(){
      return wins;
    }

    /**
     * Compares this player to another player based on the number of wins.
     * @param player The player to compare to.
     * @return A negative integer, zero, or a positive integer as this player has less than, equal to, or greater than the specified player.
     */
    @Override
    public int compareTo(Player player) {
        return Integer.compare(player.getWins(), this.wins);
    }

    /**
     * Returns a string representation of the player.
     * @return A string representation of the player.
     */
    @Override
       public String toString() {
            return ("\n" +this.wins + ": " + this.name);
       }

    /**
     * Gets the number of logins.
     * @return The number of logins.
     */
    public int getLogins() {
        return logins;
    }

    /**
     * Sets the number of logins.
     * @param logins The number of logins to set.
     */
    public void setLogins(int logins) {
        this.logins = logins;
    }

    /**
     * Gets the name of the player.
     * @return The name of the player.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the number of points.
     * @return The number of points.
     */
    public int getPoints() {
        return points;
    }

    /**
     * Sets the number of points.
     * @param points The number of points to set.
     */
    public void setPoints(int points) {
        this.points = points;
    }
}