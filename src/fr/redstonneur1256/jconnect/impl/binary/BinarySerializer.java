package fr.redstonneur1256.jconnect.impl.binary;

import fr.redstonneur1256.jconnect.api.PacketSerializer;
import fr.redstonneur1256.redutilities.io.GrowingBuffer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class BinarySerializer implements PacketSerializer<BinaryPacket> {

    private Map<Integer, Supplier<? extends BinaryPacket>> byID;
    private Map<Class<? extends BinaryPacket>, Integer> byType;

    public BinarySerializer() {
        byID = new HashMap<>();
        byType = new HashMap<>();
    }

    public <T extends BinaryPacket> void registerPacket(int id, Class<T> type, Supplier<T> supplier) {
        byID.put(id, supplier);
        byType.put(type, id);
    }

    @Override
    public void write(BinaryPacket packet, GrowingBuffer buffer) {
        Class<? extends BinaryPacket> type = packet.getClass();
        Integer integer = byType.get(type);
        if(integer == null) {
            throw new UnsupportedOperationException("Unknown packet type " + type);
        }
        buffer.putInt(integer);
        packet.write(buffer);
    }

    @Override
    public BinaryPacket read(GrowingBuffer buffer) {
        int id = buffer.getInt();
        Supplier<? extends BinaryPacket> supplier = byID.get(id);
        if(supplier == null) {
            throw new UnsupportedOperationException("Received unknown packet with id " + id);
        }
        BinaryPacket packet = supplier.get();
        packet.read(buffer);
        return packet;
    }

}
