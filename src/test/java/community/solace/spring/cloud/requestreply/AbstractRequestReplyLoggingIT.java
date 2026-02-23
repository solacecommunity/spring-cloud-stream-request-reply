package community.solace.spring.cloud.requestreply;

import community.solace.spring.cloud.requestreply.sampleapps.logging.RequestReplyTestLoggingApplication;
import community.solace.spring.cloud.requestreply.sampleapps.logging.integration.RequestReplyTestEndpoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.listeners.MockCreationListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static community.solace.spring.cloud.requestreply.AbstractRequestReplyLoggingIT.PROFILE_LOCAL_APP;
import static community.solace.spring.cloud.requestreply.AbstractRequestReplyLoggingIT.PROFILE_TEST_LOGGING;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ActiveProfiles({PROFILE_TEST_LOGGING, PROFILE_LOCAL_APP})
@SpringBootTest()
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(classes = {
        RequestReplyTestLoggingApplication.class,
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "SOLACE_MSG_VPN=vpn",
        "SOLACE_USERNAME=user",
        "SOLACE_PASSWORD=password",
        "HOSTNAME=test"
})
public abstract class AbstractRequestReplyLoggingIT {
    public static final String PROFILE_TEST_LOGGING = "testLogging";
    public static final String PROFILE_LOCAL_APP = "localApp";

    public final Timestamp Ten_oClock = new Timestamp(1682928000000L); // 2023-05-01 10:00:00
    public final Timestamp Eleven_oClock = new Timestamp(1682931600000L); // 2023-05-01 11:00:00
    public final Timestamp Twelve_oClock = new Timestamp(1682935200000L); // 2023-05-01 12:00:00

    private static List<Object> mocks = new ArrayList<>();

    static {
        Mockito.framework().addListener((MockCreationListener) (mock, settings) -> mocks.add(mock));
    }

    @Autowired
    protected RequestReplyTestEndpoint testEndpoint;

    @BeforeEach
    protected void resetMocks() {
        if (!mocks.isEmpty()) {
            Mockito.reset(mocks.toArray());
        }
    }

    @AfterEach
    public void validateTestEndpoint() {
        try {
            assertFalse(testEndpoint.hasExceptions());
        }
        finally {
            testEndpoint.reset();
        }
    }
}
