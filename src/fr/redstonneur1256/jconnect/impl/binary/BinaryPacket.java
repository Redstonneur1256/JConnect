package fr.redstonneur1256.jconnect.impl.binary;

import fr.redstonneur1256.jconnect.impl.BasicPacket;
import fr.redstonneur1256.redutilities.io.GrowingBuffer;

public abstract class BinaryPacket extends BasicPacket {

    public abstract void write(GrowingBuffer buffer);

    public abstract void read(GrowingBuffer buffer);

}
