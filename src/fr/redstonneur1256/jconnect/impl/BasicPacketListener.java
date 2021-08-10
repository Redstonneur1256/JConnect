package fr.redstonneur1256.jconnect.impl;

import fr.redstonneur1256.jconnect.api.client.PacketListener;

import java.util.function.Consumer;

public class BasicPacketListener<P extends BasicPacket> implements PacketListener<P> {

    private AbstractConnection<P> connection;
    private Class<P> type;
    private Consumer<P> listener;

    public BasicPacketListener(AbstractConnection<P> connection, Class<P> type, Consumer<P> listener) {
        this.connection = connection;
        this.type = type;
        this.listener = listener;
    }

    @Override
    public AbstractConnection<P> getConnection() {
        return connection;
    }

    @Override
    public Class<P> getType() {
        return type;
    }

    @Override
    public Consumer<P> getListener() {
        return listener;
    }

    @Override
    public void unregister() {
        connection.removeListener(this);
    }

}
