package io.github.magician403.raftkv.server.pojo

import kotlin.properties.Delegates

class NodeState {
    /**
     * 本机节点的id
     */
    lateinit var id: String

    /**
     * 本机节点的port
     */
    var port: Int by Delegates.notNull()

    /**
     * 已知已提交的最高的日志条目的索引（初始值为0，单调递增）
     */
    var commitIndex: ULong? = null

    /**
     * 已经被应用到状态机的最高的日志条目的索引（初始值为0，单调递增）
     */
    var lastApplied: ULong? = null

    /**
     * 其它的节点信息
     */
    var remoteNodes = ArrayList<NodeInfo>()

    /**
     * 本机节点的角色类型
     */
    var role: NodeRole = NodeRole.CANDIDATE

    /**
     * 服务器已知最新的任期（在服务器首次启动时初始化为0，单调递增）
     */
    var currentTerm: ULong? = null

    /**
     * 当前任期内收到选票的candidateId，如果没有投给任何候选人,则为空
     */
    var voteFor: String? = null

    /**
     * leader节点（远程）的id
     */
    var leaderId: String? = null

    /**
     * leader节点（远程）的id
     */
    var leaderHost: String? = null

    /**
     * leader节点（远程）的port
     */
    var leaderPort: Int? = null
}
