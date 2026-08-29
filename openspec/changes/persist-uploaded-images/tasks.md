## 1. Kubernetes 持久化配置

- [ ] 1.1 在后端 Kubernetes 清单中声明独立的 `uploads-data` PVC，使用 5Gi、`ReadWriteOnce` 与默认存储类。
- [ ] 1.2 将 `uploads-data` 挂载到后端容器的 `/opt/clas/uploads`，不改变现有 MySQL PVC。

## 2. 验证与文档

- [ ] 2.1 校验 Kubernetes 清单中 PVC、卷声明与挂载路径的引用关系。
- [ ] 2.2 更新 README 的持久化存储说明，明确数据库与上传图片的 PVC 范围及单节点限制。
- [ ] 2.3 在云端应用清单，验证 PVC 为 Bound、后端就绪，并确认重启后上传目录仍存在。
