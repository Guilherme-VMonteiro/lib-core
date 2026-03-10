package br.com.lucra.core.mapper;

import java.util.List;

public interface Mapper<S, T> {
    
    T toTarget(S source);
    
    S toSource(T target);
    
    default List<T> toTargetList(List<S> source) {
        if (source == null) {
            return List.of();
        }

        return source.stream().map(this::toTarget).toList();
    }
    
    default List<S> toSourceList(List<T> target) {
        if (target == null) {
            return List.of();
        }

        return target.stream().map(this::toSource).toList();
    }
}
