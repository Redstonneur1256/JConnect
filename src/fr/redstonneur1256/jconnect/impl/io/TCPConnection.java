package fr.redstonneur1256.jconnect.impl.io;

import fr.redstonneur1256.jconnect.api.PacketSerializer;
import fr.redstonneur1256.jconnect.api.client.ConnectionListener;
import fr.redstonneur1256.jconnect.impl.AbstractConnection;
import fr.redstonneur1256.jconnect.impl.BasicPacket;
import fr.redstonneur1256.redutilities.async.Threads;
import fr.redstonneur1256.redutilities.io.GrowingBuffer;
import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;

public class TCPConnection<P extends BasicPacket> extends AbstractConnection<P> {

    protected Socket socket;
    protected DataInputStream input;
    protected DataOutputStream output;
    protected Thread readerThread;

    public TCPConnection(@NotNull PacketSerializer<P> serializer) {
        super(serializer);
    }

    @Override
    protected boolean isConnected0() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    @Override
    protected void open0(@NotNull InetSocketAddress address, int timeout) throws IOException {
        Socket socket = new Socket();
        socket.connect(address, timeout);
        open(socket);
    }

    public void open(@NotNull Socket socket) throws IOException {
        Objects.requireNonNull(socket, "Socket cannot be null");

        this.socket = socket;
        this.input = new DataInputStream(socket.getInputStream());
        this.output = new DataOutputStream(socket.getOutputStream());
        this.readerThread = Threads.daemon("TCPConnection-" + socket.getRemoteSocketAddress(), this::readData);

        for(ConnectionListener<P> listener : listeners) {
            listener.connectionOpened();
        }
    }

    @Override
    protected void close0() {
        if(!readerThread.isInterrupted()) {
            readerThread.interrupt();
        }
        try {
            socket.close();
        }catch(IOException ignored) {
        }
    }

    @Override
    protected void send0(byte[] data, int length) throws IOException {
        output.writeInt(length);
        output.write(data, 0, length);
    }

    private void readData(Thread thread) {
        try {
            DataInputStream stream = this.input;

            while(!thread.isInterrupted()) {
                int length = stream.readInt();
                byte[] bytes = new byte[length];
                stream.readFully(bytes);

                try {
                    handleData(GrowingBuffer.wrap(bytes));
                }catch(Throwable throwable) {
                    quietException(throwable);
                }
            }
        }catch(Throwable throwable) {
            if(throwable instanceof SocketException && !isConnected()) {
                return;
            }
            close(throwable);
        }
    }

}
