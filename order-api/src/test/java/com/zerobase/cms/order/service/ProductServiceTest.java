package com.zerobase.cms.order.service;

import com.zerobase.cms.order.domain.model.Product;
import com.zerobase.cms.order.domain.model.ProductItem;
import com.zerobase.cms.order.domain.product.AddProductForm;
import com.zerobase.cms.order.domain.product.AddProductItemForm;
import com.zerobase.cms.order.domain.product.UpdateProductForm;
import com.zerobase.cms.order.domain.product.UpdateProductItemForm;
import com.zerobase.cms.order.domain.repository.ProductRepository;
import com.zerobase.cms.order.exception.CustomException;
import com.zerobase.cms.order.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("상품 추가 성공")
    void addProduct_Success() {
        //given
        Long sellerId = 1L;
        AddProductForm form = AddProductForm.builder()
                .name("Product A")
                .description("Description A")
                .items(Arrays.asList(
                        AddProductItemForm.builder()
                                .productId(1L)
                                .name("Item1")
                                .price(1000)
                                .count(10)
                                .build(),
                        AddProductItemForm.builder()
                                .productId(2L)
                                .name("Item2")
                                .price(2000)
                                .count(5)
                                .build()
                ))
                .build();

        //when
        Product result = productService.addProduct(sellerId, form);

        //then
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("Product A", result.getName());
        assertEquals("Description A", result.getDescription());

        // 상품 아이템 검증
        assertEquals(2, result.getProductItems().size());
        assertTrue(result.getProductItems().stream()
                .anyMatch(item -> item.getName().equals("Item1") && item.getPrice() == 1000 && item.getCount() == 10));
        assertTrue(result.getProductItems().stream()
                .anyMatch(item -> item.getName().equals("Item2") && item.getPrice() == 2000 && item.getCount() == 5));

        // 데이터베이스에 실제로 저장되었는지 검증
        Optional<Product> persistedProduct = productRepository.findById(result.getId());
        assertTrue(persistedProduct.isPresent());
        assertEquals("Product A", persistedProduct.get().getName());
    }

    @Test
    @DisplayName("상품 추가 성공 - 아이템 제외")
    void addProduct_NoItems_Success() {
        //given
        Long sellerId = 1L;
        AddProductForm form = AddProductForm.builder()
                .name("Product B")
                .description("Description B")
                .items(new ArrayList<>())
                .build();

        //when
        Product result = productService.addProduct(sellerId, form);

        //then
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("Product B", result.getName());
        assertEquals("Description B", result.getDescription());
        assertTrue(result.getProductItems().isEmpty());
    }

    @Test
    @DisplayName("상품 추가 성공 - 여러 상품 연속 추가 성공")
    void addMultipleProducts_Success() {
        //given
        Long sellerId = 1L;

        AddProductForm form1 = AddProductForm.builder()
                .name("Product A")
                .description("Description A")
                .items(Arrays.asList(
                        AddProductItemForm.builder()
                                .productId(1L)
                                .name("Item1")
                                .price(1000)
                                .count(10)
                                .build()
                ))
                .build();

        AddProductForm form2 = AddProductForm.builder()
                .name("Product B")
                .description("Description B")
                .items(Arrays.asList(
                        AddProductItemForm.builder()
                                .productId(2L)
                                .name("Item2")
                                .price(1000)
                                .count(10)
                                .build()
                ))
                .build();

        //when
        Product result1 = productService.addProduct(sellerId, form1);
        Product result2 = productService.addProduct(sellerId, form2);

        //then
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result1.getId());
        assertNotNull(result2.getId());
        assertEquals("Product A", result1.getName());
        assertEquals("Product B", result2.getName());

        // 데이터 베이스 검증
        Optional<Product> persistedProduct1 =
                productRepository.findById(result1.getId());
        assertTrue(persistedProduct1.isPresent());
        assertEquals("Product A", persistedProduct1.get().getName());

        Optional<Product> persistedProduct2 =
                productRepository.findById(result2.getId());
        assertTrue(persistedProduct2.isPresent());
        assertEquals("Product B", persistedProduct2.get().getName());
    }

    @Test
    @DisplayName("상품 수정 성공")
    void updateProduct_Success() {
        //given
        Long sellerId = 1L;

        AddProductForm addForm = AddProductForm.builder()
                .name("Product A")
                .description("Description A")
                .items(Arrays.asList(
                        AddProductItemForm.builder()
                                .productId(1L)
                                .name("Item1")
                                .price(1000)
                                .count(10)
                                .build(),
                        AddProductItemForm.builder()
                                .productId(2L)
                                .name("Item2")
                                .price(2000)
                                .count(5)
                                .build()
                ))
                .build();

        Product savedProduct = productService.addProduct(sellerId, addForm);

        UpdateProductForm updateForm = UpdateProductForm.builder()
                .id(savedProduct.getId())
                .name("Update Product")
                .description("Update Description")
                .items(Arrays.asList(
                        UpdateProductItemForm.builder()
                                .id(savedProduct.getProductItems().get(0).getId())
                                .name("Update Item1")
                                .price(1500)
                                .count(8)
                                .build(),
                        UpdateProductItemForm.builder()
                                .id(savedProduct.getProductItems().get(1).getId())
                                .name("Update Item2")
                                .price(2500)
                                .count(3)
                                .build()
                ))
                .build();

        //when
        Product updateProduct =
                productService.updateProduct(sellerId, updateForm);

        //then
        assertNotNull(updateProduct);
        assertEquals(savedProduct.getId(), updateProduct.getId());
        assertEquals("Update Product", updateProduct.getName());
        assertEquals("Update Description", updateProduct.getDescription());

        assertEquals(2, updateProduct.getProductItems().size());
        assertTrue(updateProduct.getProductItems().stream()
                .anyMatch(item -> item.getName().equals("Update Item1") && item.getPrice() == 1500 && item.getCount() == 8));
        assertTrue(updateProduct.getProductItems().stream()
                .anyMatch(item -> item.getName().equals("Update Item2") && item.getPrice() == 2500 && item.getCount() == 3));
    }

    @Test
    @DisplayName("상품 수정 실패 - 존재하지 않는 상품 수정")
    void updateProduct_ProductNotFound_ThrowsException() {
        //given
        Long sellerId = 1L;
        Long nonExistentProductId = 999L;
        UpdateProductForm form = UpdateProductForm.builder()
                .id(nonExistentProductId)
                .description("Updated Product")
                .items(Arrays.asList(
                        UpdateProductItemForm.builder()
                                .id(1L)
                                .name("Updated Item1")
                                .price(1500)
                                .count(8)
                                .build()
                ))
                .build();

        //when&then
        CustomException e = assertThrows(CustomException.class,
                () -> productService.updateProduct(sellerId, form));

        assertEquals(ErrorCode.NOT_FOUND_PRODUCT, e.getErrorCode());

        // 데이터 베이스 확인
        Optional<Product> persistedProduct =
                productRepository.findBySellerIdAndId(sellerId, nonExistentProductId);
        assertFalse(persistedProduct.isPresent());
    }

    @Test
    @DisplayName("상품 수정 실패 - 존재하지 않는 아이템")
    void updateProduct_ItemNotFound_ThrowsException() {
        //given
        Long sellerId = 1L;

        AddProductForm addForm = AddProductForm.builder()
                .name("Product A")
                .description("Description A")
                .items(Arrays.asList(
                        AddProductItemForm.builder()
                                .productId(1L)
                                .name("Item1")
                                .price(1000)
                                .count(10)
                                .build()
                ))
                .build();

        Product savedProduct = productService.addProduct(sellerId, addForm);

        Long nonExistentProductId = 999L;
        UpdateProductForm updateForm = UpdateProductForm.builder()
                .id(savedProduct.getId())
                .name("Updated Product")
                .description("Updated Description")
                .items(Arrays.asList(
                        UpdateProductItemForm.builder()
                                .id(nonExistentProductId)
                                .name("Updated Item1")
                                .price(1500)
                                .count(8)
                                .build()
                ))
                .build();

        //when&then
        CustomException e = assertThrows(CustomException.class,
                () -> productService.updateProduct(sellerId, updateForm));

        assertEquals(ErrorCode.NOT_FOUND_ITEM, e.getErrorCode());
    }
}