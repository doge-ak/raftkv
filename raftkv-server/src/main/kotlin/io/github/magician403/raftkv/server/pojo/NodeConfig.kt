package io.github.magician403.raftkv.server.pojo

class NodeConfig(
    val port: Int,                       // 节点绑定的端口号
    val nodes: List<NodeAddress>,        // 其他的节点
    val requestVoteTimeoutMs: Long,      // 请求投票超时时间
    val electionTimeoutMs: Long,         // 选举超时时间
    val heartbeatTimeoutMs: Long,        // 心跳超时时间
    val heartBeatTimeMs: Long,           // 心跳间隔时间
    val voteTimeoutMaxMs: Long,          // 投票最大超时时间
    val voteTimeoutMinMs: Long,          // 投票最小超时时间
)