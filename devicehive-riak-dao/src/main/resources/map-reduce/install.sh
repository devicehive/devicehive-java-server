#!/bin/bash
mkdir -p /etc/riak/dh-mr/src/
mkdir -p /etc/riak/dh-mr/ebin/
cp devicehive-riak-dao/src/main/resources/map-reduce/*.erl /etc/riak/dh-mr/src/
/usr/lib/riak/$(ls /usr/lib/riak/ | grep erts)/bin/erlc -o /etc/riak/dh-mr/ebin /etc/riak/dh-mr/src/*.erl
echo '[{riak_kv, [{add_paths, ["/etc/riak/dh-mr/ebin"]}]}].' > /etc/riak/advanced.config
