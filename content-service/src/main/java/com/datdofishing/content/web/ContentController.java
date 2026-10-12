package com.datdofishing.content.web;

import com.datdofishing.common.web.PageResponse;
import com.datdofishing.content.service.ContentService;
import com.datdofishing.content.service.ContentService.ArticleDetail;
import com.datdofishing.content.service.ContentService.ArticleRequest;
import com.datdofishing.content.service.ContentService.ArticleSummary;
import com.datdofishing.content.service.ContentService.BannerDto;
import com.datdofishing.content.service.ContentService.PageRequestDto;
import com.datdofishing.content.service.ContentService.PageResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ContentController {

    private final ContentService content;

    public ContentController(ContentService content) {
        this.content = content;
    }

    // ----- Công khai -----
    @GetMapping("/api/content/articles")
    public PageResponse<ArticleSummary> articles(@RequestParam(required = false) String tag,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        return content.published(tag, page, size);
    }

    @GetMapping("/api/content/articles/{slug}")
    public ArticleDetail article(@PathVariable String slug) {
        return content.readPublished(slug);
    }

    @GetMapping("/api/content/pages/{slug}")
    public PageResponseDto page(@PathVariable String slug) {
        return content.page(slug);
    }

    @GetMapping("/api/content/banners")
    public List<BannerDto> banners(@RequestParam(defaultValue = "HOME_SLIDER") String position) {
        return content.banners(position);
    }

    // ----- Admin -----
    @GetMapping("/api/admin/content/articles")
    public PageResponse<ArticleSummary> adminArticles(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return content.adminList(page, size);
    }

    @GetMapping("/api/admin/content/articles/{id}")
    public ArticleDetail adminArticle(@PathVariable Long id) {
        return content.adminGet(id);
    }

    @PostMapping("/api/admin/content/articles")
    @ResponseStatus(HttpStatus.CREATED)
    public ArticleDetail createArticle(@Valid @RequestBody ArticleRequest req) {
        return content.saveArticle(null, req);
    }

    @PutMapping("/api/admin/content/articles/{id}")
    public ArticleDetail updateArticle(@PathVariable Long id, @Valid @RequestBody ArticleRequest req) {
        return content.saveArticle(id, req);
    }

    @DeleteMapping("/api/admin/content/articles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteArticle(@PathVariable Long id) {
        content.deleteArticle(id);
    }

    @GetMapping("/api/admin/content/pages")
    public List<PageResponseDto> pages() {
        return content.allPages();
    }

    @PutMapping("/api/admin/content/pages")
    public PageResponseDto savePage(@Valid @RequestBody PageRequestDto req) {
        return content.savePage(req);
    }

    @GetMapping("/api/admin/content/banners")
    public List<BannerDto> allBanners() {
        return content.allBanners();
    }

    @PostMapping("/api/admin/content/banners")
    @ResponseStatus(HttpStatus.CREATED)
    public BannerDto createBanner(@Valid @RequestBody BannerDto req) {
        return content.saveBanner(null, req);
    }

    @PutMapping("/api/admin/content/banners/{id}")
    public BannerDto updateBanner(@PathVariable Long id, @Valid @RequestBody BannerDto req) {
        return content.saveBanner(id, req);
    }

    @DeleteMapping("/api/admin/content/banners/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBanner(@PathVariable Long id) {
        content.deleteBanner(id);
    }
}
