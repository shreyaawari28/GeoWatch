package com.safety.womensafety.config;

import com.safety.womensafety.service.MetricsService;
import org.junit.jupiter.api.Test;
import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class MetricsProxyDataSourceTest {

    @Test
    void testNullArgsSafety() throws Exception {
        // Create a basic MetricsService instance
        MetricsService metricsService = new MetricsService();

        // Create a mock PreparedStatement using dynamic proxy to return default values
        PreparedStatement mockPreparedStatement = (PreparedStatement) Proxy.newProxyInstance(
                MetricsProxyDataSourceTest.class.getClassLoader(),
                new Class[]{PreparedStatement.class},
                (proxy, method, args) -> {
                    if (method.getReturnType().equals(boolean.class)) {
                        return false;
                    }
                    if (method.getReturnType().equals(int.class)) {
                        return 0;
                    }
                    return null;
                }
        );

        // Create a mock Connection using dynamic proxy that returns the mockPreparedStatement
        Connection mockConnection = (Connection) Proxy.newProxyInstance(
                MetricsProxyDataSourceTest.class.getClassLoader(),
                new Class[]{Connection.class},
                (proxy, method, args) -> {
                    if ("createStatement".equals(method.getName())) {
                        return mockPreparedStatement;
                    }
                    if ("prepareStatement".equals(method.getName())) {
                        return mockPreparedStatement;
                    }
                    if (method.getReturnType().equals(boolean.class)) {
                        return false;
                    }
                    return null;
                }
        );

        // Create a mock DataSource
        DataSource mockDataSource = (DataSource) Proxy.newProxyInstance(
                MetricsProxyDataSourceTest.class.getClassLoader(),
                new Class[]{DataSource.class},
                (proxy, method, args) -> {
                    if ("getConnection".equals(method.getName())) {
                        return mockConnection;
                    }
                    if (method.getReturnType().equals(boolean.class)) {
                        return false;
                    }
                    return null;
                }
        );

        // Wrap the DataSource
        DataSource wrappedDataSource = MetricsProxyDataSource.wrap(mockDataSource, metricsService);
        assertNotNull(wrappedDataSource);

        // Test unwrapping
        // The wrap handler invokes method with args. Calling unwrap without args is not standard,
        // but we want to make sure it handles null args safely.
        // We can simulate method invocations with null args by calling the Proxy handler directly,
        // or by calling methods on the proxy that the JVM maps to null args.
        // For example, getConnection() has 0 parameters, so the proxy handler will receive args = null or args = empty array.
        Connection wrappedConnection = wrappedDataSource.getConnection();
        assertNotNull(wrappedConnection);

        // createStatement() has 0 parameters, so proxy handler will receive args = null
        Statement wrappedStatement = wrappedConnection.createStatement();
        assertNotNull(wrappedStatement);

        // close() has 0 parameters -> args = null
        wrappedConnection.close();

        // getAutoCommit() has 0 parameters -> args = null
        assertFalse(wrappedConnection.getAutoCommit());

        // commit() has 0 parameters -> args = null
        wrappedConnection.commit();

        // rollback() has 0 parameters -> args = null
        wrappedConnection.rollback();

        // isClosed() has 0 parameters -> args = null
        assertFalse(wrappedConnection.isClosed());

        // PreparedStatement execute() has 0 parameters -> args = null
        PreparedStatement wrappedPreparedStatement = (PreparedStatement) wrappedStatement;
        assertFalse(wrappedPreparedStatement.execute());
    }
}
