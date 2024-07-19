package com.securesphere.securesphereapi;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPostConfirmationEvent;
import com.securesphere.securesphereapi.service.DataBaseService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CognitoTriggerHandlerTest {

    @Mock
    static DataBaseService dataBaseServiceMock;
    @Mock

    CognitoUserPoolPostConfirmationEvent event;
    @Mock
    Context context;
    @Mock
    LambdaLogger lambdaLoggerMock;
    @Mock
    CognitoUserPoolPostConfirmationEvent.Request requestMock;

    @InjectMocks
    CognitoTriggerHandler handler;

    private static Map<String,String> userAttributeMap;

    @BeforeAll
    private static void initializeUserAttributeMap() {
        userAttributeMap=new HashMap<>();
        userAttributeMap.put("name","testName");
        userAttributeMap.put("email","testEmail");
        userAttributeMap.put("phone_number","+448888888888");
        userAttributeMap.put("custom:userId","UUID");
    }
    @Test
    public void testHandleRequest_WhenEventProvided_ReturnEventAfterDBInsertion() {
        when(context.getLogger()).thenReturn(lambdaLoggerMock);
        when(event.getRequest()).thenReturn(requestMock);
        when(requestMock.getUserAttributes()).thenReturn(userAttributeMap);
        doNothing().when(dataBaseServiceMock).insertUserData(any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any());

        CognitoUserPoolPostConfirmationEvent responseEvent = handler.handleRequest(event, context);
        verify(lambdaLoggerMock, times(1)).log(anyString());
    }
}
