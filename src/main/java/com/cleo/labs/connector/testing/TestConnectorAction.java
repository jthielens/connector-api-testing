package com.cleo.labs.connector.testing;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import com.cleo.connector.api.ConnectorClient;
import com.cleo.connector.api.directory.Entry;
import com.cleo.connector.api.interfaces.IConnectorIncoming;
import com.cleo.connector.api.interfaces.IConnectorOutgoing;
import com.cleo.connector.api.property.ConnectorPropertyException;
import com.cleo.connector.shell.CommandProcessor;
import com.cleo.connector.shell.ConnectorHostException;
import com.cleo.connector.shell.interfaces.IConnector;
import com.cleo.connector.shell.interfaces.IConnectorAction;
import com.cleo.connector.shell.interfaces.IConnectorCommand;

public class TestConnectorAction implements IConnectorAction {
    private Map<String,String> values;
    private CommandProcessor commandProcessor;
    private IConnector connector;
    private ConnectorClient connectorClient;

    public TestConnectorAction(IConnector connector, ConnectorClient connectorClient) throws ConnectorPropertyException {
        this.values = new HashMap<>();
        this.commandProcessor = null; // defer construction because of circular linkages between connector shell classes
        this.connector = connector;
        this.connectorClient = connectorClient;
    }

    public TestConnectorAction set(String key, String value) {
        values.put(key, value);
        return this;
    }

    @Override
    public boolean isInterrupted() {
        return false;
    }

    @Override
    public String resolveSource(String source) throws ConnectorHostException {
        return source;
    }

    @Override
    public String resolveHostCommand(String command, String filename, String destination)
            throws ConnectorHostException {
        return command;
    }

    @Override
    public String resolveDestination(String destination, Optional<String> filename) throws ConnectorHostException {
        return destination;
    }

    @Override
    public String genTransferId(String schemeName) {
        return schemeName+"-"+UUID.randomUUID().toString();
    }

    @Override
    public IConnectorOutgoing[] findFiles(IConnectorCommand connectorCommand)
            throws ConnectorHostException, IOException {
        throw new ConnectorHostException("not implemented");
    }

    @Override
    public IConnectorIncoming getIncoming(IConnectorCommand connectorCommand, Entry entry, String transferId)
            throws ConnectorHostException, IOException {
        throw new ConnectorHostException("not implemented");
    }

    @Override
    public void getFilterOutputStream(IConnectorIncoming incoming, boolean append)
            throws ConnectorHostException, IOException {
        if (append) {
            throw new ConnectorHostException("not implemented");
        }
        // no-op
    }

    @Override
    public void getFilterInputStream(IConnectorOutgoing outgoing) throws ConnectorHostException, IOException {
        // no-op
    }

    @Override
    public IConnectorCommand parseCommand(String command) throws ConnectorHostException {
        throw new ConnectorHostException("not implemented");
    }

    @Override
    public boolean match(String pattern, String name) {
        return Pattern.matches(pattern, name);
    }

    @Override
    public Optional<String> getPropertyValue(String key) {
        return Optional.ofNullable(values.get(key));
    }

    @Override
    public String genTransferId(String schemeName, String transferId) {
        return genTransferId(schemeName);
    }

    @Override
    public CommandProcessor getCommandProcessor() {
        synchronized (this) {
            if (commandProcessor == null) {
                try {
                    commandProcessor = new CommandProcessor(connector, connectorClient);
                } catch (ConnectorPropertyException e) {
                    connectorClient.getLogger().logThrowable(e);
                }
            }
            return commandProcessor;
        }
    }

    @Override
    public String getInbox() throws Exception {
        throw new ConnectorHostException("not implemented");
    }

    @Override
    public String getOutbox() throws Exception {
        throw new ConnectorHostException("not implemented");
    }

    @Override
    public String getSentbox() throws Exception {
        throw new ConnectorHostException("not implemented");
    }

    @Override
    public String getReceivedbox() throws Exception {
        throw new ConnectorHostException("not implemented");
    }
}