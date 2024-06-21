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

    // constructor, getters, setters
    public Player(String name, int wins){
      this.wins = wins;
      this.name = name;
      this.logins = 1;
      this.points = 0;
    }

    public int getWins(){
      return wins;
    }

    @Override
    public int compareTo(Player player) {
        return Integer.compare(player.getWins(), this.wins);
    }

    @Override
       public String toString() {
            return ("\n" +this.wins + ": " + this.name);
       }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getLogins() {
        return logins;
    }

    public void setLogins(int logins) {
        this.logins = logins;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public void incrementWins() {
        this.wins++;
    }
    public int getPoints() {
        return points;
    }

    public void addPoints(int points) {
        this.points += points;
    }

    public void setPoints(int points) {
        this.points = points;
    }



}