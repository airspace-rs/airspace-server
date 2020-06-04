package AiRSpace.AiRSpaceServer;

import AiRSpace.AiRSpaceServer.Exceptions.*;
import AiRSpace.AiRSpaceServer.Model.Character;
import AiRSpace.AiRSpaceServer.Handlers.ClientHandler;
import AiRSpace.AiRSpaceServer.Legacy.IsaacCipher;
import AiRSpace.AiRSpaceServer.Legacy.Stream;
import AiRSpace.AiRSpaceServer.Utility.PacketBuilder;
import AiRSpace.AiRSpaceServer.Utility.Helper;
import AiRSpace.AiRSpaceServer.Utility.Setting;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

public class Client
{
    // Login types
    // TODO, I think this is a packet id so we might be able to merge the whole logging in process with generic packet handling
    private static final int LOGIN_TYPE_REQUEST = 14;
    private static final int LOGIN_TYPE_NEW = 16;
    private static final int LOGIN_TYPE_RECONNECT = 18;

    // Return codes
    private static final int RETURN_CODE_DO_KEY_EXCHANGE = 0;
    private static final int RETURN_CODE_WAIT_2_SECONDS = 1;
    private static final int RETURN_CODE_SUCCESS = 2;
    private static final int RETURN_CODE_INVALID_USERNAME_PASSWORD = 3;
    private static final int RETURN_CODE_ACCOUNT_DISABLED = 4;
    private static final int RETURN_CODE_ALREADY_ONLINE = 5;
    private static final int RETURN_CODE_GAME_UPDATED = 6;
    private static final int RETURN_CODE_WORLD_FULL = 7;
    private static final int RETURN_CODE_LOGIN_SERVER_OFFLINE = 8;
    private static final int RETURN_CODE_LOGIN_LIMIT_EXCEEDED = 9; // If you connect from the same address too many times
    private static final int RETURN_CODE_INVALID_SESSION_ID = 10;
    private static final int RETURN_CODE_SESSION_REJECTED = 11;
    private static final int RETURN_CODE_MEMBERS_ONLY = 12;
    private static final int RETURN_CODE_COULD_NOT_COMPLETE_LOGIN = 13;
    private static final int RETURN_CODE_UPDATING = 14;
    private static final int RETURN_CODE_LOGIN_ATTEMPTS_EXCEEDED = 16; // You've tried to log in too much, prevent brute forcing
    private static final int RETURN_CODE_MEMBERS_ONLY_AREA = 17;
    private static final int RETURN_CODE_INVALID_LOGIN_SERVER = 20;
    private static final int RETURN_CODE_ONLY_JUST_LEFT_ANOTHER_WORLD = 21;

    // List of packet sizes. Lifted from Moparscape
    // TODO I'm not 100% but there's almost certainly a nicer way to store these. This is an eyesore and a code smell.
    public static final int PACKET_SIZES[] = {
        0, 0, 0, 1, -1, 0, 0, 0, 0, 0, //0
        0, 0, 0, 0, 8, 0, 6, 2, 2, 0,  //10
        0, 2, 0, 6, 0, 12, 0, 0, 0, 0, //20
        0, 0, 0, 0, 0, 8, 4, 0, 0, 2,  //30
        2, 6, 0, 6, 0, -1, 0, 0, 0, 0, //40
        0, 0, 0, 12, 0, 0, 0, 0, 8, 0, //50
        0, 8, 0, 0, 0, 0, 0, 0, 0, 0,  //60
        6, 0, 2, 2, 8, 6, 0, -1, 0, 6, //70
        0, 0, 0, 0, 0, 1, 4, 6, 0, 0,  //80
        0, 0, 0, 0, 0, 3, 0, 0, -1, 0, //90
        0, 13, 0, -1, 0, 0, 0, 0, 0, 0,//100
        0, 0, 0, 0, 0, 0, 0, 6, 0, 0,  //110
        1, 0, 6, 0, 0, 0, -1, 0, 2, 6, //120
        0, 4, 6, 8, 0, 6, 0, 0, 0, 2,  //130
        0, 0, 0, 0, 0, 6, 0, 0, 0, 0,  //140
        0, 0, 1, 2, 0, 2, 6, 0, 0, 0,  //150
        0, 0, 0, 0, -1, -1, 0, 0, 0, 0,//160
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  //170
        0, 8, 0, 3, 0, 2, 0, 0, 8, 1,  //180
        0, 0, 12, 0, 0, 0, 0, 0, 0, 0, //190
        2, 0, 0, 0, 0, 0, 0, 0, 4, 0,  //200
        4, 0, 0, 0, 7, 8, 0, 0, 10, 0, //210
        0, 0, 0, 0, 0, 0, -1, 0, 6, 0, //220
        1, 0, 0, 0, 6, 0, 6, 8, 1, 0,  //230
        0, 4, 0, 0, 0, 0, -1, 0, -1, 4,//240
        0, 0, 6, 6, 0, 0, 0            //250
    };

    public Socket socket = null; // Variable for socket

    // IO stuff; I sleep well not knowing precisely how this works
    private InputStream in = null;
    private OutputStream out = null;
    private Stream inStream = null;
    private Stream outStream = null;

    // Encryption/decryption for the above
    private IsaacCipher inStreamDecryption = null;
    private IsaacCipher outStreamDecryption = null;

    private int bufferSize = 5000; // Amount of bytes in the buffer
    private int packetType = -1; // Latest packet type
    private int packetSize = 0; // Latest packet size
    public byte[] buffer; // All the data
    public int readPointer, writePointer; // When we've read up to and where we've written up to

    private int slot = 0; // The slot the client occupying
    private boolean isLowMemory = false;
    public Character character = null; // The character they're logged in as
    public boolean initialised = false; // Have they complete the initialisation?
    private boolean appearanceUpdateRequired = true; // Has something about the characters physical appearance changed?

    public Client(Socket s, int nextSlot)
    {
        socket = s;
        slot = nextSlot;
    }

    // This is entry point for a new connection from the client
    // TODO Break this up into individually handled packets
    public void run()
    {
        // Tell the console we've got a bite
        System.out.println("Connection accepted from " + socket.getRemoteSocketAddress().toString() + " in slot " + slot);

        // Create new streams with the buffer size
        inStream = new Stream(new byte[bufferSize]);
        inStream.currentOffset = 0;
        outStream = new Stream(new byte[bufferSize]);
        outStream.currentOffset = 0;

        buffer = new byte[bufferSize];

        try {
            // Get I/O streams
            in = socket.getInputStream();
            out = socket.getOutputStream();
        } catch (IOException ioe) {
            // Not even sure what'd cause this
            System.out.println("Could not get IO stream.");
        }

        // Create server/client keys for encryption
        long serverSessionKey = 0;
        long clientSessionKey = 0;

        // Let's generate a session key for encryption
        serverSessionKey = ((long)(java.lang.Math.random() * 99999999D) << 32) + (long) (java.lang.Math.random() * 99999999D);

        try {
            // Get the first 2 bytes from the data coming in
            // TODO Determine if we just remove this as it's blocking
            fillInStream(2);

            // If the first byte isn't 14, what the are they trying to do exactly?
            if (inStream.readUnsignedByte() != LOGIN_TYPE_REQUEST) {
                System.out.println("Expected login packet ID of 14.");
                // We don't wanna deal with them
                ClientHandler.kick(slot);
                return;
            }

            // I *think* this is a hash of part of the username...
            // Rumor has it this was used to "randomly" pick a world to connect to
            // I think that's bullshit.
            // Either way, it's not used
            int namePart = inStream.readUnsignedByte();

            // We spew back a load of junk, but the client loves this shit
            for (int i = 0; i < 8; i++) {
                out.write(0);
            }

            // Tell the client we want to do a key exchange
            out.write(RETURN_CODE_DO_KEY_EXCHANGE);

            // Send the server half of the session key
            outStream.writeQWord(serverSessionKey);

            // Flush what we've written to the client thus far
            directFlushOutStream();

            // Grab the next 2 bytes
            // TODO Determine if we just remove this as it's blocking
            fillInStream(2);

            // The first byte is the login type
            int loginType = inStream.readUnsignedByte();

            // We basically handle them both the same
            if (loginType != LOGIN_TYPE_NEW && loginType != LOGIN_TYPE_RECONNECT) {
                System.out.println("Unexpected login type " + loginType);
                ClientHandler.kick(slot);
                return;
            }

            // Grab the packet size
            int loginPacketSize = inStream.readUnsignedByte();

            // The client inflates by 40 for some reason
            int adjustedPacketSize = loginPacketSize - (36 + 1 + 1 + 2);

            // After it's decrypted is it 0? (I think)
            if (adjustedPacketSize <= 0) {
                System.out.println("Cannot have a 0 packet size.");
                ClientHandler.kick(slot);
                return;
            }

            // Get ready to send it back
            fillInStream(loginPacketSize);

            // Require the magic number to be 255
            int magicNumber = inStream.readUnsignedByte();
            if (magicNumber != 255) {
                ClientHandler.kick(slot);
                return;
            }

            // Require the client version to be 317
            int clientVersion = inStream.readUnsignedWord();
            if (clientVersion != 317) {
                ClientHandler.kick(slot);
                return;
            }

            // Are they running low memory?
            isLowMemory = inStream.readUnsignedByte() == 1;

            // We fire back some "junk", but apparently these are do with CRC values or something
            for (int i = 0; i < 9; i++) {
                String junk = Integer.toHexString(inStream.readDWord());
            }

            adjustedPacketSize--;

            // Grab the next byte
            int remainingPacket = inStream.readUnsignedByte();

            // If that value isn't equal to the packet size tell them to go away
            if (remainingPacket != adjustedPacketSize) {
                System.out.println("Encrypted packet data is not what they said it would be.");
                ClientHandler.kick(slot);
                return;
            }

            int connectionStatus = inStream.readUnsignedByte();
            if (connectionStatus != 10) {
                System.out.println("Encrypted packet data is not what they said it would be.");
                ClientHandler.kick(slot);
                return;
            }

            // Right. Receive the client session key and the server session key back
            clientSessionKey = inStream.readQWord();
            serverSessionKey = inStream.readQWord();

            // The uid that's in the signlink
            int uid = inStream.readDWord();

            // Read the username and password
            String username = inStream.readString();
            String password = inStream.readString();

            // This seems to be the server ID but we don't use it...
            try {
                String server = inStream.readString();
            } catch (Exception e) {
                String server = "default.com";
                // TODO Do something with this
            }

            // Okay, format the usernames
            username = username.toLowerCase();
            username = username.replaceAll(" ", "_");
            username = username.replaceAll("'", "");

            // Store the session keys for encrypting and decrypting
            int sessionKey[] = new int[4];
            sessionKey[0] = (int) (clientSessionKey >> 32);
            sessionKey[1] = (int) clientSessionKey;
            sessionKey[2] = (int) (serverSessionKey >> 32);
            sessionKey[3] = (int) serverSessionKey;
            inStreamDecryption = new IsaacCipher(sessionKey);

            // Add 50 because that's the client does - don't change or everything breaks
            for (int i = 0; i < 4; i++) {
                sessionKey[i] += 50;
            }

            outStreamDecryption = new IsaacCipher(sessionKey);
            outStream.packetEncryption = outStreamDecryption;

            // Let's get and send back a response code
            try {
                // Try to log in
                login(username, password);
            } catch (AlreadyLoggedIn ali) {
                // They're already logged in, get rid of them
                out.write(RETURN_CODE_ALREADY_ONLINE);
                ClientHandler.kick(slot);
                return;
            } catch (BadLogin bl) {
                // Bad username password, get rid of them
                out.write(RETURN_CODE_INVALID_USERNAME_PASSWORD);
                ClientHandler.kick(slot);
                return;
            } catch (CharacterBanned cb) {
                // They were banned, get rid of them
                out.write(RETURN_CODE_ACCOUNT_DISABLED);
                ClientHandler.kick(slot);
                return;
            } catch (CharacterNotFound cnf) {
                // Character doesn't exist, get rid of them
                out.write(RETURN_CODE_INVALID_USERNAME_PASSWORD);
                ClientHandler.kick(slot);
                return;
            }

            // Oh, it went well? Send them a response.
            out.write(RETURN_CODE_SUCCESS);

            // Tell them if they have privilege
            int player_rights = 0;
            if (character.isAdmin()) {
                player_rights = 2;
            } else if (character.isMod()) {
                player_rights = 1;
            }
            out.write(player_rights);

            // If they're suspicious, set this to 1. It means information about their actions will be sent
            // to the server. As this is RSPS, we might not care and can probably do without the extra packet
            // handling to be honest. Maybe review it later.
            out.write(0);

            directFlushOutStream();

        } catch (IOException ioe) {
            // If anything weird happens, just kick them
            System.out.println("Unknown error");
            ClientHandler.kick(slot);
        }
    }

    public void fillInStream(int length) throws IOException
    {
        inStream.currentOffset = 0;
        in.read(inStream.buffer, 0, length);
    }

    public void flushOutStream()
    {
        if (outStream.currentOffset == 0) return;

        synchronized (this) {
            int maxWritePtr = (readPointer + bufferSize - 2) % bufferSize;
            for (int i = 0; i < outStream.currentOffset; i++) {
                buffer[writePointer] = outStream.buffer[i];
                writePointer = (writePointer + 1) % bufferSize;
                if (writePointer == maxWritePtr) {
                    // TODO Gracefully handle (kick the client)
                    System.out.println("Buffer overflow");
                    System.exit(1);
                    outStream.currentOffset = 0;
                }
            }
            outStream.currentOffset = 0;

            notify();
        }
    }

    public void directFlushOutStream() throws IOException
    {
        out.write(outStream.buffer, 0, outStream.currentOffset);
        outStream.currentOffset = 0; // reset
    }

    public void process()
    {
        // This is where we could handle all the clients changes

        // The client sends 5 packets at a time, so we'll take 5 to stop them queueing up
        for (int i = 0; i < 5; i++) {
            captureNextPacket();
        }
    }

    public void login(String username, String password) throws AlreadyLoggedIn, BadLogin, CharacterNotFound, CharacterBanned
    {
        System.out.println("Logging in with " + username + " " + password);
        // See if the user is already logged in
        int i = ClientHandler.getClientByUsername(username);
        if (i > -1) {
            // They are, which means they didn't lo0g out properly.
            throw new AlreadyLoggedIn("That character is already logged in");
        }
        // Otherwise set this client's character.
        character = Character.readUsingCredentials(username, password);
    }

    private void logout()
    {
        PacketBuilder.logout(outStream);
        ClientHandler.kick(slot);
    }

    public boolean isLoggedIn()
    {
        // Basically, if we've assigned a character to this client, they're logged in
        if (character == null) {
            return false;
        }

        return true;
    }

    private void captureNextPacket()
    {
        try {
            if (in == null) {
                return;
            }

            int availablePacketCount = in.available();
            if (availablePacketCount == 0) {
                return;
            }

            // No currently handled packet
            if (packetType == -1) {
                packetType = in.read() & 0xff;
                if (inStreamDecryption != null) {
                    packetType = packetType - inStreamDecryption.getNextKey() & 0xff;
                }
                packetSize = PACKET_SIZES[packetType];
                availablePacketCount--;
            }

            // Variable packet size
            if (packetSize == -1) {
                if (availablePacketCount > 0) {
                    // this is a variable size packet, the next byte containing the length of said
                    packetSize = in.read() & 0xff;
                    availablePacketCount--;
                } else {
                    return;
                }
            }

            // Packet is incomplete
            if (availablePacketCount < packetSize) {
                return;
            }

            fillInStream(packetSize);
            parseCurrentPacket();
            packetType = -1;

        } catch (IOException ioe) {
            System.out.println("Exception reading packet in");
        }
    }

    public static final int PACKET_TYPE_IDLE = 0;
    public static final int PACKET_TYPE_CAMERA_MOVED = 86;
    public static final int PACKET_TYPE_UPDATE_CHAT_OPTIONS = 95;
    public static final int PACKET_TYPE_WALK = 164;
    public static final int PACKET_TYPE_GUI_BUTTON_CLICK = 185;
    public static final int PACKET_TYPE_CLICK = 241;

    private void parseCurrentPacket()
    {
        // Todo add proper packet router

        switch (packetType) {
            case PACKET_TYPE_IDLE:
                // Idle timer
                break;

            case PACKET_TYPE_CAMERA_MOVED:

                break;

            case PACKET_TYPE_GUI_BUTTON_CLICK:
                int actionButton = Helper.hexToInt(inStream.buffer, 0, packetSize);

                switch (actionButton) {
                    case 9154:
                        System.out.println("Received logout instruction");
                        logout();
                        break;
                    default:
                        System.out.println("Button " + actionButton + " was clicked.");
                        break;
                }
                break;

            case PACKET_TYPE_CLICK:

                break;

            default:
                System.out.println("Unhandled packet id '" + packetType + "'.");
                break;
        }
    }

    public void initialise()
    {
        // Send membership and player id first
        PacketBuilder.initialisePlayer(
            outStream,
            character.isMember() ? 1 : 0,
            slot
        );

        PacketBuilder.setChatOptions(
            outStream,
            2,
            2,
           2
        );

        for (int i = 0; i < 25; i++) {
            if (i == 3) { // HP
                PacketBuilder.setSkillLevel(
                    outStream,
                    i,
                    10,
                    1154
                );
            } else {
                PacketBuilder.setSkillLevel(
                    outStream,
                    i,
                    1,
                    1
                );
            }
        }

        PacketBuilder.resetCamera(outStream);
        PacketBuilder.resetSidebar(outStream);

        // If wilderness, attack?
        PacketBuilder.addRightClickCommand(outStream, 1, 0, "Trade with");
        PacketBuilder.addRightClickCommand(outStream, 2, 0, "Follow");
        // If admin, kick, mute, ban?

        PacketBuilder.openWelcomeScreen(
            outStream,
            0,
            0,
            false,
            0,
            0
        );

        String serverName = Setting.get("Name");

        PacketBuilder.sendGlobalMessage(
            outStream,
            "Welcome to " + serverName + "!"
        );

        PacketBuilder.setPlayerEnergy(outStream, 100);

        PacketBuilder.assignInterfaceText(outStream, serverName + ", always use the", 2451);
        flushOutStream();

        PacketBuilder.assignInterfaceText(outStream, "Welcome to " + serverName, 6067);
        flushOutStream();

        PacketBuilder.assignInterfaceText(outStream, serverName + " staff will NEVER email you. We use the", 6071);
        flushOutStream();

        // Assume unarmed for now
        PacketBuilder.setWeaponInterface(outStream, -1, 5855);
        PacketBuilder.assignInterfaceText(outStream, "Unarmed", 5857);
        flushOutStream();

        updatePlayer();

        setPlayerEquipment();

//        resetInventory();
//        resetBank();
//        flushOutStream();

//         Lastly flag as initialised
        initialised = true;
    }

    public void updatePlayer()
    {
        // TODO If system update running

        updatePlayerMovement();
//        updateNPCs();
//        flushOutStream();
    }

    public void updatePlayerMovement()
    {
        // Set location (We only need to do this if the region changed)
        outStream.createFrame(73);
        outStream.writeWordA((character.getX() / 8) + 6);
        outStream.writeWord((character.getY() / 8) + 6);
        flushOutStream();

        // Tell the client that we teleported
        outStream.createFrameVarSizeWord(PacketBuilder.PACKET_TYPE_UPDATE_LOCAL_PLAYER);
        outStream.initBitAccess();
        outStream.writeBits(1, 1); // Currently updating our player
        outStream.writeBits(2, 3); // Update type
        outStream.writeBits(2, 0); // Player height
        outStream.writeBits(1, 1); // set to true, if discarding (client-side) walking queue
        outStream.writeBits(1, (appearanceUpdateRequired ? 1 : 0)); // Update required?
        outStream.writeBits(7, ((character.getY() % 8) + 8)); // Player's localY
        outStream.writeBits(7, ((character.getX() % 8) + 8)); // Player's localX

        outStream.writeBits(8, 0); // Number of other players to update

        outStream.writeBits(11, 2047);
        outStream.finishBitAccess();

        updatePlayerAppearance(outStream);
        outStream.endFrameVarSizeWord();
    }

    public void updatePlayerAppearance(Stream stream)
    {
        int updateMask = 0;
        /*
         * Here we can handle:
         *  - Overhead chat 0x80
         *  - Appearance 0x10
         *  - Damage Splash 0x20
         *  - Direction updates 0x40
         *
         * For now, we're just handling the appearance
          */

        if (appearanceUpdateRequired) {
            updateMask |= 0x10;
        }

        if (appearanceUpdateRequired) {
            stream.writeByte(updateMask);
            Stream tempStream = new Stream(new byte[100]);
            tempStream.writeByte(0); // 0 for male, 1 for female
            tempStream.writeByte(0); // Player status mask (for prayers, wildy skull)

            // If character is not pretending to be an NPC
            tempStream.writeByte(0); // Hat
            tempStream.writeByte(0); // Cape
            tempStream.writeByte(0); // Amulet
            tempStream.writeByte(0); // Weapon
            tempStream.writeWord(0x100 + 15); // Torso
            tempStream.writeByte(0); // Shield
            tempStream.writeWord(0x100 + 29); // Arms (0 if platebody)
            tempStream.writeWord(0x100 + 39); // Legs
            tempStream.writeWord(0x100 + 7); // Head (0 if it's a full-helm/mask for example)
            tempStream.writeWord(0x100 + 35); // Hands
            tempStream.writeWord(0x100 + 44); // Feet
            tempStream.writeByte(0); // Not sure?!
            // Else
//            tempStream.writeWord(-1);
//            tempStream.writeWord(7);

            // Update colours of shit and stuff
            tempStream.writeByte(7);
            tempStream.writeByte(8);
            tempStream.writeByte(9);
            tempStream.writeByte(5);
            tempStream.writeByte(0);

            tempStream.writeWord(0x328); // Standing animation
            tempStream.writeWord(0x337); // Standing turn animation
            tempStream.writeWord(0x333); // Walking animation
            tempStream.writeWord(0x334); // 180 turn animation
            tempStream.writeWord(0x335); // 90 clockwise turn animation
            tempStream.writeWord(0x336); // 90 counter-clockwise turn animation
            tempStream.writeWord(0x338); // Run animation

            // Convert name to 64-bit integer
            String name = character.getUsername();
            long l = 0L;
            for (int i = 0; i < name.length() && i < 12; i++) {
                char c = name.charAt(i);
                l *= 37L;
                if (c >= 'A' && c <= 'Z') l += (1 + c) - 65;
                else if (c >= 'a' && c <= 'z') l += (1 + c) - 97;
                else if (c >= '0' && c <= '9') l += (27 + c) - 48;
            }
            while (l % 37L == 0L && l != 0L) l /= 37L;

            tempStream.writeQWord(l); // Send player name
            tempStream.writeByte(3); // Combat level
            tempStream.writeWord(0); // Skill level (in certain areas, I think)

            stream.writeByteC(tempStream.currentOffset);
            stream.writeBytes(tempStream.buffer, tempStream.currentOffset, 0);
        }

        appearanceUpdateRequired = false;
    }

    public void updateNPCs()
    {
        PacketBuilder.updateNPCs(outStream);
    }

    public void setPlayerEquipment()
    {
        PacketBuilder.setEquipmentSlot(outStream, -1, 0, 0);
        PacketBuilder.setEquipmentSlot(outStream, -1, 0, 1);
        PacketBuilder.setEquipmentSlot(outStream, -1, 0, 2);
        PacketBuilder.setEquipmentSlot(outStream, -1, 0, 3);
        PacketBuilder.setEquipmentSlot(outStream, -1, 0, 4);
        PacketBuilder.setEquipmentSlot(outStream, -1, 0, 5);
        PacketBuilder.setEquipmentSlot(outStream, -1, 0, 7);
        PacketBuilder.setEquipmentSlot(outStream, -1, 0, 9);
        PacketBuilder.setEquipmentSlot(outStream, -1, 0, 10);
        PacketBuilder.setEquipmentSlot(outStream, -1, 0, 12);
        PacketBuilder.setEquipmentSlot(outStream, -1, 0, 13);
    }

    public void resetInventory()
    {
        PacketBuilder.setInventory(outStream);
    }

    public void resetBank()
    {
        PacketBuilder.setBank(outStream);
    }

    public void outputBuffer() throws java.net.SocketException
    {
        // relays any data currently in the buffer
        int numBytesInBuffer, offset;
        synchronized (this) {
            if (writePointer == readPointer) {
//                try {
//                    wait();
//                } catch (java.lang.InterruptedException _ex) {
//                }
            }

            offset = readPointer;
            if (writePointer >= readPointer) {
                numBytesInBuffer = writePointer - readPointer;
            } else {
                numBytesInBuffer = bufferSize - readPointer;
            }
        }
        if (numBytesInBuffer > 0) {
            try {
                out.write(buffer, offset, numBytesInBuffer);
                readPointer = (readPointer + numBytesInBuffer) % bufferSize;
                if (writePointer == readPointer) out.flush();
            } catch (SocketException socketException) {
                ClientHandler.kick(slot);
            } catch (java.lang.Exception __ex) {
                __ex.printStackTrace();
            }
        }
    }
}
