package org.example.connector.table;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.connector.source.ScanTableSource;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.LogicalTypeRoot;
import org.apache.flink.types.Row;
import org.apache.flink.types.RowKind;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.regex.Pattern;

public class SocketSourceFunction extends RichSourceFunction<RowData> implements ResultTypeQueryable<RowData> {

    private final String hostname;
    private final int port;
    private final byte byteDelimiter;
    private final String columnDelimiter;
    private final TypeInformation<RowData> producedTypeInfo;
    private final List<LogicalType> parsingTypes;
    private final DynamicTableSource.DataStructureConverter converter;
    private volatile boolean isRunning = true;
    private Socket currentSocket;

    public SocketSourceFunction(String hostname, int port, byte byteDelimiter,
                                String columnDelimiter, TypeInformation<RowData> producedTypeInfo,
                                List<LogicalType> parsingTypes, DynamicTableSource.DataStructureConverter converter) {
        this.hostname = hostname;
        this.port = port;
        this.byteDelimiter = byteDelimiter;
        this.columnDelimiter = columnDelimiter;
        this.producedTypeInfo = producedTypeInfo;
        this.parsingTypes = parsingTypes;
        this.converter = converter;
    }

    @Override
    public TypeInformation<RowData> getProducedType() {
        return producedTypeInfo;
    }

    @Override
    public void run(SourceContext<RowData> sourceContext) throws Exception {
        while (isRunning) {
            // open and consume from socket
            try (final Socket socket = new Socket()) {
                currentSocket = socket;
                socket.connect(new InetSocketAddress(hostname, port), 0);
                try (InputStream stream = socket.getInputStream()) {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));
                    String info = null;
                    while ((info = bufferedReader.readLine()) != null) {
                        sourceContext.collect(deserialize(info));
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace(); // print and continue
            }
            Thread.sleep(1000);
        }
    }

    @Override
    public void cancel() {
        isRunning = false;
    }

    //-----------------------------------help-------------------------------------------

    public RowData deserialize(String message) {
        // parse the columns including a changelog flag
        final String[] columns = message.split(Pattern.quote(columnDelimiter));
        final RowKind kind = RowKind.valueOf(columns[0]);
        final Row row = new Row(kind, parsingTypes.size());
        for (int i = 0; i < parsingTypes.size(); i++) {
            row.setField(i, parse(parsingTypes.get(i).getTypeRoot(), columns[i + 1]));
        }
        // convert to internal data structure
        return (RowData) converter.toInternal(row);
    }

    private static Object parse(LogicalTypeRoot root, String value) {
        switch (root) {
            case INTEGER:
                return Integer.parseInt(value);
            case VARCHAR:
                return value;
            default:
                throw new IllegalArgumentException();
        }
    }
}
