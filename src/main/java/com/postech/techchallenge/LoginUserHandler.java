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
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

public class LoginUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final CognitoUserService cognitoUserService;
  private final String appClientId;
  private final String appClientSecret;

  public LoginUserHandler() {
    this.cognitoUserService = new CognitoUserService(System.getenv("AWS_REGION"));
    this.appClientId = System.getenv("MY_COGNITO_POOL_APP_CLIENT_ID");//EncryptUtils.decryptKey("MY_COGNITO_POOL_APP_CLIENT_ID");
    this.appClientSecret =
        System.getenv("MY_COGNITO_POOL_APP_CLIENT_SECRET");//EncryptUtils.decryptKey("MY_COGNITO_POOL_APP_CLIENT_SECRET");
  }

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
    LambdaLogger logger  = context.getLogger();

    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");

    APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
        .withHeaders(headers);

    JsonObject loginDetails;

    try {
      if (input.getBody() == null ||  input.getBody().isBlank()) {
        logger.log("Not registered user. Using anonymous user");
        loginDetails = JsonParser.parseString(getAnonymousUser()).getAsJsonObject();
        logger.log("Anonymous User: " +  loginDetails);
      } else {
        loginDetails = JsonParser.parseString(input.getBody()).getAsJsonObject();
      }

      JsonObject loginResult = cognitoUserService.userLogin(loginDetails, appClientId, appClientSecret);
      response.withBody(new Gson().toJson(loginResult, JsonObject.class));
      response.withStatusCode(200);
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

  public String getAnonymousUser() {
    Map<String, Object> jsonMap = new HashMap<>();
    jsonMap.put("email", "techchallengefiapalex@gmail.com");
    jsonMap.put("password", "Admin123@");

    return new Gson().toJson(jsonMap);
  }
}