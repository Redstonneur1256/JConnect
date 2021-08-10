package fr.redstonneur1256.jconnect.examples;

import fr.redstonneur1256.jconnect.api.PacketSerializer;
import fr.redstonneur1256.jconnect.examples.ExampleSerializer;
import fr.redstonneur1256.jconnect.examples.MessagePacket;
import fr.redstonneur1256.jconnect.impl.binary.BinaryPacket;
import fr.redstonneur1256.jconnect.impl.io.TCPConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerExample {

    public static void main(String[] args) throws IOException {
        // Create the same packet serializer than the Client
        PacketSerializer<BinaryPacket> serializer = ExampleSerializer.createPacketSerializer();

        // Create a server on the port 1256
        ServerSocket server = new ServerSocket(1256);

        // Wait for incoming connections
        while(server.isBound()) {
            Socket socket = server.accept();

            System.out.printf("Accepting connection %s%n", socket);

            TCPConnection<BinaryPacket> connection = new TCPConnection<>(serializer);

            // When we get an incoming MessagePacket
            connection.handlePacket(MessagePacket.class, (MessagePacket packet) -> {
                System.out.printf("Received message '%s'%n", packet.message);

                if(packet.message.equals("Hello")) {
                    // Send a MessagePacket("World") as a reply to the received packet
                    connection.sendReply(packet, new MessagePacket("World"));
                }
            });

            // Tell the connection to use an already existing socket
            connection.open(socket);
        }
    }

}
