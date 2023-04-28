package ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.errorMessage;

public class RemoteErrorException extends Exception {

    public RemoteErrorException(String errorMessage) {
        super(errorMessage);
    }
}
