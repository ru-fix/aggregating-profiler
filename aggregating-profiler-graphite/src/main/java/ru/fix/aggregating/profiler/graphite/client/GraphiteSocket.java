package ru.fix.aggregating.profiler.graphite.client;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Created by mocichenko on 22.07.2016.
 */
public abstract class GraphiteSocket implements AutoCloseable {

    protected final String host;
    protected final int port;
    private final int metricBatchSize;

    protected GraphiteSocket(String host, int port, int metricBatchSize) {
        this.host = host;
        this.port = port;
        this.metricBatchSize = metricBatchSize;
    }

    public abstract GraphiteSocket openSocket() throws IOException;

    public void write(String metricPrefix, List<GraphiteEntity> metrics) throws IOException {
        StringBuilder lines = new StringBuilder();
        int numberMetricsInBatch = 0;
        Iterator<GraphiteEntity> iterator = metrics.iterator();
        while (iterator.hasNext()) {
            ++numberMetricsInBatch;
            GraphiteEntity entity = iterator.next();
            lines.append(metricPrefix)
                    .append('.')
                    .append(entity.getName())
                    .append(' ')
                    .append(entity.getValue())
                    .append(' ')
                    .append(entity.getTimeInSec())
                    .append('\n');
            if (numberMetricsInBatch >= metricBatchSize || !iterator.hasNext()) {
                write(lines.toString());
                lines.setLength(0);
                numberMetricsInBatch = 0;
            }
        }
    }

    public abstract void write(String lines) throws IOException;

    public abstract boolean validateSocket();

    public abstract boolean isClosed();
}
