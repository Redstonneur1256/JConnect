package fr.redstonneur1256.jconnect.api.server;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.List;

public interface JServer<P> {

    boolean isOpen();

    void bind(int port) throws IOException;

    void bind(@NotNull SocketAddress address) throws IOException;

    void close() throws IOException;

    void addListener(@NotNull ServerListener<P> listener);

    void removeListener(ServerListener<P> listener);

    @NotNull List<ServerListener<P>> getListeners();

}
