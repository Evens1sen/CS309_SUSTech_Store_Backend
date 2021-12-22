package com.project.store.controller;


import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.project.store.entity.Orders;
import com.project.store.entity.Product;
import com.project.store.entity.User;
import com.project.store.enums.OrdersStatus;
import com.project.store.service.OrdersService;
import com.project.store.service.ProductService;
import com.project.store.service.UserService;
import com.project.store.vo.OrdersVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author ${author}
 * @since 2021-11-19
 */
@Api(tags = "OrdersController")
@RestController
@RequestMapping("/order")
public class OrdersController {

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    //FIXME: Add more restrictions on orders, and money
    @ApiOperation(value = "用户添加订单，订单状态为0")
    @PostMapping("/add/{productId}/{useAddress}")
    public Integer add(@PathVariable Integer productId, @PathVariable String useAddress) {
        User user = userService.getById(StpUtil.getLoginIdAsInt());
        Product product = productService.getById(productId);
        return ordersService.save(product, useAddress, user);
    }

    //FIXME: Add more restrictions on the users
    @ApiOperation(value = "用户支付订单，订单状态改变为1", notes = "仅对用户扣款")
    @PutMapping("/payById/{id}")
    public boolean payById(@PathVariable String id) {
        User user = userService.getById(StpUtil.getLoginIdAsInt());
        Orders orders = ordersService.getById(id);
        boolean result = userService.pay(user.getUid(), orders.getCost());
        if (!result) {
            return false;
        }
        orders.setStatus(OrdersStatus.PAYED);
        ordersService.saveOrUpdate(orders);

        User owner = userService.getById(orders.getOwnerId());
        userService.sendNotification(owner.getEmail());

        return true;
    }

    @ApiOperation(value = "卖家确认订单，订单状态改变为2")
    @PutMapping("/confirmById/{id}")
    public boolean confirmById(@PathVariable String id) {
        Orders orders = ordersService.getById(id);
        if (orders.getStatus() == OrdersStatus.PAYED) {
            UpdateWrapper wrapper = new UpdateWrapper();
            wrapper.set("status", 2);
            wrapper.eq("id", id);
            return ordersService.update(wrapper);
        }
        return false;
    }

    @ApiOperation(value = "用户确认收货，订单状态改变为3", notes = "对卖家转账")
    @PutMapping("close/{id}")
    public boolean closeById(@PathVariable Integer id) {
        Orders orders = ordersService.getById(id);
        if (orders.getStatus() == OrdersStatus.SHIPPED) {
            UpdateWrapper wrapper = new UpdateWrapper();
            wrapper.set("status", 3);
            wrapper.eq("id", id);
            ordersService.update(wrapper);
            wrapper = new UpdateWrapper();
            wrapper.set("balance", orders.getCost());
            wrapper.eq("uid", orders.getOwnerId());
            return userService.update(wrapper);
        }
        return false;
    }

    @ApiOperation(value = "彻底删除订单")
    @DeleteMapping("/deleteById/{id}")
    public boolean deleteById(@PathVariable Integer id) {
        return ordersService.removeById(id);
    }

    @ApiOperation(value = "获取当前登录用户所有购买订单")
    @GetMapping("/listBuy")
    public List<Orders> listBuy() {
        User user = userService.getById(StpUtil.getLoginIdAsInt());
        QueryWrapper wrapper = new QueryWrapper();
        wrapper.eq("buyer_id", user.getUid());
        return ordersService.list(wrapper);
    }

    @ApiOperation(value = "获取当前登录用户所有购买订单VO")
    @GetMapping("/listBuyVO")
    public List<OrdersVO> listBuyVO() {
        User user = userService.getById(StpUtil.getLoginIdAsInt());
        return ordersService.findAllOrdersVOByBuyerID(user.getUid());
    }

    @ApiOperation(value = "获取当前登录用户所有卖出订单")
    @GetMapping("/listSell")
    public List<Orders> listSell() {
        User user = userService.getById(StpUtil.getLoginIdAsInt());
        QueryWrapper wrapper = new QueryWrapper();
        wrapper.eq("owner_id", user.getUid());
        return ordersService.list(wrapper);
    }

    @ApiOperation(value = "获取当前登录用户所有卖出订单VO")
    @GetMapping("/listSellVO")
    public List<OrdersVO> listSellVO() {
        User user = userService.getById(StpUtil.getLoginIdAsInt());
        return ordersService.findAllOrdersVOByOwnerID(user.getUid());
    }
}

