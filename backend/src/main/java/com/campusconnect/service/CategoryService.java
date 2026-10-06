package com.campusconnect.service;

import com.campusconnect.dto.CategoryDto;
import com.campusconnect.dto.CategoryRequest;
import com.campusconnect.exception.ApiException;
import com.campusconnect.model.Category;
import com.campusconnect.repository.CategoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> list(boolean includeInactive) {
        List<Category> categories = includeInactive ? categoryRepository.findAllByOrderByNameAsc()
                : categoryRepository.findByActiveTrueOrderByNameAsc();
        return categories.stream().map(CategoryDto::from).toList();
    }

    @Transactional
    public CategoryDto create(CategoryRequest req) {
        String name = req.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw ApiException.conflict("DUPLICATE_CATEGORY", "A category with this name already exists.");
        }
        Category category = new Category(name, AuthService.blankToNull(req.description()));
        if (req.active() != null) {
            category.setActive(req.active());
        }
        return CategoryDto.from(categoryRepository.save(category));
    }

    /** Categories are never deleted (old complaints point to them); set active=false to hide one. */
    @Transactional
    public CategoryDto update(Long id, CategoryRequest req) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> ApiException.notFound("Category"));
        String name = req.name().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw ApiException.conflict("DUPLICATE_CATEGORY", "A category with this name already exists.");
        }
        category.setName(name);
        category.setDescription(AuthService.blankToNull(req.description()));
        if (req.active() != null) {
            category.setActive(req.active());
        }
        return CategoryDto.from(category);
    }
}
