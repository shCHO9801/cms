package com.zerobase.cms.order.service;

import com.zerobase.cms.order.client.RedisClient;
import com.zerobase.cms.order.domain.product.AddProductCartForm;
import com.zerobase.cms.order.domain.redis.Cart;
import com.zerobase.cms.order.exception.CustomException;
import com.zerobase.cms.order.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private RedisClient redisClient;

    @InjectMocks
    private CartService cartService;

    private static final Long CUSTOMER_ID = 1L;
    private static final Long SELLER_ID = 1L;

    private Cart existingCart;
    private AddProductCartForm addProductCartForm;

    @BeforeEach
    void setUp() {
        // 기존 장바구니 설정
        existingCart = createCart(
                CUSTOMER_ID,
                createProduct(
                        1L,
                        SELLER_ID,
                        "Product A",
                        "Description A",
                        createProductItem(1L, "Item1", 2, 1000)
                )
        );
    }

    /**
     * 헬퍼 메서드: Cart 객체 생성
     */
    private Cart createCart(Long customerId, Cart.Product... products) {
        Cart cart = new Cart(customerId);
        cart.setProducts(new ArrayList<>(Arrays.asList(products))); // 가변 리스트로 변경
        cart.setMessages(new ArrayList<>());
        return cart;
    }

    /**
     * 헬퍼 메서드: Cart.Product 객체 생성
     */
    private Cart.Product createProduct(Long id, Long sellerId, String name, String description, Cart.ProductItem... items) {
        return Cart.Product.builder()
                .id(id)
                .sellerId(sellerId)
                .name(name)
                .description(description)
                .items(new ArrayList<>(Arrays.asList(items))) // 가변 리스트로 변경
                .build();
    }

    /**
     * 헬퍼 메서드: Cart.ProductItem 객체 생성
     */
    private Cart.ProductItem createProductItem(Long id, String name, Integer count, Integer price) {
        return Cart.ProductItem.builder()
                .id(id)
                .name(name)
                .count(count)
                .price(price)
                .build();
    }

    /**
     * 헬퍼 메서드: AddProductCartForm 객체 생성
     */
    private AddProductCartForm createAddProductCartForm(Long id, Long sellerId, String name, String description, AddProductCartForm.ProductItem... items) {
        return AddProductCartForm.builder()
                .id(id)
                .sellerId(sellerId)
                .name(name)
                .description(description)
                .items(new ArrayList<>(Arrays.asList(items))) // 가변 리스트로 변경
                .build();
    }

    /**
     * 헬퍼 메서드: AddProductCartForm.ProductItem 객체 생성
     */
    private AddProductCartForm.ProductItem createAddProductCartFormItem(Long id, String name, Integer count, Integer price) {
        return AddProductCartForm.ProductItem.builder()
                .id(id)
                .name(name)
                .count(count)
                .price(price)
                .build();
    }

    @Test
    @DisplayName("getCart - 장바구니가 존재하는 경우")
    void getCart_ExistingCart() {
        // Given
        when(redisClient.get(eq(CUSTOMER_ID), eq(Cart.class))).thenReturn(existingCart);

        // When
        Cart cart = cartService.getCart(CUSTOMER_ID);

        // Then
        assertNotNull(cart);
        assertEquals(CUSTOMER_ID, cart.getCustomerId());
        assertEquals(1, cart.getProducts().size());
        verify(redisClient, times(1)).get(eq(CUSTOMER_ID), eq(Cart.class));
    }

    @Test
    @DisplayName("putCart - 장바구니를 Redis에 저장")
    void putCart_Success() {
        // Given
        Cart newCart = new Cart(CUSTOMER_ID);
        newCart.setProducts(new ArrayList<>());
        newCart.setMessages(new ArrayList<>());

        // When
        Cart returnedCart = cartService.putCart(CUSTOMER_ID, newCart);

        // Then
        assertNotNull(returnedCart);
        assertEquals(CUSTOMER_ID, returnedCart.getCustomerId());
        verify(redisClient, times(1)).put(eq(CUSTOMER_ID), eq(newCart));
    }

    @Test
    @DisplayName("addCart - 빈 장바구니에 새로운 상품 추가")
    void addCart_EmptyCart_AddNewProduct() {
        // Given
        AddProductCartForm.ProductItem formItem =
                createAddProductCartFormItem(2L, "Item2", 1, 2000);

        addProductCartForm = createAddProductCartForm(
                2L,
                SELLER_ID,
                "Product B",
                "Description B",
                formItem
        );

        // 예상되는 장바구니 상태
        Cart.Product expectedProduct = createProduct(
                2L,
                SELLER_ID,
                "Product B",
                "Description B",
                createProductItem(2L, "Item2", 1, 2000)
        );

        Cart expectedCart = createCart(
                CUSTOMER_ID,
                expectedProduct
        );

        // Mocking
        when(redisClient.get(eq(CUSTOMER_ID), eq(Cart.class))).thenReturn(null);
        doNothing().when(redisClient).put(eq(CUSTOMER_ID), any(Cart.class));

        // When
        Cart cart = cartService.addCart(CUSTOMER_ID, addProductCartForm);

        // Then
        assertNotNull(cart);
        assertEquals(CUSTOMER_ID, cart.getCustomerId());
        assertEquals(1, cart.getProducts().size());

        Cart.Product addedProduct = cart.getProducts().get(0);
        assertEquals(2L, addedProduct.getId());
        assertEquals("Product B", addedProduct.getName());
        assertEquals(1, addedProduct.getItems().size());

        Cart.ProductItem addItem = addedProduct.getItems().get(0);
        assertEquals(2L, addItem.getId());
        assertEquals("Item2", addItem.getName());
        assertEquals(1, addItem.getCount());
        assertEquals(2000, addItem.getPrice());

        verify(redisClient, times(1)).get(eq(CUSTOMER_ID), eq(Cart.class));
        verify(redisClient, times(1)).put(eq(CUSTOMER_ID), any(Cart.class));
    }

    @Test
    @DisplayName("addCart - 기존 장바구니에 기존 상품에 새로운 아이템 추가")
    void addCart_ExistingCart_AddNewItemToExistingProduct() {
        // Given
        AddProductCartForm.ProductItem newFormItem = createAddProductCartFormItem(3L, "Item3", 1, 1500);

        addProductCartForm = createAddProductCartForm(
                1L, // 기존 상품 ID
                SELLER_ID,
                "Product A", // 기존 상품 이름
                "Description A",
                newFormItem
        );

        // 예상되는 장바구니 상태
        Cart.Product updatedProduct = createProduct(
                1L,
                SELLER_ID,
                "Product A",
                "Description A",
                createProductItem(1L, "Item1", 2, 1000),
                createProductItem(3L, "Item3", 1, 1500)
        );

        Cart updatedCart = createCart(
                CUSTOMER_ID,
                updatedProduct
        );

        // Mocking
        when(redisClient.get(eq(CUSTOMER_ID), eq(Cart.class))).thenReturn(existingCart);
        doNothing().when(redisClient).put(eq(CUSTOMER_ID), any(Cart.class));

        // When
        Cart cart = cartService.addCart(CUSTOMER_ID, addProductCartForm);

        // Then
        assertNotNull(cart);
        assertEquals(CUSTOMER_ID, cart.getCustomerId());
        assertEquals(1, cart.getProducts().size());

        Cart.Product product = cart.getProducts().get(0);
        assertEquals(1L, product.getId());
        assertEquals("Product A", product.getName());
        assertEquals(2, product.getItems().size());

        // 기존 아이템 검증
        Cart.ProductItem existingItem = product.getItems().stream()
                .filter(item -> item.getId().equals(1L))
                .findFirst()
                .orElse(null);
        assertNotNull(existingItem);
        assertEquals("Item1", existingItem.getName());
        assertEquals(2, existingItem.getCount());
        assertEquals(1000, existingItem.getPrice());

        // 새로운 아이템 검증
        Cart.ProductItem newItem = product.getItems().stream()
                .filter(item -> item.getId().equals(3L))
                .findFirst()
                .orElse(null);
        assertNotNull(newItem);
        assertEquals("Item3", newItem.getName());
        assertEquals(1, newItem.getCount());
        assertEquals(1500, newItem.getPrice());

        verify(redisClient, times(1)).get(eq(CUSTOMER_ID), eq(Cart.class));
        verify(redisClient, times(1)).put(eq(CUSTOMER_ID), any(Cart.class));
    }

    @Test
    @DisplayName("addCart - 기존 상품에 기존 아이템 수량 증가")
    void addCart_ExistingCart_AddExistingItemCount() {
        // Given
        AddProductCartForm.ProductItem formItem = createAddProductCartFormItem(1L, "Item1", 3, 1000); // 기존 수량 2 + 3 = 5

        addProductCartForm = createAddProductCartForm(
                1L,
                SELLER_ID,
                "Product A",
                "Description A",
                formItem
        );

        // 예상되는 장바구니 상태
        Cart.Product updatedProduct = createProduct(
                1L,
                SELLER_ID,
                "Product A",
                "Description A",
                createProductItem(1L, "Item1", 5, 1000) // 수량 5
        );

        Cart updatedCart = createCart(
                CUSTOMER_ID,
                updatedProduct
        );

        // Mocking
        when(redisClient.get(eq(CUSTOMER_ID), eq(Cart.class))).thenReturn(existingCart);
        doNothing().when(redisClient).put(eq(CUSTOMER_ID), any(Cart.class));

        // When
        Cart cart = cartService.addCart(CUSTOMER_ID, addProductCartForm);

        // Then
        assertNotNull(cart);
        assertEquals(CUSTOMER_ID, cart.getCustomerId());
        assertEquals(1, cart.getProducts().size());

        Cart.Product product = cart.getProducts().get(0);
        assertEquals(1L, product.getId());
        assertEquals("Product A", product.getName());
        assertEquals(1, product.getItems().size());

        Cart.ProductItem item = product.getItems().get(0);
        assertEquals(1L, item.getId());
        assertEquals("Item1", item.getName());
        assertEquals(5, item.getCount());
        assertEquals(1000, item.getPrice());

        verify(redisClient, times(1)).get(eq(CUSTOMER_ID), eq(Cart.class));
        verify(redisClient, times(1)).put(eq(CUSTOMER_ID), any(Cart.class));
    }
}
