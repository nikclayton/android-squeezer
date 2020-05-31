package uk.org.ngo.squeezer.service;

public class PlayerNotFoundException extends Exception {
    public PlayerNotFoundException(String playerId) {
        super("Player not found: " + playerId);
    }
}
