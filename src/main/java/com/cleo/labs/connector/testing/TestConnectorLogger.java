package com.cleo.labs.connector.testing;

import java.io.PrintStream;
import java.util.Optional;

import com.cleo.connector.api.command.ConnectorCommandResult;
import com.cleo.connector.api.command.ConnectorCommandResult.Status;
import com.cleo.connector.api.directory.Entry;
import com.cleo.connector.api.helper.Attributes;
import com.cleo.connector.api.interfaces.IConnectorIncoming;
import com.cleo.connector.api.interfaces.IConnectorOutgoing;
import com.cleo.connector.api.logging.CommandResultParms;
import com.cleo.connector.api.logging.CommandResultParms.ResultLogger;
import com.cleo.connector.api.logging.DetailParms;
import com.cleo.connector.api.logging.DetailParms.DetailLogger;
import com.cleo.connector.api.logging.FileParms;
import com.cleo.connector.api.logging.FileParms.FileLogger;
import com.cleo.connector.api.logging.RequestParms;
import com.cleo.connector.api.logging.RequestParms.RequestLogger;
import com.cleo.connector.api.logging.ResponseParms;
import com.cleo.connector.api.logging.ResponseParms.ResponseLogger;
import com.cleo.connector.api.logging.ThrowableParms;
import com.cleo.connector.api.logging.ThrowableParms.ThrowableLogger;
import com.cleo.connector.shell.interfaces.IConnectorLogger;
import com.cleo.connector.shell.streams.ConnectorPipedInputStream;
import com.cleo.connector.shell.streams.ConnectorPipedOutputStream;
import com.google.common.base.Strings;

public class TestConnectorLogger implements IConnectorLogger {
    private PrintStream out;

    private static String pad(String prefix, Optional<String> s) {
        if (s.isPresent()) {
            return prefix+s.get().toString();
        }
        return "";
    }
    private static String pad(String prefix, long value) {
        if (value > 0) {
            return prefix+String.valueOf(value);
        }
        return "";
    }
    private static String pad(String prefix, String s) {
        if (!Strings.isNullOrEmpty(s)) {
            return prefix+s;
        }
        return "";
    }
    private static String padDate(String prefix, long date) {
        if (date > 0) {
            return prefix+Attributes.toLocalDateTime(date).toString();
        }
        return "";
    }
    private static String pad(Entry e) {
        if (e != null) {
            return String.format(" \"%s\"", e.getPath());
        }
        return "";
    }

    /*
    @Override
    public void logFile(IConnectorIncoming incoming, Entry entry, int fileNum, int fileCount) {
    }

    @Override
    public void logFile(IConnectorOutgoing outgoing, Entry entry, int fileNum, int fileCount) {
    }

    @Override
    public ConnectorCommandResult logResult(ConnectorCommandResult connectorCommandResult) {
        return connectorCommandResult;
    }

    @Override
    public ConnectorCommandResult logResult(ConnectorCommandResult connectorCommandResult, String copyPath) {
        return connectorCommandResult;
    }

    @Override
    public ConnectorCommandResult logResult(ConnectorCommandResult connectorCommandResult, long fileSize,
            long lastModified, String copyPath) {
        // TODO Auto-generated method stub
        return connectorCommandResult;
    }

    @Override
    public ConnectorCommandResult logResult(Status status, Optional<String> message) {
        return new ConnectorCommandResult(status);
    }

    @Override
    public ConnectorCommandResult logResult(Status status, Optional<String> message, String copyPath) {
        return new ConnectorCommandResult(status);
    }

    @Override
    public ConnectorCommandResult logResult(Status status, Optional<String> message, long fileSize, long lastModified,
            String copyPath) {
        return new ConnectorCommandResult(status);
    }

    @Override
    public void logException(Exception ex, boolean logResult) {
    }

    @Override
    public void logRequest(String requestType, String requestText) {
        out.println(String.format("%s: %s", requestType, requestText));
    }

    @Override
    public void logResponse(Integer responseCode, String responseText, String message, Status status) {
        out.println(String.format("%s (%s): %s - %s", responseText, responseCode, message, status.name()));
    }

    @Override
    public void logResponse(String responseLine, String message, Status status) {
        out.println(String.format("%s: %s - %s", responseLine, message, status.name()));
    }

    @Override
    public void logDetail(String message, int level) {
        out.println(String.format("%d: %s", level, message));
    }

    @Override
    public void logWarning(String message) {
        out.println(String.format("WARN: %s", message));
    }

    @Override
    public void logError(String message) {
        out.println(String.format("ERROR: %s", message));
    }

    @Override
    public void logThrowable(Throwable throwable) {
        out.println(String.format("EXCEPTION: %s", throwable.getMessage()));
    }
    */

    @Override
    public void logHint(String content) {
        out.println(String.format("HINT: %s", content));
    }

    @Override
    public void debug(String message, Throwable throwable) {
        if (message != null || throwable != null) {
            if (message == null) {
                out.println(String.format("DEBUG: %s", throwable.getMessage()));
            } else if (throwable == null) {
                out.println(String.format("DEBUG: %s", message));
            } else {
                out.println(String.format("DEBUG: %s - %s", message, throwable.getMessage()));
            }
        }
    }

    public TestConnectorLogger(PrintStream out) {
        this.out = out;
    }

    @Override
    public FileLogger file() {
        return new FileParms.FileLogger(this);
    }

    @Override
    public void logFile(FileParms parms) {
        if (parms.getIncoming() != null) {
            out.println("FILE: retrieving file "+
                    pad(parms.getEntry())+
                    pad(" to ",destName(parms.getIncoming()))+
                    pad(" file ",parms.getFileNum())+
                    pad(" of ",parms.getFileCount()));
        } else if (parms.getOutgoing() != null) {
            out.println("FILE: storing file "+
                    sourceName(parms.getOutgoing())+
                    pad(" to ",pad(parms.getEntry()))+
                    pad(" file ",parms.getFileNum())+
                    pad(" of ",parms.getFileCount()));
        }
    }

    private String sourceName(IConnectorOutgoing outgoing) {
        String result = null;
        if (outgoing.isFile()) {
            result = outgoing.getFile().getRelative();
        } else {
            if (outgoing.getStream() instanceof ConnectorPipedInputStream) {
                String uriResourcePath = ((ConnectorPipedInputStream)outgoing.getStream()).getUriResourcePath();
                if (uriResourcePath != null) {
                    result = uriResourcePath;
                }
            }
            if (result == null) {
                result = "*stream*"+pad("/", outgoing.getName());
            }
        }
        return result;
    }

    private String destName(IConnectorIncoming incoming) {
        String result = null;
        if (incoming.isFile()) {
            result = incoming.getFile().getRelative();
        } else {
            if (incoming.getStream() instanceof ConnectorPipedOutputStream) {
                String uriResourcePath = ((ConnectorPipedOutputStream)incoming.getStream()).getUriResourcePath();
                if (uriResourcePath != null) {
                    result = uriResourcePath;
                }
            }
            if (result == null) {
                result = "*stream*"+pad("/", incoming.getName());
            }
        }
        return result;
    }

    @Override
    public ResultLogger commandResult() {
        return new CommandResultParms.ResultLogger(this);
    }

    @Override
    public ConnectorCommandResult logCommandResult(CommandResultParms parms) {
        ConnectorCommandResult result = parms.getConnectorCommandResult();
        if (result != null) {
            if (result.getStatus() == ConnectorCommandResult.Status.Exception && result.getException().isPresent()) {
                out.println("EXCEPTION: "+result.getException().get());
            } else if (result.isSuccess()) {
                if (result.getDirEntries().isPresent()) {
                    result.getDirEntries().get().forEach(e -> out.println("ENTRY: "+e.toString()));
                }
            }
        } else {
            result = new ConnectorCommandResult(parms.getStatus(), parms.getStatusMessage());
        }
        out.println("RESULT: "+parms.getStatus().name()+
                pad(" ", parms.getStatusMessage())+
                pad(" cwd=", result.getDirCurrent())+
                pad(" size=", parms.getFileSize())+
                padDate(" modified=", parms.getLastModified())+
                pad(" copyPath=", parms.getCopyPath())+
                (parms.isDeleteFailed() ? "deleteError" : ""));
        return result;
    }

    @Override
    public RequestLogger request() {
        return new RequestParms.RequestLogger(this);
    }

    @Override
    public void logRequest(RequestParms parms) {
        out.println(String.format("REQUEST: %s type=%s", parms.getRequestText(), parms.getRequestType()));
    }

    @Override
    public ResponseLogger response() {
        return new ResponseParms.ResponseLogger(this);
    }

    @Override
    public void logResponse(ResponseParms parms) {
        // get default value from input parms -- perhaps they'll be adjusted within this method
        String responseLine = parms.getResponseLine();
        Status status = parms.getStatus();
        String message = parms.getStatusMessage();

        // possibly adjust responseLine
        if (parms.getResponseCode() != null &&
            (parms.getResponseText() == null || !parms.getResponseText().startsWith(parms.getResponseCode().toString()))) {
            responseLine = parms.getResponseCode().toString();
        }
        if (parms.getResponseText() != null) {
            if (responseLine.length() > 0) {
              responseLine += " ";
            }
            responseLine += parms.getResponseText();
        }

        // possibly adjust status
        if (status == null && parms.getResponseCode() != null) {
            if (parms.getResponseCode() >= 400) {
                status = Status.Error;
            }
        }

        // finally, log the response event
        out.println("RESULT: "+status+
                pad(" ", responseLine)+
                pad(" ", message));
    }

    @Override
    public DetailLogger detail() {
        return new DetailParms.DetailLogger(this);
    }

    @Override
    public void logDetail(DetailParms parms) {
        out.println((parms.isError() ? "ERROR:" : parms.isWarning() ? "WARNING:" : "DETAIL:")+
                pad(" message=",parms.getMessage())+
                pad(" level=",parms.getLevel()));
    }

    @Override
    public DetailLogger warning() {
        return new DetailParms.DetailLogger(this).setWarning(true);
    }

    @Override
    public DetailLogger error() {
        return new DetailParms.DetailLogger(this).setError(true);
    }

    @Override
    public ThrowableLogger throwable() {
        return new ThrowableParms.ThrowableLogger(this);
    }

    @Override
    public void logThrowable(ThrowableParms parms) {
        Throwable e = parms.getThrowable();
        if (e instanceof Exception) {
            out.println("EXCEPTION: "+e.toString());
        } else {
            out.println("ERROR: "+e.toString());
        }
        e.printStackTrace(out);
    }
}
