package ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.errormessage;

public class RemoteErrorException extends Exception {

    public RemoteErrorException(String errorMessage) {
        super(errorMessage);
    }
}
