package fr.redstonneur1256.jconnect.examples;

import fr.redstonneur1256.jconnect.api.PacketSerializer;
import fr.redstonneur1256.jconnect.impl.binary.BinaryPacket;
import fr.redstonneur1256.jconnect.impl.binary.BinarySerializer;

public class ExampleSerializer {

    public static PacketSerializer<BinaryPacket> createPacketSerializer() {
        BinarySerializer serializer = new BinarySerializer();

        // Register the class MessagePacket with ID 0
        serializer.registerPacket(0, MessagePacket.class, MessagePacket::new);

        return serializer;
    }

}
