package com.bradmcevoy.utils;

public interface With<T,O> {
    O use(T t) throws Exception;
}
