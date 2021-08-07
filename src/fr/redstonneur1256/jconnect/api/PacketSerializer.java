package fr.redstonneur1256.jconnect.api;

import fr.redstonneur1256.redutilities.io.GrowingBuffer;

public interface PacketSerializer<P> {

    void write(P packet, GrowingBuffer buffer);

    P read(GrowingBuffer buffer);

}
