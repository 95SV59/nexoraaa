package com.nexora.game.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public class PageResponse<T> {
    public List<T> items;
    public int page;
    public int size;
    public long totalItems;
    public int totalPages;

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        PageResponse<T> dto = new PageResponse<>();
        dto.items = page.getContent().stream().map(mapper).toList();
        dto.page = page.getNumber();
        dto.size = page.getSize();
        dto.totalItems = page.getTotalElements();
        dto.totalPages = page.getTotalPages();
        return dto;
    }
}
