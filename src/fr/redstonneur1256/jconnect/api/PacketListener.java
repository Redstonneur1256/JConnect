package fr.redstonneur1256.jconnect.api;

import java.util.function.Consumer;

public interface PacketListener<P> {

    Class<P> getType();

    Consumer<P> getListener();

    void unregister();

}
