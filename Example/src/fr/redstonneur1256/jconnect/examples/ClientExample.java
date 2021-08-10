package fr.redstonneur1256.jconnect.examples;

import fr.redstonneur1256.jconnect.api.PacketSerializer;
import fr.redstonneur1256.jconnect.api.client.JConnection;
import fr.redstonneur1256.jconnect.impl.binary.BinaryPacket;
import fr.redstonneur1256.jconnect.impl.io.TCPConnection;
import fr.redstonneur1256.redutilities.async.Task;

import java.io.IOException;

public class ClientExample {

    public static void main(String[] args) throws IOException {
        // Create a packet serializer with the MessagePacket registered
        PacketSerializer<BinaryPacket> serializer = ExampleSerializer.createPacketSerializer();

        // Create a TCP connection
        JConnection<BinaryPacket> connection = new TCPConnection<>(serializer);

        // Connect to localhost:1256 with a timeout of 5000 ms
        connection.connect("localhost", 1256, 5000);

        // Send a MessagePacket with content "Hello" and expect a MessagePacket as a reply
        Task<MessagePacket> task = connection.sendPacket(new MessagePacket("Hello"), MessagePacket.class);

        // When the task complete print the received message and close the connection
        task.onComplete((MessagePacket result) -> {
            System.out.printf("Got response '%s'%n", result.message);

            connection.disconnect();
        });

        // In case of an exception, print it
        // The current exceptions can be:
        // - ClassCastException: You expected to receive a different packet type
        task.onFail(Throwable::printStackTrace);


        // Block here till the task is complete. All Connection threads are daemon, we don't want that example to exit too soon
        task.waitComplete();
    }

}
