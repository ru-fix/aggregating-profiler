package ru.fix.aggregating.profiler.graphite.client;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;

/**
 * Created by mocichenko on 23.07.2016.
 */
public class TCPGraphiteSocket extends GraphiteSocket {

    private Socket socket;
    Writer writer;

    public TCPGraphiteSocket(String host, int port, int metricBatchSize) {
        super(host, port, metricBatchSize);
    }

    @Override
    public GraphiteSocket openSocket() throws IOException {
        socket = new Socket(host, port);
        socket.setKeepAlive(true);
        socket.setReuseAddress(true);
        writer = new OutputStreamWriter(socket.getOutputStream());
        return this;
    }

    @Override
    public void write(String msg) throws IOException {
        writer.write(msg);
        writer.flush();
    }

    @Override
    public boolean validateSocket() {
        return socket.isConnected()
                && socket.isBound()
                && !socket.isClosed()
                && !socket.isInputShutdown()
                && !socket.isOutputShutdown();
    }

    @Override
    public boolean isClosed() {
        return socket == null || socket.isClosed();
    }

    @Override
    public void close() throws Exception {
        writer.close();
        if (socket != null) {
            socket.close();
        }
    }
}
