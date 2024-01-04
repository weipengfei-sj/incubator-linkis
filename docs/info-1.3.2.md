## 参数变化

| 模块名(服务名)         | 类型  | 参数名                                                                  | 默认值  | 描述                                                    |
|------------------| ----- |----------------------------------------------------------------------|------| ------------------------------------------------------- |
| linkis-jobhistory | 新增  | wds.linkis.jobhistory.admin | hadoop |可以查看所有历史任务的用户 注意：wds.linkis.governance.station.admin 为管理用户（也具有可以查看所有历史任务的权限）|
| linkis | 新增  | wds.linkis.governance.station.admin.token |   /具有管理员权限的特殊token|
| cg-entrance | 新增  | linkis.entrance.auto.clean.dirty.data.enable | true |entrance重启调用ps-jobhistory接口是否开启，ture为开启，取值范围：true或false|
