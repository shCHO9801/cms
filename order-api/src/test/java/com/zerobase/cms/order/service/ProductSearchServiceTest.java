package com.zerobase.cms.order.service;

import com.zerobase.cms.order.domain.model.Product;
import com.zerobase.cms.order.domain.product.AddProductForm;
import com.zerobase.cms.order.domain.product.AddProductItemForm;
import com.zerobase.cms.order.domain.repository.ProductItemRepository;
import com.zerobase.cms.order.domain.repository.ProductRepository;
import com.zerobase.cms.order.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static com.zerobase.cms.order.exception.ErrorCode.NOT_FOUND_PRODUCT;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ProductSearchServiceTest {

    @Autowired
    private ProductSearchService productSearchService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductItemRepository productItemRepository;

    private Product createAndSaveProduct(
            Long sellerId, String name, String description, AddProductItemForm... items
    ) {
        AddProductForm addForm = AddProductForm.builder()
                .name(name)
                .description(description)
                .items(Arrays.asList(items))
                .build();
        return productService.addProduct(sellerId, addForm);
    }

    @Test
    @DisplayName("상품 이름으로 검색 성공 - 해당 이름을 포함하는 상품들이 반환된다.")
    void searchNyName_Success() {
        //given
        Long sellerId = 1L;
        createAndSaveProduct(
                sellerId,
                "TestName"
                ,"TestDescription",
                AddProductItemForm.builder()
                        .productId(1L)
                        .name("Item1")
                        .price(1000)
                        .count(10)
                        .build()
        );
        createAndSaveProduct(
                sellerId,
                "TestName2",
                "TestDescription2",
                AddProductItemForm.builder()
                        .productId(2L)
                        .name("Item2")
                        .price(2000)
                        .count(5)
                        .build()
        );
        createAndSaveProduct(
                sellerId,
                "NestName3",
                "TestDescription3",
                AddProductItemForm.builder()
                        .productId(3L)
                        .name("Item3")
                        .price(1500)
                        .count(7)
                        .build()
        );

        //when
        List<Product> result = productSearchService.searchByName("Test");

        //then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(product -> product.getName().equals("TestName")));
        assertTrue(result.stream().anyMatch(product -> product.getName().equals("TestName2")));
    }

    @Test
    @DisplayName("상품 이름으로 검색 실패 - 일치하는 상품이 없음")
    void searchByName_NoMatch() {
        //given
        Long sellerId = 1L;
        createAndSaveProduct(
                sellerId,
                "TestName",
                "TestDescription",
                AddProductItemForm.builder()
                        .productId(4L)
                        .name("Item4")
                        .price(1200)
                        .count(6)
                        .build()
        );

        //when
        List<Product> result = productSearchService.searchByName("ccc");

        //then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("상품 상세 조회 성공 - 존재하는 상품 ID로 조회시 해당 상품 반환")
    void getByProductId_Success() {
        //given
        Long sellerId = 1L;
        Product savedProduct = createAndSaveProduct(
                sellerId,
                "TestName",
                "TestDescription",
                AddProductItemForm.builder()
                        .productId(5L)
                        .name("Item5")
                        .price(3000)
                        .count(4)
                        .build(),
                AddProductItemForm.builder()
                        .productId(6L)
                        .name("Item6")
                        .price(2500)
                        .count(3)
                        .build()
        );

        //when
        Product retrievedProduct = productSearchService.getByProductId(savedProduct.getId());

        //then
        assertNotNull(retrievedProduct);
        assertEquals("TestName", retrievedProduct.getName());
        assertEquals("TestDescription", retrievedProduct.getDescription());
        assertEquals(2, retrievedProduct.getProductItems().size());
        assertTrue(retrievedProduct.getProductItems().stream()
                .anyMatch(item -> item.getName().equals("Item5") &&
                        item.getPrice() == 3000 &&
                        item.getCount() == 4));
        assertTrue(retrievedProduct.getProductItems().stream()
                .anyMatch(item -> item.getName().equals("Item6") &&
                        item.getPrice() == 2500 &&
                        item.getCount() == 3));
    }

    @Test
    @DisplayName("상품 상세 조회 실패 - 존재하지 않는 상품 ID로 조회 시 NOT_FOUND_PRODUCT CustomException 반환")
    void getByProductId_NotFound() {
        //given
        Long nonExistProductId = 999L;

        //when&then
        CustomException e = assertThrows(CustomException.class,
                () -> productSearchService.getByProductId(nonExistProductId));

        assertEquals(NOT_FOUND_PRODUCT, e.getErrorCode());
    }

    @Test
    @DisplayName("상품 ID 목록으로 조회 성공 - 존재하는 모든 상품 반환")
    void getListByProductIds_Success() {
        //given
        Long sellerId = 1L;
        Product product1 = createAndSaveProduct(
                sellerId,
                "TestName",
                "TestDescription",
                AddProductItemForm.builder()
                        .productId(7L)
                        .name("Item7")
                        .price(3500)
                        .count(2)
                        .build()
        );
        Product product2 = createAndSaveProduct(
                sellerId,
                "TestName2",
                "TestDescription2",
                AddProductItemForm.builder()
                        .productId(8L)
                        .name("Item8")
                        .price(4000)
                        .count(1)
                        .build()
        );

        List<Long> productIds = Arrays.asList(product1.getId(), product2.getId());

        //when
        List<Product> result = productSearchService.getListByProductIds(productIds);

        //then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(product -> product.getId().equals(product1.getId())));
        assertTrue(result.stream().anyMatch(product -> product.getId().equals(product2.getId())));
    }

    @Test
    @DisplayName("상품 ID 목록으로 조회 - 일부 ID가 존재하지 않음")
    void getListByProductIds_SomeNonExisting() {
        //given
        Long sellerId = 1L;
        Product product1 = createAndSaveProduct(
                sellerId,
                "TestName",
                "TestDescription",
                AddProductItemForm.builder()
                        .productId(9L)
                        .name("Item9")
                        .price(2200)
                        .count(5)
                        .build()
        );

        Long nonExistentProductId = 1000L;
        List<Long> productIds = Arrays.asList(product1.getId(), nonExistentProductId);

        //when
        List<Product> result = productSearchService.getListByProductIds(productIds);

        //then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("TestName", result.get(0).getName());
    }

    @Test
    @DisplayName("상품 ID 목록으로 조회 - 빈 리스트 반환")
    void getListByProductIds_EmptyList() {
        //given
        List<Long> productIds = Arrays.asList();

        //when
        List<Product> result = productSearchService.getListByProductIds(productIds);

        //then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}