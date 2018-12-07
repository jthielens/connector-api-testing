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
import com.cleo.connector.api.property.ConnectorPropertyException;
import com.cleo.connector.shell.interfaces.IConnectorHost;
import com.google.common.io.ByteStreams;

public class TestConnectorClientBuilder {

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

    public TestConnectorClientBuilder logger(PrintStream logger) {
        this.logger = logger;
        return this;
    }

    public TestConnectorClientBuilder set(String...keyValue) {
        if (keyValue.length % 2 != 0) {
            throw new IllegalArgumentException(String.format("key value list cannot have an odd length (%d) - missing value?", keyValue.length));
        }
        for (int i = 0; i < keyValue.length; i += 2) {
            settings.add(new Setting(keyValue[i], keyValue[i+1]));
        }
        return this;
    }

    public TestConnectorClientBuilder set(UnaryOperator<TestConnector> setter) {
        settings.add(new Setting(setter));
        return this;
    }

    public TestConnectorClientBuilder debug(boolean debug) {
        settings.add(new Setting(CommonProperty.EnableDebug.name(), String.valueOf(debug)));
        return this;
    }

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

    public static ConnectorClient setup(Class<? extends ConnectorConfig> schemaClass, UnaryOperator<TestConnector> settings, Object...args) {
        Client clientAnnotation = schemaClass.getAnnotation(Client.class);
        if (clientAnnotation != null) {
            try {
                ConnectorConfig schema;
                schema = schemaClass.newInstance();
                schema.setup();
                Class<? extends ConnectorClient> clientClass = clientAnnotation.value();
                List<Class<?>> constructorTypes = new ArrayList<>();
                List<Object> constructorArgs = new ArrayList<>();
                constructorTypes.add(schemaClass);
                constructorArgs.add(schema);
                for (Object arg : args) {
                    constructorTypes.add(arg.getClass());
                    constructorArgs.add(arg);
                }
                //Constructor<? extends ConnectorClient> constructor = clientClass.getDeclaredConstructor(constructorTypes.toArray(new Class<?>[constructorTypes.size()]));
                Constructor<? extends ConnectorClient> constructor = findConstructor(clientClass, constructorArgs);
                ConnectorClient client = constructor.newInstance(constructorArgs.toArray());
                TestConnector connector = new TestConnector(System.err, client);
                settings.apply(connector);
                IConnectorHost connectorHost = new TestConnectorHost(client);
                client.setup(connector, schema, connectorHost);
                return client;
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                    SecurityException | IllegalArgumentException | InvocationTargetException |
                    ConnectorPropertyException e) {
                // fall through and return null
                e.printStackTrace();
            }
        }
        return null;
    }

}
