package org.example.connector.table;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.apache.flink.table.data.RowData;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SocketSourceFunction extends RichSourceFunction<RowData> implements ResultTypeQueryable<RowData> {

    @Override
    public TypeInformation<RowData> getProducedType() {
        return null;
    }

    @Override
    public void run(SourceContext<RowData> sourceContext) throws Exception {

    }

    @Override
    public void cancel() {

    }
}
