package fr.redstonneur1256.jconnect.examples;

import fr.redstonneur1256.jconnect.api.PacketSerializer;
import fr.redstonneur1256.jconnect.api.client.JConnection;
import fr.redstonneur1256.jconnect.api.server.ServerListener;
import fr.redstonneur1256.jconnect.impl.binary.BinaryPacket;
import fr.redstonneur1256.jconnect.impl.nio.NIOServer;
import fr.redstonneur1256.redutilities.async.Threads;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class NIOServerExample {

    public static void main(String[] args) throws IOException {
        // Create the same packet serializer than the Client
        PacketSerializer<BinaryPacket> serializer = ExampleSerializer.createPacketSerializer();

        // Create a server
        NIOServer<BinaryPacket> server = new NIOServer<>(serializer);

        // Open the server on port 1256
        server.bind(1256);

        // Add a listener that will accept the connections
        server.addListener(new ServerListener<BinaryPacket>() {
            @Override
            public void connectionReceived(@NotNull JConnection<BinaryPacket> connection) {
                System.out.printf("Accepting connection %s%n", connection);

                // When we get an incoming MessagePacket
                connection.handlePacket(MessagePacket.class, (MessagePacket packet) -> {
                    System.out.printf("Received message '%s'%n", packet.message);

                    if(packet.message.equals("Hello")) {
                        // Send a MessagePacket("World") as a reply to the received packet
                        connection.sendReply(packet, new MessagePacket("World"));
                    }
                });
            }

            @Override
            public void quietException(@NotNull Throwable throwable) {
                System.out.println("quietException");
                throwable.printStackTrace(System.out);
            }

            @Override
            public void serverClosed(@Nullable Throwable throwable) {
                System.out.println("Closed");
            }
        });

        // The server thread is daemon, we need to block the program for it to actually work
        Threads.sleep(Long.MAX_VALUE);
    }

}
