package ch.sbb.tms.capaopt.springbootstarter.requestreply;

import static ch.sbb.tms.capaopt.springbootstarter.requestreply.AbstractRequestReplyIT.PROFILE_LOCAL_APP;
import static ch.sbb.tms.capaopt.springbootstarter.requestreply.AbstractRequestReplyIT.PROFILE_TEST;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.listeners.MockCreationListener;
import org.mockito.mock.MockCreationSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ch.sbb.tms.capaopt.springbootstarter.requestreply.configuration.RequestReplyTestConfiguration;
import ch.sbb.tms.capaopt.springbootstarter.requestreply.integration.RequestReplyTestEndpoint;

@ActiveProfiles({ PROFILE_TEST, PROFILE_LOCAL_APP })
@SpringBootTest()
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(classes = { //
        RequestReplyTestApplication.class, //
        RequestReplyTestConfiguration.class //
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@AutoConfigureMockMvc
@TestPropertySource(properties = { //
        "SOLACE_HOSTS=tcps://solace.cloud:55443", //
        "SOLACE_MSG_VPN=vpn", //
        "SOLACE_USERNAME=user", //
        "SOLACE_PASSWORD=password", //
        "HOSTNAME=test" //
})
public abstract class AbstractRequestReplyIT {
    public static final String PROFILE_TEST = "test";
    public static final String PROFILE_LOCAL_APP = "localApp";

    private static List<Object> mocks = new ArrayList<>();

    @Autowired
    protected RequestReplyTestEndpoint testEndpoint;

    static {
        Mockito.framework().addListener(new MockCreationListener() {
            @Override
            public void onMockCreated(Object mock, @SuppressWarnings("rawtypes") MockCreationSettings settings) {
                mocks.add(mock);
            }
        });
    }

    @BeforeEach
    private void resetMocks() {
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
