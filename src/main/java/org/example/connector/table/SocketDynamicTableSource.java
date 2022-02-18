package org.example.connector.table;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.connector.source.ScanTableSource;
import org.apache.flink.table.connector.source.SourceFunctionProvider;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.types.RowKind;

import java.util.List;

public class SocketDynamicTableSource implements ScanTableSource {

    private final String hostname;
    private final int port;
    private final byte byteDelimiter;
    private final String columnDelimiter;
    private final DataType producedDataType;

    public SocketDynamicTableSource(String hostname, int port, byte byteDelimiter, String columnDelimiter, DataType producedDataType) {
        this.hostname = hostname;
        this.port = port;
        this.byteDelimiter = byteDelimiter;
        this.columnDelimiter = columnDelimiter;
        this.producedDataType = producedDataType;
    }

    @Override
    public ChangelogMode getChangelogMode() {
        return ChangelogMode.newBuilder().addContainedKind(RowKind.INSERT).addContainedKind(RowKind.DELETE).build();
    }

    @Override
    public ScanRuntimeProvider getScanRuntimeProvider(ScanContext scanContext) {
        List<LogicalType> parsingTypes = producedDataType.getLogicalType().getChildren();
        DynamicTableSource.DataStructureConverter converter = scanContext.createDataStructureConverter(producedDataType);
        TypeInformation<RowData> producedTypeInfo = scanContext.createTypeInformation(producedDataType);
        final SourceFunction<RowData> sourceFunction = new SocketSourceFunction(
                hostname,
                port,
                byteDelimiter,
                columnDelimiter,
                producedTypeInfo,
                parsingTypes,
                converter);

        return SourceFunctionProvider.of(sourceFunction, false);
    }

    @Override
    public DynamicTableSource copy() {
        return new SocketDynamicTableSource(this.hostname, this.port, this.byteDelimiter, this.columnDelimiter, this.producedDataType);
    }

    @Override
    public String asSummaryString() {
        return "Demo Table Source";
    }
}
