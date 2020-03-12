package taber.ranks;

import com.earth2me.essentials.Essentials;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Created by Sven on 28/08/2014. All self coded.
 */
public class CadiaRanks extends JavaPlugin {

    private static CadiaRanks instance;
    private Permission perms;
    private Essentials essen;

    public Permission getPerms() {
        return perms;
    }

    public static CadiaRanks getInstance() {
        return instance;
    }

    public Essentials getEssentials() {
        return essen;
    }

    private RankListener rankListener;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        Database.connect();
        setupPermissions();

        essen = (Essentials) this.getServer().getPluginManager().getPlugin("Essentials");
        if (essen == null) {
            this.getServer().getLogger().warning("Essentials wasn't found! CadiaRanks is shutting down!");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        rankListener = new RankListener();
        this.getServer().getPluginManager().registerEvents(rankListener, this);

        //  <add|take|set|check>
        getCommand("rankscore").setExecutor(new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
                if (strings.length == 1) {
                    return false;
                }
                if (strings.length == 2) {
                    if(strings[0].equals("list")){

                        sender.sendMessage("Rank Score List (Generated from gocadia/bravo::scorelist.sh) : ");
                        sender.sendMessage("- Default: 1");
                        sender.sendMessage("- Premium: 50");
                        sender.sendMessage("- Sponsor: 100");
                        sender.sendMessage("- VIP: 200");
                        sender.sendMessage("- MVP: 350");
                        sender.sendMessage("- Executive: 500");
                        sender.sendMessage("- Legend: 800");
                        sender.sendMessage("- GOD: 1050");
                        sender.sendMessage("- Cadian: 2500");
                    }
                    if (strings[0].equals("check")) {
                        OfflinePlayer ofPlayer = null;
                        try {
                            UUID id = UUID.fromString(strings[1]);
                            ofPlayer = instance.getServer().getOfflinePlayer(id);
                        } catch (Exception e) {
                            // ignore
                        }
                        if (ofPlayer == null) {
                            ofPlayer = getOfflinePlayer(strings[1]);
                            if ( ofPlayer == null){
                                sender.sendMessage("That player has never joined the server before.");
                                return true;
                            }
                        }
                        rankListener.initPlayer(ofPlayer.getUniqueId());
                        int score = rankListener.getScore(ofPlayer.getUniqueId());
                        sender.sendMessage(ofPlayer.getName() + " has " + score + " score.");
                        return true;
                    }
                }
                if (strings.length == 3) {
                    if (strings[0].equals("set")) {
                        OfflinePlayer ofPlayer = null;
                        try {
                            UUID id = UUID.fromString(strings[1]);
                            ofPlayer = instance.getServer().getOfflinePlayer(id);
                        } catch (Exception e) {
                            // ignore
                        }
                        if (ofPlayer == null) {
                            ofPlayer = getOfflinePlayer(strings[1]);
                            if ( ofPlayer == null){
                                sender.sendMessage("That player has never joined the server before.");
                                return true;
                            }
                        }
                        int amount;
                        try {
                            amount = Integer.parseInt(strings[2]);
                        } catch (Exception e) {
                            sender.sendMessage("How fucking hard is it to input a NUMBER?!");
                            return false;
                        }
                        final UUID id = ofPlayer.getUniqueId();
                        final int am = amount;
                        instance.getServer().getScheduler().runTaskAsynchronously(instance,new Runnable() {
                            @Override
                            public void run() {
                                rankListener.initPlayer(id);
                                rankListener.setScore(id, am);
                            }
                        });
                        sender.sendMessage(ofPlayer.getName() + " has now a score of " + amount);
                        return true;
                    } else if (strings[0].equals("add")) {
                        OfflinePlayer ofPlayer = null;
                        try {
                            UUID id = UUID.fromString(strings[1]);
                            ofPlayer = instance.getServer().getOfflinePlayer(id);
                        } catch (Exception e) {
                            // ignore
                        }
                        if (ofPlayer == null) {
                            ofPlayer = getOfflinePlayer(strings[1]);
                            if ( ofPlayer == null){
                                sender.sendMessage("That player has never joined the server before.");
                                return true;
                            }
                        }
                        int amount;
                        try {
                            amount = Integer.parseInt(strings[2]);
                        } catch (Exception e) {
                            sender.sendMessage("How fucking hard is it to input a NUMBER?!");
                            return false;
                        }
                        rankListener.initPlayer(ofPlayer.getUniqueId());
                        int curScore = rankListener.getScore(ofPlayer.getUniqueId());
                        if ( handleScore(curScore,ofPlayer.getUniqueId()) ){
                            curScore = rankListener.getScore(ofPlayer.getUniqueId());
                        }
                        rankListener.setScore(ofPlayer.getUniqueId(), curScore + amount);
                        sender.sendMessage(ofPlayer.getName() + " received " + amount + ", total score: " + String.valueOf(curScore + amount));
                        return true;
                    } else if (strings[0].equals("take")) {
                        OfflinePlayer ofPlayer = null;
                        try {
                            UUID id = UUID.fromString(strings[1]);
                            ofPlayer = instance.getServer().getOfflinePlayer(id);
                        } catch (Exception e) {
                            // ignore
                        }
                        if (ofPlayer == null) {
                            ofPlayer = getOfflinePlayer(strings[1]);
                            if ( ofPlayer == null){
                                sender.sendMessage("That player has never joined the server before.");
                                return true;
                            }
                        }
                        int amount;
                        try {
                            amount = Integer.parseInt(strings[2]);
                        } catch (Exception e) {
                            sender.sendMessage("How fucking hard is it to input a NUMBER?!");
                            return false;
                        }
                        rankListener.initPlayer(ofPlayer.getUniqueId());
                        int curScore = rankListener.getScore(ofPlayer.getUniqueId());
                        if ( handleScore(curScore,ofPlayer.getUniqueId()) ){
                            curScore = rankListener.getScore(ofPlayer.getUniqueId());
                        }
                        rankListener.setScore(ofPlayer.getUniqueId(), curScore - amount);
                        sender.sendMessage(ofPlayer.getName() + " lost " + amount + ", total score: " + String.valueOf(curScore - amount));
                        return true;
                    }
                }
                return true;
            }
        });

    }

    public boolean handleScore(int curScore, UUID playerID){
        if ( curScore == 0){
            rankListener.transferOldRankToDB(playerID);
            return true;
        }else{
            return false;
        }
    }

    public OfflinePlayer getOfflinePlayer(String name) {
        try {
            //This contacts the mojang servers, so better use UUID's in the commands.
            return instance.getServer().getOfflinePlayer(name);
            //Essentials is giving the wrong UUID
//            OfflinePlayer id  = this.getServer().getOfflinePlayer(essen.getUser(name).getUniqueId());
//            getLogger().info("Essentials offline player lookup: UUID "+id.getUniqueId().toString() + " found for "+name+" with the name "+String.valueOf(id.getName()));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }
}
