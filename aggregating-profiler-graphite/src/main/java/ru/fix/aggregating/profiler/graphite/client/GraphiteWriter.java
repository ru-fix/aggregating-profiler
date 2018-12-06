package ru.fix.aggregating.profiler.graphite.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Created by mocichenko on 22.07.2016.
 */
public class GraphiteWriter {

    private static final Logger log = LoggerFactory.getLogger(GraphiteWriter.class);

    private GraphiteSocket socket;
    private GraphiteSettings settings;
    private boolean closed = false;

    public synchronized void createAndOpen(GraphiteSettings settings) throws Exception {
        if (closed) {
            return;
        }
        this.settings = settings;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        socket = createSocket(settings);
        try {
            socket.openSocket();
        } catch (IOException e) {
            log.error("Couldn't open connection with host: {} on port: {}, protocol: {}.",
                    settings.getHost(), settings.getPort(), settings.getProtocol(), e);
        }
    }

    public synchronized void write(String metricPrefix, List<GraphiteEntity> metrics) throws Exception {
        try {
            if (socket == null || !socket.validateSocket()) {
                log.info("Socket was closed or connect was lost with host {}:{}",
                        settings.getHost(), settings.getPort());
                createAndOpen(settings);
            }
            socket.write(metricPrefix, metrics);
        } catch (Exception e) {
            log.info("Write to socket failed due to exc: {}. Recreate connection {}:{}", e.getMessage(),
                    settings.getHost(), settings.getPort(), e);
            createAndOpen(settings);
        }
    }

    public synchronized void write(String metric) throws Exception {
        try {
            if (socket == null || !socket.validateSocket()) {
                log.info("Socket was closed or connect was lost with host {}:{}",
                        settings.getHost(), settings.getPort());
                createAndOpen(settings);
            }
            socket.write(metric);
        } catch (Exception e) {
            log.info("Write to socket failed due to exc: {}. Recreate connection {}:{}", e.getMessage(),
                    settings.getHost(), settings.getPort(), e);
            createAndOpen(settings);
        }
    }

    public synchronized void close() throws Exception {
        closed = true;
        socket.close();
    }

    private static GraphiteSocket createSocket(GraphiteSettings settings) {
        String host = settings.getHost();
        int port = settings.getPort();
        int metricBatchSize = settings.getBatchSize();
        return ProtocolType.UDP.equals(settings.getProtocol()) ?
                new UDPGraphiteSocket(host, port, metricBatchSize) :
                new TCPGraphiteSocket(host, port, metricBatchSize);
    }
}
