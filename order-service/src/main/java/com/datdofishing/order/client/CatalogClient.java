package com.datdofishing.order.client;

import com.datdofishing.common.error.BusinessException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CatalogClient {

    public record VariantInfo(Long variantId, Long productId, String productName, String productSlug,
                              String variantName, String sku, Long price, Long compareAtPrice, String imageUrl,
                              boolean available) {
    }

    private final RestClient client;

    public CatalogClient(@Qualifier("catalogRestClient") RestClient client) {
        this.client = client;
    }

    public Map<Long, VariantInfo> variants(Collection<Long> ids) {
        String joined = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        try {
            List<VariantInfo> list = client.get()
                    .uri(u -> u.path("/internal/variants").queryParam("ids", joined).build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<VariantInfo>>() {
                    });
            return list == null ? Map.of()
                    : list.stream().collect(Collectors.toMap(VariantInfo::variantId, Function.identity(), (a, b) -> a));
        } catch (RestClientException e) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "CATALOG_UNAVAILABLE",
                    "Không lấy được thông tin sản phẩm, vui lòng thử lại");
        }
    }
}
