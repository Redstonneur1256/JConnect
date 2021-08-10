package fr.redstonneur1256.jconnect.api.client;

import java.util.function.Consumer;

public interface PacketListener<P> {

    JConnection<P> getConnection();

    Class<P> getType();

    Consumer<P> getListener();

    void unregister();

}
