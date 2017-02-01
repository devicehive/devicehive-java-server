#!/bin/bash

###
# #%L
# DeviceHive Dao Riak Implementation
# %%
# Copyright (C) 2016 DataArt
# %%
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#      http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# #L%
###
mkdir -p /etc/riak/dh-mr/src/
mkdir -p /etc/riak/dh-mr/ebin/
cp devicehive-riak-dao/src/main/resources/map-reduce/*.erl /etc/riak/dh-mr/src/
/usr/lib/riak/$(ls /usr/lib/riak/ | grep erts)/bin/erlc -o /etc/riak/dh-mr/ebin /etc/riak/dh-mr/src/*.erl
echo '[{riak_kv, [{add_paths, ["/etc/riak/dh-mr/ebin"]}]}].' > /etc/riak/advanced.config
