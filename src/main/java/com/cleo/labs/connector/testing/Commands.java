package com.cleo.labs.connector.testing;

import static com.cleo.connector.api.command.ConnectorCommandName.DELETE;
import static com.cleo.connector.api.command.ConnectorCommandName.DIR;
import static com.cleo.connector.api.command.ConnectorCommandName.GET;
import static com.cleo.connector.api.command.ConnectorCommandName.MKDIR;
import static com.cleo.connector.api.command.ConnectorCommandName.PUT;
import static com.cleo.connector.api.command.ConnectorCommandName.RENAME;
import static com.cleo.connector.api.command.ConnectorCommandName.RMDIR;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.cleo.connector.api.ConnectorClient;
import com.cleo.connector.api.ConnectorException;
import com.cleo.connector.api.command.ConnectorCommandOption;
import com.cleo.connector.api.command.ConnectorCommandResult;
import com.cleo.connector.api.command.DirCommand;
import com.cleo.connector.api.command.GetCommand;
import com.cleo.connector.api.command.OtherCommand;
import com.cleo.connector.api.command.PutCommand;
import com.cleo.connector.api.directory.Directory.Type;
import com.cleo.connector.api.directory.Entry;
import com.cleo.connector.api.interfaces.IConnectorIncoming;
import com.cleo.connector.api.interfaces.IConnectorOutgoing;

public class Commands {

    static private final Set<String> NO_OPTIONS = Collections.emptySet();
    static private final Map<String, Object> NO_PARAMETERS = Collections.emptyMap();
    static private final String NO_DESTINATION = null;

    static public Dir dir(String path) {
        return new Dir().path(path);
    }

    static public Put put(IConnectorOutgoing source, String path) {
        return new Put().source(source).destination(path);
    }

    static public Get get(String path, IConnectorIncoming destination) {
        return new Get().source(path).destination(destination);
    }

    static public Delete delete(String path) {
        return new Delete().source(path);
    }

    static public Mkdir mkdir(String path) {
        return new Mkdir().source(path);
    }

    static public Rmdir rmdir(String path) {
        return new Rmdir().source(path);
    }

    static public Rename rename(String source, String destination) {
        return new Rename().source(source).destination(destination);
    }

    static public Attr attr(String path) {
        return new Attr().source(path);
    }

    static public class Dir {
        private String path = null;
        private Set<String> options = new HashSet<>();
        private String pattern = null;

        public Dir path(String path) {
            this.path = path;
            return this;
        }

        public Dir option(String option) {
            this.options.add(option);
            return this;
        }

        public Dir pattern(String pattern) {
            this.pattern = pattern;
            return this;
        }

        public ConnectorCommandResult go(ConnectorClient client) throws ConnectorException, IOException {
            DirCommand command = new DirCommand(DIR.name(), options, new Entry(Type.dir).setPath(path), pattern,
                    NO_DESTINATION, NO_PARAMETERS);
            return client.doDir(command);
        }
    }

    static public class Put {
        private IConnectorOutgoing source = null;
        private String destination = null;
        private Set<String> options = new HashSet<>();
        private Map<String, Object> parameters = new HashMap<>();

        public Put source(IConnectorOutgoing source) {
            this.source = source;
            return this;
        }

        public Put destination(String destination) {
            this.destination = destination;
            return this;
        }

        public Put option(String option) {
            this.options.add(option);
            return this;
        }

        public Put option(ConnectorCommandOption option) {
            return option(option.toString());
        }

        public Put parameter(String parameter, Object value) {
            this.parameters.put(parameter, value);
            return this;
        }

        public ConnectorCommandResult go(ConnectorClient client) throws ConnectorException, IOException {
            PutCommand command = new PutCommand(PUT, options, new IConnectorOutgoing[] { source },
                    new Entry(Type.file).setPath(destination), parameters);
            return client.doPut(command);
        }
    }

    static public class Get {
        private String source = null;
        private IConnectorIncoming destination = null;
        private Set<String> options = new HashSet<>();
        private Map<String, Object> parameters = new HashMap<>();

        public Get source(String source) {
            this.source = source;
            return this;
        }

        public Get destination(IConnectorIncoming destination) {
            this.destination = destination;
            return this;
        }

        public Get option(String option) {
            this.options.add(option);
            return this;
        }

        public Get option(ConnectorCommandOption option) {
            return option(option.toString());
        }

        public Get parameter(String parameter, Object value) {
            this.parameters.put(parameter, value);
            return this;
        }

        public ConnectorCommandResult go(ConnectorClient client) throws ConnectorException, IOException {
            GetCommand command = new GetCommand(GET, options, new Entry(Type.file).setPath(source), destination,
                    parameters);
            return client.doGet(command);
        }
    }

    static public class Delete {
        private String source = null;
        private Set<String> options = new HashSet<>();

        public Delete source(String source) {
            this.source = source;
            return this;
        }

        public Delete option(String option) {
            this.options.add(option);
            return this;
        }

        public Delete option(ConnectorCommandOption option) {
            return option(option.toString());
        }

        public ConnectorCommandResult go(ConnectorClient client) throws ConnectorException, IOException {
            OtherCommand command = new OtherCommand(DELETE.name(), options, source, NO_DESTINATION, NO_PARAMETERS,
                    DELETE.name() + " " + source);
            return client.doOther(command);
        }
    }

    static public class Mkdir {
        private String source = null;

        public Mkdir source(String source) {
            this.source = source;
            return this;
        }

        public ConnectorCommandResult go(ConnectorClient client) throws ConnectorException, IOException {
            OtherCommand command = new OtherCommand(MKDIR.name(), NO_OPTIONS, source, NO_DESTINATION, NO_PARAMETERS,
                    MKDIR.name() + " " + source);
            return client.doOther(command);
        }
    }

    static public class Rmdir {
        private String source = null;

        public Rmdir source(String source) {
            this.source = source;
            return this;
        }

        public ConnectorCommandResult go(ConnectorClient client) throws ConnectorException, IOException {
            OtherCommand command = new OtherCommand(RMDIR.name(), NO_OPTIONS, source, NO_DESTINATION, NO_PARAMETERS,
                    RMDIR.name() + " " + source);
            return client.doOther(command);
        }
    }

    static public class Rename {
        private String source = null;
        private String destination = null;

        public Rename source(String source) {
            this.source = source;
            return this;
        }

        public Rename destination(String destination) {
            this.destination = destination;
            return this;
        }

        public ConnectorCommandResult go(ConnectorClient client) throws ConnectorException, IOException {
            OtherCommand command = new OtherCommand(RENAME.name(), NO_OPTIONS, source, destination, NO_PARAMETERS,
                    RENAME.name() + " " + source + " " + destination);
            return client.doOther(command);
        }
    }

    static public class Attr {
        private String source = null;

        public Attr source(String source) {
            this.source = source;
            return this;
        }

        public BasicFileAttributeView go(ConnectorClient client) throws ConnectorException, IOException {
            return (BasicFileAttributeView)client.doGetAttributes(source);
        }
    }
}
