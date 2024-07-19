package com.securesphere.securesphereapi;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.CognitoUserPoolPostConfirmationEvent;
import com.securesphere.securesphereapi.service.DataBaseService;
import com.securesphere.securesphereapi.utils.DecryptionUtil;
import com.securesphere.securesphereapi.utils.LogUtil;

import java.util.Map;

/*
https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-events.html
https://awstip.com/adding-amazon-cognito-users-to-amazon-dynamodb-using-lambda-function-in-java-5e54cb25034c
sam deploy creates this lambda. After this manually add the lambda trigger to cognito. Then as per the
above link add these policies to by going to the execution role Under the Permissions dropdown -
AmazonCognitoPowerUser, AWSLambdaBasicExecutionRole, AWSLambdaRole.
For DB, we might have to add another role.

----------------------------
Automation did not work for me. So commented out the automation part in sam template file.
sam deploy --parameter-overrides UserPoolId=XXXXX

sam deploy --parameter-overrides UserPoolName=XXXX

https://github.com/serverless/serverless/issues/4207
 */
/*
VPC related modifications for lambda
https://repost.aws/knowledge-center/connect-lambda-to-an-rds-instance
(https://stackoverflow.com/questions/41177965/the-provided-execution-role-does-not-have-permissions-to-call-describenetworkint)
1. Create a custom Inline Policy to the Lambda execution role under the Permissions tab. -
    {
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ec2:DescribeNetworkInterfaces",
        "ec2:CreateNetworkInterface",
        "ec2:DeleteNetworkInterface",
        "ec2:DescribeInstances",
        "ec2:AttachNetworkInterface"
      ],
      "Resource": "*"
    }
  ]
}
2. Add Lambda to the vpc of rds.(Only IPV4 as IPV6 was giving an error)
3. Follow https://repost.aws/knowledge-center/connect-lambda-to-an-rds-instance
A Lambda function and RDS instance in the same VPC
4. Attach AWSKeyManagementServicePowerUser policy and this custom policy to Lambda.
{
	"Version": "2012-10-17",
	"Statement": [
		{
			"Effect": "Allow",
			"Action": [
				"kms:Decrypt"
			],
			"Resource": "arn:aws:kms:eu-west-2:981714115215:key/47c79d81-a7e6-428e-8a94-06d82298d67e"
		}
	]
}
4. Create VPC endpoint for the lambda function to interact with the KMS by following this
tutorial. Its just that in the service for the endpoint we need to select KMS(where this guy has selected step function)
https://www.youtube.com/watch?v=jo3X_aay4Vs&t=232s
 */
public class CognitoTriggerHandler implements RequestHandler<CognitoUserPoolPostConfirmationEvent, CognitoUserPoolPostConfirmationEvent> {

    private final String DBUSERNAME;
    private final String DBPASSWORD;
    private final String DBPORT;
    private final String DBNAME;
    private final String DBURL;
    private final DataBaseService dataBaseService;

    public CognitoTriggerHandler(String username, String password, String port,
                                 String dbName, String dbURL,DataBaseService dbService) {
        this.DBUSERNAME=username;
        this.DBPASSWORD=password;
        this.DBPORT=port;
        this.DBNAME=dbName;
        this.DBURL=dbURL;
        this.dataBaseService=dbService;
    }

    public CognitoTriggerHandler() {
        //https://docs.aws.amazon.com/kms/latest/developerguide/kms-vpc-endpoint.html
        //https://stackoverflow.com/questions/40577994/attempting-to-decrypt-ciphertext-within-a-lambda-function-using-kms-results-in-t
        this.DBUSERNAME = DecryptionUtil.decryptKey("DB_USERNAME");
        this.DBPASSWORD = DecryptionUtil.decryptKey("DB_PASSWORD");
        this.DBPORT = DecryptionUtil.decryptKey("DB_PORT");
        this.DBNAME = DecryptionUtil.decryptKey("DB_NAME");
        this.DBURL = DecryptionUtil.decryptKey("DB_URL");
        String fullDBURL = "jdbc:mysql://" + DBURL + ":" + DBPORT + "/" + DBNAME;
        this.dataBaseService = new DataBaseService(fullDBURL);
    }

    @Override
    public CognitoUserPoolPostConfirmationEvent handleRequest(
            CognitoUserPoolPostConfirmationEvent event, Context context) {

        LambdaLogger logger = context.getLogger();
        LogUtil logUtil = new LogUtil(logger);
        String requestId = context.getAwsRequestId();
        String functionName = context.getFunctionName();
        String functionVersion = context.getFunctionVersion();
        logUtil.logInfo(requestId,
                "CognitoUserPoolPostConfirmationEvent input received", "",
                functionName, functionVersion);

        Map<String, String> userAttributes = event.getRequest().getUserAttributes();
        //logUtil.logInfo(requestId, "UserAttributes", userAttributes.entrySet().toString(),functionName, functionVersion);
        String name = userAttributes.get("name");
        String email = userAttributes.get("email");
        String phone = userAttributes.get("phone_number");
        String userUUID = userAttributes.get("custom:userId");
        this.dataBaseService.insertUserData(name, email, phone, userUUID, DBUSERNAME, DBPASSWORD,
                logUtil, requestId, functionName, functionVersion);
        return event;
    }

}
