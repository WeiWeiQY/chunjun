/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtstack.flinkx.connector.jdbc.converter;

import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.logical.RowType;

import com.dtstack.flinkx.connector.jdbc.statement.FieldNamedPreparedStatement;
import com.dtstack.flinkx.converter.AbstractRowConverter;
import io.vertx.core.json.JsonArray;

import java.sql.ResultSet;

/** Base class for all converters that convert between JDBC object and Flink internal object. */
public abstract class AbstractJdbcRowConverter<T>
        extends AbstractRowConverter<ResultSet, JsonArray, FieldNamedPreparedStatement, T> {

    private static final long serialVersionUID = -3704861870612289508L;

    public AbstractJdbcRowConverter(RowType rowType) {
        super(rowType);
    }

    public AbstractJdbcRowConverter(int converterSize) {
        super(converterSize);
    }

    @Override
    public RowData toInternal(ResultSet resultSet) throws Exception {
        GenericRowData genericRowData = new GenericRowData(rowType.getFieldCount());
        for (int pos = 0; pos < rowType.getFieldCount(); pos++) {
            Object field = resultSet.getObject(pos + 1);
            genericRowData.setField(pos, toInternalConverters[pos].deserialize(field));
        }
        return genericRowData;
    }

    @Override
    public RowData toInternalLookup(JsonArray jsonArray) throws Exception {
        GenericRowData genericRowData = new GenericRowData(rowType.getFieldCount());
        for (int pos = 0; pos < rowType.getFieldCount(); pos++) {
            Object field = jsonArray.getValue(pos);
            genericRowData.setField(pos, toInternalConverters[pos].deserialize(field));
        }
        return genericRowData;
    }

    @Override
    public FieldNamedPreparedStatement toExternal(
            RowData rowData, FieldNamedPreparedStatement statement) throws Exception {
        for (int index = 0; index < rowData.getArity(); index++) {
            toExternalConverters[index].serialize(rowData, index, statement);
        }
        return statement;
    }
}
