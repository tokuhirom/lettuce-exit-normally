# Issue

JVM doesn't exit after connecting to the cluster.

# How do I reproduce this?

To start redis cluster.

    docker run -e "IP=0.0.0.0" -p 7000-7005:7000-7005 grokzen/redis-cluster:latest

Then run:

    make test
