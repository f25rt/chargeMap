package ph.chargemap.common.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import ph.chargemap.config.ChargeMapProperties;

/**
 * Builds {@link Pageable} values from raw request parameters, applying the configured
 * default size, maximum size, and a non-negative page index (Requirement 13.2).
 */
@Component
public class PageRequests {

    private final ChargeMapProperties props;

    public PageRequests(ChargeMapProperties props) {
        this.props = props;
    }

    public Pageable of(Integer page, Integer size) {
        return of(page, size, Sort.unsorted());
    }

    public Pageable of(Integer page, Integer size, Sort sort) {
        int resolvedPage = (page == null || page < 0) ? 0 : page;
        int defaultSize = props.getPagination().getDefaultSize();
        int maxSize = props.getPagination().getMaxSize();

        int resolvedSize = (size == null || size <= 0) ? defaultSize : size;
        if (resolvedSize > maxSize) {
            resolvedSize = maxSize;
        }
        return PageRequest.of(resolvedPage, resolvedSize, sort);
    }
}
