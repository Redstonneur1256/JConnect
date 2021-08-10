package fr.redstonneur1256.jconnect.api.client;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ConnectionListener<P> {

    default void connectionOpened() {
    }

    /**
     * Called when the connection is terminated
     *
     * @param throwable the exception that caused the end of the connection or null if {@link JConnection#disconnect()} was called
     */
    default void connectionClosed(@Nullable Throwable throwable) {
    }

    /**
     * Called when a non-fatal exception occurs
     *
     * @param throwable the exception
     */
    default void quietException(@NotNull Throwable throwable) {
    }

    default void packetReceived(@NotNull P packet) {
    }

    default void packetSent(@NotNull P packet) {
    }

}
