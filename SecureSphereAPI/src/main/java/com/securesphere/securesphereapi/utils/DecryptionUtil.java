package com.securesphere.securesphereapi.utils;

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.util.Base64;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class DecryptionUtil {
    public static String decryptKey(String keyName) {
        String result = "";
        try {
            byte[] encryptedKey = Base64.decode(System.getenv(keyName));
        /*Map<String, String> encryptionContext = new HashMap<>();
        encryptionContext.put("LambdaFunctionName",
                System.getenv("AWS_LAMBDA_FUNCTION_NAME"));*/
            AWSKMS client = AWSKMSClientBuilder.defaultClient();
            DecryptRequest request = new DecryptRequest()
                    .withCiphertextBlob(ByteBuffer.wrap(encryptedKey));
            //.withEncryptionContext(encryptionContext);
            //https://docs.aws.amazon.com/kms/latest/developerguide/kms-vpc-endpoint.html
            ByteBuffer plainTextKey = client.decrypt(request).getPlaintext();
            result = new String(plainTextKey.array(), Charset.forName("UTF-8")).trim();
        } catch (Exception e) {
            System.out.println("msg=exception_occurred_while_decrypting_key "+e.getMessage());
        }
        return result;
    }
}
