package com.shike.delivery.common;

import java.util.List;

public record PageResult<T>(List<T> items, long total, long page, long pageSize) {
}
