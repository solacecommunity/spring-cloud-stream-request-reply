package community.solace.spring.cloud.requestreply;

import community.solace.spring.cloud.requestreply.sampleapps.simple.RequestReplyTestSimpleApplication;
import community.solace.spring.cloud.requestreply.sampleapps.simple.integration.RequestReplyTestEndpoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.listeners.MockCreationListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static community.solace.spring.cloud.requestreply.AbstractRequestReplySimpleIT.PROFILE_LOCAL_APP;
import static community.solace.spring.cloud.requestreply.AbstractRequestReplySimpleIT.PROFILE_TEST_SIMPLE;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ActiveProfiles({PROFILE_TEST_SIMPLE, PROFILE_LOCAL_APP})
@SpringBootTest()
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(classes = {
        RequestReplyTestSimpleApplication.class,
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "SOLACE_HOSTS=tcps://solace.cloud:55443",
        "SOLACE_MSG_VPN=vpn",
        "SOLACE_USERNAME=user",
        "SOLACE_PASSWORD=password",
        "HOSTNAME=test"
})
public abstract class AbstractRequestReplySimpleIT {
    public static final String PROFILE_TEST_SIMPLE = "testSimple";
    public static final String PROFILE_LOCAL_APP = "localApp";

    public final Timestamp Ten_oClock = new Timestamp(1682928000); // 2023-05-01 10:00:00
    public final Timestamp Eleven_oClock = new Timestamp(1682931600); // 2023-05-01 11:00:00
    public final Timestamp Twelve_oClock = new Timestamp(1682935200); // 2023-05-01 12:00:00

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
    private void validateMocks() {
        if (!mocks.isEmpty()) {
            Mockito.verifyNoMoreInteractions(mocks.toArray());
        }
    }

    @AfterEach
    private void validateTestEndpoint() {
        try {
            assertFalse(testEndpoint.hasExceptions());
        }
        finally {
            testEndpoint.reset();
        }
    }
}
