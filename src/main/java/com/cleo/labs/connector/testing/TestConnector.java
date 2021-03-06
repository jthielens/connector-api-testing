package com.cleo.labs.connector.testing;

import java.io.File;
import java.io.PrintStream;

import com.cleo.connector.api.ConnectorClient;
import com.cleo.connector.api.interfaces.IConnectorFile;
import com.cleo.connector.api.property.ConnectorPropertyException;
import com.cleo.connector.shell.interfaces.IConnector;
import com.cleo.connector.shell.interfaces.IConnectorAction;
import com.cleo.connector.shell.interfaces.IConnectorConnection;
import com.cleo.connector.shell.interfaces.IConnectorLogger;

public class TestConnector implements IConnector {
    private IConnectorConnection connection;
    private TestConnectorAction action;
    private IConnectorLogger logger;

    public TestConnector(PrintStream out, ConnectorClient connectorClient) throws ConnectorPropertyException {
        this.connection = new TestConnectorConnection();
        this.action = new TestConnectorAction(this, connectorClient);
        this.logger = new TestConnectorLogger(out);
    }

    public TestConnector set(String key, String value) {
        this.action.set(key, value);
        return this;
    }

    @Override
    public IConnectorConnection getConnectorConnection() {
        return connection;
    }

    @Override
    public IConnectorAction getConnectorAction() {
        return action;
    }

    @Override
    public IConnectorLogger getConnectorLogger() {
        return logger;
    }

    @Override
    public IConnectorFile newFile() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public IConnectorFile newFile(File file) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public IConnectorFile newFile(String path) {
        throw new UnsupportedOperationException("not implemented");
    }

}
