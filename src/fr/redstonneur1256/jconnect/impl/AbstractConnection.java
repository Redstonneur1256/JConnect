package fr.redstonneur1256.jconnect.impl;

import fr.redstonneur1256.jconnect.api.client.ConnectionListener;
import fr.redstonneur1256.jconnect.api.client.JConnection;
import fr.redstonneur1256.jconnect.api.client.PacketListener;
import fr.redstonneur1256.jconnect.api.PacketSerializer;
import fr.redstonneur1256.redutilities.async.Task;
import fr.redstonneur1256.redutilities.io.GrowingBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@SuppressWarnings("unchecked")
public abstract class AbstractConnection<P extends BasicPacket> implements JConnection<P> {

    /**
     * The packet nonce will never be negative, so we can use the first bit to say there is no nonce
     */
    protected static final byte BIT8 = (byte) 0b10000000;

    protected PacketSerializer<P> serializer;
    protected List<ConnectionListener<P>> listeners;
    protected Map<Class<? extends P>, List<Consumer<?>>> typeListeners;
    protected Map<Short, WaitingListener<?>> packetListeners;
    protected int nonce;

    public AbstractConnection(@NotNull PacketSerializer<P> serializer) {
        this.serializer = serializer;
        this.listeners = new CopyOnWriteArrayList<>();
        this.typeListeners = new HashMap<>();
        this.packetListeners = new HashMap<>();
    }

    @Override
    public boolean isConnected() {
        return isConnected0();
    }

    @Override
    public void connect(@NotNull String address, int port) throws IOException {
        connect(address, port, DEFAULT_TIMEOUT);
    }

    @Override
    public void connect(@NotNull String address, int port, int timeout) throws IOException {
        connect(new InetSocketAddress(address, port), timeout);
    }

    @Override
    public void connect(@NotNull InetSocketAddress address) throws IOException {
        connect(address, DEFAULT_TIMEOUT);
    }

    @Override
    public synchronized void connect(@NotNull InetSocketAddress address, int timeout) throws IOException {
        if(isConnected()) {
            throw new IllegalStateException("The connection is already connected.");
        }
        open0(address, timeout);
    }

    @Override
    public synchronized void disconnect() {
        if(!isConnected()) {
            throw new IllegalStateException("The connection isn't connected");
        }
        close(null);
    }

    @Override
    public void sendDirect(@NotNull P packet) {
        Objects.requireNonNull(packet, "packet cannot be null");
        sendPacket(packet, null, null);
    }

    @Override
    public void sendReply(@NotNull P original, @NotNull P reply) {
        Objects.requireNonNull(original, "original cannot be null");
        Objects.requireNonNull(reply, "reply cannot be null");
        sendPacket(reply, original, null);
    }

    @NotNull
    @Override
    public <R extends P> Task<R> sendReply(@NotNull P original, @NotNull P reply, @NotNull Class<R> expectedReply) {
        Objects.requireNonNull(original, "original cannot be null");
        Objects.requireNonNull(reply, "reply cannot be null");
        Objects.requireNonNull(expectedReply, "expectedReply cannot be null");
        return sendPacket(reply, original, expectedReply);
    }

    @NotNull
    @Override
    public <R extends P> Task<R> sendPacket(@NotNull P packet, @NotNull Class<R> expectedReply) {
        Objects.requireNonNull(packet, "packet cannot be null");
        Objects.requireNonNull(expectedReply, "expectedReply cannot be null");
        return sendPacket(packet, null, expectedReply);
    }

    @NotNull
    @Override
    public <T extends P> PacketListener<T> handlePacket(@NotNull Class<T> type, @NotNull Consumer<T> listener) {
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(listener, "listener cannot be null");

        typeListeners.computeIfAbsent(type, clazz -> new CopyOnWriteArrayList<>()).add(listener);
        return new BasicPacketListener<>((AbstractConnection<T>) this, type, listener);
    }

    public void removeListener(PacketListener<?> listener) {
        List<Consumer<?>> listeners = typeListeners.get(listener.getType());
        if(listeners != null) {
            listeners.remove(listener.getListener());
        }
    }

    protected abstract boolean isConnected0();

    protected abstract void open0(@NotNull InetSocketAddress address, int timeout) throws IOException;

    protected abstract void close0();

    protected abstract void send0(byte[] data, int length) throws IOException;

    @NotNull
    @Override
    public PacketSerializer<P> getSerializer() {
        return serializer;
    }

    @Override
    public void addListener(@NotNull ConnectionListener<P> listener) {
        Objects.requireNonNull(listener, "listener cannot be null");

        listeners.add(listener);
    }

    @Override
    public void removeListener(ConnectionListener<P> listener) {
        listeners.remove(listener);
    }

    @NotNull
    @Override
    public List<ConnectionListener<P>> getListeners() {
        return listeners;
    }


    protected synchronized <R extends P> Task<R> sendPacket(@NotNull P packet, @Nullable P original, @Nullable Class<R> expectedReply) {
        Task<R> task = expectedReply == null ? null : new Task<>();

        GrowingBuffer buffer = GrowingBuffer.allocate(512);
        buffer.position(0);

        short nonce = incrementNonce();
        packet.nonce = nonce;
        buffer.putShort(nonce);

        if(original == null) {
            buffer.put(BIT8);
        }else {
            buffer.put((byte) ((original.nonce & 0xFF00) >> 8));
            buffer.put((byte) (original.nonce & 0x00FF));
        }

        serializer.write(packet, buffer);

        if(expectedReply != null) {
            packetListeners.put(nonce, new WaitingListener<>(expectedReply, task));
        }

        try {
            send0(buffer.array(), buffer.position());
        }catch(IOException exception) {
            packetListeners.remove(nonce);
            throw new RuntimeException(exception);
        }

        for(ConnectionListener<P> listener : listeners) {
            listener.packetSent(packet);
        }

        return task;
    }

    protected void handleData(@NotNull GrowingBuffer buffer) {
        short nonce = buffer.getShort();

        byte a = buffer.get();
        boolean hasListener = a != BIT8;
        byte b = hasListener ? buffer.get() : 0;

        P packet = serializer.read(buffer);
        packet.nonce = nonce;

        for(ConnectionListener<P> listener : listeners) {
            listener.packetReceived(packet);
        }

        Class<? extends BasicPacket> packetClass = packet.getClass();

        List<Consumer<?>> listeners = typeListeners.get(packetClass);
        if(listeners != null) {
            for(Consumer<?> listener : listeners) {
                ((Consumer<P>) listener).accept(packet);
            }
        }

        WaitingListener<?> listener;
        if(hasListener && (listener = packetListeners.remove((short) (a << 8 | b))) != null) {
            if(!listener.type.isAssignableFrom(packetClass)) {
                String message = String.format("Received packet type %s is not assignable from the listener expected type %s",
                        packetClass.getName(), listener.type.getName());
                listener.task.fail(new ClassCastException(message));
            }else {
                ((Task<P>) listener.task).complete(packet);
            }
        }

    }

    protected short incrementNonce() {
        if(nonce >= Short.MAX_VALUE) {
            nonce = 0;
        }
        return (short) nonce++;
    }

    protected void close(@Nullable Throwable throwable) {
        close0();

        for(ConnectionListener<P> listener : listeners) {
            listener.connectionClosed(throwable);
        }
    }

    protected void quietException(Throwable throwable) {
        for(ConnectionListener<P> listener : listeners) {
            listener.quietException(throwable);
        }
    }

    protected static class WaitingListener<T> {

        protected Class<T> type;
        protected Task<T> task;

        protected WaitingListener(Class<T> type, Task<T> task) {
            this.type = type;
            this.task = task;
        }

    }

}
