package com.datdofishing.content.service;

import com.datdofishing.common.error.BusinessException;
import com.datdofishing.common.web.PageResponse;
import com.datdofishing.content.domain.Article;
import com.datdofishing.content.domain.Banner;
import com.datdofishing.content.domain.Page;
import com.datdofishing.content.repo.ArticleRepository;
import com.datdofishing.content.repo.BannerRepository;
import com.datdofishing.content.repo.PageRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ContentService {

    private final ArticleRepository articles;
    private final PageRepository pages;
    private final BannerRepository banners;

    public ContentService(ArticleRepository articles, PageRepository pages, BannerRepository banners) {
        this.articles = articles;
        this.pages = pages;
        this.banners = banners;
    }

    // ---------- DTO ----------
    public record ArticleSummary(Long id, String title, String slug, String summary, String coverImageUrl,
                                 List<String> tags, String status, Instant publishedAt, long viewCount) {
    }

    public record ArticleDetail(Long id, String title, String slug, String summary, String coverImageUrl,
                                String content, List<String> tags, Map<String, Object> meta, String status,
                                String authorName, long viewCount, Instant publishedAt, Instant updatedAt) {
    }

    public record ArticleRequest(@NotBlank @Size(max = 255) String title, String slug, @Size(max = 500) String summary,
                                 @Size(max = 1000) String coverImageUrl, @NotBlank String content, List<String> tags,
                                 Map<String, Object> meta, String status, String authorName) {
    }

    public record PageRequestDto(@NotBlank String slug, @NotBlank String title, @NotBlank String content) {
    }

    public record PageResponseDto(Long id, String slug, String title, String content, Instant updatedAt) {
    }

    public record BannerDto(Long id, String title, @NotBlank String imageUrl, String linkUrl, String position,
                            int sortOrder, Boolean active) {
    }

    // ---------- Bài viết ----------
    @Transactional(readOnly = true)
    public PageResponse<ArticleSummary> published(String tag, int page, int size) {
        int s = Math.min(Math.max(size, 1), 50);
        if (StringUtils.hasText(tag)) {
            String tagJson = "[\"" + tag.replace("\"", "") + "\"]";
            return PageResponse.of(articles.findPublishedByTag(tagJson,
                    PageRequest.of(page, s, Sort.by(Sort.Direction.DESC, "published_at"))), ContentService::toSummary);
        }
        return PageResponse.of(articles.findByStatus("PUBLISHED",
                PageRequest.of(page, s, Sort.by(Sort.Direction.DESC, "publishedAt"))), ContentService::toSummary);
    }

    @Transactional
    public ArticleDetail readPublished(String slug) {
        Article a = articles.findBySlugAndStatus(slug, "PUBLISHED")
                .orElseThrow(() -> BusinessException.notFound("Bài viết"));
        articles.incrementView(a.getId());
        return toDetail(a);
    }

    @Transactional(readOnly = true)
    public PageResponse<ArticleSummary> adminList(int page, int size) {
        return PageResponse.of(articles.findAll(PageRequest.of(page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "id"))), ContentService::toSummary);
    }

    @Transactional(readOnly = true)
    public ArticleDetail adminGet(Long id) {
        return toDetail(articles.findById(id).orElseThrow(() -> BusinessException.notFound("Bài viết")));
    }

    @Transactional
    public ArticleDetail saveArticle(Long id, ArticleRequest req) {
        Article a = id == null ? new Article()
                : articles.findById(id).orElseThrow(() -> BusinessException.notFound("Bài viết"));
        a.setTitle(req.title().trim());
        String slug = slugify(StringUtils.hasText(req.slug()) ? req.slug() : req.title());
        if (!slug.equals(a.getSlug()) && articles.existsBySlug(slug)) {
            throw BusinessException.conflict("SLUG_EXISTS", "Slug bài viết đã tồn tại");
        }
        a.setSlug(slug);
        a.setSummary(req.summary());
        a.setCoverImageUrl(req.coverImageUrl());
        a.setContent(req.content());
        a.setTags(req.tags() == null ? new ArrayList<>() : new ArrayList<>(req.tags()));
        a.setMeta(req.meta());
        a.setAuthorName(req.authorName());
        String status = StringUtils.hasText(req.status()) ? req.status().toUpperCase(Locale.ROOT) : "DRAFT";
        if ("PUBLISHED".equals(status) && a.getPublishedAt() == null) {
            a.setPublishedAt(Instant.now());
        }
        a.setStatus(status);
        return toDetail(articles.save(a));
    }

    @Transactional
    public void deleteArticle(Long id) {
        articles.deleteById(id);
    }

    // ---------- Trang tĩnh ----------
    @Transactional(readOnly = true)
    public PageResponseDto page(String slug) {
        return toPage(pages.findBySlug(slug).orElseThrow(() -> BusinessException.notFound("Trang")));
    }

    @Transactional(readOnly = true)
    public List<PageResponseDto> allPages() {
        return pages.findAll(Sort.by("slug")).stream().map(ContentService::toPage).toList();
    }

    @Transactional
    public PageResponseDto savePage(PageRequestDto req) {
        Page p = pages.findBySlug(req.slug()).orElseGet(Page::new);
        p.setSlug(slugify(req.slug()));
        p.setTitle(req.title());
        p.setContent(req.content());
        return toPage(pages.save(p));
    }

    // ---------- Banner ----------
    @Transactional(readOnly = true)
    public List<BannerDto> banners(String position) {
        return banners.findByPositionAndActiveTrueOrderBySortOrderAsc(position).stream().map(ContentService::toBanner)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BannerDto> allBanners() {
        return banners.findAllByOrderByPositionAscSortOrderAsc().stream().map(ContentService::toBanner).toList();
    }

    @Transactional
    public BannerDto saveBanner(Long id, BannerDto req) {
        Banner b = id == null ? new Banner()
                : banners.findById(id).orElseThrow(() -> BusinessException.notFound("Banner"));
        if (!req.imageUrl().startsWith("https://")) {
            throw BusinessException.badRequest("INVALID_IMAGE_URL", "Link ảnh banner phải bắt đầu bằng https://");
        }
        b.setTitle(req.title());
        b.setImageUrl(req.imageUrl());
        b.setLinkUrl(req.linkUrl());
        b.setPosition(StringUtils.hasText(req.position()) ? req.position() : "HOME_SLIDER");
        b.setSortOrder(req.sortOrder());
        b.setActive(req.active() == null || req.active());
        return toBanner(banners.save(b));
    }

    @Transactional
    public void deleteBanner(Long id) {
        banners.deleteById(id);
    }

    static String slugify(String text) {
        String s = text.replace('đ', 'd').replace('Đ', 'D');
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
    }

    private static ArticleSummary toSummary(Article a) {
        return new ArticleSummary(a.getId(), a.getTitle(), a.getSlug(), a.getSummary(), a.getCoverImageUrl(),
                a.getTags(), a.getStatus(), a.getPublishedAt(), a.getViewCount());
    }

    private static ArticleDetail toDetail(Article a) {
        return new ArticleDetail(a.getId(), a.getTitle(), a.getSlug(), a.getSummary(), a.getCoverImageUrl(),
                a.getContent(), a.getTags(), a.getMeta(), a.getStatus(), a.getAuthorName(), a.getViewCount(),
                a.getPublishedAt(), a.getUpdatedAt());
    }

    private static PageResponseDto toPage(Page p) {
        return new PageResponseDto(p.getId(), p.getSlug(), p.getTitle(), p.getContent(), p.getUpdatedAt());
    }

    private static BannerDto toBanner(Banner b) {
        return new BannerDto(b.getId(), b.getTitle(), b.getImageUrl(), b.getLinkUrl(), b.getPosition(),
                b.getSortOrder(), b.isActive());
    }
}
