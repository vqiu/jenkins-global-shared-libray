## 示例1、 多分支流水线构建

```
@Library('jenkins-global-shared-libray') _
multiPipeline{
    projectName    =    "demo"                           ----> 项目名称
    appName        =    "static-h5"                      ----> 项目模块名称
    emailList      =    "zhang3@vqiu.cn,li4@vqiu.cn"     ----> 构建报告邮件接收者，多人以逗号(半角)隔开
    nameSpace      =    "zhiqiu-system"                  ----> Kubernetes部署命名空间(测试、生产一致)
}
```



## 示例2、 自定义的流水线构建

这种场景适用同个项目，比如nameSpace、projectName、emailList这些参数都是一致的，我们就可以单独创建个模版使用，充分发挥“懒”字美德。

```
@Library('jenkins-global-shared-libray') _
multiPipelineMyhr{
    appName        =    "static-h5"                      ----> 项目模块名称
}
```



## 邮件通知模板

每个仓库下面需要创建1个名为`manifests`的目录，将邮件通知的模版置于此，否则流水将执行失败。
```
# ls manifests
report.html
```

> **提示：**`report.html`内容参考仓库中同名文件。



