package com.mazlove.backend.models;

import java.io.Serializable;
import java.util.Objects;

public class FavoriteId implements Serializable {
    private Long user;
    private Long provider;

    public FavoriteId() {}

    public FavoriteId(Long user, Long provider) {
        this.user = user;
        this.provider = provider;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof FavoriteId id)) return false;
        return Objects.equals(user, id.user) && Objects.equals(provider, id.provider);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, provider);
    }
}
