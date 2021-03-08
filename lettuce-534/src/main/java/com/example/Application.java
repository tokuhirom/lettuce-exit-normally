package com.example;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterClientOptions.Builder;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;

public class Application {
    public static void main(String[] args) {
        final ClientResources clientResources =
                DefaultClientResources.builder()
                                      .build();

        final List<RedisURI> redisUris = Stream.of(7000, 7001, 7003)
                                               .map(port -> RedisURI.create("127.0.0.1", port))
                                               .collect(Collectors.toList());

        final RedisClusterClient redisClusterClient = RedisClusterClient.create(
                clientResources, redisUris
        );
        Builder builder = ClusterClientOptions.builder();
        builder.pingBeforeActivateConnection(true);
        ClusterClientOptions clientOptions = builder.build();

        redisClusterClient.setOptions(clientOptions);

        StatefulRedisClusterConnection<String, String> connection = redisClusterClient.connect();
        connection.close();

        clientResources.shutdown();
        redisClusterClient.shutdown();

        System.out.println("DONE");

        // dump non-daemon threads
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        for (ThreadInfo threadInfo : threadMXBean.dumpAllThreads(false, false)) {
            if (!threadInfo.isDaemon()) {
                System.out.println(threadInfo);
            }
        }
    }
}
