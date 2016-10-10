!#/bin/bash
mkdir -p /etc/riak/dh-mr/src/
cp *.erl /etc/riak/dh-mr/src/
/usr/lib/riak/$(ls /usr/lib/riak/ | grep erts)/bin/erlc -o /etc/riak/dh-mr/ebin /etc/riak/dh-mr/src/*.erl
echo '[{riak_kv, [{add_paths, ["/etc/riak/dh-mr/ebin"]}]}].' > /etc/riak
