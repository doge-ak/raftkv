package io.github.magician403.raftkv.server.pojo

/**
 * 节点角色类型
 */
enum class NodeRole {
    /**
     * 跟随者
     */
    FOLLOWER,

    /**
     * 候选人
     */
    CANDIDATE,

    /**
     * 领袖
     */
    LEADER
}