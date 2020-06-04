package AiRSpace.AiRSpaceServer.Utility;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Setting
{
    private static boolean loaded = false;
    private static Map<String, String> settings = new HashMap<String, String>();

    public static String get(String key)
    {
        // We load the whole file the first time Setting.get is called
        // If it hasn't been loaded, let's do that
        if (!loaded) {
            // Read the file into a buffer
            try (BufferedReader br = new BufferedReader(new FileReader("settings.rsb"))) {
                String line;
                // While there are still lines, read them
                while ((line = br.readLine()) != null) {
                    // If the line starts with a #, it's a comment
                    if (line.startsWith("#")) {
                        continue;
                    }
                    // If the line is empty, just do nothing
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    // Store everything before the first space as a key
                    String keyy = line.substring(0, line.indexOf(" "));
                    // Store everything after the first space as the value
                    String value = line.substring(line.indexOf(" ") + 1);
                    // Put it in the hashmap
                    settings.put(keyy, value);
                }
                loaded = true;
            } catch (IOException ioe) {
                // If we can't read the settings file, what's the point?
                System.out.println("Could not open file reading.");
                System.exit(1);
            }
        }

        // Do we have a setting?
        if (!settings.containsKey(key)) {
            // TODO We probably want to throw an exception here
            return "Missing Value";
        }

        // Return the setting
        return settings.get(key);
    }

    public static int getMaxPlayers()
    {
        // Max players is required as an int, so let's parse it
        return Integer.parseInt(get("MaxPlayers"));
    }

    public static int getPort()
    {
        // getPort is required as an int, so let's parse it
        return Integer.parseInt(get("ServerPort"));
    }
}
