package com.supconit.k8sdeploy.controller;

import com.supconit.k8sdeploy.config.DeployConfig;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

/**
 * @author 黄珺
 * @date 2020/1/9
 **/
@SuppressWarnings("ALL")
@Slf4j
@Api(tags = "kubernetes应用更新部署")
@RestController
@RequestMapping
public class DeployController {

    public static String PATCH_URL_TEMPLATE = "http://%s:32567/k8s-api/apis/apps/v1/namespaces/%s/deployments/svc-%s";

    public static String PATCH_CONTENT_TEMPLATE = "{\"spec\":{\"template\":{\"spec\":{\"containers\":[{\"name\":\"%s\",\"image\":\"%s\"}]}}}}";

    public static String IMAGE_TEMPLATE = "%s/supconit/%s:%s_%s";

    public static String LATEST_IMAGE_TEMPLATE = "%s/supconit/%s:%s_latest";

    public static String DOCKERFILE_TEMPLATE = "FROM java:8-jre\n" +
            "MAINTAINER huangjun <huangjun@supconit.com>\n" +
            "\n" +
            "RUN echo \"Asia/Shanghai\" > /etc/timezone\n" +
            "RUN dpkg-reconfigure -f noninteractive tzdata\n" +
            "\n" +
            "COPY app.jar app.jar\n" +
            "\n" +
            "ENV JAVA_OPTS=\"\"\n" +
            "ENV PARAMS=\"\"\n" +
            "\n" +
            "ENTRYPOINT exec java -server $JAVA_OPTS -jar /app.jar $PARAMS";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private DeployConfig deployConfig;

    @ApiOperation(value = "部署应用", notes = "只能更新版本")
    @PostMapping("/deploy/{applicationName}")
    public String deploy(@PathVariable @ApiParam(value = "应用名称", required = true) String applicationName,
                         @RequestParam @ApiParam(value = "镜像名称", required = true) String imageName,
                         @RequestParam(required = false) @ApiParam("镜像版本号,不填则默认为当前时间戳") String version,
                         @RequestPart(name = "jar") MultipartFile applicationJar) throws IOException {
        if (StringUtils.isEmpty(applicationName)) {
            return "应用名称不能为空";
        }
        if (StringUtils.isEmpty(version)) {
            version = String.valueOf(System.currentTimeMillis() / 1000);
        }

        String image = String.format(IMAGE_TEMPLATE, deployConfig.getHarborIp(), imageName, deployConfig.getNamespace(), version);
        String latestImage = String.format(LATEST_IMAGE_TEMPLATE, deployConfig.getHarborIp(), imageName, deployConfig.getNamespace());

        // 创建应用部署文件夹
        File targetDir = new File("/tmp/" + applicationName);
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        File targetJar = new File("/tmp/" + applicationName + "/app.jar");
        if (targetJar.exists()) {
            targetJar.delete();
        }
        FileCopyUtils.copy(applicationJar.getBytes(), targetJar);
        File dockerFile = new File("/tmp/" + applicationName + "/Dockerfile");
        if (!dockerFile.exists()) {
            dockerFile.createNewFile();
            FileWriter fileWriter = new FileWriter(dockerFile.getAbsoluteFile());
            try (BufferedWriter bw = new BufferedWriter(fileWriter);) {
                bw.write(DOCKERFILE_TEMPLATE);
                bw.close();
            } catch (IOException e) {
                log.error("dockerfile文件写入异常", e);
                return "dockerfile文件写入异常";
            }
        }

        // 打包镜像
        String cmd = String.format("cd %s && docker build -t %s . && docker tag %s %s && docker push %s && docker push %s", "/tmp/" + applicationName, image, image, latestImage, image, latestImage);
        String[] cmds = new String[]{
                "/bin/sh",   
                "-c",
                cmd
        };
        Process exec = null;
        BufferedReader bufrIn = null;
        BufferedReader bufrError = null;
        try {
            log.info("命令[{}]开始执行", cmd);
            exec = Runtime.getRuntime().exec(cmds);
            final boolean success = exec.waitFor() == 0;
            StringBuilder result = new StringBuilder();
            bufrIn = new BufferedReader(new InputStreamReader(exec.getInputStream(), "UTF-8"));
            bufrError = new BufferedReader(new InputStreamReader(exec.getErrorStream(), "UTF-8"));
            // 读取输出
            String line = null;
            while ((line = bufrIn.readLine()) != null) {
                result.append(line).append('\n');
            }
            while ((line = bufrError.readLine()) != null) {
                result.append(line).append('\n');
            }
            log.info("执行结果:\n{}\n执行完成", result);
            if (!success) {
                throw new RuntimeException("打包或上传镜像失败" + result);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            closeStream(bufrIn);
            closeStream(bufrError);

            // 销毁子进程
            if (exec != null) {
                exec.destroy();
            }
        }

        // 发送更新镜像版本请求
        HttpHeaders header = new HttpHeaders();
        header.set("content-type", "application/strategic-merge-patch+json");
        header.set("Authorization", deployConfig.getToken());
        String patchUrl = String.format(PATCH_URL_TEMPLATE, deployConfig.getIp(), deployConfig.getNamespace(), applicationName);
        String patchContent = String.format(PATCH_CONTENT_TEMPLATE, applicationName, image);
        HttpEntity<String> httpEntity = new HttpEntity<>(patchContent, header);
        try {
            String result = restTemplate.patchForObject(patchUrl, httpEntity, String.class);
        } catch (RestClientException ex) {
            return "发布失败：" + ex;
        }

        return applicationName + "发布成功";
    }

    private static void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (Exception e) {
                // nothing
            }
        }
    }
}

