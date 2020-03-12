package taber.ranks;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Created by Sven on 28/08/2014. All self coded.
 */
public class RankListener implements Listener {

    private CadiaRanks instance;

    public RankListener() {
        instance = CadiaRanks.getInstance();
        initTable();
        for (Player player : instance.getServer().getOnlinePlayers()) {
            handleRankOnLogin(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerLoginEvent event) {
        handleRankOnLogin(event.getPlayer());
    }

    /**
     * Called when a player logs in or is online when the plugin is loading.
     *
     * @param player
     */
    public void handleRankOnLogin(Player player) {
        final UUID playerID = player.getUniqueId();
        instance.getServer().getScheduler().runTaskAsynchronously(instance, new Runnable() {
            @Override
            public void run() {
                try {
                    //The default init value will always be 0. Which means the player hasn't gotten a rank check yet.
                    initPlayer(playerID);
                    final int score = getScore(playerID);
                    instance.getServer().getScheduler().runTask(instance, new Runnable() {
                        @Override
                        public void run() {
                            //If the player has never been assigned a rank, look up his old rank.
                            if (score == 0) {
                                transferOldRankToDB(playerID);
                            } else
                            //The player his old rank from before the transfer has already been put in the db.
                            {
                                removeAllGroups(playerID);
                                instance.getPerms().playerAddGroup(null, instance.getServer().getOfflinePlayer(playerID), getRank(score));
                                fixDefaultGroup(playerID);
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
    }

    public void transferOldRankToDB(UUID playerID){
        String highestRank = getHighestRankFromPex(playerID);
        //The player has no valid score ranks, give him the default rank.
        if (highestRank == null) {
            setScore(playerID, 1);
            instance.getPerms().playerAddGroup(null, instance.getServer().getOfflinePlayer(playerID), "default");
        }
        //The player has a valid rank for the first time. Assigning rank.
        else {

            int score = getScoreOfRank(highestRank);//Shouldn't ever be 0 unless I mistyped something in the code.
            if (score == 0) {
                setScore(playerID, 1);
                removeAllGroups(playerID);
                instance.getPerms().playerAddGroup(null, instance.getServer().getOfflinePlayer(playerID), "default");
                fixDefaultGroup(playerID);
            } else {
                setScore(playerID, score);
                removeAllGroups(playerID);
                instance.getPerms().playerAddGroup(null, instance.getServer().getOfflinePlayer(playerID), highestRank);
                fixDefaultGroup(playerID);
            }
        }
    }

    /**
     * Get the name of the rank that has the highest score value.
     *
     * @param playerID
     * @return null if no valid score ranks were found.
     */
    public String getHighestRankFromPex(UUID playerID) {
        String[] groups = instance.getPerms().getPlayerGroups(null, instance.getServer().getOfflinePlayer(playerID));
        int highestScore = -1;
        String highestRank = null;
        for (String rankName : groups) {
            int tempScore = getScoreOfRank(rankName);
            if (tempScore != 0) {
                if (tempScore > highestScore) {
                    highestScore = tempScore;
                    highestRank = rankName;
                }
            }
        }
        return highestRank;
    }

    public void removeAllGroups(UUID playerID) {
        removeAllGroups(instance.getServer().getOfflinePlayer(playerID));
    }

    /**
     * After removing all ranks the player will always be in the default group.
     * If the player did get another rank because of his score, he would still be in his default group AND the donor rank.
     * This removes that default group.
     * @param playerID
     */
    public void fixDefaultGroup(UUID playerID){
        String highestRank = getHighestRankFromPex(playerID);
        if ( highestRank == null){
            instance.getPerms().playerAddGroup(null,instance.getServer().getOfflinePlayer(playerID),"default");
        }else if ( !highestRank.equalsIgnoreCase("default")){
            instance.getPerms().playerRemoveGroup(null,instance.getServer().getOfflinePlayer(playerID),"default");
        }
    }

    public void removeAllGroups(OfflinePlayer player) {
        //World name is null so all worlds are affected.
        instance.getPerms().playerAddGroup(null,player,"default");
        instance.getPerms().playerRemoveGroup(null, player, "Premium");
        instance.getPerms().playerRemoveGroup(null, player, "Sponsor");
        instance.getPerms().playerRemoveGroup(null, player, "VIP");
        instance.getPerms().playerRemoveGroup(null, player, "MVP");
        instance.getPerms().playerRemoveGroup(null, player, "Executive");
        instance.getPerms().playerRemoveGroup(null, player, "Legend");
        instance.getPerms().playerRemoveGroup(null, player, "GOD");
        instance.getPerms().playerRemoveGroup(null, player, "Cadian");
    }

    public String getRank(int score) {
        if (score < 50) {
            return "default";
        } else if (score < 100) {
            return "Premium";
        } else if (score < 200) {
            return "Sponsor";
        } else if (score < 350) {
            return "VIP";
        } else if (score < 500) {
            return "MVP";
        } else if (score < 800) {
            return "Executive";
        } else if (score < 1050) {
            return "Legend";
        } else if (score < 2500) {
            return "GOD";
        } else if (score >= 2500) {
            return "Cadian";
        } else {
            return "default";
        }
    }

    //If the rank is not a normal rank that has score (eg. if it is a staff rank), it will return 0
    public int getScoreOfRank(String rankName) {
        if (rankName.equalsIgnoreCase("default")) {
            return 1;
        } else if (rankName.equalsIgnoreCase("Premium")) {
            return 50;
        } else if (rankName.equalsIgnoreCase("Sponsor")) {
            return 100;
        } else if (rankName.equalsIgnoreCase("VIP")) {
            return 200;
        } else if (rankName.equalsIgnoreCase("MVP")) {
            return 350;
        } else if (rankName.equalsIgnoreCase("Executive")) {
            return 500;
        } else if (rankName.equalsIgnoreCase("Legend")) {
            return 800;
        } else if (rankName.equalsIgnoreCase("GOD")) {
            return 1050;
        } else if (rankName.equalsIgnoreCase("Cadian")) {
            return 2500;
        } else {
            return 0;
        }
    }

    private void initTable() {
        try {
            PreparedStatement getTable = Database.getConnection().prepareStatement(
                    "SELECT * FROM rankscore LIMIT 1"
            );
            try {
                ResultSet resultGet = getTable.executeQuery();
            } catch (SQLException e) {
                if (e.getErrorCode() == 1146) {
                    PreparedStatement createTable = Database.getConnection().prepareStatement(
                            "CREATE TABLE rankscore( uuid CHAR(36) NOT NULL, score INTEGER default '0', PRIMARY KEY(uuid))"
                    );
                    createTable.execute();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            instance.getPluginLoader().disablePlugin(instance);
        }
    }

    public void initPlayer(UUID playerID) {
        try {
            PreparedStatement getKills = Database.getConnection()
                    .prepareStatement(
                            "SELECT score FROM rankscore WHERE uuid='"
                                    + playerID.toString() + "'"
                    );
            ResultSet result = getKills.executeQuery();
            if (!result.next()) {
                PreparedStatement addPlayer = Database.getConnection()
                        .prepareStatement(
                                "INSERT INTO rankscore VALUES('"
                                        + playerID.toString() + "', '0')"
                        );
                addPlayer.execute();
                CadiaRanks.getInstance().getLogger().info(
                        "Player " + playerID.toString()
                                + " added to the MySQL rankscore Table"
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setScore(UUID playerID, int kills) {
        try {
            PreparedStatement setKills = Database.getConnection()
                    .prepareStatement(
                            "UPDATE rankscore SET score=" + kills
                                    + " WHERE uuid='" + playerID.toString()
                                    + "'"
                    );
            setKills.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getScore(UUID playerID) {
        int kills = 0;
        try {
            PreparedStatement getKills = Database.getConnection()
                    .prepareStatement(
                            "SELECT score FROM rankscore WHERE uuid='"
                                    + playerID.toString() + "'"
                    );
            ResultSet result = getKills.executeQuery();
            if ( result.next() ) {
                kills = result.getInt("score");
            }else{
                return 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return kills;
    }
}
