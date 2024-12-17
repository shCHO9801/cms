package com.zerobase.cms.order.application;

import com.zerobase.cms.order.domain.model.Product;
import com.zerobase.cms.order.domain.product.AddProductCartForm;
import com.zerobase.cms.order.domain.product.AddProductForm;
import com.zerobase.cms.order.domain.product.AddProductItemForm;
import com.zerobase.cms.order.domain.redis.Cart;
import com.zerobase.cms.order.domain.repository.ProductRepository;
import com.zerobase.cms.order.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
class CartApplicationTest {

    @Autowired
    private CartApplication cartApplication;
    @Autowired
    private ProductService productService;
    @Autowired
    private ProductRepository productRepository;

    @Test
    void ADD_TEST_MODIFY() {

        Long customerId = 100L;

        cartApplication.clearCart(customerId);
        Product p = add_product();
        Product result = productRepository.findWithProductItemsById(p.getId()).get();

        assertNotNull(result);

        // 나머지 필드들에 대한 검증
        assertEquals(result.getName(), "나이키 에어포스");
        assertEquals(result.getDescription(), "신발");

        assertEquals(result.getProductItems().size(), 3);
        assertEquals(result.getProductItems().get(0).getName(), "나이키 에어포스0");
        assertEquals(result.getProductItems().get(0).getPrice(), 10000);
        assertEquals(result.getProductItems().get(0).getCount(), 10);

        Cart cart = cartApplication.addCart(customerId, makeAddForm(result));
        // 데이터가 잘 들어갔는지
        assertEquals(cart.getMessages().size(), 0);

        cart = cartApplication.getCart(customerId);
        assertEquals(cart.getMessages().size(), 1);
        assertEquals(1, cart.getProducts().size(), "장바구니에 추가된 상품 수가 1이어야 합니다.");
        Cart.Product addedProduct = cart.getProducts().get(0);
        assertEquals(p.getId(), addedProduct.getId(), "추가된 상품의 ID가 일치해야 합니다.");
        assertEquals(p.getName(), addedProduct.getName(), "추가된 상품의 이름이 일치해야 합니다.");
        assertEquals(p.getDescription(), addedProduct.getDescription(), "추가된 상품의 설명이 일치해야 합니다.");

        assertEquals(1, addedProduct.getItems().size(), "추가된 상품의 아이템 수가 1이어야 합니다.");
        Cart.ProductItem addedItem = addedProduct.getItems().get(0);
        assertEquals(p.getProductItems().get(0).getId(), addedItem.getId(), "추가된 아이템의 ID가 일치해야 합니다.");
        assertEquals(p.getProductItems().get(0).getName(), addedItem.getName(), "추가된 아이템의 이름이 일치해야 합니다.");
        assertEquals(5, addedItem.getCount(), "추가된 아이템의 수량이 일치해야 합니다.");
        assertEquals(10000, addedItem.getPrice(), "추가된 아이템의 가격이 일치해야 합니다.");
    }

    AddProductCartForm makeAddForm(Product p) {
        AddProductCartForm.ProductItem productItem =
                AddProductCartForm.ProductItem.builder()
                        .id(p.getProductItems().get(0).getId())
                        .name(p.getProductItems().get(0).getName())
                        .count(5)
                        .price(20000)
                        .build();

        return AddProductCartForm.builder()
                        .id(p.getId())
                        .sellerId(p.getSellerId())
                        .name(p.getName())
                        .description(p.getDescription())
                        .items(List.of(productItem))
                        .build();
    }

    Product add_product() {
        Long sellerId = 1L;
        AddProductForm form = makeProductForm("나이키 에어포스", "신발", 3);
        return productService.addProduct(sellerId, form);
    }

    private static AddProductForm makeProductForm(String name, String description, int itemCount) {
        List<AddProductItemForm> itemForms = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            itemForms.add(makeProductItemForm(null, name+i));
        }
        return AddProductForm.builder()
                .name(name)
                .description(description)
                .items(itemForms)
                .build();
    }

    private static AddProductItemForm makeProductItemForm(Long productId, String name) {
        return AddProductItemForm.builder()
                .productId(productId)
                .name(name)
                .price(10000)
                .count(10)
                .build();
    }
}