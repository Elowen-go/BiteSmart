package com.ws.bitesmart.common.util;

/**
 * 雪花算法（Snowflake）ID 生成器
 *
 * 原理说明：
 *   生成的 ID 是一个 64 位的 long 型整数，按位分割如下：
 *   ┌─────────────────────────────────────────────────────────────────┐
 *   │ 1位符号位 | 41位时间戳 | 5位数据中心ID | 5位工作节点ID | 12位序列号 │
 *   └─────────────────────────────────────────────────────────────────┘
 *   - 符号位：始终为 0，保证 ID 为正数
 *   - 时间戳：相对于 EPOCH 的毫秒偏移量，可用约 69 年
 *   - 数据中心ID：5位，支持 32 个数据中心
 *   - 工作节点ID：5位，每个数据中心支持 32 个节点
 *   - 序列号：12位，同一毫秒内支持 4096 个 ID
 *
 * 性能特性：
 *   - 全局唯一（分布式环境下不依赖数据库自增）
 *   - 趋势递增（按时间有序，有利于 MySQL InnoDB 索引性能）
 *   - 高可用（ID 生成仅依赖本机时钟，无网络开销）
 *
 * 注意事项：
 *   - 依赖系统时钟，如果发生时钟回拨（NTP 同步等），会拒绝生成 ID
 *   - 生成的是 Long 类型，传给前端 JS 时需要序列化为 String（参见 JacksonConfig）
 *
 * @author BiteSmart
 */
public class SnowflakeUtil {

    // ==================== 位运算常量定义 ====================

    /** 起始时间戳（2023-11-15），单位毫秒，避免 ID 起始值过大 */
    private static final long EPOCH = 1700000000000L;

    /** 工作节点 ID 的位数 */
    private static final long WORKER_ID_BITS = 5L;

    /** 数据中心 ID 的位数 */
    private static final long DATACENTER_ID_BITS = 5L;

    /** 序列号的位数（同一毫秒内可生成 2^12 = 4096 个 ID） */
    private static final long SEQUENCE_BITS = 12L;

    /** 工作节点 ID 的最大值（32） */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    /** 数据中心 ID 的最大值（32） */
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

    /** 序列号掩码（4095），用于截取序列号的低12位 */
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    /** 序列号左移位数（12） */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /** 数据中心 ID 左移位数（17 = 12 + 5） */
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    /** 时间戳左移位数（22 = 12 + 5 + 5） */
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    // ==================== 实例状态 ====================

    /** 本机工作节点 ID */
    private final long workerId;

    /** 本机数据中心 ID */
    private final long datacenterId;

    /** 上次生成 ID 的时间戳（用于判断时钟回拨和序列号重置） */
    private long lastTimestamp = -1L;

    /** 当前毫秒内的序列号（0~4095，超出则等待下一毫秒） */
    private long sequence = 0L;

    /** 单例实例（默认 workerId=1, datacenterId=1） */
    private static final SnowflakeUtil INSTANCE = new SnowflakeUtil(1, 1);

    /**
     * 私有构造方法，通过 {@link #getInstance()} 获取单例
     *
     * @param workerId      工作节点 ID（0~31）
     * @param datacenterId  数据中心 ID（0~31）
     */
    private SnowflakeUtil(long workerId, long datacenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("workerId超出范围 [0, " + MAX_WORKER_ID + "]");
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId超出范围 [0, " + MAX_DATACENTER_ID + "]");
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    /**
     * 获取单例实例
     *
     * @return SnowflakeUtil 实例
     */
    public static SnowflakeUtil getInstance() {
        return INSTANCE;
    }

    /**
     * 生成下一个唯一 ID
     *
     * 算法步骤：
     *   1. 获取当前毫秒时间戳
     *   2. 如果时间戳 < 上次生成时间，说明时钟回拨，抛出异常
     *   3. 如果时间戳 == 上次生成时间，序列号 +1（超限则等待下一毫秒）
     *   4. 如果时间戳 > 上次生成时间，序列号重置为 0
     *   5. 将各段按位组合成 64 位 ID
     *
     * @return 全局唯一 ID
     */
    public synchronized long nextId() {
        long timestamp = timeGen();

        // 1. 时钟回拨检查：如果当前时间小于上次生成时间，拒绝生成
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(String.format(
                    "时钟回拨，拒绝生成ID %d 毫秒", lastTimestamp - timestamp));
        }

        // 2. 同一毫秒内：序列号递增
        if (lastTimestamp == timestamp) {
            // 序列号达到最大值（4095）时，等待下一毫秒
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                // 序列号溢出，等待到下一毫秒
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 3. 不同毫秒：序列号重置为 0
            sequence = 0L;
        }

        // 记录本次生成的时间戳，供下次判断时钟回拨使用
        lastTimestamp = timestamp;

        // 4. 位运算拼接各部分，生成最终 64 位 ID
        return ((timestamp - EPOCH) << TIMESTAMP_LEFT_SHIFT)   // 时间戳段
                | (datacenterId << DATACENTER_ID_SHIFT)        // 数据中心段
                | (workerId << WORKER_ID_SHIFT)                // 工作节点段
                | sequence;                                    // 序列号段
    }

    /**
     * 静态快捷方法，直接生成 ID
     *
     * @return 全局唯一 ID
     */
    public static long generate() {
        return INSTANCE.nextId();
    }

    /**
     * 自旋等待到下一毫秒
     * 当同一毫秒内序列号用完时，阻塞等待到下一个毫秒
     *
     * @param lastTimestamp 上次生成的时间戳
     * @return 新的毫秒时间戳（> lastTimestamp）
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 获取当前系统时间戳（毫秒）
     *
     * @return 当前毫秒时间戳
     */
    private long timeGen() {
        return System.currentTimeMillis();
    }

}
