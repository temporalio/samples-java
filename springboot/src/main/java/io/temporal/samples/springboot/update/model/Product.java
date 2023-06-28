/*
 *  Copyright (c) 2020 Temporal Technologies, Inc. All Rights Reserved
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package io.temporal.samples.springboot.update.model;

import javax.persistence.*;

@Entity
public class Product {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(nullable = false)
  public String name;

  @Column(nullable = false)
  public String code;

  @Column(nullable = false)
  public String description;

  @Column(nullable = false)
  public int price = 0;

  @Column(nullable = false)
  private int stock = 20;

  public Product() {}

  public Product(Integer id, String name, String code, String description, int price, int stock) {
    this.id = id;
    this.name = name;
    this.code = code;
    this.description = description;
    this.price = price;
    this.stock = stock;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public int getPrice() {
    return price;
  }

  public void setPrice(int price) {
    this.price = price;
  }

  public int getStock() {
    return stock;
  }

  public void setStock(int stock) {
    this.stock = stock;
  }

  public boolean removeStock() {
    if (this.stock > 0) {
      this.stock--;
      return true;
    } else {
      return false;
    }
  }

  public boolean removeStock(int value) {
    if (this.stock > 0) {
      this.stock -= value;
      return true;
    } else {
      return false;
    }
  }

  public void addStock() {
    this.stock++;
  }

  public void addStock(int value) {
    this.stock += value;
  }
}
