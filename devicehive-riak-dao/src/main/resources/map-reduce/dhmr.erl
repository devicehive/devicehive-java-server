%%% @doc DeviceHive map reduce functions
-module(dhmr).

-export([map_values/3, reduce_filter/2, reduce_sort/2, reduce_pagination_filter/2, reduce_add_index/2, reduce_delete_index/2]).

%% @doc json decode of existed object's value
map_values(Object, _Keydata, _Arg) ->
    case dict:find(<<"X-Riak-Deleted">>, riak_object:get_metadata(Object)) of
        {ok, "true"} ->
            [];
        _ ->
            {struct, Value} = mochijson2:decode(riak_object:get_value(Object)),
            [Value]
    end.

%% @doc Data filtration. Argument is a list with three elements: parameter to filter, operator and value 
reduce_filter(List, [ParamName, Operator, Value]) ->
    case string:to_lower(binary_to_list(Operator)) of
        "=" ->
            reduce_filter(List, ParamName, Value, fun(V1, V2) -> V1 == V2 end);
        ">" ->
            reduce_filter(List, ParamName, Value, fun(V1, V2) -> V1 > V2 end);
        "<" ->
            reduce_filter(List, ParamName, Value, fun(V1, V2) -> V1 < V2 end);
        ">=" ->
            reduce_filter(List, ParamName, Value, fun(V1, V2) -> V1 >= V2 end);
        "<=" ->
            reduce_filter(List, ParamName, Value, fun(V1, V2) -> V1 =< V2 end);
        "!=" ->
            reduce_filter(List, ParamName, Value, fun(V1, V2) -> V1 =/= V2 end);
        "regex" ->
            reduce_filter(List, ParamName, Value,
                          fun(Val, Regex) ->
                              case re:run(Val, Regex) of
                                  {match, _} -> true;
                                  nomatch -> false
                              end
                          end);
		"in" -> reduce_filter(List, ParamName, Value,
                          fun(V1, V2) -> lists:member(V1, V2) end);
		"contains" -> reduce_filter(List, ParamName, Value,
                          fun(V1, V2) -> lists:member(V2, V1) end)
    end.

reduce_filter(List, ParamName, Value, Comparator) ->
    FilerFun = fun(Object) ->
        case get_value(ParamName, Object) of
            undefined -> false;
            ObjectValue -> Comparator(ObjectValue, Value)
        end
    end,
    lists:filter(FilerFun, List).

%% @doc Data sorting. Argument is a list with two elements: parameter name and order {asc, desc}
reduce_sort(List, [ParamName, Order]) ->
    case string:to_lower(binary_to_list(Order)) of
        "asc" ->
            lists:sort(fun(O1, O2) ->
                           get_value(ParamName, O1) < proplists:get_value(ParamName, O2)
                       end, List);
        "desc" ->
            lists:sort(fun(O1, O2) ->
                           get_value(ParamName, O1) > proplists:get_value(ParamName, O2)
                       end, List)
    end.

%% @doc Data pagination. Argument is a list with two elements: skip and count.
%% Objects in list should have following structure {"index": _, "object": _}. Use reduce_add_index.
reduce_pagination_filter(List, [Skip, Count]) ->
	FilerFun = fun(Object) ->
        case get_value(<<"index">>, Object) of
            undefined -> true;
            Index -> (Index > Skip) and (Index =< Skip + Count)
        end
    end,
    lists:filter(FilerFun, List).

%% @doc Add index to objects in list
%% Returns list with objects like {"index": _, "object": _}.
reduce_add_index(List, _Arg) ->
	ListWithIndexes = lists:zip(lists:seq(1, length(List)), List),
	lists:map(fun ({Index, Object}) -> [{<<"index">>, Index}, {<<"object">>, Object}] end, ListWithIndexes).

%% @doc Delete index from objects in list
reduce_delete_index(List, _Arg) ->
	lists:map(fun delete_index/1, List).

delete_index([{<<"index">>, _}, {<<"object">>, Object}]) ->
	delete_index(Object);
delete_index(Object) ->
	Object.

get_value([H | T], Object) ->
    case proplists:get_value(list_to_binary(H), Object) of
        {struct, SubObject} ->
            get_value(T, SubObject);
        undefined ->
            undefined;
        SubObject ->
            get_value([], SubObject)
    end;
get_value([], Object) ->
    Object;
get_value(Property, Object) ->
    get_value(string:tokens(binary_to_list(Property), "."), Object).