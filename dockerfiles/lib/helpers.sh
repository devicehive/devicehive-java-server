#!/bin/bash

test_port () {
    host=$1
    port=$2
    echo "Checking that $host:$port is open:"
    nc -v -z -w1 "$host" "$port"
    return $?
}
