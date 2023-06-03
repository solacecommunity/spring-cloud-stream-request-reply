package community.solace.spring.cloud.requestreply.service.header.parser.errormessage;

public class RemoteErrorException extends Exception {

    public RemoteErrorException(String errorMessage) {
        super(errorMessage);
    }
}
