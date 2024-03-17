package com.postech.techchallenge;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.postech.techchallenge.service.CognitoUserService;
import com.postech.techchallenge.util.ErrorResponse;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

/**
 * Handler for requests to Lambda function.
 */
public class RegisterUser implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final CognitoUserService cognitoUserService;
  private final String appClientId;
  private final String appClientSecret;

  public RegisterUser(CognitoUserService cognitoUserService,
                      String appClientId,
                      String appClientSecret) {
    this.cognitoUserService = cognitoUserService;
    this.appClientId = appClientId;
    this.appClientSecret = appClientSecret;
  }

  public RegisterUser() {
    this.cognitoUserService  = new CognitoUserService(System.getenv("AWS_REGION"));
    this.appClientId = System.getenv("MY_COGNITO_POOL_APP_CLIENT_ID");//EncryptUtils.decryptKey("MY_COGNITO_POOL_APP_CLIENT_ID");
    this.appClientSecret = System.getenv("MY_COGNITO_POOL_APP_CLIENT_SECRET");//EncryptUtils.decryptKey("MY_COGNITO_POOL_APP_CLIENT_SECRET");
  }

  public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input,
                                                    final Context context) {
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");

    APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
        .withHeaders(headers);

    String requestBody = input.getBody();
    LambdaLogger logger  = context.getLogger();

    JsonObject userDetails = null;

    try {
      userDetails = JsonParser.parseString(requestBody).getAsJsonObject();
      JsonObject createUserResult = cognitoUserService.createUser(userDetails,
          appClientId,
          appClientSecret);
      response.withStatusCode(200);
      response.withBody(new Gson().toJson(createUserResult, JsonObject.class));
    } catch (AwsServiceException ex) {
      logger.log(ex.awsErrorDetails().errorMessage());
      ErrorResponse errorResponse = new ErrorResponse(ex.awsErrorDetails().errorMessage());
      String errorResponseJsonString = new Gson().toJson(errorResponse, ErrorResponse.class);
      response.withBody(errorResponseJsonString);
      response.withStatusCode(ex.awsErrorDetails().sdkHttpResponse().statusCode());
    } catch (Exception ex) {
      logger.log(ex.getMessage());
      ErrorResponse errorResponse = new ErrorResponse(ex.getMessage());
      String errorResponseJsonString = new GsonBuilder().serializeNulls().create().toJson(errorResponse, ErrorResponse.class);
      response.withBody(errorResponseJsonString);
      response.withStatusCode(500);
    }

    return response;

  }

}
