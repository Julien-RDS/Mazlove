package com.mazlove.backend.util;

import org.springframework.data.domain.PageRequest;

/** Bornes de pagination communes à tous les endpoints listés. */
public final class Paging {

    /** Au-delà, un client malveillant pourrait demander size=100000 et saturer la base. */
    public static final int MAX_PAGE_SIZE = 50;

    private Paging() {
    }

    /** Page négative ramenée à 0, taille ramenée entre 1 et MAX_PAGE_SIZE. */
    public static PageRequest of(int page, int size) {
        return PageRequest.of(Math.max(page, 0), clampSize(size));
    }

    public static int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }
}
