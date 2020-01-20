package com.supconit.k8sdeploy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;

/**
 * @author 黄珺
 * @date 2020/1/9
 **/
@Data
@Configuration
@ConfigurationProperties(prefix = "kubernetes.deploy")
public class DeployConfig {

    /**
     * kubernetes任意节点的IP
     */
    private String ip;

    /**
     * 部署应用的命令空间
     */
    private String namespace;

    /**
     * kuboard的token
     */
    private String token;

    /**
     * 镜像中心的IP
     */
    private String harborIp;

    @PostConstruct
    public void init() {
        if (StringUtils.isEmpty(ip)) {
            throw new IllegalArgumentException("k8s节点IP(任意节点)不能为空");
        }
        if (StringUtils.isEmpty(namespace)) {
            throw new IllegalArgumentException("命名空间不能为空");
        }
        if (StringUtils.isEmpty(token)) {
            throw new IllegalArgumentException("kuboard token不能为空");
        }
        if (StringUtils.isEmpty(harborIp)) {
            throw new IllegalArgumentException("harborIp不能为空");
        }

        setToken("Bearer " + getToken());
    }
}
