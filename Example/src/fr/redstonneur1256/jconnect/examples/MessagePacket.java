package fr.redstonneur1256.jconnect.examples;

import fr.redstonneur1256.jconnect.impl.binary.BinaryPacket;
import fr.redstonneur1256.redutilities.io.GrowingBuffer;

public class MessagePacket extends BinaryPacket {

    public String message;

    public MessagePacket() {

    }

    public MessagePacket(String message) {
        this.message = message;
    }

    @Override
    public void write(GrowingBuffer buffer) {
        buffer.putString(message);
    }

    @Override
    public void read(GrowingBuffer buffer) {
        message = buffer.getString();
    }

}
