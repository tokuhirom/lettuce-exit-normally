package com.example;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterClientOptions.Builder;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;

public class Application {
    static final String KEYKEYKEY = "lettuceeykeykey";

    public static void main(String[] args) throws Exception {
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

        mainRoutine(redisClusterClient);

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

    private static void mainRoutine(RedisClusterClient redisClusterClient) throws Exception {
        initialize(redisClusterClient);
        perform(10, 10000, redisClusterClient);
    }

    private static void initialize(RedisClusterClient redisClusterClient) {
        StatefulRedisClusterConnection<String, String> connection = redisClusterClient.connect();
        RedisAdvancedClusterCommands<String, String> sync = connection.sync();

        // set it
        System.out.println("=== Deleting keys");
        List<String> hkeys = sync.hkeys(KEYKEYKEY);
        if (hkeys.size() > 0) {
            sync.hdel(KEYKEYKEY, hkeys.toArray(new String[0]));
        }
        System.out.println("=== Inserting keys");
        var initialData = IntStream.rangeClosed(1, 10000)
                                   .boxed()
                                   .collect(Collectors.toMap(
                                           i -> Integer.toString(i),
                                           i -> Integer.toString(i * 2)
                                   ));
        sync.hset(KEYKEYKEY, initialData);
        System.out.printf("=== Data size: %d\n", sync.hkeys(KEYKEYKEY).size());

        connection.close();
    }

    private static void perform(int nthreads, int iterations, RedisClusterClient redisClusterClient)
            throws InterruptedException {
        ExecutorService executorService2 = Executors.newFixedThreadPool(nthreads);

        long t1 = System.currentTimeMillis();
        for (long j = 0; j < nthreads; j++) {
            long finalJ = j;
            executorService2.submit(() -> {
                StatefulRedisClusterConnection<String, String> connection =
                        redisClusterClient.connect();
                RedisAdvancedClusterCommands<String, String> sync
                        = connection.sync();
                for (long i = 0; i < iterations; i++) {
                    if (i % 5000 == 0) {
                        System.out.printf("----- thread=%d %d %dsecs\n", finalJ, i,
                                          ManagementFactory.getRuntimeMXBean()
                                                           .getUptime() / 1000);
                    }
                    var b = sync.hgetall("a");
                    // System.out.println(a.get());
                }
                System.out.printf("** DONE thread=%d\n", finalJ);
                connection.close();
            });
        }
        System.out.println("** Awaiting...");
        executorService2.shutdown();
        System.out.println(executorService2.awaitTermination(100, TimeUnit.MINUTES));
        System.out.println("done");
        long t2 = System.currentTimeMillis();

        System.out.printf("HGETALL: Elapsed: %d\n", t2 - t1);

        executorService2.shutdownNow();
    }
}
