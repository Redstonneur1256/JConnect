package fr.redstonneur1256.jconnect.impl.nio;

import fr.redstonneur1256.jconnect.api.PacketSerializer;
import fr.redstonneur1256.jconnect.api.server.JServer;
import fr.redstonneur1256.jconnect.api.server.ServerListener;
import fr.redstonneur1256.jconnect.impl.BasicPacket;
import fr.redstonneur1256.redutilities.async.Threads;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

@SuppressWarnings("unchecked")
public class NIOServer<P extends BasicPacket> implements JServer<P> {

    private PacketSerializer<P> serializer;
    private List<ServerListener<P>> listeners;
    private Selector selector;
    private ServerSocketChannel channel;
    private Thread updateThread;
    private int emptySelects;

    public NIOServer(PacketSerializer<P> serializer) {
        this.serializer = serializer;
        this.listeners = new CopyOnWriteArrayList<>();
    }

    @Override
    public boolean isOpen() {
        return channel != null && channel.isOpen();
    }

    @Override
    public void bind(int port) throws IOException {
        bind(new InetSocketAddress(port));
    }

    @Override
    public void bind(@NotNull SocketAddress address) throws IOException {
        if(isOpen()) {
            throw new IllegalStateException("The server is already open.");
        }

        selector = Selector.open();

        channel = selector.provider().openServerSocketChannel();
        channel.socket().bind(address);
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_ACCEPT);

        updateThread = Threads.daemon("NIOServer", this::updateThread);

        for(ServerListener<P> listener : listeners) {
            listener.serverOpened();
        }
    }

    @Override
    public void close() {
        if(!isOpen()) {
            throw new IllegalStateException("The server is not open.");
        }

        close(null);
    }

    @Override
    public void addListener(@NotNull ServerListener<P> listener) {
        Objects.requireNonNull(listener, "listener cannot be null");
        listeners.add(listener);
    }

    @Override
    public void removeListener(ServerListener<P> listener) {
        listeners.remove(listener);
    }

    @NotNull
    @Override
    public List<ServerListener<P>> getListeners() {
        return listeners;
    }

    private void updateThread(Thread thread) {
        while(!thread.isInterrupted()) {
            try {
                update();
            }catch(IOException exception) {
                exception.printStackTrace();
                if(!isOpen()) {
                    return;
                }

                close(exception);
            }
        }
    }

    private void update() throws IOException {
        int channels = selector.select();

        if(channels == 0) {
            if(emptySelects++ >= 100) {
                emptySelects = 0;

                // NIO sometimes return 0 with no delay ??
                try {
                    Thread.sleep(25);
                }catch(InterruptedException ignored) {
                }
            }

            return;
        }
        Set<SelectionKey> keys = selector.selectedKeys();

        for(SelectionKey key : keys) {
            try {
                int ops = key.readyOps();
                NIOConnection<P> connection = (NIOConnection<P>) key.attachment();

                if((ops & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT && connection == null) {
                    acceptConnection();
                }
                if((ops & SelectionKey.OP_READ) == SelectionKey.OP_READ && connection != null) {
                    connection.read();
                }
            }catch(Exception exception) {
                exception.printStackTrace();
                quietException(exception);
            }
        }

        keys.clear();
    }

    private void acceptConnection() throws IOException {
        SocketChannel socket = channel.accept();

        if(socket == null) {
            return;
        }
        NIOConnection<P> connection = new NIOConnection<>(serializer, socket);

        socket.configureBlocking(false);
        socket.register(selector, SelectionKey.OP_READ, connection);

        for(ServerListener<P> listener : listeners) {
            listener.connectionReceived(connection);
        }
    }

    protected void close(Throwable throwable) {
        if(!updateThread.isInterrupted()) {
            updateThread.interrupt();
        }
        try {
            channel.close();
            selector.close();
        }catch(IOException ignored) {
        }

        for(ServerListener<P> listener : listeners) {
            listener.serverClosed(throwable);
        }
    }

    protected void quietException(Throwable throwable) {
        for(ServerListener<P> listener : listeners) {
            listener.quietException(throwable);
        }
    }

}
