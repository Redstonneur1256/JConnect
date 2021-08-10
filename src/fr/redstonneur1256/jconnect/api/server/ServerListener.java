package fr.redstonneur1256.jconnect.api.server;

import fr.redstonneur1256.jconnect.api.client.JConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ServerListener<P> {

    default void serverOpened() {
    }

    default void serverClosed(@Nullable Throwable throwable) {
    }

    default void quietException(@NotNull Throwable throwable) {
    }

    default void connectionReceived(@NotNull JConnection<P> connection) {
    }

}
