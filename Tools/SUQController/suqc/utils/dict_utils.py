#!/usr/bin/env python3

import numbers  # required to check for numeric types
from copy import deepcopy
from functools import reduce
from typing import List

import numpy as np
from suqc.opp.config_parser import OppConfigFileBase

SYMBOL_KEY_CHAINING = "."


def _deep_dict_breadth_first(d: dict, key: str):

    cur_level = 0
    level_found = 100 ** 100
    stack = [(cur_level, [], d)]

    final_path = None
    val = None

    while stack:

        cur_level, cur_path, iter_stack = stack[0]

        for k, v in iter_stack.items():
            if k == key and cur_level <= level_found:

                if val is not None:
                    raise RuntimeError("please report bug with stacktrace")

                val = v
                cur_path.append(k)
                level_found = cur_level
                final_path = deepcopy(cur_path)
            elif isinstance(v, dict):
                path = deepcopy(cur_path)
                path.append(k)
                stack.append((cur_level + 1, path, v))
        else:
            stack = stack[1:]

    return val, final_path


def key_split(key: str):
    path = key.split("|")
    return path[:-1], path[-1]


def _handle_chained_keys(d, key, check_final_leaf, check_unique_key):
    key_chain = key.split(SYMBOL_KEY_CHAINING)  # get the user specified key chain
    cur_d = deepcopy(d)  # start with the root dict
    cur_p = list()  # cur_p = current path

    if len(key_chain) == 2 and key_chain[0] == "":  # special case if value follows root
        val, path = _deep_dict_breadth_first(
            d, key_chain[1]
        )  # look directly breath first for the target value
        return val, path
    else:

        if key_chain[0] == "":  # remove unnecessary root and remove empty
            key_chain = key_chain[1:]

        for k in key_chain[:-1]:  # do breath first for all path-descriptive keys
            d_, p_ = _deep_dict_breadth_first(cur_d, k)
            cur_d = deepcopy(d_)
            cur_p += p_

        val, p_ = deep_dict_lookup(
            cur_d,
            key_chain[-1],  # carry out depth first for last sub-dictionary
            check_final_leaf=check_final_leaf,
            check_unique_key=check_unique_key,
        )

    cur_p += p_
    return val, cur_p


def _is_subselection(key):
    return "[" in key and "]" in key


def _split_subselection_key(selection):
    assert "==" in selection, "For now only equality selections supported"
    key_selection, val_selection = selection.split("==")

    try:
        val_selection = int(val_selection)
    except KeyError:
        raise KeyError("For now only equalities with integer is supported!")

    return key_selection, val_selection


def _get_selected_json(json_elements: List[dict], selection):

    selection = selection.lstrip("[").rstrip(
        "]"
    )  # Remove potential syntax elements for sub-selection

    key_selection, val_selection = _split_subselection_key(selection)

    json_selected = None
    for el in json_elements:
        val, _ = deep_dict_lookup(
            el, key_selection, check_final_leaf=True, check_unique_key=True
        )

        if val_selection == val:
            if json_selected is not None:
                raise KeyError(
                    f"There are multiple selections with {key_selection}=={val_selection} in list "
                    f"{json_elements}."
                )
            json_selected = el

    if json_selected is None:
        raise KeyError(
            f"No json with the selection {selection} could be found in \n {json_elements} "
        )

    return json_selected


def _handle_subselection_keys(d, key, check_final_leaf, check_unique_key):

    import re

    keychain = re.split(pattern="[\[\]]", string=key)

    assert len(keychain) == 3, "For now only one sub-selection supported in the key."
    keychain2array = keychain[0].rstrip(
        SYMBOL_KEY_CHAINING
    )  # remove trailing separator

    if not keychain2array:  # check for empty string
        raise KeyError(
            f"The key {key} is invalid, the uniquely identifying path to the list of jsons has to be given in"
            f"front. This means the key is not allowed to start with a [ ] condition."
        )

    subjsons, path2array = _handle_chained_keys(
        d, keychain2array, check_final_leaf, check_unique_key
    )

    # TODO: actually I need to guarantee that the array is full of dicts (json)
    assert isinstance(subjsons, list), "The path to sub-selection is no array."

    selection = keychain[1]
    selarraypath = keychain[2]

    if not selarraypath:
        # check if selected array path is empty (i.e. a conditional key chain element [a==1] appears last
        raise KeyError(
            f'The key {key} is invalid, the selection of a json in a list (i.e. inside brackets "[ ]" '
            f"cannot be last."
        )

    # get the json from the selection (it is one in a list where the condition is true)
    json_selected = _get_selected_json(subjsons, selection)

    # carry out the path in this selected json
    val, element_path = _handle_chained_keys(
        json_selected, selarraypath, check_final_leaf, check_unique_key
    )

    final_path = path2array + [f"[{selection}]"] + element_path

    return val, final_path


def deep_dict_lookup(d: dict, key: str, check_final_leaf=True, check_unique_key=True):
    """Return a value corresponding to the specified key in the (possibly nested) dictionary d. If there is no item
    with that key raise ValueError.

    :param d: dictionary to look up the key
    :param key: key to look up deep in `d`
    :param check_final_leaf: checks if the returned value is a final value and not another sub-directory
    :param check_unique_key: checks if there are multiple keys with name `key`; if yes throw ValueError
    """
    # "Stack of Iterators" pattern: http://garethrees.org/2016/09/28/pattern/
    # and see https://stackoverflow.com/q/14962485

    if SYMBOL_KEY_CHAINING in key:
        if _is_subselection(key):
            return _handle_subselection_keys(d, key, check_final_leaf, check_unique_key)
        else:
            return _handle_chained_keys(d, key, check_final_leaf, check_unique_key)

    value = None

    current_path = []  # store the absolute path to the variable as a list of keys
    path_to_value = None  # path to the final key

    stack = [iter(d.items())]
    while stack:
        for k, v in stack[-1]:  # go through (sub-) directories
            current_path.append(k)
            if k == key:  # if this is the key we are looking for...
                if (
                    check_unique_key
                ):  # keep looking at all keys in 'd', to check if there is a conflict
                    if (
                        value is not None
                    ):  # here was already another value -> not unique
                        raise ValueError(
                            f"There is a conflict (two or more) of key {key} in the dictionary. \n {d}",
                            f"1. path: {path_to_value} \n" f"2. path: {current_path}",
                        )
                    path_to_value = deepcopy(
                        current_path
                    )  # deepcopy because lists are mutable
                    value = v  # set to final value
                else:
                    # if the integrity is not checked, return immediately the key and path
                    if check_final_leaf and isinstance(v, dict):
                        raise ValueError(
                            f"Value to return for key {key} is not a leaf (i.e. value) but a "
                            f"sub-dictionary."
                        )
                    return v, deepcopy(current_path)

            if isinstance(v, dict):  # fill stack with more subdicts
                stack.append(iter(v.items()))
                break
            else:  # remove last key again from list
                current_path = current_path[:-1]
        else:
            # if/else statement: if loop ended normally then run this: remove last key from path and remove this
            # entry from the stack
            current_path = current_path[:-1]
            stack.pop()

    if value is None:
        raise KeyError(f"Key {key} not found. \n {d}")
    return (
        value,
        path_to_value,
    )  # NOTE: there is another return in the loop, when check_integrity is False


def all_nested_keys(d: dict):
    """Returns all keys present in the dictionary."""

    all_keys = []
    stack = [iter(d.items())]
    while stack:
        for k, v in stack[-1]:
            all_keys.append(k)
            if isinstance(v, dict):
                stack.append(iter(v.items()))
                break
        else:
            stack.pop()

    return all_keys


def get_dict_value_keylist(d: dict, path: list, last_key: str):
    return reduce(dict.__getitem__, path, d)[last_key]


def set_dict_value_keylist(d: dict, path: list, last_key: str, value):
    # reduce(dict.__getitem__, path, d)[last_key] = value

    cur_dict = d

    for key in path:
        if _is_subselection(key):
            # Note: cur_dir should now be a list of dicts/json
            # TODO maybe check this! (check also possible inside _get_selected_json)
            cur_dict = _get_selected_json(cur_dict, key)

        else:
            cur_dict = cur_dict[key]

    cur_dict[last_key] = value

    return d


def _avoid_numpy_types(new_value):

    # see explanation: https://stackoverflow.com/questions/27050108/convert-numpy-type-to-python
    # and issue: #75

    if isinstance(new_value, np.integer):
        new_value = int(new_value)
    elif isinstance(new_value, np.floating):
        new_value = float(new_value)
    elif isinstance(new_value, np.ndarray):
        new_value = new_value.tolist()

    return new_value


def change_value(d: dict, path: list, last_key: str, exist_val, new_value):

    if isinstance(exist_val, dict):
        raise ValueError("Currently, setting of new sub-directories is not supported!")

    # Security: if there is a completely new type (which cannot be casted) then throw error
    if type(exist_val) != type(new_value):

        # check for numerical types (casting between float and integer)
        if isinstance(exist_val, numbers.Number) and isinstance(
            new_value, numbers.Number
        ):

            new_value = _avoid_numpy_types(new_value)

            # pass cases, where numpy floating point wrappers were removed
            if (isinstance(exist_val, float) and isinstance(new_value, float)) or (
                isinstance(exist_val, int) and isinstance(new_value, int)
            ):
                pass  # all good now
            else:  # print warning in cases where e.g. the existing value is an int and the new value is a float
                print(
                    f"WARNING: key {last_key} at path {path} has type {type(exist_val)} but the new value has type "
                    f"{type(new_value)}. The value is not casted."
                )
        else:
            print(
                f"WARNING: There is a type casting from type {type(new_value)} (set value) to type {exist_val} "
                f"(existing value)"
            )
            try:
                new_value = type(exist_val)(
                    new_value
                )  # try to cast, if it failes raise error
            except ValueError as e:
                print(f"Type-cast failed for key {last_key} at path {path}.")
                raise e

    return set_dict_value_keylist(d, path, last_key, new_value)


def change_dict_ini(ini_object: OppConfigFileBase, changes: dict):

    for key, value in changes.items():
        ini_object[key] = value

    return ini_object


def change_dict(json_dict: dict, changes: dict):

    # dictionaries are mutable! Make a deepcopy for security:
    json_dict = deepcopy(json_dict)

    for key_chain, new_val in changes.items():
        exist_val, fullkeypath = deep_dict_lookup(json_dict, key_chain)
        path2key, final_key = fullkeypath[:-1], fullkeypath[-1]

        # exist val is given for sanity checks (e.g. not replace a string with an integer)
        json_dict = change_value(json_dict, path2key, final_key, exist_val, new_val)

        # Security check:
        check_val, _ = deep_dict_lookup(json_dict, key_chain)
        assert check_val == new_val, (
            f"Something went wrong with setting a new value "
            f"in the scenario! "
            f"Check val={check_val} vs. new_val={new_val}."
        )
        check_val, _ = deep_dict_lookup(json_dict, key_chain)
        assert check_val == new_val, (
            f"Something went wrong with setting a new value "
            f"in the scenario! "
            f"Check val={check_val} vs. new_val={new_val}."
        )

    return json_dict


if __name__ == "__main__":

    import json

    with open("New_SimpleHKHKJ.scenario", "r") as f:
        d2 = json.load(f)

    val, path = deep_dict_lookup(d2, "dynamicElements.[attributes.id==-1]")

    print(val)
    print(path)

    new_d = change_dict(
        d2, changes={"dynamicElements.[attributes.id==-1].position.x": 100}
    )

    print(new_d)

    exit()

    d1 = {"a": {"b": 1, "c": {"x": 3}, "d": {"f": 3}}}
    # print(deep_dict_lookup(d, "x", True))

    print(change_dict(d1, {"a.b": 3}))

    # print(deep_subdict(d, ["c"]))
    # print(abs_path_key(d, "a|c|x"))
