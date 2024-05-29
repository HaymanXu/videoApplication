package LambdaCronFunctions.LikeOrDislike;

import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.internal.IteratorSupport;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.s3.endpoints.internal.GetAttr;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LikeHandlerTest {

    @Mock
    private DynamoDB mockedDynamoDB;
    @Mock
    private Table mockedTable;
    @Mock
    private GetAttr.Part.Index mockedIndex;
    @Mock
    private ItemCollection<QueryOutcome> mockedItemCollection;
    @Mock
    private Iterator<Item> mockedItemIterator;
    @Mock
    private UpdateItemOutcome mockedUpdateItemOutcome;

    private LikeHandler handler;
    private APIGatewayProxyRequestEvent requestEvent;
    private Context context;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        handler = new LikeHandler();  // Assuming DynamoDB is initialized inside the handler if not passed via constructor
        when(mockedDynamoDB.getTable(anyString())).thenReturn(mockedTable);
        requestEvent = new APIGatewayProxyRequestEvent();
        context = mock(Context.class);
    }

    @Test
    void testValidLikeOperation() throws Exception {
        requestEvent.setBody("{\"video_id\": \"123\", \"user_id\": \"456\"}");
        setupMockedDynamoDBResponses(true, -1);

        APIGatewayProxyResponseEvent response = handler.handleRequest(requestEvent, context);
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("updateLikeOrDislikeTableItemOutcome"));
    }

    @Test
    void testInvalidInput() {
        requestEvent.setBody("invalid json");

        APIGatewayProxyResponseEvent response = handler.handleRequest(requestEvent, context);
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid input provided"));
    }

    @Test
    void testNoItemToUpdate() throws Exception {
        requestEvent.setBody("{\"video_id\": \"123\", \"user_id\": \"456\"}");
        setupMockedDynamoDBResponses(false, 0);

        APIGatewayProxyResponseEvent response = handler.handleRequest(requestEvent, context);
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("No item found"));
    }

    private void setupMockedDynamoDBResponses(boolean itemExists, int likeValue) throws Exception {
        when(mockedTable.query(any(QuerySpec.class))).thenReturn(mockedItemCollection);
        when(mockedItemCollection.iterator()).thenReturn((IteratorSupport<Item, QueryOutcome>) mockedItemIterator);
        when(mockedItemIterator.hasNext()).thenReturn(itemExists);

        if (itemExists) {
            Item mockedItem = new Item()
                    .withPrimaryKey("video_id", "123", "user_id", "456")
                    .withInt("like", likeValue);
            when(mockedItemIterator.next()).thenReturn(mockedItem);
        }

        when(mockedTable.updateItem(any(UpdateItemSpec.class))).thenReturn(mockedUpdateItemOutcome);
    }
}