 package io.milton.cloud.common;

/**
 * General purpose callback interface
 *
 * @author brad
 */
public interface With<T,O> {
    O use(T t) throws Exception;
}