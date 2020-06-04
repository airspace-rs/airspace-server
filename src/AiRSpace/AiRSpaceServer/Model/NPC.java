package AiRSpace.AiRSpaceServer.Model;

import AiRSpace.AiRSpaceServer.Exceptions.NPCNotFound;
import AiRSpace.AiRSpaceServer.Utility.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class NPC
{
    // TODO? Define default values here in case someone spawns an NPC that doesn't exist
    private Integer id;
    private String name;
    private Integer combat;
    private Integer health;

    public static NPC read(int id) throws NPCNotFound
    {
        // Get the connection
        Connection connection = Database.getInstance();
        // Create an empty instance
        NPC instance = new NPC();
        try {
            // Prepare a statement
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM npc WHERE id = ? LIMIT 1;");
            // Set the id of the npc we want to lookup
            statement.setInt(1, id);
            // Execute the statement
            statement.execute();
            // Grab all the results
            ResultSet results = statement.getResultSet();
            // Go to the last row
            results.last();
            // Is the last row 0?
            if (results.getRow() < 1) {
                // Throw a wobbly
                throw new NPCNotFound("An NPC with id '" + id + "' could not be found");
            }
            // Grab the first row
            results.first();
            // Store all the info about the NPC
            instance.id = results.getInt("id");
            instance.name = results.getString("name");
            instance.combat = results.getInt("combat");
            instance.health = results.getInt("health");
        } catch (SQLException sqle) {
            // SQL didn't work
            System.out.println("Something failed when looking for an NPC");
            // TODO Throw an exception here to we can gracefully handle this
            System.exit(1);
        }

        // Return the NPC
        return instance;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getCombat() {
        return combat;
    }

    public Integer getHealth() {
        return health;
    }
}
