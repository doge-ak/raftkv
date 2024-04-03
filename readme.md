# RaftKv

RaftKv 是一个基于raft共识算法的kv数据库，使用了`Vert.x`网络库和`Kotlin`的协程

> 本项目处于开发阶段，可能会出现意想不到的行为

## 启动方式

1. 环境：Java 21+
2. 命令：

```bash
cd raftkv-server
./mvnw compile
./mvnw exec:java
``` 