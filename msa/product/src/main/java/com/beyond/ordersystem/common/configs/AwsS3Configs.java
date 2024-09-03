package com.beyond.ordersystem.common.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsS3Configs {

    @Value("${cloud.aws.credentials.access-key}")
    private String accesskey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String secretkey;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Bean // 스프링 빈 등록 -> 가져다 쓰는 쪽에서는 Autowired해서 쓰면 된다.
    // 사용하는쪽에서 버킷 정보를 입력하면 됨.
    public S3Client s3Client(){
        AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(accesskey, secretkey);
        return S3Client.builder()
                .region(Region.of(region)) // region 정보
                .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
                .build();
    }
}
