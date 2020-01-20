package com.supconit.k8sdeploy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * @author 黄珺
 * @date 2020/1/9
 */
@EnableConfigurationProperties
@SpringBootApplication
public class K8sDeployApplication {

    public static void main(String[] args) {
        SpringApplication.run(K8sDeployApplication.class, args);
    }

}
