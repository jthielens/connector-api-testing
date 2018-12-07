package com.cleo.labs.connector.testing;

import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import com.cleo.connector.api.ConnectorClient;
import com.cleo.connector.api.ConnectorConfig;
import com.cleo.connector.api.ConnectorException;
import com.cleo.connector.api.annotations.Client;
import com.cleo.connector.api.property.CommonProperty;
import com.cleo.connector.shell.interfaces.IConnectorHost;
import com.google.common.io.ByteStreams;

public class TestConnectorClientBuilder {

    /**
     * Finds a constructor compatiable with an argument list using {@code isAssignableFrom}, which
     * is more flexible than just using {@code getDeclaredConstructor} with a list of types.
     * @param clientClass the class to construct
     * @param args the argument list to match
     * @return an appropriate {@code Constructor}
     * @throws NoSuchMethodException if no appropriate constructor can be found
     */
    @SuppressWarnings("unchecked")
    private static Constructor<? extends ConnectorClient> findConstructor(Class<? extends ConnectorClient> clientClass, List<Object> args) throws NoSuchMethodException {
        constructor:
        for (Constructor<?> constructor : clientClass.getDeclaredConstructors()) {
            Class<?>[] types = constructor.getParameterTypes();
            if (types.length == args.size()) {
                for (int i = 0; i < types.length; i++) {
                    if (!types[i].isAssignableFrom(args.get(i).getClass())) {
                        continue constructor;
                    }
                }
                return (Constructor<? extends ConnectorClient>) constructor;
            }
        }
        throw new NoSuchMethodException();
    }

    /**
     * A helper class to keep track of settings to apply in the
     * {@code build} step.
     */
    private static class Setting {
        public String key = null;
        public String value = null;
        public UnaryOperator<TestConnector> setter = null;
        public Setting(String key, String value) {
            this.key = key;
            this.value = value;
        }
        public Setting(UnaryOperator<TestConnector> setter) {
            this.setter = setter;
        }
        public void apply(TestConnector connector) {
            if (key != null && value != null) {
                connector.set(key, value);
            }
            if (setter != null) {
                setter.apply(connector);
            }
        }
    }

    Client clientAnnotation;
    ConnectorConfig schema;
    Class<? extends ConnectorClient> clientClass;
    private PrintStream logger;
    private List<Setting> settings;

    public TestConnectorClientBuilder(Class<? extends ConnectorConfig> schemaClass) throws ConnectorException {
        clientAnnotation = schemaClass.getAnnotation(Client.class);
        if (clientAnnotation == null) {
            throw new ConnectorException(String.format("not a ConnectorShell schema class: @Client annotation not found: %s", schemaClass.getSimpleName()));
        }
        clientClass = clientAnnotation.value();
        try {
            schema = schemaClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ConnectorException(e);
        }
        schema.setup();
        logger = new PrintStream(ByteStreams.nullOutputStream());
        settings = new ArrayList<>();
    }

    /**
     * Select a logging destination (/dev/null by default).
     * @param logger where the log output should go
     * @return {@code this}
     */
    public TestConnectorClientBuilder logger(PrintStream logger) {
        this.logger = logger;
        return this;
    }

    /**
     * Record some settings to apply at {@code build} time.
     * @param keyValue an (even-length-or-else) list of key/value Strings
     * @return {@code this}
     */
    public TestConnectorClientBuilder set(String...keyValue) {
        if (keyValue.length % 2 != 0) {
            throw new IllegalArgumentException(String.format("key value list cannot have an odd length (%d) - missing value?", keyValue.length));
        }
        for (int i = 0; i < keyValue.length; i += 2) {
            settings.add(new Setting(keyValue[i], keyValue[i+1]));
        }
        return this;
    }

    /**
     * Record some settings to apply at {@code build} time.
     * @param setter a {@code UnaryOperator} to apply desired settings to the {@code TestConnector}
     * @return {@code this}
     */
    public TestConnectorClientBuilder set(UnaryOperator<TestConnector> setter) {
        settings.add(new Setting(setter));
        return this;
    }

    /**
     * Set the standard DEBUG flag on or off.
     * @param debug the DEBUG flag setting
     * @return {@code this}
     */
    public TestConnectorClientBuilder debug(boolean debug) {
        settings.add(new Setting(CommonProperty.EnableDebug.name(), String.valueOf(debug)));
        return this;
    }

    /**
     * Build and return a ConnectorClient using the schema and any accumulated
     * settings.  Most ConnectorClient constructors take only the schema instance
     * as an argument, but some have supplementary constructors for unit testing
     * that take additional arguments (e.g. to set up mocks etc.).  The schema
     * object is always passed, but any additional arguments supplied here are also
     * passed, if an appropriate constructor can be found through reflection.
     *
     * @param args the (possibly empty) list of additional constructor arguments
     * @return a ConnectorClient
     * @throws ConnectorException if there is a problem
     */
    public ConnectorClient build(Object...args) throws ConnectorException {
        try {
            List<Object> constructorArgs = new ArrayList<>();
            constructorArgs.add(schema);
            for (Object arg : args) {
                constructorArgs.add(arg);
            }
            Constructor<? extends ConnectorClient> constructor = findConstructor(clientClass, constructorArgs);
            ConnectorClient client = constructor.newInstance(constructorArgs.toArray());
            TestConnector connector = new TestConnector(logger, client);
            settings.forEach(s -> s.apply(connector));
            IConnectorHost connectorHost = new TestConnectorHost(client);
            client.setup(connector, schema, connectorHost);
            return client;
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new ConnectorException(e);
        }
    }

}
