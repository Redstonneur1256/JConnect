package fr.redstonneur1256.jconnect.impl.nio;

import fr.redstonneur1256.jconnect.api.PacketSerializer;
import fr.redstonneur1256.jconnect.impl.AbstractConnection;
import fr.redstonneur1256.jconnect.impl.BasicPacket;
import fr.redstonneur1256.redutilities.io.GrowingBuffer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class NIOConnection<P extends BasicPacket> extends AbstractConnection<P> {

    protected ByteBuffer readBuffer;
    protected SocketChannel channel;
    protected int waitingLength;
    protected GrowingBuffer buffer;

    public NIOConnection(@NotNull PacketSerializer<P> serializer, SocketChannel channel) {
        super(serializer);
        this.readBuffer = ByteBuffer.allocate(512);
        this.channel = channel;
        this.waitingLength = -1;
    }

    @Override
    protected boolean isConnected0() {
        return channel != null && channel.isConnected();
    }

    @Override
    protected void open0(@NotNull InetSocketAddress address, int timeout) {
        throw new UnsupportedOperationException("NIO Connections are only available for server");
    }

    @Override
    protected void close0() {
        try {
            channel.close();
        }catch(IOException ignored) {
        }
    }

    @Override
    protected void send0(byte[] data, int length) throws IOException {
        channel.write((ByteBuffer) ByteBuffer.allocate(4).putInt(length).position(0));
        channel.write(ByteBuffer.wrap(data, 0, length));
    }

    protected void read() throws IOException {
        readBuffer.position(0);
        int length = channel.read(readBuffer);

        // When the connection is closed by the peer an OP_READ is triggered with a -1 length
        if(length == -1) {
            close(null);
            return;
        }

        // Just in case
        if(length == 0) {
            return;
        }

        if(buffer == null) {
            buffer = GrowingBuffer.allocate(length);
        }
        buffer.put(readBuffer.array(), 0, length);

        if(waitingLength == -1 && buffer.position() > 4) {
            waitingLength = buffer.getInt(0);
        }

        if(waitingLength != -1 && buffer.position() >= waitingLength) {

            int remainingBytes = buffer.position() - waitingLength;

            // Skip the length integer
            buffer.position(4);
            try {
                handleData(buffer);
            }catch(Throwable throwable) {
                quietException(throwable);
            }

            if(remainingBytes > 0) {
                // If there is bytes from the next packet, move them to the start of the buffer
                byte[] data = buffer.array();
                System.arraycopy(data, buffer.position(), data, 0, buffer.capacity() - buffer.position());
            }

            // Invalidate the data
            waitingLength = -1;
        }
    }

}
