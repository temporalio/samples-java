package io.temporal.samples.springboot.camel;

import javax.persistence.*;

@Entity
@Table(name = "officeorder")
public class OfficeOrder {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(nullable = false)
  private String number;

  @Column(nullable = false)
  private String desc;

  @Column(nullable = false)
  private String date;

  @Column(nullable = false)
  private double price;

  public OfficeOrder() {}

  public OfficeOrder(Integer id, String number, String desc, String date, double price) {
    this.id = id;
    this.number = number;
    this.desc = desc;
    this.date = date;
    this.price = price;
  }

  public String getDesc() {
    return desc;
  }

  public void setDesc(String desc) {
    this.desc = desc;
  }

  public String getNumber() {
    return number;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public void setNumber(String number) {
    this.number = number;
  }

  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public double getPrice() {
    return price;
  }

  public void setPrice(double price) {
    this.price = price;
  }
}
