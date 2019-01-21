package ru.fix.aggregating.profiler.graphite.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

/**
 * Created by mocichenko on 23.07.2016.
 */
public class UDPGraphiteSocket extends GraphiteSocket {

    private DatagramSocket socket;
    private InetSocketAddress address;

    public UDPGraphiteSocket(String host, int port, int metricBatchSize) {
        super(host, port, metricBatchSize);
    }

    @Override
    public GraphiteSocket openSocket() throws IOException {
        socket = new DatagramSocket(null);
        socket.setReuseAddress(true);
        address = new InetSocketAddress(this.host, this.port);
        return this;
    }

    @Override
    public void write(String lines) throws IOException {
        byte[] message = lines.getBytes();
        DatagramPacket packet = new DatagramPacket(message, message.length, address);
        socket.send(packet);
    }

    @Override
    public boolean validateSocket() {
        return socket.isConnected()
                && !socket.isClosed();
    }

    @Override
    public boolean isClosed() {
        return socket.isClosed();
    }

    @Override
    public void close() throws Exception {
        if (socket != null) {
            socket.close();
        }
    }
}
