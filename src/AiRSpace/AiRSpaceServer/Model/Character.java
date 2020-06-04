package AiRSpace.AiRSpaceServer.Model;

import AiRSpace.AiRSpaceServer.Exceptions.BadLogin;
import AiRSpace.AiRSpaceServer.Exceptions.CharacterBanned;
import AiRSpace.AiRSpaceServer.Exceptions.CharacterNotFound;
import AiRSpace.AiRSpaceServer.Utility.BCrypt;
import AiRSpace.AiRSpaceServer.Utility.Database;

import java.sql.*;

public class Character
{
    private Integer id = null;
    private String username = null;
    private String password = null;
    private Integer x = null;
    private Integer y = null;
    private Integer banned = null;
    private Integer height = null;
    private Integer rights = null;
    private Integer member = null;
    private String last_connection = null;
    private String last_login = null;
    private Integer energy = null;
    private Integer game_time = null;
    private Integer game_count = null;
    private Integer look_0 = null;
    private Integer look_1 = null;
    private Integer look_2 = null;
    private Integer look_3 = null;
    private Integer look_4 = null;
    private Integer look_5 = null;

    public static Character read(int id) throws CharacterNotFound
    {
        // Create an empty instance of character
        Character instance = new Character();

        // Get the connection
        Connection connection = Database.getInstance();
        try {
            // Prepare a statement, no SQL injection here mother fuckers
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM `users` WHERE `id` = ? LIMIT 1;");
            // Set the id that we want to look up on
            statement.setInt(1, id);
            // Execute the statement
            statement.execute();
            // Pull the results out
            ResultSet results = statement.getResultSet();
            // Go to the last row
            results.last();
            // Get the row number
            int count = results.getRow();
            // If it's 0, there are no rows, so the character doesn't exist
            if (count < 1) {
                // Bitch and moan about it
                throw new CharacterNotFound("Character with ID '" + id + "' doesn't exist.");
            }
            // Otherwise, go to the first row
            results.first();
            // Start putting the data into the object
            instance.id = results.getInt("id");
            instance.username = results.getString("username");
            instance.password = results.getString("password");
            instance.x = results.getInt("x");
            instance.y = results.getInt("y");
            instance.height = results.getInt("height");
            instance.rights = results.getInt("rights");
            instance.banned = results.getInt("banned");
            instance.member = results.getInt("member");
            instance.last_connection = results.getString("last_connection");
            instance.last_login = results.getString("last_login");
            instance.energy = results.getInt("energy");
            instance.game_time = results.getInt("game_time");
            instance.game_count = results.getInt("game_count");
            instance.look_0 = results.getInt("look_0");
            instance.look_1 = results.getInt("look_1");
            instance.look_2 = results.getInt("look_2");
            instance.look_3 = results.getInt("look_3");
            instance.look_4 = results.getInt("look_4");
            instance.look_5 = results.getInt("look_5");
        } catch (SQLException sqle) {
            // If the SQL is malformed, fall over here
            System.out.println(sqle.getMessage());
            System.exit(1);
        }

        return instance;
    }

    public static Character readUsingCredentials(String username, String password) throws CharacterNotFound, CharacterBanned, BadLogin
    {
        // Create a holder
        Character character = null;

        // Get the connection
        Connection connection = Database.getInstance();
        try {
            // Prepare a statement
            PreparedStatement statement = connection.prepareStatement("SELECT `id` FROM `users` WHERE `username` = ? LIMIT 1;");
            // Set the username we're looking up on
            statement.setString(1, username);
            // Execute the statement
            statement.execute();
            // Get the results
            ResultSet results = statement.getResultSet();
            // Go to the last row
            results.last();
            // Get the row number
            int count = results.getRow();
            // Row number is 0 so there isn't a user with that name
            if (count < 1) {
                // User basically logging in with the wrong username. Bubble this exception up so it can be handled later
                throw new CharacterNotFound("Character with username '" + username + "' doesn't exist.");
            }
            // Go to the first row
            results.first();

            // Read the character by it's id
            character = read(results.getInt("id"));

            // Is the character banned?
            if (character.isBanned()) {
                // Bubble the exception up... further up the code, tell them to get lost
                throw new CharacterBanned("This character has been banned.");
            }

            // Check if the password they gave us matches the hash we stored
            // I'm using bcrypt because I want to create a PHP control panel for this later and bcrypt is
            // best practice over in that land of superior programming languages.
            if (!BCrypt.checkpw(password, character.getPassword())) {
                throw new BadLogin("Bad username/password combination");
            }

            // Return the object
            return character;
        } catch (SQLException sqle) {
            // If the SQL is malformed, throw a wobbly and kill the game
            System.out.println(sqle.getMessage());
            // TODO Probably want to throw an exception here so we can save games before everyone is booted
            System.exit(1);
        }

        return character;
    }

    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        // We have to do this because the people at bcrypt fucked up
        // When PHP did it, changing the prefix was stupid. But when *they* did it...
        return password.replace("$2y", "$2a");
    }

    public boolean isAdmin()
    {
        if (rights >= 2) {
            return true;
        }
        return false;
    }

    public boolean isMod()
    {
        // If you are an admin you are also a mod
        if (rights >= 1) {
            return true;
        }
        return false;
    }

    public boolean isBanned()
    {
        if (banned >= 1) {
            return true;
        }
        return false;
    }

    public Integer getId() {
        return id;
    }

    public Integer getX() {
        return x;
    }

    public Integer getY() {
        return y;
    }

    public Integer getHeight() {
        return height;
    }

    public boolean isMember() {
        if (member >= 1) {
            return true;
        }
        return false;
    }

    public String getLastConnection() {
        return last_connection;
    }

    public String getLastLogin() {
        return last_login;
    }

    public Integer getEnergy() {
        return energy;
    }

    public Integer getGameTime() {
        return game_time;
    }

    public Integer getGameCount() {
        return game_count;
    }

    public Integer getLook0() {
        return look_0;
    }

    public Integer getLook1() {
        return look_1;
    }

    public Integer getLook2() {
        return look_2;
    }

    public Integer getLook3() {
        return look_3;
    }

    public Integer getLook4() {
        return look_4;
    }

    public Integer getLook5() {
        return look_5;
    }
}
