package me.otisdiver.otisarena.event;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerQuitEvent;

import me.otisdiver.otisarena.OtisArena;
import me.otisdiver.otisarena.game.Game;
import me.otisdiver.otisarena.game.GameState;
import me.otisdiver.otisarena.task.Countdown;
import me.otisdiver.otisarena.task.StartGame;

public class JoinQuit extends Event {
    
    // Configurables //
    
    // message format for countdown messages
    private final String countdownMessage = ChatColor.DARK_GREEN + " Game starting in " + ChatColor.YELLOW + "%d" + ChatColor.DARK_GREEN + " seconds!";
    
    // join message when the game isn't recruiting
    private final String joinMessageDefault = null;
    
    // join message when the game is recruiting
    private final String joinMessageRecruiting = ChatColor.GOLD + "%s joined!";
    
    // quit message when the game isn't recruiting
    private final String quitMessageDefault = ChatColor.GRAY + "%s quit.";
    
    // quit message when the game is recruiting
    private final String quitMessageRecruiting = ChatColor.GRAY + "%s quit.";
    
    // chat message sent to people joining during games
    private final String gameInProgress = ChatColor.YELLOW + " A game is currently in process. Please wait for the next one to start.";
    
    // Other Class Members //

    private Game game;
    
    private final int minimumPlayers;
    
    private Countdown interval5;
    private Countdown interval1;
    private StartGame startGame;
    
    /** JoinQuit handles all player join events.
     * 
     * @param main instance of JavaPlugin */
    public JoinQuit(OtisArena main) {
        
        // save the dependency classes
        super(main);
        game = main.getGame();
        
        // fetch # of players to start at
        minimumPlayers = game.getMinimumPlayers();
    }
    
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        
        // remove join message
        e.setJoinMessage(joinMessageDefault);
        
        // find the player and register as online
        Player player = e.getPlayer();
        game.getActivePlayers().add(player);
        
        // find the current game status
        GameState state = GameState.getCurrent();
        
        // determine join message
        String joinMessage = "";
        if (joinMessageDefault != null) {
            String.format(joinMessageDefault, player.getName());
        }
        
        switch(state) {
            
            case RECRUITING:
                
                // format the join message, if any (this is used after the switch)
                if (joinMessageRecruiting != null)
                joinMessage = String.format(joinMessageRecruiting, player.getName());
                
                // if the game is ready to start
                if (game.getActivePlayers().size() > minimumPlayers) {
                    
                    // start countdowns (20, 15, 10, 5; 4, 3, 2, 1)
                    interval5 = new Countdown(main, 20, 5, countdownMessage);
                    interval5.runFuture(20);
                    
                    interval1 = new Countdown(main, 4, 1, countdownMessage);
                    interval1.runFuture(320);
                    
                    // start the game after the countdown, 22 sec (* 20 ticks/sec = 440) from now
                    startGame = new StartGame(main);
                    startGame.runFuture(440);
                    
                }
                
                break;
            case PREPARING:
                
                // format the join message, if any (set after the switch)
                if (joinMessageRecruiting != null)
                joinMessage = String.format(joinMessageRecruiting, player.getName());
                
                break;
            case STARTING:
                
                makeSpectator(e.getPlayer());
                
                break;
            case PLAYING:
                
                makeSpectator(e.getPlayer());
                
                break;
            default:
                break;
        }
        
        // apply the join message
        e.setJoinMessage(joinMessage);
        
    }
    
    @EventHandler(priority = EventPriority.LOW)
    public void onLogin(PlayerLoginEvent e) {
        
        GameState current = GameState.getCurrent();
        
        switch (current) {
            
            case LOADING:
                e.disallow(Result.KICK_OTHER, "Game loading. Please reconnect.");
                
                break;
            default:
                break;
        }
        
    }
    
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        
        // find the player
        Player player = e.getPlayer();
        
        // find the current state
        GameState state = GameState.getCurrent();
        
        // remove 'em from the list of active players
        if (game.getActivePlayers().contains(player)) {
            game.getActivePlayers().remove(player);
        }
        
        // what the quit message will be
        String quitMessage = "";
        
        // fill in the formatted default value (if any)
        if (quitMessageDefault != null) {
            quitMessage = String.format(quitMessageDefault, player.getName());
        }
        
        switch (state) {
            
            case RECRUITING:
                
                // set the quit message, if any (applied after the switch)
                if (quitMessageRecruiting != null)
                quitMessage = String.format(quitMessageRecruiting, player.getName());
                
                break;
            case PREPARING:
                
                // set the quit message, if any (applied after the switch)
                if (quitMessageRecruiting != null)
                quitMessage = String.format(quitMessageRecruiting, player.getName());
                
                // if this causes the number of players to fall below minimum, stop the count down
                if (game.getActivePlayers().size() <= minimumPlayers) {
                    
                    // cancel countdowns, their messages, and the game starting
                    interval5.override();
                    interval1.override();
                    startGame.cancel();
                }
                
                break;
            default:
                break;
        }
        
        // apply the quit message
        e.setQuitMessage(quitMessage);
    }
    
    private void makeSpectator(Player player) {
        
        player.setGameMode(GameMode.SPECTATOR);
        
        player.sendMessage(gameInProgress);
        
    }
    
}