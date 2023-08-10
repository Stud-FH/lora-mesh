package v2.shared.api.domain;

import v2.core.log.Logger;

public class LogEntry {
    public Logger.Severity severity;
    public String moduleInfo;
    public byte[] data;
}
