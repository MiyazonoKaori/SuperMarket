package com.supermarket.cart.service;

import com.supermarket.cart.dao.CartDao;
import com.supermarket.cart.exception.MsgException;
import com.supermarket.common.domain.Cart;
import com.supermarket.common.domain.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private CartDao cartDao = null;

    @Autowired
    private RestTemplate restTemplate = null;

    @Override
    public List<Cart> queryCart(String userId) {
        return this.cartDao.selectCarts(null, userId, null, null, null, null, null);
    }

    @Transactional(rollbackFor = MsgException.class)
    @Override
    public void addCart(Cart cart) {
        String productId = cart.getProductId();
        Product product = this.restTemplate.getForObject(
                "http://product/manage/item/" + productId,
                Product.class
        );
        if (product == null)
            throw new MsgException("商品不存在");
        // 暂存num，为了调用mybatis的sql模板
        Integer num = cart.getNum();
        cart.setNum(null);
        List<Cart> carts = this.cartDao.selectCarts(cart);
        if (carts == null || carts.size() == 0) {
            // TODO 购物车中还没有这个商品，执行 insert into
            cart.setProductImage(product.getProductImgurl());
            cart.setProductName(product.getProductName());
            cart.setProductPrice(product.getProductPrice());
            cart.setNum(num);
            this.cartDao.insert(cart);
        } else {
            // TODO 购物车中已经有了这个商品，执行 update
            cart.setNum(num);
            this.cartDao.updateCart(cart);
        }
        if (cart.getNum() > product.getProductNum())
            throw new MsgException(String.format("购买数量不能大于库存%d", product.getProductNum()));
    }

    @Transactional(rollbackFor = MsgException.class)
    @Override
    public void update(Cart cart) {
        this.cartDao.update(cart);
        String productId = cart.getProductId();
        Product product = this.restTemplate.getForObject(
                "http://product/manage/item/" + productId,
                Product.class
        );
        if (product == null)
            throw new MsgException("商品不存在");
        if (cart.getNum() > product.getProductNum())
            throw new MsgException(String.format("购买数量不能大于库存%d", product.getProductNum()));
    }

    @Override
    public void delete(Cart cart) {
        this.cartDao.delete(cart);
    }
}
