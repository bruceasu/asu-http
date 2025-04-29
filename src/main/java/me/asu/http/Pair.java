package me.asu.http;

import lombok.Data;

@Data
public class Pair<T, E> {

    T key;
    E value;

    public Pair(T k, E v) {
        key = k;
        value = v;
    }
}