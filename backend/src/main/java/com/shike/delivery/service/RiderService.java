package com.shike.delivery.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shike.delivery.common.ApiException;
import com.shike.delivery.domain.DeliveryTask;
import com.shike.delivery.domain.DomainEnums.DeliveryStatus;
import com.shike.delivery.domain.DomainEnums.OrderStatus;
import com.shike.delivery.domain.DomainEnums.PaymentStatus;
import com.shike.delivery.domain.FoodOrder;
import com.shike.delivery.mapper.DeliveryTaskMapper;
import com.shike.delivery.mapper.FoodOrderMapper;
import com.shike.delivery.notification.NotificationWebSocketHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RiderService {

    private final DeliveryTaskMapper deliveryTaskMapper;
    private final FoodOrderMapper orderMapper;
    private final OrderService orderService;
    private final NotificationWebSocketHandler notificationHandler;

    public RiderService(
        DeliveryTaskMapper deliveryTaskMapper,
        FoodOrderMapper orderMapper,
        OrderService orderService,
        NotificationWebSocketHandler notificationHandler
    ) {
        this.deliveryTaskMapper = deliveryTaskMapper;
        this.orderMapper = orderMapper;
        this.orderService = orderService;
        this.notificationHandler = notificationHandler;
    }

    public Map<String, Object> dashboard(Long riderId) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        List<DeliveryTask> tasks = deliveryTaskMapper.selectList(
            Wrappers.<DeliveryTask>lambdaQuery()
                .eq(DeliveryTask::getRiderId, riderId)
                .ge(DeliveryTask::getAssignedAt, start)
                .orderByAsc(DeliveryTask::getAssignedAt)
        );
        long active = tasks.stream()
            .filter(task -> task.getStatus() == DeliveryStatus.ASSIGNED
                || task.getStatus() == DeliveryStatus.PICKED_UP)
            .count();
        long delivered = tasks.stream()
            .filter(task -> task.getStatus() == DeliveryStatus.DELIVERED)
            .count();
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("activeTasks", active);
        view.put("deliveredTasks", delivered);
        view.put("totalTasks", tasks.size());
        return view;
    }

    public List<Map<String, Object>> tasks(Long riderId, DeliveryStatus status) {
        var query = Wrappers.<DeliveryTask>lambdaQuery()
            .eq(DeliveryTask::getRiderId, riderId)
            .orderByDesc(DeliveryTask::getAssignedAt);
        if (status != null) {
            query.eq(DeliveryTask::getStatus, status);
        }
        return deliveryTaskMapper.selectList(query).stream()
            .map(task -> {
                FoodOrder order = orderService.requireOrder(task.getOrderId());
                Map<String, Object> view = new LinkedHashMap<>(orderService.orderView(order));
                view.put("taskId", task.getId());
                view.put("taskStatus", task.getStatus().name());
                view.put("assignedAt", task.getAssignedAt());
                return view;
            })
            .toList();
    }

    @Transactional
    public Map<String, Object> pickup(Long riderId, Long taskId) {
        DeliveryTask task = requireTask(riderId, taskId);
        if (task.getStatus() != DeliveryStatus.ASSIGNED) {
            throw ApiException.conflict("当前任务不能确认取餐");
        }
        FoodOrder order = orderService.requireOrder(task.getOrderId());
        if (order.getStatus() != OrderStatus.WAITING_PICKUP) {
            throw ApiException.conflict("订单尚未出餐或状态已变化");
        }
        orderService.transition(
            order,
            OrderStatus.DELIVERING,
            PaymentStatus.PAID,
            "骑手已取餐，开始配送",
            "RIDER",
            riderId
        );
        LocalDateTime now = LocalDateTime.now();
        order.setPickedUpAt(now);
        order.setUpdatedAt(now);
        orderMapper.updateById(order);
        task.setStatus(DeliveryStatus.PICKED_UP);
        task.setPickedUpAt(now);
        deliveryTaskMapper.updateById(task);
        notificationHandler.notifyOrderUpdate(order, "骑手已取餐，正在配送");
        Map<String, Object> view = new LinkedHashMap<>(orderService.orderView(order));
        view.put("taskId", task.getId());
        view.put("taskStatus", task.getStatus().name());
        return view;
    }

    @Transactional
    public Map<String, Object> deliver(Long riderId, Long taskId) {
        DeliveryTask task = requireTask(riderId, taskId);
        if (task.getStatus() != DeliveryStatus.PICKED_UP) {
            throw ApiException.conflict("当前任务不能确认送达");
        }
        FoodOrder order = orderService.requireOrder(task.getOrderId());
        if (order.getStatus() != OrderStatus.DELIVERING) {
            throw ApiException.conflict("订单状态已变化");
        }
        orderService.transition(
            order,
            OrderStatus.COMPLETED,
            PaymentStatus.PAID,
            "骑手已送达，订单完成",
            "RIDER",
            riderId
        );
        LocalDateTime now = LocalDateTime.now();
        order.setCompletedAt(now);
        order.setUpdatedAt(now);
        orderMapper.updateById(order);
        task.setStatus(DeliveryStatus.DELIVERED);
        task.setDeliveredAt(now);
        deliveryTaskMapper.updateById(task);
        notificationHandler.notifyOrderUpdate(order, "订单已送达");
        Map<String, Object> view = new LinkedHashMap<>(orderService.orderView(order));
        view.put("taskId", task.getId());
        view.put("taskStatus", task.getStatus().name());
        return view;
    }

    private DeliveryTask requireTask(Long riderId, Long taskId) {
        DeliveryTask task = deliveryTaskMapper.selectById(taskId);
        if (task == null || !riderId.equals(task.getRiderId())) {
            throw ApiException.notFound("配送任务不存在");
        }
        return task;
    }
}
