/*
 * Copyright 2016-2018 shardingsphere.io.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package io.shardingsphere.jdbc.orchestration.internal.circuit.datasource;

import io.shardingsphere.shardingjdbc.jdbc.unsupported.AbstractUnsupportedOperationDataSource;
import io.shardingsphere.jdbc.orchestration.internal.circuit.connection.CircuitBreakerConnection;

import java.io.PrintWriter;
import java.sql.Connection;
import java.util.logging.Logger;

/**
 * Circuit breaker datasource.
 * 
 * @author caohao
 */
public final class CircuitBreakerDataSource extends AbstractUnsupportedOperationDataSource implements AutoCloseable {
    
    @Override
    public void close() {
    }
    
    @Override
    public Connection getConnection() {
        return new CircuitBreakerConnection();
    }
    
    @Override
    public Connection getConnection(final String username, final String password) {
        return new CircuitBreakerConnection();
    }
    
    @Override
    public PrintWriter getLogWriter() {
        return null;
    }
    
    @Override
    public void setLogWriter(final PrintWriter out) {
    }
    
    @Override
    public Logger getParentLogger() {
        return null;
    }
}
