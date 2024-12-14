package com.zerobase.cms.order.service;

import com.zerobase.cms.order.domain.model.Product;
import com.zerobase.cms.order.domain.model.ProductItem;
import com.zerobase.cms.order.domain.product.AddProductForm;
import com.zerobase.cms.order.domain.product.AddProductItemForm;
import com.zerobase.cms.order.domain.product.UpdateProductItemForm;
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

import static com.zerobase.cms.order.exception.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ProductItemServiceTest {

    @Autowired
    private ProductItemService productItemService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductItemRepository productItemRepository;

    private Product creteAndSaveProduct(
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
    @DisplayName("상품 아이템 추가 성공")
    void addProductItem_Success() {
        //given
        Long sellerId = 1L;
        Product existingProduct = creteAndSaveProduct(
                sellerId,
                "Product",
                "Description",
                AddProductItemForm.builder()
                        .productId(1L)
                        .name("Item")
                        .price(1000)
                        .count(10)
                        .build()
        );

        AddProductItemForm newItemForm = AddProductItemForm.builder()
                .productId(existingProduct.getId())
                .name("Item2")
                .price(2000)
                .count(5)
                .build();

        //when
        Product updateProduct = productItemService.addProductItem(sellerId, newItemForm);

        //then
        assertNotNull(updateProduct);
        assertEquals(2, updateProduct.getProductItems().size());
        assertTrue(updateProduct.getProductItems().stream()
                .anyMatch(item -> item.getName().equals("Item2")
                                && item.getPrice() == 2000
                                && item.getCount() == 5));
    }

    @Test
    @DisplayName("상품 아이템 추가 실패 - 존재하지 않는 상품에 아이템 추가")
    void addProductItem_ProductNotFound_ThrowsException() {
        //given
        Long sellerId = 1L;
        Long nonExistentProductId = 999L;
        AddProductItemForm itemForm = AddProductItemForm.builder()
                .productId(nonExistentProductId)
                .name("Item1")
                .price(1000)
                .count(10)
                .build();

        //when&then
        CustomException e = assertThrows(CustomException.class, () ->
            productItemService.addProductItem(sellerId, itemForm)
        );

        assertEquals(NOT_FOUND_PRODUCT, e.getErrorCode());
    }

    @Test
    @DisplayName("상품 아이템 추가 실패 - 동일한 이름의 아이템 추가")
    void addProductItem_SameItName_ThrowsException() {
        //given
        Long sellerId = 1L;
        Product existingProduct = creteAndSaveProduct(
                sellerId,
                "Product A",
                "Description A",
                AddProductItemForm.builder()
                        .productId(1L)
                        .name("Item1")
                        .price(1000)
                        .count(10)
                        .build()
        );

        AddProductItemForm duplicateItemForm = AddProductItemForm.builder()
                .productId(existingProduct.getId())
                .name("Item1") // 중복된 이름
                .price(1500)
                .count(5)
                .build();

        //when&then
        CustomException e = assertThrows(CustomException.class, () -> {
            productItemService.addProductItem(sellerId, duplicateItemForm);
        });

        assertEquals(SAME_ITEM_NAME, e.getErrorCode());
    }

    @Test
    @DisplayName("상품 아이템 수정 성공")
    void updateProductItem_Success() {
        //given
        Long sellerId = 1L;
        Product existingProduct = creteAndSaveProduct(
                sellerId,
                "Product A",
                "Description A",
                AddProductItemForm.builder()
                        .productId(1L)
                        .name("Item1")
                        .price(1000)
                        .count(10)
                        .build()
        );

        ProductItem existingItem = existingProduct.getProductItems().get(0);

        UpdateProductItemForm updateForm = UpdateProductItemForm.builder()
                .id(existingItem.getId())
                .name("Updated Item1")
                .price(1500)
                .count(8)
                .build();

        //when
        ProductItem updatedItem = productItemService
                .updateProductItem(sellerId, updateForm);

        //then
        assertNotNull(updatedItem);
        assertEquals("Updated Item1", updatedItem.getName());
        assertEquals(1500, updatedItem.getPrice());
        assertEquals(8, updatedItem.getCount());
    }

    @Test
    @DisplayName("상품 아이템 수정 실패 - 존재하지 않는 아이템 수정")
    void updateProductItem_ItemNotFound_ThrowsException() {
        //given
        Long sellerId = 1L;
        Long nonExistentItemId = 999L;
        UpdateProductItemForm updateForm = UpdateProductItemForm.builder()
                .id(nonExistentItemId)
                .name("Updated Item1")
                .price(1500)
                .count(8)
                .build();

        //when&then
        CustomException e = assertThrows(CustomException.class, () -> {
            productItemService.updateProductItem(sellerId, updateForm);
        });

        assertEquals(NOT_FOUND_ITEM, e.getErrorCode());
    }

    @Test
    @DisplayName("상품 아이템 수정 실패 - 다른 판매자의 아이템 수정")
    void updateProductItem_OtherSellerItem_ThrowsException() {
        //given
        Long sellerId = 1L;
        Long otherSellerId = 2L;

        // 판매자 A가 상품과 아이템을 추가
        Product productA = creteAndSaveProduct(
                sellerId,
                "Product A",
                "Description A",
                AddProductItemForm.builder()
                        .productId(1L)
                        .name("Item1")
                        .price(1000)
                        .count(10)
                        .build()
        );
        ProductItem itemA = productA.getProductItems().get(0);

        // 판매자 B가 같은 아이템을 수정하려고 시도
        UpdateProductItemForm updateForm = UpdateProductItemForm.builder()
                .id(itemA.getId())
                .name("Hacked Item")
                .price(999)
                .count(1)
                .build();

        //when&then
        CustomException e = assertThrows(CustomException.class, () -> {
            productItemService.updateProductItem(otherSellerId, updateForm);
        });

        assertEquals(NOT_FOUND_ITEM, e.getErrorCode());
    }
}