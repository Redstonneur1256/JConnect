package fr.redstonneur1256.jconnect.impl;

import fr.redstonneur1256.jconnect.api.PacketListener;

import java.util.function.Consumer;

public class BasicPacketListener<P extends BasicPacket> implements PacketListener<P> {

    private Connection<?> connection;
    private Class<P> type;
    private Consumer<P> listener;

    public BasicPacketListener(Connection<?> connection, Class<P> type, Consumer<P> listener) {
        this.connection = connection;
        this.type = type;
        this.listener = listener;
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

    }

}
