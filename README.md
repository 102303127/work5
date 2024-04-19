# work5即时聊天

## 1、项目结构

```Plain Text
把第5轮 的内容整合到work4 中了
├─main
│  ├─java
│  │  └─com
│  │      └─zhang
│  │          ├─component    ## 添加了一个组件包用来实现websocket 这次主要是是 实现这个里面的
│  │          ├─config
│  │          ├─controller
│  │          ├─entity
│  │          ├─exception
│  │          ├─handler
│  │          ├─mapper
│  │          ├─service     ##做了实体类 ， 服务层
│  │          │  └─Impl
│  │          ├─utils
│  │          └─vo
```



## 2、技术栈

### 1.Websocket

> websocket协议

- Spring Boot实现的注入Bean问题：
  - 直接@Autoired注入为空，需要用set注入，在@ServerEndpoint注解类中使用@Resource等注入失败。报出空指针异常。@Autowired注解注入对象是在启动的时候就把对象注入，而不是在使用A对象时才把A需要的B对象注入到A中。

### 2.RabbitMQ

> 用docker运行镜像在云服务器

- 遇到的问题解决：
  - 消息渠道的动态订阅，如果使用Spring Boot的注解实现，会在项目启动的时候就固定要监听的队列，无法实现动态注入
  - 所以就使用了原始的创造连接，创造监听，摒弃了Spring Boot的注解实现

### 3.Quartz

> 定时执行操作 在做项目的时候也遇到过定时执行命令的一些东东，因为之前对Linux上部署的docker并不熟悉，所以在docker上运行redis容器就没有设置密码，后来云服务器给发邮件说有风险，看了之后就给redis设置了密码，也没太在意（现在想想当时也是为时已晚），等到出现问题就是最近再次连接服务器的redis，发现之前设置的密码被清空了，就试着用可视化工具看redis，发现里面莫名出现一坨：：http://45.83.122.25/3nFTk7/init.sh     .........     问chatGPT说是一大堆定时执行的命令，远程执行什么什么，ROIS学长说是之前没设置密码被留了后门，所以就...幸好不是在本机就没什么事，，，

- 服务端收到消息先把消息存储在redis中
- 定时把redis中的消息持久到数据库中
- 持久化数据库对未读消息不做持久化

### 4.redis 主从复制

> 这方面技术并不熟练，只是在服务器试着创建了一下

- 把redis中用户已读的消息，同步到数据库，未读的消息存储在redis中
- redis采用主从复制，哨兵模式，防止redis宕机造成数据丢失

### 5.SpringCache

> 用来改善项目性能，降低查询负担的 这个技术栈也只是了解，并没有太熟练，因为牵扯到数据一致性问题，就先搁置了 后面的项目会用到

- @Cacheable、@CachePut、@CacheEvict等注解来定义缓存策略
- Spring的声明式事务管理来实现，在方法上添加@Transactional

## 3、消息传递流程

```Plain Text
+-------------------+            +-------------------+             +----------------------+
|                   |            |   服务端           |             |                      |
|  客户端发送消息     +----------->|  WebSocket 接收消息 |------------>|  RabbitMQ 发送消息    |
|                   |            |                   |             |                      |
+-------------------+            +-------------------+             +----------------------+
                                      |                             服务端生成对应的发送消息的队列
                                      |
                                      v
                               +--------------+
                               |              |
                               |  监听消息队列  |
                               | 发送消息的同时 |
                               |  创建监听消息  |
                               +--------------+
                                      |
                                      |
                                      v
                               +--------------+
                               |              |
                               |  存储到 Redis |
                               |    等待拉取   |
                               +--------------+
                                      |
                                      |
                                      v
                      +--------------------------+
                      |                          |
                      |  定时持久化到数据库         |
                      |                          |
                      +--------------------------+

```
## 4.docker 部署redis主从复制，哨兵模式

### 1.主从复制

>运行主节点

```
docker run -d --name redis-master -p 6379:6379 redis --requirepass Yj20o48zLkCwI1h112500
```

>部署从节点1

```
docker run -d --name redis-slave1 -p 6380:6379 redis --requirepass Yj20o48zLkCwI1h112500 --slaveof redis-master 6379
```

>部署从节点2

```
docker run -d --name redis-slave2 -p 6381:6379 redis --requirepass Yj20o48zLkCwI1h112500 --slaveof redis-master 6379
```



### 2.哨兵模式

>部署哨兵节点

```
docker run -d --name redis-sentinel -p 26379:26379 redis \
  redis-sentinel --sentinel monitor mymaster redis-master 6379 2
```


