#### 安装

将本应用打成jar包，拷贝至harbor服务器上。

- kubernetes.deploy.ip：k8s集群任意节点的IP
- kubernetes.deploy.token：kuboard访问token
- kubernetes.deploy.namespace：部署应用的命名空间
- kubernetes.deploy.harbor-ip：harbor服务器的IP

使用命令 

`
nohup java --kubernetes.deploy.ip=xx.xx.xx.xx --kubernetes.deploy.token=ej...... --kubernetes.deploy.namespace=its --kubernetes.deploy.harbor-ip=xx.xx.xx.xx -jar k8s-deploy.jar &
`

#### 使用

打开harborip:5000/swagger-ui.html

![swagger-ui](http://172.20.41.234/huangjun/k8s-app-deploy/raw/master/doc/swagger-ui.png)

填写参数

- applicationName：在k8s配置中的应用名称
- imageName： 在k8s配置中的镜像名称
- version：镜像的版本号，不填默认为当前时间戳
- jar：应用的jar包

![args](http://172.20.41.234/huangjun/k8s-app-deploy/raw/master/doc/args.png)

上传文件成功后等待30~90s,响应**发布成功**即可