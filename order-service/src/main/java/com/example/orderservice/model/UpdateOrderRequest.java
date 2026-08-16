package com.example.orderservice.model;

public class UpdateOrderRequest {

    private String description;
    private Double amount;
    private String status;

    public UpdateOrderRequest() {
    }

    public UpdateOrderRequest(String description, Double amount, String status) {
        this.description = description;
        this.amount = amount;
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}


