package fr.redstonneur1256.jconnect.api;

import org.jetbrains.annotations.NotNull;

public interface ConnectionListener<P> {

    default void connectionOpened() {
    }

    /**
     * Called when the connection is terminated
     *
     * @param throwable the exception that caused the end of the connection or null if {@link JConnection#disconnect()} was called
     */
    default void connectionClosed(@NotNull Throwable throwable) {
    }

    /**
     * Called when a non-fatal exception occurs
     *
     * @param throwable the exception
     */
    default void quietException(@NotNull Throwable throwable) {
    }

    default void packetReceived(P packet) {
    }

    default void packetSent(P packet) {
    }

}
