package dev.vfyjxf.cloudlib.api.network.expose.reverse;

import dev.vfyjxf.cloudlib.api.network.expose.common.Expose;

/**
 * @param <T> the exposed type
 * @param <S> the type of the value to send to the server
 * @param <R> the type of the value received at the server
 */
public interface ReverseExpose<T, S, R> extends Expose<T>, Reversed<S, R> {


}
