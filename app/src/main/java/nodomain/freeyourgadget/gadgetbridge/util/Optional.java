/*  Copyright (C) 2022 José Rebelo

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.util;

import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * An API21-compatible alternative to java.util.Optional.
 */
public final class Optional<T> {
    private final T value;

    private Optional() {
        this.value = null;
    }

    private Optional(final T value) {
        this.value = Objects.requireNonNull(value);
    }

    public T get() {
        if (value == null) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    public boolean isPresent() {
        return value != null;
    }

    public T orElse(final T other) {
        return value != null ? value : other;
    }

    public static <T> Optional<T> empty() {
        return new Optional<>();
    }

    public static <T> Optional<T> of(final T value) {
        return new Optional<>(value);
    }

    public static <T> Optional<T> ofNullable(final T value) {
        return value == null ? empty() : of(value);
    }
}
