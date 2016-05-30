package net.md_5.bungee.protocol;

import java.util.Arrays;
import java.util.List;

public class ProtocolConstants
{

    public static final int MINECRAFT_1_8 = 47;
    public static final int MINECRAFT_1_9 = 107;
    public static final int MINECRAFT_1_9_1 = 108;
    public static final int MINECRAFT_1_9_2 = 109;
    public static final int MINECRAFT_1_9_4 = 110;
    public static final int MINECRAFT_16w21b = 203;
    public static final List<String> SUPPORTED_VERSIONS = Arrays.asList(
            "1.8.x",
            "1.9.x"
            "16w21b"
    );
    public static final List<Integer> SUPPORTED_VERSION_IDS = Arrays.asList(ProtocolConstants.MINECRAFT_1_8,
            ProtocolConstants.MINECRAFT_1_9,
            ProtocolConstants.MINECRAFT_1_9_1,
            ProtocolConstants.MINECRAFT_1_9_2,
            ProtocolConstants.MINECRAFT_1_9_4,
            ProtocolConstants.MINECRAFT_16w21b
    );

    public enum Direction
    {

        TO_CLIENT, TO_SERVER;
    }
}
