package fr.redstonneur1256.jconnect.api;

import fr.redstonneur1256.redutilities.async.Task;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.function.Consumer;

public interface JConnection<P> {

    int DEFAULT_TIMEOUT = 5000;

    boolean isConnected();

    void connect(@NotNull String address, int port) throws IOException;

    void connect(@NotNull String address, int port, int timeout) throws IOException;

    void connect(@NotNull InetSocketAddress address) throws IOException;

    void connect(@NotNull InetSocketAddress address, int timeout) throws IOException;

    void open(@NotNull  Socket socket) throws IOException;

    void disconnect();

    void sendDirect(@NotNull P packet);

    void sendReply(@NotNull P original, @NotNull P reply);

    @NotNull <R extends P> Task<R> sendReply(@NotNull P original, @NotNull P reply, @NotNull Class<R> expectedReply);

    @NotNull <R extends P> Task<R> sendPacket(@NotNull P packet, @NotNull Class<R> expectedReply);

    @NotNull <T extends P> PacketListener<T> handlePacket(@NotNull Class<T> type, @NotNull Consumer<T> listener);

    @NotNull PacketSerializer<P> getSerializer();

    void addListener(@NotNull ConnectionListener<P> listener);

    void removeListener(ConnectionListener<P> listener);

    @NotNull List<ConnectionListener<P>> getListeners();

}
