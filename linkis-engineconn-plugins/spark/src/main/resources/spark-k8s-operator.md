
### 1. spark-on-k8s-operator document

```text
https://github.com/GoogleCloudPlatform/spark-on-k8s-operator/blob/master/docs/quick-start-guide.md
```


### 2. spark-on-k8s-operator install

```text
helm repo add spark-operator https://googlecloudplatform.github.io/spark-on-k8s-operator

helm install my-release spark-operator/spark-operator --namespace spark-operator --create-namespace  --set webhook.enable=true  
```

### 3. spark-on-k8s-operator test task submit

```text
kubectl apply -f examples/spark-pi.yaml
```

### 4. If an error is reported: Message: Forbidden!Configured service account doesn't have access. Service account may have been revoked. pods "spark-pi-driver" is forbidden: error looking up service account spark/spark: serviceaccount "spark" not found.

```text
kubectl create serviceaccount spark

kubectl create clusterrolebinding spark-role --clusterrole=edit --serviceaccount=default:spark --namespace=default
```

### 5. spark-on-k8s-operator Uninstall (usually not required, uninstall after installation problems)

```text
helm uninstall my-release  --namespace spark-operator

kubectl delete serviceaccounts my-release-spark-operator --namespace spark-operator

kubectl delete clusterrole my-release-spark-operator --namespace spark-operator

kubectl delete clusterrolebindings my-release-spark-operator --namespace spark-operator
```

### 6. Submitting tasks with Restful API
```text
POST /api/rest_j/v1/entrance/submit
```

```json
{
  "executionContent": {
    "runType": "jar",
    "code": "show databases"
  },
  "params": {
    "variable": {
    },
    "configuration": {
      "startup": {
        "spark.executor.memory": "1g",
        "spark.driver.memory": "1g",
        "spark.executor.cores": "1",
        "spark.app.main.class": "org.apache.spark.examples.SparkPi",
        "spark.app.name": "spark-submit-jar-cjtest",
        "spark.app.resource": "local:///opt/spark/examples/jars/spark-examples_2.12-3.2.1.jar",
        "spark.executor.instances": 1,
        "spark.master":"k8s-operator",
        "linkis.spark.k8s.config.file":"~/.kube/config",
        "linkis.spark.k8s.serviceAccount":"spark"
      }
    }
  },
  "source":  {
    "scriptPath": "file:///tmp/hadoop/test.sql"
  },
  "labels": {
    "engineType": "spark-3.2.1",
    "engineConnMode": "once",
    "userCreator": "linkis-IDE"
  }
}
```

### 7. Submitting tasks via Linkis-cli

```text
sh ./bin/linkis-cli  --mode once -labelMap engineConnMode=once  -engineType spark-3.2.1 -codeType jar    -submitUser hadoop -proxyUser hadoop  -jobContentMap runType=jar  -jobContentMap spark.app.main.class=org.apache.spark.examples.SparkPi   -confMap spark.app.name=spark-submit-jar-test -confMap spark.app.resource=local:///opt/spark/examples/jars/spark-examples_2.12-3.2.1.jar     -confMap spark.executor.instances=1    -confMap spark.kubernetes.file.upload.path=local:///opt/spark/tmp  -confMap spark.executor.memory=1g  -confMap spark.driver.memory=1g  -confMap spark.executor.cores=1   -confMap spark.master=k8s-operator  -confMap linkis.spark.k8s.config.file=/home/hadoop/.kube/config -confMap linkis.spark.k8s.serviceAccount=spark
```

### 8. Matters needing attention

```text
You need to check whether hosts is configured,Such as:
k8s-master-ip  lb.kubesphere.local
```

### 9. Reference document
```text
https://github.com/GoogleCloudPlatform/spark-on-k8s-operator

https://github.com/fabric8io/kubernetes-client/

https://github.com/apple/batch-processing-gateway

https://www.lightbend.com/blog/how-to-manage-monitor-spark-on-kubernetes-introduction-spark-submit-kubernetes-operator

https://www.lightbend.com/blog/how-to-manage-monitor-spark-on-kubernetes-deep-dive-kubernetes-operator-for-spark
```


