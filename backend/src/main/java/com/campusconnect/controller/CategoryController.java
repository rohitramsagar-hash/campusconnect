package com.campusconnect.controller;

import com.campusconnect.dto.CategoryDto;
import com.campusconnect.dto.CategoryRequest;
import com.campusconnect.service.CategoryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /** Active categories for everyone; admins can pass ?all=true to include hidden ones. */
    @GetMapping
    public List<CategoryDto> list(@RequestParam(name = "all", defaultValue = "false") boolean all,
                                  org.springframework.security.core.Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return categoryService.list(all && isAdmin);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryDto create(@Valid @RequestBody CategoryRequest req) {
        return categoryService.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryDto update(@PathVariable("id") Long id, @Valid @RequestBody CategoryRequest req) {
        return categoryService.update(id, req);
    }
}
