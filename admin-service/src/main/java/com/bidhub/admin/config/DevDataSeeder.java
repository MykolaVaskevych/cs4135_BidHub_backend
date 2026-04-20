package com.bidhub.admin.config;

import com.bidhub.admin.domain.model.Category;
import com.bidhub.admin.domain.repository.CategoryRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private static final List<String> CATEGORIES = List.of(
            "Electronics", "Vehicles", "Furniture", "Clothing",
            "Books", "Sports & Outdoors", "Home & Garden", "Other");

    private final CategoryRepository categoryRepository;

    public DevDataSeeder(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        Set<String> existing = categoryRepository.findAll().stream()
                .map(Category::getName)
                .collect(Collectors.toSet());

        List<Category> toSeed = CATEGORIES.stream()
                .filter(name -> !existing.contains(name))
                .map(name -> Category.create(name, null, null))
                .toList();

        if (toSeed.isEmpty()) {
            log.info("Dev categories already present");
            return;
        }

        categoryRepository.saveAll(toSeed);
        log.info("Seeded {} default categories: {}", toSeed.size(),
                toSeed.stream().map(Category::getName).collect(Collectors.joining(", ")));
    }
}
