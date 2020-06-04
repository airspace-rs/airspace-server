package AiRSpace.AiRSpaceServer.Utility;

import AiRSpace.AiRSpaceServer.Legacy.Stream;

public class PacketBuilder
{
    private static int PACKET_TYPE_UPDATE_EQUIPMENT_SLOT   = 34;
    private static int PACKET_TYPE_UPDATE_INVENTORY        = 53;
    public static int  PACKET_TYPE_UPDATE_NPCS             = 65;
    public static int  PACKET_TYPE_UPDATE_LOCAL_PLAYER     = 81;
    private static int PACKET_TYPE_ADD_RIGHT_CLICK_COMMAND = 104;
    private static int PACKET_TYPE_RESET_CAMERA            = 107;
    private static int PACKET_TYPE_LOGOUT                  = 109;
    private static int PACKET_TYPE_ASSIGN_INTERFACE_TEXT   = 126;
    private static int PACKET_TYPE_SET_SKILL_LEVEL         = 134;
    private static int PACKET_TYPE_INTERFACE_ENERGY        = 149;
    private static int PACKET_TYPE_OPEN_WELCOME_SCREEN     = 176;
    private static int PACKET_TYPE_SET_CHAT_OPTIONS        = 206;
    private static int PACKET_TYPE_INITIALISE_PLAYER       = 249;
    private static int PACKET_TYPE_SEND_GLOBAL_MESSAGE     = 253;

    public static void addRightClickCommand(Stream stream, int slot, int onTop, String description)
    {
        stream.createFrameVarSize(PACKET_TYPE_ADD_RIGHT_CLICK_COMMAND);
        stream.writeByteC(slot);
        stream.writeUnsignedByte(onTop);
        stream.writeString(description);
        stream.endFrameVarSize();
    }

    public static void assignInterfaceText(Stream stream, String text, int interfaceId)
    {
        stream.createFrameVarSizeWord(PACKET_TYPE_ASSIGN_INTERFACE_TEXT);
        stream.writeString(text);
        stream.writeWordA(interfaceId);
        stream.endFrameVarSizeWord();
    }

    public static void initialisePlayer(Stream stream, int isMember, int slotId)
    {
        stream.createFrame(PACKET_TYPE_INITIALISE_PLAYER);
        stream.writeUnsignedByte(isMember);
        stream.writeWordBigEndianA(slotId);
    }

    public static void logout(Stream stream)
    {
        stream.createFrame(PACKET_TYPE_LOGOUT);
    }

    public static void openWelcomeScreen(Stream stream, int daysSinceRecoveryDetailsChanged, int unreadMessageCount, boolean shouldWarnAboutNonMembersWorld, int lastIPConnection, int daysSinceLastLogin)
    {
        stream.createFrame(PACKET_TYPE_OPEN_WELCOME_SCREEN);
        stream.writeByteC(daysSinceRecoveryDetailsChanged);
        stream.writeWordA(unreadMessageCount);
        stream.writeByte(shouldWarnAboutNonMembersWorld ? 1 : 0);
        stream.writeDWord_v2(lastIPConnection);
        stream.writeWord(daysSinceLastLogin);
    }

    public static void resetCamera(Stream stream)
    {
        stream.createFrame(PACKET_TYPE_RESET_CAMERA);
    }

    public static void resetSidebar(Stream stream)
    {
        // TODO Store interface IDs
        setSidebarInterface(stream, 0, 2423);
        setSidebarInterface(stream, 1, 3917);
        setSidebarInterface(stream, 2, 638);
        setSidebarInterface(stream, 3, 3213);
        setSidebarInterface(stream, 4, 1644);
        setSidebarInterface(stream, 5, 5608);
        setSidebarInterface(stream, 6, 1151);
        setSidebarInterface(stream, 7, 1);
        setSidebarInterface(stream, 8, 5065);
        setSidebarInterface(stream, 9, 5715);
        setSidebarInterface(stream, 10, 2449);
        setSidebarInterface(stream, 11, 4445);
        setSidebarInterface(stream, 12, 147);
        setSidebarInterface(stream, 13, 6299);
    }

    public static void sendGlobalMessage(Stream stream, String message)
    {
        stream.createFrameVarSize(PACKET_TYPE_SEND_GLOBAL_MESSAGE);
        stream.writeString(message);
        stream.endFrameVarSize();
    }

    public static void setBank(Stream stream)
    {
        stream.createFrameVarSizeWord(PACKET_TYPE_UPDATE_INVENTORY);
        stream.writeWord(5382);
        stream.writeWord(350);
        for (int i = 0; i < 350; i++) {
            stream.writeByte(0); // Quantity
            stream.writeWordBigEndianA(0);
        }
        stream.endFrameVarSizeWord();
    }

    public static void setChatOptions(Stream stream, int publicChat, int privateChat, int tradeBlock)
    {
        // 0 = On, Friends = 1, Off = 2, Hide = 3
        stream.createFrame(PACKET_TYPE_SET_CHAT_OPTIONS);
        stream.writeByte(publicChat);
        stream.writeByte(privateChat);
        stream.writeByte(tradeBlock);
    }

    public static void setEquipmentSlot(Stream stream, int itemId, int quantity, int slot)
    {
        stream.createFrameVarSizeWord(PACKET_TYPE_UPDATE_EQUIPMENT_SLOT);
        stream.writeWord(1688);
        stream.writeByte(slot);
        stream.writeWord((itemId + 1));
        if (quantity > 254) {
            stream.writeByte(255);
            stream.writeDWord(quantity);
        } else {
            stream.writeByte(quantity);
        }
        stream.endFrameVarSizeWord();
    }

    public static void setInventory(Stream stream)
    {
        stream.createFrameVarSizeWord(PACKET_TYPE_UPDATE_INVENTORY);
        stream.writeWord(3214);
        stream.writeWord(0);
        stream.endFrameVarSizeWord();
    }

    private static void setSidebarInterface(Stream stream, int menuId, int interfaceId)
    {
        stream.createFrame(71);
        stream.writeWord(interfaceId);
        stream.writeUnsignedByte(menuId);
    }

    public static void setSkillLevel(Stream stream, int skill, int level, int experience)
    {
        stream.createFrame(PACKET_TYPE_SET_SKILL_LEVEL);
        stream.writeByte(skill);
        stream.writeDWord_v1(experience);
        stream.writeByte(level);
    }

    public static void setPlayerEnergy(Stream stream, int percent)
    {
        String percentText = percent + "%";
        assignInterfaceText(stream, percentText, PACKET_TYPE_INTERFACE_ENERGY);
    }

    public static void setWeaponInterface(Stream stream, int itemId, int interfaceId)
    {
        setSidebarInterface(stream, 0, interfaceId);
    }

    public static void updateNPCs(Stream stream)
    {
        stream.createFrameVarSizeWord(PACKET_TYPE_UPDATE_NPCS);
        stream.initBitAccess();
        stream.writeBits(8, 0);
        stream.finishBitAccess();
        stream.endFrameVarSizeWord();
    }
}
